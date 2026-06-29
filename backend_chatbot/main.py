import os
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from dotenv import load_dotenv
from contextlib import asynccontextmanager
from rag import RAGSystem

# Load environment variables
load_dotenv()

# Get environment variables
QDRANT_URL = os.getenv("QDRANT_URL")
QDRANT_API_KEY = os.getenv("QDRANT_API_KEY")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")

# Global variable to hold the RAG system instance
rag_system = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Asynchronous context manager to handle startup and shutdown events.
    """
    global rag_system
    print("Chatbot Server starting up...")
    
    # Initialize the RAG system in the background
    rag_system = RAGSystem(
        embedding_model_name="sentence-transformers/paraphrase-multilingual-mpnet-base-v2",
        reranker_model_name="amberoad/bert-multilingual-passage-reranking-msmarco",
        qdrant_url=QDRANT_URL,
        qdrant_api_key=QDRANT_API_KEY,
        gemini_api_key=GEMINI_API_KEY,
    )
    print("RAG system initialized.")
    yield
    # Clean up resources on shutdown
    print("Chatbot Server shutting down...")
    rag_system = None

# Initialize FastAPI app with the lifespan context manager
app = FastAPI(
    title="TNUT Chatbot Service",
    description="Chatbot service using RAG for TNUT students.",
    version="1.0.0",
    lifespan=lifespan,
)

# Add CORS middleware to allow requests from the frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost",
        "http://localhost:3000",
        "http://localhost:5173",  # Default Vite port
        "http://127.0.0.1:5173",
        "http://10.0.2.2:8001",  # For Android emulator
    ],
    allow_credentials=True,
    allow_methods=["*"],  # Allow all methods
    allow_headers=["*"],  # Allow all headers
)

class QueryRequest(BaseModel):
    question: str

class QueryResponse(BaseModel):
    answer: str
    query_time: float

class SearchRequest(BaseModel):
    query: str
    search_type: str = "hybrid"  # "vector", "exact", "hybrid"

class SearchResponse(BaseModel):
    results: list
    total_found: int
    query_time: float

@app.post("/query", response_model=QueryResponse)
async def query(request: QueryRequest):
    """
    Receives a question, gets an answer from the RAG system, and returns it.
    """
    try:
        if rag_system is None:
            raise HTTPException(
                status_code=503,
                detail="RAG system is not ready yet. Please try again in a moment."
            )
        answer, query_time = rag_system.query(request.question)
        return {"answer": answer, "query_time": query_time}
    except Exception as e:
        import traceback
        print("❌ Error in /query:", str(e))
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Query failed: {str(e)}")

@app.post("/search", response_model=SearchResponse)
async def search_documents(request: SearchRequest):
    """
    Search documents directly without generating answers - useful for debugging
    """
    if rag_system is None:
        raise HTTPException(status_code=503, detail="RAG system is not ready yet. Please try again in a moment.")
    
    import time
    start_time = time.time()
    
    if request.search_type == "hybrid":
        from rag import extract_student_codes
        student_codes = extract_student_codes(request.query)
        docs = rag_system.hybrid_search(request.query, student_codes)
    else:
        # Default vector search
        docs = rag_system.vector_store.similarity_search(request.query, k=20)
    
    end_time = time.time()
    
    # Format results
    results = []
    for i, doc in enumerate(docs[:10]):  # Limit to top 10 for response
        results.append({
            "rank": i + 1,
            "content": doc.page_content[:500] + "..." if len(doc.page_content) > 500 else doc.page_content,
            "metadata": getattr(doc, 'metadata', {})
        })
    
    return {
        "results": results,
        "total_found": len(docs),
        "query_time": end_time - start_time
    }

@app.get("/")
def read_root():
    return {"message": "Welcome to the TNUT Chatbot Service API"}

@app.get("/health")
def health_check():
    return {"status": "healthy", "service": "chatbot", "rag_ready": rag_system is not None}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)