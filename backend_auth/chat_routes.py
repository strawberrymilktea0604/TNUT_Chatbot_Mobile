from fastapi import APIRouter, HTTPException, Depends
from database import DatabaseService
from auth_routes import get_current_user
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
import httpx
import os

# Chatbot service URL
CHATBOT_SERVICE_URL = os.getenv("CHATBOT_SERVICE_URL", "http://localhost:5000")

router = APIRouter(prefix="/api/chat", tags=["chat"])
db_service = DatabaseService()

class ChatSessionCreate(BaseModel):
    title: str

class ChatMessage(BaseModel):
    content: str
    session_id: str

class ChatMessageSave(BaseModel):
    session_id: str
    user_message: str
    bot_response: str
    response_time: float

class ChatResponse(BaseModel):
    message: str
    session_id: str

@router.post("/sessions")
async def create_chat_session(session_data: ChatSessionCreate, current_user = Depends(get_current_user)):
    """Create new chat session"""
    try:
        session_id = await db_service.create_chat_session(
            current_user["user_id"],
            session_data.title
        )
        
        return {
            "message": "Chat session created successfully",
            "session_id": session_id,
            "title": session_data.title
        }
    except Exception as e:
            print(f"Error creating chat session: {e}")
            raise HTTPException(status_code=500, detail=str(e))

@router.get("/sessions")
async def get_chat_sessions(current_user = Depends(get_current_user)):
    """Get all chat sessions for user"""
    try:
        sessions = await db_service.get_user_chat_sessions(current_user["user_id"])
        return {
            "sessions": sessions
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail="Failed to get chat sessions")

@router.get("/sessions/{session_id}/messages")
async def get_session_messages(session_id: str, current_user = Depends(get_current_user)):
    """Get all messages from a chat session (only current session messages, not regenerated)"""
    try:
        messages = await db_service.get_session_messages_for_display(session_id, current_user["user_id"])
        return {
            "session_id": session_id,
            "messages": messages
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail="Failed to get session messages")

@router.post("/sessions/{session_id}/messages")
async def send_message(session_id: str, message_data: dict, current_user = Depends(get_current_user)):
    """Send message to chat session and get AI response"""
    try:
        user_message = message_data["content"]
        
        # Add user message to database
        await db_service.add_message_to_session(
            session_id,
            current_user["user_id"],
            "user",
            user_message
        )
        
        # Integrate with RAG system by calling the chatbot service
        ai_response = ""
        try:
            async with httpx.AsyncClient() as client:
                chatbot_request = {"question": user_message}
                response = await client.post(
                    f"{CHATBOT_SERVICE_URL}/query",
                    json=chatbot_request,
                    timeout=60.0
                )
                
                if response.status_code == 200:
                    result = response.json()
                    ai_response = result["answer"]
                else:
                    # Handle error from chatbot service
                    ai_response = "Xin lỗi, tôi đang gặp sự cố và không thể trả lời lúc này."
        except httpx.RequestError as e:
            # Handle network-related errors
            print(f"Error calling chatbot service: {e}")
            ai_response = "Xin lỗi, không thể kết nối đến dịch vụ AI. Vui lòng thử lại sau."

        # Add AI response to database
        await db_service.add_message_to_session(
            session_id,
            current_user["user_id"],
            "assistant",
            ai_response
        )
        
        return {
            "user_message": user_message,
            "ai_response": ai_response,
            "session_id": session_id
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to send message: {str(e)}")

@router.post("/sessions/{session_id}/regenerate")
async def regenerate_message(session_id: str, message_data: dict, current_user = Depends(get_current_user)):
    """Regenerate the last AI response for a given user message"""
    try:
        user_message = message_data["content"]
        
        # Mark the last AI response as regenerated
        await db_service.mark_last_ai_message_as_regenerated(session_id, current_user["user_id"])
        
        # Generate new AI response
        ai_response = ""
        try:
            async with httpx.AsyncClient() as client:
                chatbot_request = {"question": user_message}
                response = await client.post(
                    f"{CHATBOT_SERVICE_URL}/query",
                    json=chatbot_request,
                    timeout=60.0
                )
                
                if response.status_code == 200:
                    result = response.json()
                    ai_response = result["answer"]
                else:
                    ai_response = "Xin lỗi, tôi đang gặp sự cố và không thể trả lời lúc này."
        except httpx.RequestError as e:
            print(f"Error calling chatbot service: {e}")
            ai_response = "Xin lỗi, không thể kết nối đến dịch vụ AI. Vui lòng thử lại sau."

        # Add new AI response to database (not marked as regenerated)
        await db_service.add_message_to_session(
            session_id,
            current_user["user_id"],
            "assistant",
            ai_response,
            is_regenerated=False
        )
        
        return {
            "user_message": user_message,
            "ai_response": ai_response,
            "session_id": session_id,
            "regenerated": True
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to regenerate message: {str(e)}")

@router.post("/messages")
async def save_chat_message(message_data: ChatMessageSave, current_user = Depends(get_current_user)):
    """Save chat message from chatbot service"""
    try:
        # Add user message to database
        await db_service.add_message_to_session(
            message_data.session_id,
            current_user["user_id"],
            "user",
            message_data.user_message
        )
        
        # Add bot response to database
        await db_service.add_message_to_session(
            message_data.session_id,
            current_user["user_id"],
            "assistant",
            message_data.bot_response
        )
        
        return {"message": "Chat message saved successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail="Failed to save chat message")

@router.delete("/sessions/{session_id}")
async def delete_chat_session(session_id: str, current_user = Depends(get_current_user)):
    """Delete a chat session"""
    try:
        await db_service.delete_chat_session(session_id, current_user["user_id"])
        return {"message": "Chat session deleted successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail="Failed to delete chat session")

@router.put("/sessions/{session_id}")
async def update_session_title(session_id: str, update_data: dict, current_user = Depends(get_current_user)):
    """Update chat session title"""
    try:
        new_title = update_data["title"]
        await db_service.update_session_title(session_id, current_user["user_id"], new_title)
        return {"message": "Session title updated successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail="Failed to update session title")
