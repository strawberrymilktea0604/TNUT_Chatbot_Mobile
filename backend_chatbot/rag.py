import os
import torch
import time
import re
from langchain_community.vectorstores import Qdrant
from langchain_huggingface import HuggingFaceEmbeddings
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import PromptTemplate
from langchain_core.runnables import RunnablePassthrough, RunnableLambda
from langchain_community.document_loaders import DirectoryLoader, UnstructuredMarkdownLoader
from langchain.retrievers import ContextualCompressionRetriever
from langchain_community.cross_encoders import HuggingFaceCrossEncoder
from langchain.retrievers.document_compressors import CrossEncoderReranker
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, Filter, FieldCondition, MatchText
from tqdm import tqdm

def format_docs(docs):
    """Converts a list of documents into a single string."""
    return "\n\n".join(doc.page_content for doc in docs)

def extract_student_codes(text):
    """Extract potential student codes from text using enhanced regex patterns."""
    patterns = [
        r'\bSG\d{12}\b',         # Format like SG233510604001 (SG + 12 digits)
        r'\b[A-Z]{2}\d{10,12}\b', # Format like AB1234567890 (2 letters + 10-12 digits)
        r'\b[A-Z]{2}\d{6,8}\b',  # Format like AB123456 (shorter format)
        r'\b\d{8,12}\b',         # 8-12 digit numbers
        r'\bSV\d{6,12}\b',       # Format like SV123456 (SV + digits)
        r'\b[0-9]{2}[A-Z]{2}[0-9]{4,8}\b',  # Format like 20AB1234
        r'\b233510604001\b',     # Direct match for the specific ID
        r'\bSG233510604001\b'    # Full code with prefix
    ]
    
    student_codes = set()
    for pattern in patterns:
        matches = re.findall(pattern, text, re.IGNORECASE)
        for match in matches:
            # Normalize the match
            normalized = match.upper()
            student_codes.add(normalized)
            
            # Also add without prefix if it starts with letters
            if len(normalized) > 2 and normalized[:2].isalpha():
                student_codes.add(normalized[2:])
            
            # Add with SG prefix if it's just numbers and looks like student ID
            if normalized.isdigit() and len(normalized) >= 10:
                student_codes.add(f"SG{normalized}")
    
    return list(student_codes)

def is_student_query(query):
    """Check if query is about student information."""
    student_keywords = [
        'mã sinh viên', 'mã số sinh viên', 'student id', 'student code',
        'điểm', 'score', 'grade', 'transcript', 'bảng điểm', 'kết quả học tập'
    ]
    query_lower = query.lower()
    return any(keyword in query_lower for keyword in student_keywords) or bool(extract_student_codes(query))

class RAGSystem:
    def __init__(self, embedding_model_name, reranker_model_name, qdrant_url, qdrant_api_key, gemini_api_key):
        self.embedding_model_name = embedding_model_name
        self.reranker_model_name = reranker_model_name
        self.qdrant_url = qdrant_url
        self.qdrant_api_key = qdrant_api_key
        self.collection_name = "tnut_student_support_chatbot"
        self.vector_store = None
        self.qdrant_client = None
        self.debug_mode = True  # Enable debug mode
        
        self.embeddings = HuggingFaceEmbeddings(model_name=self.embedding_model_name)
        
        try:
            self.qdrant_client = QdrantClient(
                url=self.qdrant_url, 
                api_key=self.qdrant_api_key, 
                prefer_grpc=True, 
                timeout=60
            )
            if self.debug_mode:
                print("✓ Qdrant client created successfully with gRPC")
        except Exception as e:
            print(f"Error creating Qdrant client with gRPC, trying HTTP: {e}")
            self.qdrant_client = QdrantClient(
                url=self.qdrant_url, 
                api_key=self.qdrant_api_key, 
                prefer_grpc=False, 
                timeout=60
            )
            if self.debug_mode:
                print("✓ Qdrant client created successfully with HTTP")
        
        # Check if collection exists
        collection_exists = False
        try:
            collections = self.qdrant_client.get_collections()
            collection_names = [col.name for col in collections.collections]
            print(f"Available collections: {collection_names}")
            
            if self.collection_name in collection_names:
                collection_info = self.qdrant_client.get_collection(collection_name=self.collection_name)
                print(f"Collection '{self.collection_name}' already exists with {collection_info.points_count} points. Loading from existing collection.")
                try:
                    self.vector_store = Qdrant(
                        client=self.qdrant_client,
                        collection_name=self.collection_name,
                        embeddings=self.embeddings,
                    )
                    if self.debug_mode:
                        print("✓ Vector store loaded successfully")
                except Exception as e:
                    print(f"Error loading existing collection: {e}")
                    # Try creating a new connection without gRPC
                    try:
                        fallback_client = QdrantClient(
                            url=self.qdrant_url, 
                            api_key=self.qdrant_api_key, 
                            prefer_grpc=False, 
                            timeout=60
                        )
                        self.vector_store = Qdrant(
                            client=fallback_client,
                            collection_name=self.collection_name,
                            embeddings=self.embeddings,
                        )
                        if self.debug_mode:
                            print("✓ Vector store loaded with fallback client")
                    except Exception as e2:
                        print(f"Fallback also failed: {e2}")
                        collection_exists = False
                        
                if self.vector_store is not None:
                    collection_exists = True
            else:
                print(f"Collection '{self.collection_name}' not found in available collections.")
        except Exception as e:
            print(f"Error checking collection: {type(e).__name__}: {e}")
            print(f"Will create a new collection...")
        
        if not collection_exists:
            print(f"Collection '{self.collection_name}' not found. Creating a new one...")
        if not collection_exists:
            print(f"Creating new collection '{self.collection_name}'...")
            
            # Load documents in parallel with a progress bar
            loader = DirectoryLoader(
                '../dataset/',
                glob="**/*.md",
                loader_cls=UnstructuredMarkdownLoader,
                show_progress=True,
                use_multithreading=True
            )
            print("Loading documents...")
            documents = loader.load()

            # Split documents with a progress bar - Increase chunk size for better context
            text_splitter = RecursiveCharacterTextSplitter(
                chunk_size=1500,  # Increased from 800
                chunk_overlap=300,  # Reduced overlap ratio
                separators=["\n\n", "\n", ". ", "! ", "? ", " ", ""]  # Smart separators
            )
            print("Splitting documents...")
            docs = text_splitter.split_documents(tqdm(documents, desc="Splitting documents"))
            
            # Upload to Qdrant with a progress bar
            print("Uploading to Qdrant...")
            try:
                self.vector_store = Qdrant.from_documents(
                    docs,
                    self.embeddings,
                    url=self.qdrant_url,
                    api_key=self.qdrant_api_key,
                    collection_name=self.collection_name,
                    prefer_grpc=True,
                    force_recreate=False,
                )
            except Exception as e:
                print(f"Error creating collection with gRPC, trying HTTP: {e}")
                self.vector_store = Qdrant.from_documents(
                    docs,
                    self.embeddings,
                    url=self.qdrant_url,
                    api_key=self.qdrant_api_key,
                    collection_name=self.collection_name,
                    prefer_grpc=False,
                    force_recreate=False,
                )
        
        self.llm = ChatGoogleGenerativeAI(
            model="gemini-2.5-flash",
            google_api_key=gemini_api_key,
            convert_system_message_to_human=True,
            max_tokens=8192  # Increase token limit for longer responses
        )
        
        # Enhanced retriever with more documents for comprehensive search
        base_retriever = self.vector_store.as_retriever(search_kwargs={"k": 20})  # Increased from 10

        if self.reranker_model_name:
            reranker_model = HuggingFaceCrossEncoder(
                model_name=self.reranker_model_name,
                model_kwargs={"trust_remote_code": True}
            )
            compressor = CrossEncoderReranker(model=reranker_model, top_n=8)  # Increased from 3
            retriever = ContextualCompressionRetriever(
                base_compressor=compressor, base_retriever=base_retriever
            )
        else:
            retriever = base_retriever

        template = """<s>[INST] Bạn là một trợ lý AI hữu ích cho sinh viên trường Đại học Kỹ thuật Công nghiệp - TNUT. 

Hãy trả lời câu hỏi của sinh viên dựa trên bối cảnh được cung cấp bằng tiếng Việt. 

HƯỚNG DẪN TRÌNH BÀY:
- Nếu câu hỏi về điểm số hoặc thông tin sinh viên cụ thể, hãy tìm kiếm toàn bộ thông tin liên quan và trình bày đầy đủ
- Với câu hỏi phức tạp, hãy chia nhỏ thông tin và trình bày có cấu trúc rõ ràng  
- Nếu thông tin không đầy đủ, hãy nói rõ phần nào có thể trả lời và phần nào cần bổ sung
- Luôn luôn trả lời bằng tiếng Việt

NGỮ CẢNH:
{context}

CÂU HỎI: {question} [/INST]

TRẢ LỜI:"""
        prompt = PromptTemplate.from_template(template)

        self.qa_chain = (
            {
                "context": retriever | RunnableLambda(format_docs),
                "question": RunnablePassthrough(),
            }
            | prompt
            | self.llm
            | StrOutputParser()
        )

    def direct_qdrant_search(self, student_codes):
        """
        Direct search using Qdrant client for exact text matching
        """
        all_results = []
        
        if not self.qdrant_client:
            return all_results
            
        try:
            for code in student_codes:
                print(f"Searching for student code: {code}")
                
                # Method 1: Scroll through all points and filter by content
                scroll_result = self.qdrant_client.scroll(
                    collection_name=self.collection_name,
                    limit=1000,  # Get a large batch
                    with_payload=True,
                    with_vectors=False
                )
                
                points = scroll_result[0]
                print(f"Retrieved {len(points)} points for filtering")
                
                # Filter points that contain the student code
                for point in points:
                    if hasattr(point, 'payload') and 'page_content' in point.payload:
                        content = point.payload['page_content']
                        if code in content:
                            print(f"Found matching content for {code}")
                            # Create a document-like object
                            class SimpleDoc:
                                def __init__(self, content, metadata):
                                    self.page_content = content
                                    self.metadata = metadata
                            
                            doc = SimpleDoc(
                                content=content,
                                metadata=point.payload.get('metadata', {})
                            )
                            all_results.append(doc)
                
                # If we have more points, continue scrolling
                while scroll_result[1] is not None:
                    scroll_result = self.qdrant_client.scroll(
                        collection_name=self.collection_name,
                        offset=scroll_result[1],
                        limit=1000,
                        with_payload=True,
                        with_vectors=False
                    )
                    
                    points = scroll_result[0]
                    for point in points:
                        if hasattr(point, 'payload') and 'page_content' in point.payload:
                            content = point.payload['page_content']
                            if code in content:
                                print(f"Found matching content for {code}")
                                class SimpleDoc:
                                    def __init__(self, content, metadata):
                                        self.page_content = content
                                        self.metadata = metadata
                                
                                doc = SimpleDoc(
                                    content=content,
                                    metadata=point.payload.get('metadata', {})
                                )
                                all_results.append(doc)
                                
        except Exception as e:
            print(f"Error in direct Qdrant search: {e}")
            
        return all_results

    def hybrid_search(self, query_text, student_codes=None):
        """
        Enhanced search combining vector similarity and direct Qdrant search
        """
        all_docs = []
        
        # If student codes are detected, use direct search first
        if student_codes:
            print(f"Using direct Qdrant search for student codes: {student_codes}")
            direct_results = self.direct_qdrant_search(student_codes)
            all_docs.extend(direct_results)
            print(f"Direct search found {len(direct_results)} documents")
        
        # Vector search for semantic similarity
        try:
            vector_docs = self.vector_store.similarity_search(query_text, k=15)
            all_docs.extend(vector_docs)
            print(f"Vector search found {len(vector_docs)} documents")
        except Exception as e:
            print(f"Error in vector search: {e}")
        
        # Remove duplicates while preserving order
        seen_content = set()
        unique_docs = []
        for doc in all_docs:
            content_hash = hash(doc.page_content)
            if content_hash not in seen_content:
                seen_content.add(content_hash)
                unique_docs.append(doc)
        
        print(f"Total unique documents after deduplication: {len(unique_docs)}")
        return unique_docs

    def query(self, query_text):
        """
        Enhanced query method with improved hybrid search for student information
        """
        start_time = time.time()
        
        # Check if this is a student-related query
        student_codes = extract_student_codes(query_text)
        is_student_related = is_student_query(query_text)
        
        print(f"Query: {query_text}")
        print(f"Student codes detected: {student_codes}")
        print(f"Is student related: {is_student_related}")
        
        if is_student_related or student_codes:
            print(f"Processing student query with enhanced search...")
            
            # Use hybrid search for better coverage
            relevant_docs = self.hybrid_search(query_text, student_codes)
            print(f"Found {len(relevant_docs)} relevant documents")
            
            # Format documents for context - use more documents for student queries
            context = format_docs(relevant_docs[:20])  # Increased context size
            
            # Create enhanced prompt for student queries
            enhanced_template = """<s>[INST] Bạn là một trợ lý AI hữu ích cho sinh viên trường Đại học Kỹ thuật Công nghiệp - TNUT.

QUAN TRỌNG: Đây là truy vấn về thông tin sinh viên. Hãy tìm kiếm KỸ LƯỠNG trong toàn bộ ngữ cảnh được cung cấp.

Mã sinh viên được phát hiện: {student_codes}

HƯỚNG DẪN XỬ LÝ:
- Đọc cẩn thận toàn bộ ngữ cảnh để tìm thông tin về mã sinh viên được nhắc đến
- Nếu tìm thấy thông tin, hãy trình bày đầy đủ và chi tiết
- Liệt kê tất cả các môn học, điểm số, và thông tin liên quan
- Nếu KHÔNG tìm thấy thông tin cụ thể về mã sinh viên, hãy nói rõ ràng: "Tôi không tìm thấy thông tin về mã sinh viên [mã số] trong dữ liệu hiện có."
- Luôn luôn trả lời bằng tiếng Việt

NGỮ CẢNH:
{context}

CÂU HỎI: {question} [/INST]

TRẢ LỜI:"""
            
            enhanced_prompt = PromptTemplate.from_template(enhanced_template)
            
            # Create chain with enhanced context
            enhanced_chain = (
                {
                    "context": lambda x: context,
                    "question": lambda x: query_text,
                    "student_codes": lambda x: ", ".join(student_codes) if student_codes else "Không phát hiện"
                }
                | enhanced_prompt
                | self.llm
                | StrOutputParser()
            )
            
            raw_answer = enhanced_chain.invoke({})
            
            # Add debug information if no specific student info found
            if student_codes and ("không tìm thấy" in raw_answer.lower() or "không có thông tin" in raw_answer.lower()):
                print(f"Warning: Student codes {student_codes} not found in context")
                print(f"Context preview: {context[:500]}...")
                
        else:
            # Use standard RAG for general queries
            raw_answer = self.qa_chain.invoke(query_text)
        
        end_time = time.time()
        query_time = end_time - start_time
        
        # Clean the output
        cleaned_answer = raw_answer
        
        prefixes_to_remove = [
            "TRẢ LỜI:",
            "ANSWER:",
            "Dựa trên ngữ cảnh được cung cấp, đây là câu trả lời:",
            "Here is the answer based on the context provided:"
        ]
        
        for prefix in prefixes_to_remove:
            if cleaned_answer.lower().startswith(prefix.lower()):
                cleaned_answer = cleaned_answer[len(prefix):].strip()
        
        return cleaned_answer, query_time