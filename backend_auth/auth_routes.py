from fastapi import APIRouter, HTTPException, Depends, Request
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from auth import AuthService, UserRegistration, UserLogin, EmailVerification, PasswordReset, PasswordChange, EmailChange, GoogleAuth, GoogleAuthManual
from database import DatabaseService
from google_auth import GoogleAuthService, GoogleTokenVerification
from pydantic import BaseModel
import jwt
import os
import httpx

router = APIRouter(prefix="/api/auth", tags=["authentication"])
security = HTTPBearer()
# Remove global instances to use dependency injection
# auth_service = AuthService()
# db_service = DatabaseService()
# google_auth_service = GoogleAuthService()

# Use singleton instances to maintain state
_auth_service = None
_db_service = None
_google_auth_service = None

def get_auth_service():
    global _auth_service
    if _auth_service is None:
        _auth_service = AuthService()
    return _auth_service

def get_db_service():
    global _db_service
    if _db_service is None:
        _db_service = DatabaseService()
    return _db_service

def get_google_auth_service():
    global _google_auth_service
    if _google_auth_service is None:
        _google_auth_service = GoogleAuthService()
    return _google_auth_service

# Chatbot service URL
CHATBOT_SERVICE_URL = os.getenv("CHATBOT_SERVICE_URL", "http://localhost:5000")

# Models for chatbot
class QueryRequest(BaseModel):
    question: str
    session_id: str = None

class QueryResponse(BaseModel):
    answer: str
    query_time: float

def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):
    """Get current user from JWT token"""
    try:
        token = credentials.credentials
        payload = jwt.decode(token, os.getenv("JWT_SECRET_KEY", "your-secret-key"), algorithms=["HS256"])
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token expired")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="Invalid token")

@router.get("/verify-token")
async def verify_token(current_user = Depends(get_current_user), db_service: DatabaseService = Depends(get_db_service), auth_service: AuthService = Depends(get_auth_service)):
    """Verify JWT token, get latest user info from DB, and return a new token."""
    try:
        print(f"Verifying token for user: {current_user['email']}")
        
        # First try to find user by email in token
        user = await db_service.get_user_by_email(current_user["email"])
        
        # If not found, try to find by user_id (in case email was changed)
        if not user and "user_id" in current_user:
            print(f"User not found by email, trying by user_id: {current_user['user_id']}")
            user = await db_service.get_user_by_id(current_user["user_id"])
        
        if not user:
            print(f"User not found in database: {current_user['email']}")
            raise HTTPException(status_code=404, detail="User not found")

        is_verified = user.get("is_verified", False)
        print(f"User found: {user['email']}, verified: {is_verified}")

        # Issue a new token with the latest user info from database
        new_token = auth_service.generate_jwt_token(
            user_id=user["id"],
            username=user["username"],
            email=user["email"],  # Use current email from database
            is_verified=is_verified
        )

        return {
            "access_token": new_token,
            "user_id": user["id"],
            "username": user["username"],
            "email": user["email"],
            "is_verified": is_verified
        }
    except HTTPException:
        raise  # Re-raise HTTP exceptions as they are
    except Exception as e:
        print(f"Error verifying token: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Failed to verify token: {str(e)}")

@router.post("/register")
async def register(user_data: UserRegistration, auth_service: AuthService = Depends(get_auth_service)):
    """Register new user, create account, and send verification email."""
    if user_data.password != user_data.confirm_password:
        raise HTTPException(status_code=400, detail="Passwords do not match")

    if not auth_service.validate_gmail(user_data.email):
        raise HTTPException(status_code=400, detail="Email must be a valid Gmail address")

    existing_user = await auth_service.check_user_exists(user_data.username, user_data.email)
    if existing_user:
        if existing_user['username'] == user_data.username:
            raise HTTPException(status_code=409, detail="Username already exists")
        if existing_user['email'] == user_data.email:
            raise HTTPException(status_code=409, detail="Email already exists")

    try:
        user_id = await auth_service.create_user(
            user_data.username,
            user_data.email,
            user_data.password
        )
        
        # User is created, now send verification email with a token link
        verification_token = auth_service.generate_verification_token(user_data.email, "registration")
        email_sent = await auth_service.send_verification_email(user_data.email, verification_token, "registration")

        if not email_sent:
            # Log this issue but don't fail the registration
            print(f"Warning: Failed to send verification email to {user_data.email}")

        # Generate JWT token for immediate login
        token = auth_service.generate_jwt_token(user_id, user_data.username, user_data.email)
        
        return {
            "message": "Registration successful. Please verify your email.",
            "access_token": token,
            "user_id": user_id,
            "username": user_data.username,
            "email": user_data.email
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to create user account: {str(e)}")

@router.get("/verify-email/{token}")
async def verify_email(token: str, auth_service: AuthService = Depends(get_auth_service), db_service: DatabaseService = Depends(get_db_service)):
    """Verify user's email using a token from a link."""
    email = auth_service.verify_verification_token(token, "registration")
    if not email:
        # The exception is already raised in the verify function
        return # Should not be reached

    try:
        await auth_service.verify_user_email(email)
        
        # Get user details to generate a new token
        user = await db_service.get_user_by_email(email)
        if not user:
            raise HTTPException(status_code=404, detail="User not found after verification.")
            
        # Generate a new token with updated verification status
        new_token = auth_service.generate_jwt_token(
            user_id=user["id"],
            username=user["username"],
            email=user["email"],
            is_verified=True
        )
        
        return {
            "message": "Email verified successfully. You can now close this tab.",
            "access_token": new_token,
            "user": {
                "id": user["id"],
                "username": user["username"],
                "email": user["email"],
                "is_verified": True
            }
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to verify email: {str(e)}")

class ResendVerificationRequest(BaseModel):
    email: str

@router.post("/resend-verification")
async def resend_verification_email(request: ResendVerificationRequest, auth_service: AuthService = Depends(get_auth_service), db_service: DatabaseService = Depends(get_db_service)):
    """Resend verification email for a given email address."""
    email = request.email
    
    user = await db_service.get_user_by_email(email)
    if not user:
        return {"message": "If an account with this email exists, a new verification email has been sent."}

    if user.get("is_verified"):
        raise HTTPException(status_code=400, detail="Email đã được xác thực. Không cần gửi lại mã xác nhận.")

    verification_token = auth_service.generate_verification_token(email, "registration")
    email_sent = await auth_service.send_verification_email(email, verification_token, "registration")
    
    if not email_sent:
        raise HTTPException(status_code=500, detail="Failed to send verification email")

    return {"message": "A new verification email has been sent."}

@router.post("/login")
async def login(login_data: UserLogin, auth_service: AuthService = Depends(get_auth_service)):
    """User login"""
    user = await auth_service.verify_user_credentials(login_data.username, login_data.password)
    
    if not user:
        raise HTTPException(status_code=401, detail="Invalid username or password")
    
    # Generate JWT token
    token = auth_service.generate_jwt_token(
        user["id"],
        user["username"],
        user["email"],
        user.get("is_verified", False)
    )
    
    return {
        "message": "Login successful",
        "access_token": token,
        "user_id": user["id"],
        "username": user["username"],
        "email": user["email"],
        "is_verified": user.get("is_verified", False)
    }

@router.post("/google-auth")
async def google_auth(token_data: GoogleTokenVerification, auth_service: AuthService = Depends(get_auth_service), db_service: DatabaseService = Depends(get_db_service), google_auth_service: GoogleAuthService = Depends(get_google_auth_service)):
    """Google OAuth authentication. Login if user exists, return 404 if user doesn't exist."""
    user_info = await google_auth_service.verify_google_token(token_data.id_token)
    
    if not user_info or not user_info.email_verified:
        raise HTTPException(status_code=400, detail="Invalid Google token or email not verified")

    if not google_auth_service.is_gmail_account(user_info.email):
        raise HTTPException(status_code=400, detail="Only Gmail accounts are supported")

    existing_user = await auth_service.check_user_exists(email=user_info.email)
    
    if existing_user:
        # Check if user is not verified yet
        is_verified = existing_user.get("is_verified", False)
        
        # If user is not verified, automatically verify them since they logged in via Google
        # This handles the case where a user registered manually but didn't verify their email,
        # then later logs in with Google OAuth using the same email
        if not is_verified:
            await db_service.set_user_verified(user_info.email)
            is_verified = True
            print(f"Auto-verified user {user_info.email} via Google OAuth login")
        
        # User exists, log them in
        token = auth_service.generate_jwt_token(
            existing_user["id"],
            existing_user["username"],
            existing_user["email"],
            is_verified
        )
        return {
            "message": "Google login successful",
            "access_token": token,
            "user_id": existing_user["id"],
            "username": existing_user["username"],
            "email": existing_user["email"],
            "is_verified": is_verified
        }
    else:
        # User doesn't exist, return 404 to trigger registration flow
        raise HTTPException(status_code=404, detail="User not found. Please register first.")


@router.post("/google-register")
async def google_register(user_data: GoogleAuthManual, auth_service: AuthService = Depends(get_auth_service), db_service: DatabaseService = Depends(get_db_service)):
    """Register new user with manual Google registration process."""
    if user_data.password != user_data.confirm_password:
        raise HTTPException(status_code=400, detail="Passwords do not match")

    if not auth_service.validate_gmail(user_data.email):
        raise HTTPException(status_code=400, detail="Email must be a valid Gmail address")

    # Check if user already exists
    existing_user = await auth_service.check_user_exists(user_data.username, user_data.email)
    if existing_user:
        if existing_user['username'] == user_data.username:
            raise HTTPException(status_code=409, detail="Username already exists")
        if existing_user['email'] == user_data.email:
            raise HTTPException(status_code=409, detail="Email already exists")

    try:
        # Create user account
        user_id = await auth_service.create_user(
            user_data.username,
            user_data.email,
            user_data.password
        )
        
        # Set up Google profile data
        profile_data = {
            "auth_provider": "google"
        }
        await db_service.update_user_profile(user_id, profile_data)
        
        # Since this is Google auth, mark user as verified immediately
        await db_service.set_user_verified(user_data.email)
        
        # Generate JWT token
        token = auth_service.generate_jwt_token(
            user_id,
            user_data.username,
            user_data.email,
            is_verified=True
        )
        
        return {
            "message": "Google registration successful",
            "access_token": token,
            "user_id": user_id,
            "username": user_data.username,
            "email": user_data.email,
            "is_verified": True
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to create user account: {str(e)}")


@router.post("/forgot-password")
async def forgot_password(reset_data: PasswordReset, auth_service: AuthService = Depends(get_auth_service)):
    """Request password reset"""
    # Check if user exists
    user = await auth_service.check_user_exists(email=reset_data.email)
    if not user:
        raise HTTPException(status_code=404, detail="Email không tồn tại trong hệ thống")
    
    # Check if user's email is verified
    if not user.get("is_verified", False):
        raise HTTPException(status_code=400, detail="Email chưa được xác thực. Vui lòng xác thực email trước khi đặt lại mật khẩu")
    
    # Generate new random 6-digit password
    new_password = auth_service.generate_random_password()
    
    # Update password in database
    await auth_service.update_password(reset_data.email, new_password)
    
    # Send new password via email
    email_sent = await auth_service.send_new_password_email(reset_data.email, new_password)
    if not email_sent:
        raise HTTPException(status_code=500, detail="Không thể gửi email. Vui lòng thử lại sau")
    
    return {
        "message": "Mật khẩu mới đã được gửi về email của bạn",
        "email": reset_data.email
    }

# @router.post("/verify-reset-code")
# async def verify_reset_code(verification_data: EmailVerification, auth_service: AuthService = Depends(get_auth_service)):
#     """Verify password reset code - DEPRECATED: Password reset now sends new password directly"""
#     raise HTTPException(status_code=410, detail="Endpoint deprecated. Use /forgot-password directly")

# @router.post("/reset-password")
# async def reset_password(password_data: PasswordChange, email: str, auth_service: AuthService = Depends(get_auth_service)):
#     """Reset password after verification - DEPRECATED: Password reset now sends new password directly"""
#     raise HTTPException(status_code=410, detail="Endpoint deprecated. Use /forgot-password directly")

@router.post("/change-password")
async def change_password(request: Request, current_user = Depends(get_current_user), auth_service: AuthService = Depends(get_auth_service)):
    """Change password for authenticated user"""
    
    try:
        # Check if user account is verified
        if not current_user.get("is_verified", False):
            raise HTTPException(status_code=403, detail="Account must be verified before changing password")
        
        # Get raw request body for debugging
        body = await request.body()
        print(f"Raw request body: {body}")
        
        # Parse JSON manually to handle potential issues
        import json
        try:
            request_data = json.loads(body)
            print(f"Parsed request data: {request_data}")
        except json.JSONDecodeError as e:
            print(f"JSON decode error: {e}")
            raise HTTPException(status_code=422, detail="Invalid JSON format")
        
        # Extract password fields
        current_password = request_data.get("current_password") or request_data.get("currentPassword")
        new_password = request_data.get("new_password") or request_data.get("newPassword")
        
        print(f"Change password request for user: {current_user['email']}")
        print(f"Password data received: current_password present: {bool(current_password)}, new_password present: {bool(new_password)}")
        
        # Validate password data
        if not current_password or not new_password:
            raise HTTPException(status_code=422, detail="Both current_password/currentPassword and new_password/newPassword are required")
        
        # Validate new password strength
        if len(new_password) < 6:
            raise HTTPException(status_code=422, detail="New password must be at least 6 characters long")
        
        # Verify current password
        is_valid_password = await auth_service.verify_password_by_email(current_user["email"], current_password)
        if not is_valid_password:
            raise HTTPException(status_code=400, detail="Current password is incorrect")
        
        # Check if new password is same as current
        is_same_password = await auth_service.verify_password_by_email(current_user["email"], new_password)
        if is_same_password:
            raise HTTPException(status_code=400, detail="New password must be different from current password")
        
        await auth_service.update_password(current_user["email"], new_password)
        print(f"Password updated successfully for user: {current_user['email']}")
        return {"success": True, "message": "Password changed successfully"}
    except HTTPException:
        raise  # Re-raise HTTP exceptions as they are
    except Exception as e:
        print(f"Error changing password: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail="Failed to change password")

@router.post("/change-email")
async def change_email_request(new_email_data: dict, current_user = Depends(get_current_user), auth_service: AuthService = Depends(get_auth_service)):
    """Request email change"""
    try:
        # Check if user account is verified
        if not current_user.get("is_verified", False):
            raise HTTPException(status_code=403, detail="Account must be verified before changing email")
        
        print(f"Change email request for user: {current_user['email']}")
        print(f"Request data: {new_email_data}")
        
        # Validate input data
        if "newEmail" not in new_email_data:
            raise HTTPException(status_code=422, detail="newEmail field is required")
        
        new_email = new_email_data["newEmail"]
        print(f"New email: {new_email}")
        
        # Validate email format
        if not new_email or not isinstance(new_email, str):
            raise HTTPException(status_code=422, detail="Invalid email format")
        
        # Check if new email is the same as current
        if new_email == current_user["email"]:
            raise HTTPException(status_code=400, detail="New email is the same as current email")
        
        # Validate Gmail format first
        if not auth_service.validate_gmail(new_email):
            raise HTTPException(status_code=400, detail="Email must be a valid Gmail address")
        
        # Check if new email already exists in database
        existing_user = await auth_service.check_user_exists(email=new_email)
        if existing_user:
            raise HTTPException(status_code=409, detail="This email is already registered by another user")
        
        # Generate and send verification code
        verification_code = auth_service.generate_verification_code()
        await auth_service.store_verification_code(new_email, verification_code, "email_change")
        
        # Send verification email
        email_sent = await auth_service.send_verification_email(new_email, verification_code, "email_change")
        if not email_sent:
            raise HTTPException(status_code=500, detail="Failed to send verification email")
        
        print(f"Verification email sent to: {new_email}")
        return {
            "success": True,
            "message": "Verification code sent to new email"
        }
    except HTTPException:
        raise  # Re-raise HTTP exceptions as they are
    except Exception as e:
        print(f"Error in change email request: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail="Failed to process email change request")

@router.post("/verify-email-change")
async def verify_email_change(email_change_data: dict, current_user = Depends(get_current_user), auth_service: AuthService = Depends(get_auth_service)):
    """Verify and complete email change"""
    try:
        print(f"Verify email change request data: {email_change_data}")
        print(f"Current user: {current_user['email']}")
        
        # Validate input data
        if "newEmail" not in email_change_data or "verificationCode" not in email_change_data:
            raise HTTPException(status_code=422, detail="newEmail and verificationCode fields are required")
        
        new_email = email_change_data["newEmail"]
        verification_code = email_change_data["verificationCode"]
        
        print(f"New email: {new_email}")
        print(f"Verification code: '{verification_code}'")
        
        is_valid = await auth_service.verify_code(
            new_email,
            verification_code,
            "email_change"
        )
        
        print(f"Code verification result: {is_valid}")
        
        if not is_valid:
            raise HTTPException(status_code=400, detail="Invalid or expired verification code")
        
        await auth_service.update_email(current_user["email"], new_email)
        
        # Generate new token with updated email
        token = auth_service.generate_jwt_token(
            current_user["user_id"],
            current_user["username"],
            new_email,
            is_verified=True
        )
        
        return {
            "success": True,
            "message": "Email changed successfully",
            "accessToken": token,
            "isVerified": True,
            "user": {
                "id": current_user["user_id"],
                "username": current_user["username"],
                "email": new_email
            }
        }
    except HTTPException:
        raise  # Re-raise HTTP exceptions as they are
    except Exception as e:
        print(f"Error in verify email change: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail="Failed to change email")

@router.get("/profile")
async def get_profile(current_user = Depends(get_current_user), db_service: DatabaseService = Depends(get_db_service)):
    """Get user profile"""
    try:
        profile = await db_service.get_user_profile(current_user["user_id"])
        if not profile:
            raise HTTPException(status_code=404, detail="User not found")
        
        return {
            "user": {
                "id": profile["id"],
                "username": profile["username"],
                "email": profile["email"],
                "created_at": profile["created_at"],
                "profile_data": profile["profile_data"]
            }
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail="Failed to get profile")

@router.post("/logout")
async def logout(current_user = Depends(get_current_user)):
    """User logout (client should delete token)"""
    return {"message": "Logged out successfully"}

@router.post("/query", response_model=QueryResponse)
async def query_chatbot(request: QueryRequest, current_user = Depends(get_current_user)):
    """
    Proxy chatbot query request with authentication
    """
    try:
        # Forward request to chatbot service
        async with httpx.AsyncClient() as client:
            chatbot_request = {"question": request.question}
            response = await client.post(
                f"{CHATBOT_SERVICE_URL}/query",
                json=chatbot_request,
                timeout=60.0
            )
            
            if response.status_code == 200:
                result = response.json()
                
                # Save chat message to database if session_id provided
                if request.session_id:
                    try:
                        await save_chat_message(
                            current_user.get("user_id"),
                            request.session_id,
                            request.question,
                            result["answer"],
                            result["query_time"]
                        )
                    except Exception as e:
                        print(f"Warning: Failed to save chat message: {e}")
                
                return QueryResponse(
                    answer=result["answer"],
                    query_time=result["query_time"]
                )
            else:
                raise HTTPException(status_code=response.status_code, detail="Chatbot service error")
                
    except httpx.TimeoutException:
        raise HTTPException(status_code=504, detail="Chatbot service timeout")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

async def save_chat_message(user_id: str, session_id: str, question: str, answer: str, query_time: float, db_service: DatabaseService = Depends(get_db_service)):
    """Save chat message to database"""
    try:
        message_data = {
            "user_id": user_id,
            "session_id": session_id,
            "user_message": question,
            "bot_response": answer,
            "response_time": query_time,
            "timestamp": None  # Will be set by database service
        }
        await db_service.save_chat_message(message_data)
        return True
    except Exception as e:
        print(f"Failed to save chat message: {e}")
        return False
