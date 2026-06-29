import motor.motor_asyncio
import os
from datetime import datetime
from typing import Optional, Dict, List
from bson import ObjectId
from pymongo import ASCENDING, TEXT

class DatabaseService:
    def __init__(self):
        self.db_url = os.getenv("MONGODB_URL", "mongodb://host.docker.internal:27017")
        self.db_name = os.getenv("DATABASE_NAME", "tnut_chatbot")
        self.client = None
        self.db = None
    
    async def get_database(self):
        """Get database connection"""
        if self.client is None:
            self.client = motor.motor_asyncio.AsyncIOMotorClient(
                self.db_url,
                serverSelectionTimeoutMS=5000,
                connectTimeoutMS=5000,
                socketTimeoutMS=5000
            )
            self.db = self.client[self.db_name]
        return self.db
    
    async def init_database(self):
        """Initialize database collections and indexes"""
        try:
            db = await self.get_database()
            
            # Test connection first
            await db.command("ping")
            print("MongoDB connection successful!")
            
            # Create collections if they don't exist
            collections = await db.list_collection_names()
            
            if "users" not in collections:
                await db.create_collection("users")
                print("Created 'users' collection")
            
            if "chat_sessions" not in collections:
                await db.create_collection("chat_sessions")
                print("Created 'chat_sessions' collection")
            
            if "chat_messages" not in collections:
                await db.create_collection("chat_messages")
                print("Created 'chat_messages' collection")
            
            # Create indexes for better performance
            try:
                await db.users.create_index([("email", ASCENDING)], unique=True)
                await db.users.create_index([("username", ASCENDING)], unique=True)
                await db.chat_sessions.create_index([("user_id", ASCENDING)])
                await db.chat_sessions.create_index([("updated_at", ASCENDING)])
                await db.chat_messages.create_index([("session_id", ASCENDING)])
                await db.chat_messages.create_index([("user_id", ASCENDING)])
                await db.chat_messages.create_index([("timestamp", ASCENDING)])
                print("Database indexes created successfully")
            except Exception as e:
                print(f"Note: Some indexes may already exist: {e}")
                
        except Exception as e:
            print(f"MongoDB connection error: {e}")
            print("Please make sure MongoDB is running on localhost:27017")
            raise
    
    async def create_chat_session(self, user_id: str, title: str) -> str:
        """Create new chat session"""
        db = await self.get_database()
        
        session_doc = {
            "user_id": user_id,
            "title": title,
            "created_at": datetime.utcnow(),
            "updated_at": datetime.utcnow(),
            "session_data": {"messages": []}
        }
        
        result = await db.chat_sessions.insert_one(session_doc)
        return str(result.inserted_id)
    
    async def get_user_chat_sessions(self, user_id: str) -> List[Dict]:
        """Get all chat sessions for a user"""
        db = await self.get_database()
        
        cursor = db.chat_sessions.find(
            {"user_id": user_id}
        ).sort("updated_at", -1)
        
        sessions = []
        async for session in cursor:
            session_id = str(session["_id"])
            
            # Count messages for this session (only non-regenerated)
            message_count = await db.chat_messages.count_documents({
                "session_id": session_id,
                "user_id": user_id,
                "is_regenerated": {"$ne": True}
            })
            
            # Get last message (only non-regenerated)
            last_message_cursor = db.chat_messages.find({
                "session_id": session_id,
                "user_id": user_id,
                "is_regenerated": {"$ne": True}
            }).sort("timestamp", -1).limit(1)
            
            last_message = ""
            async for msg in last_message_cursor:
                last_message = msg.get("content", "")
                break
            
            session_dict = {
                "id": session_id,
                "title": session.get("title", "New Chat"),
                "created_at": session.get("created_at", "").isoformat() if session.get("created_at") else "",
                "updated_at": session.get("updated_at", "").isoformat() if session.get("updated_at") else "",
                "messageCount": message_count,
                "lastMessage": last_message
            }
            sessions.append(session_dict)
        
        return sessions
    
    async def add_message_to_session(self, session_id: str, user_id: str, 
                                   message_type: str, content: str, metadata: Dict = None, 
                                   is_regenerated: bool = False):
        """Add message to chat session"""
        db = await self.get_database()
        
        if metadata is None:
            metadata = {}
        
        # Add message to chat_messages collection
        message_doc = {
            "session_id": session_id,
            "user_id": user_id,
            "message_type": message_type,
            "content": content,
            "timestamp": datetime.utcnow(),
            "metadata": metadata,
            "is_regenerated": is_regenerated,
            "session_message_id": None  # Will be set for session-specific tracking
        }
        
        # Generate session-specific message ID if not regenerated
        if not is_regenerated:
            # Count existing non-regenerated messages in this session
            message_count = await db.chat_messages.count_documents({
                "session_id": session_id,
                "user_id": user_id,
                "is_regenerated": False
            })
            message_doc["session_message_id"] = message_count + 1
        
        await db.chat_messages.insert_one(message_doc)
        
        # Update session's updated_at timestamp
        await db.chat_sessions.update_one(
            {"_id": ObjectId(session_id)},
            {"$set": {"updated_at": datetime.utcnow()}}
        )
    
    async def get_session_messages(self, session_id: str, user_id: str, include_regenerated: bool = False) -> List[Dict]:
        """Get messages from a chat session, optionally excluding regenerated messages"""
        db = await self.get_database()
        
        query = {
            "session_id": session_id,
            "user_id": user_id
        }
        
        # Exclude regenerated messages if requested
        if not include_regenerated:
            query["is_regenerated"] = {"$ne": True}
        
        cursor = db.chat_messages.find(query).sort("timestamp", 1)
        
        messages = []
        async for message in cursor:
            message["id"] = str(message["_id"])
            del message["_id"]
            messages.append(message)
        
        return messages
    
    async def get_session_messages_for_display(self, session_id: str, user_id: str) -> List[Dict]:
        """Get only current session messages (non-regenerated) for display"""
        return await self.get_session_messages(session_id, user_id, include_regenerated=False)
    
    async def mark_last_ai_message_as_regenerated(self, session_id: str, user_id: str):
        """Mark the last AI message in a session as regenerated (for regenerate functionality)"""
        db = await self.get_database()
        
        # Find the last AI message that is not already regenerated
        last_ai_message = await db.chat_messages.find_one({
            "session_id": session_id,
            "user_id": user_id,
            "message_type": "assistant",
            "is_regenerated": {"$ne": True}
        }, sort=[("timestamp", -1)])
        
        if last_ai_message:
            await db.chat_messages.update_one(
                {"_id": last_ai_message["_id"]},
                {"$set": {"is_regenerated": True}}
            )
    
    async def delete_chat_session(self, session_id: str, user_id: str):
        """Delete a chat session and all its messages (including regenerated ones)"""
        db = await self.get_database()
        
        # Delete all messages in the session (including regenerated)
        await db.chat_messages.delete_many({
            "session_id": session_id,
            "user_id": user_id
        })
        
        # Delete the session
        await db.chat_sessions.delete_one({
            "_id": ObjectId(session_id),
            "user_id": user_id
        })
    
    async def update_session_title(self, session_id: str, user_id: str, title: str):
        """Update chat session title"""
        db = await self.get_database()
        
        await db.chat_sessions.update_one(
            {"_id": ObjectId(session_id), "user_id": user_id},
            {"$set": {"title": title, "updated_at": datetime.utcnow()}}
        )
    
    async def get_user_profile(self, user_id: str) -> Optional[Dict]:
        """Get user profile information"""
        db = await self.get_database()
        
        user = await db.users.find_one({"_id": ObjectId(user_id)})
        if user:
            user["id"] = str(user["_id"])
            del user["_id"]
            # Remove sensitive data
            if "password_hash" in user:
                del user["password_hash"]
            return user
        return None

    async def get_user_by_email(self, email: str) -> Optional[Dict]:
        """Get user by email"""
        db = await self.get_database()
        
        user = await db.users.find_one({"email": email})
        if user:
            user["id"] = str(user["_id"])
            del user["_id"]
            return user
        return None

    async def get_user_by_id(self, user_id: str) -> Optional[Dict]:
        """Get user by ID"""
        db = await self.get_database()
        
        try:
            user = await db.users.find_one({"_id": ObjectId(user_id)})
            if user:
                user["id"] = str(user["_id"])
                del user["_id"]
                return user
        except Exception as e:
            print(f"Error finding user by ID {user_id}: {e}")
        return None


    async def set_user_verified(self, email: str):
        """Set user's verification status to True"""
        db = await self.get_database()
        await db.users.update_one(
            {"email": email},
            {"$set": {"is_verified": True, "updated_at": datetime.utcnow()}}
        )
    
    async def update_user_profile(self, user_id: str, profile_data: Dict):
        """Update user profile data"""
        db = await self.get_database()
        
        await db.users.update_one(
            {"_id": ObjectId(user_id)},
            {"$set": {
                "profile_data": profile_data,
                "updated_at": datetime.utcnow()
            }}
        )
    
    async def save_chat_message(self, message_data: Dict):
        """Save chat message to database"""
        db = await self.get_database()
        
        message_data["timestamp"] = datetime.utcnow()
        message_data["created_at"] = datetime.utcnow()
        
        result = await db.chat_messages.insert_one(message_data)
        return str(result.inserted_id)
