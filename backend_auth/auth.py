import os
import re
import smtplib
import random
import string
import hashlib
import motor.motor_asyncio
from datetime import datetime, timedelta
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from fastapi import HTTPException
from pydantic import BaseModel, EmailStr
from typing import Optional
import jwt
from passlib.context import CryptContext
from bson import ObjectId
from datetime import datetime, timezone 

# Password hashing
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

class UserRegistration(BaseModel):
    username: str
    email: EmailStr
    password: str
    confirm_password: str

class UserLogin(BaseModel):
    username: str
    password: str

class EmailVerification(BaseModel):
    email: EmailStr
    verification_code: str

class PasswordReset(BaseModel):
    email: EmailStr

class PasswordChange(BaseModel):
    current_password: str
    new_password: str

class EmailChange(BaseModel):
    new_email: EmailStr
    verification_code: str

class GoogleAuth(BaseModel):
    email: EmailStr
    username: str
    password: str
    confirm_password: str

class GoogleAuthManual(BaseModel):
    email: EmailStr
    username: str
    password: str
    confirm_password: str

class AuthService:
    def __init__(self):
        self.db_url = os.getenv("MONGODB_URL", "mongodb://localhost:27017")
        self.db_name = os.getenv("DATABASE_NAME", "tnut_chatbot")
        self.jwt_secret = os.getenv("JWT_SECRET_KEY", "your-secret-key")
        self.smtp_server = os.getenv("SMTP_SERVER", "smtp.gmail.com")
        self.smtp_port = int(os.getenv("SMTP_PORT", "587"))
        self.email_address = os.getenv("MAIL_USERNAME")
        self.email_password = os.getenv("MAIL_PASSWORD")
        self.email_from = os.getenv("MAIL_FROM")
        self.verification_codes = {}  # In production, use Redis or database
        self.client = None
        self.db = None
        
    async def get_database(self):
        """Get database connection"""
        if self.client is None:
            self.client = motor.motor_asyncio.AsyncIOMotorClient(self.db_url)
            self.db = self.client[self.db_name]
        return self.db
    
    def hash_password(self, password: str) -> str:
        """Hash password using bcrypt"""
        return pwd_context.hash(password)
    
    def verify_password(self, plain_password: str, hashed_password: str) -> bool:
        """Verify password against hash"""
        return pwd_context.verify(plain_password, hashed_password)
    
    async def verify_password_by_email(self, email: str, password: str) -> bool:
        """Verify password for a user by email"""
        try:
            db = await self.get_database()
            user = await db.users.find_one({"email": email})
            if not user:
                return False
            # Check both possible password field names for backward compatibility
            password_hash = user.get("password_hash") or user.get("password")
            if not password_hash:
                return False
            return self.verify_password(password, password_hash)
        except Exception:
            return False
    
    def validate_gmail(self, email: str) -> bool:
        """Validate Gmail format"""
        gmail_pattern = r'^[a-zA-Z0-9._%+-]+@gmail\.com$'
        return re.match(gmail_pattern, email) is not None
    
    def generate_verification_token(self, email: str, purpose: str = "registration") -> str:
        """Generate a secure JWT for email verification."""
        payload = {
            "email": email,
            "purpose": purpose,
            "exp": datetime.now(timezone.utc) + timedelta(minutes=60)  # Token valid for 1 hour
        }
        return jwt.encode(payload, self.jwt_secret, algorithm="HS256")
    
    def generate_jwt_token(self, user_id: str, username: str, email: str, is_verified: bool = False) -> str:
        """Generate JWT token"""
        payload = {
            "user_id": user_id,
            "username": username,
            "email": email,
            "is_verified": is_verified,
            "exp": datetime.now(timezone.utc) + timedelta(days=7)
        }
        return jwt.encode(payload, self.jwt_secret, algorithm="HS256")
    
    def generate_verification_code(self) -> str:
        """Generate 6-digit verification code"""
        return ''.join(random.choices(string.digits, k=6))
    
    def generate_random_password(self) -> str:
        """Generate 6-digit random password"""
        return ''.join(random.choices(string.digits, k=6))
    
    async def send_verification_email(self, email: str, token: str, purpose: str = "registration"):
        """Send verification email with a clickable link."""
        if not self.email_address or not self.email_password:
            print("\nWarning: Email credentials not configured. Skipping email sending.")
            return True

        # The base URL should ideally come from config, but we'll hardcode for now.
        # This will be the frontend URL that handles the verification.
        # Since we don't have a frontend web page, we'll point to the backend endpoint directly.
        verification_url = f"http://localhost:8000/api/auth/verify-email/{token}"

        try:
            msg = MIMEMultipart()
            msg['From'] = self.email_from
            msg['To'] = email
            
            subject = ""
            body_html = ""

            if purpose == "registration":
                subject = "Xác thực tài khoản của bạn"
                body_html = f"""
                <html>
                <body>
                    <p>Chào bạn,</p>
                    <p>Cảm ơn bạn đã đăng ký. Vui lòng nhấp vào liên kết bên dưới để xác thực tài khoản của bạn:</p>
                    <p><a href="{verification_url}">Xác thực tài khoản</a></p>
                    <p>Liên kết này có hiệu lực trong 60 phút.</p>
                    <p>Trân trọng,<br>TNUT ChatBot Team</p>
                </body>
                </html>
                """
            elif purpose == "password_reset":
                # This part can be updated later if needed
                subject = "Yêu cầu đặt lại mật khẩu"
                body_html = f"""
                <html>
                <body>
                    <p>Chào bạn,</p>
                    <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Vui lòng sử dụng mã sau để tiếp tục:</p>
                    <p><b>{token}</b></p>
                    <p>Mã này có hiệu lực trong 10 phút.</p>
                    <p>Trân trọng,<br>TNUT ChatBot Team</p>
                </body>
                </html>
                """
            elif purpose == "email_change":
                subject = "Xác thực thay đổi email"
                body_html = f"""
                <html>
                <body>
                    <p>Chào bạn,</p>
                    <p>Chúng tôi nhận được yêu cầu thay đổi email cho tài khoản của bạn. Vui lòng sử dụng mã xác thực sau:</p>
                    <p><b>{token}</b></p>
                    <p>Mã này có hiệu lực trong 10 phút.</p>
                    <p>Trân trọng,<br>TNUT ChatBot Team</p>
                </body>
                </html>
                """
            
            if not subject:
                return False

            msg['Subject'] = subject
            msg.attach(MIMEText(body_html, 'html', 'utf-8'))
            
            server = smtplib.SMTP(self.smtp_server, self.smtp_port)
            server.starttls()
            server.login(self.email_address, self.email_password)
            text = msg.as_string()
            server.sendmail(self.email_from, email, text)
            server.quit()
            
            return True
        except Exception as e:
            print(f"Error sending email: {e}")
            return False
    
    async def send_new_password_email(self, email: str, new_password: str):
        """Send new password to user via email"""
        if not self.email_address or not self.email_password:
            print("\nWarning: Email credentials not configured. Skipping email sending.")
            return True

        try:
            msg = MIMEMultipart()
            msg['From'] = self.email_from
            msg['To'] = email
            
            subject = "Mật khẩu mới cho tài khoản của bạn"
            body_html = f"""
            <html>
            <body>
                <p>Chào bạn,</p>
                <p>Chúng tôi đã tạo mật khẩu mới cho tài khoản của bạn theo yêu cầu:</p>
                <p><b>Mật khẩu mới: {new_password}</b></p>
                <p>Vui lòng sử dụng mật khẩu này để đăng nhập. Bạn có thể thay đổi mật khẩu sau khi đăng nhập thành công.</p>
                <p>Trân trọng,<br>TNUT ChatBot Team</p>
            </body>
            </html>
            """

            msg['Subject'] = subject
            msg.attach(MIMEText(body_html, 'html', 'utf-8'))
            
            server = smtplib.SMTP(self.smtp_server, self.smtp_port)
            server.starttls()
            server.login(self.email_address, self.email_password)
            text = msg.as_string()
            server.sendmail(self.email_from, email, text)
            server.quit()
            
            return True
        except Exception as e:
            print(f"Error sending email: {e}")
            return False
    
    async def check_user_exists(self, username: str = None, email: str = None):
        """Check if user exists by username or email"""
        db = await self.get_database()
        
        query = {}
        if username and email:
            query = {"$or": [{"username": username}, {"email": email}]}
        elif username:
            query = {"username": username}
        elif email:
            query = {"email": email}
        else:
            return None
        
        result = await db.users.find_one(query, {"username": 1, "email": 1, "is_verified": 1})
        if result:
            result["id"] = str(result["_id"])
            del result["_id"]
            return result
        return None
    
    async def create_user(self, username: str, email: str, password: str):
        """Create new user"""
        db = await self.get_database()
        
        hashed_password = self.hash_password(password)
        user_doc = {
            "username": username,
            "email": email,
            "password_hash": hashed_password,
            "created_at": datetime.now(timezone.utc),
            "updated_at": datetime.now(timezone.utc),
            "is_verified": False,
            "profile_data": {}
        }
        
        result = await db.users.insert_one(user_doc)
        return str(result.inserted_id)
    
    async def verify_user_credentials(self, username: str, password: str):
        """Verify user login credentials"""
        db = await self.get_database()
        
        result = await db.users.find_one(
            {"username": username},
            {"username": 1, "email": 1, "password_hash": 1, "is_verified": 1}
        )
        
        if not result:
            return None
        
        if self.verify_password(password, result['password_hash']):
            return {
                "id": str(result['_id']),
                "username": result['username'],
                "email": result['email'],
                "is_verified": result.get("is_verified", False)
            }
        return None
    
    async def verify_user_email(self, email: str):
        """Verify user's email and update the database."""
        db = await self.get_database()
        await db.users.update_one(
            {"email": email},
            {"$set": {"is_verified": True, "updated_at": datetime.now(timezone.utc)}}
        )
        return True

    async def update_password(self, email: str, new_password: str):
        """Update user password"""
        db = await self.get_database()
        
        hashed_password = self.hash_password(new_password)
        await db.users.update_one(
            {"email": email},
            {"$set": {"password_hash": hashed_password, "updated_at": datetime.now(timezone.utc)}}
        )
        return True
    
    async def update_email(self, old_email: str, new_email: str):
        """Update user email"""
        db = await self.get_database()
        
        print(f"Updating email from {old_email} to {new_email}")
        
        result = await db.users.update_one(
            {"email": old_email},
            {"$set": {"email": new_email, "updated_at": datetime.now(timezone.utc)}}
        )
        
        print(f"Update result: matched={result.matched_count}, modified={result.modified_count}")
        
        if result.matched_count == 0:
            print(f"No user found with email: {old_email}")
            return False
        
        return True
    
    def verify_verification_token(self, token: str, purpose: str) -> Optional[str]:
        """Verify the email verification token and return the email if valid."""
        try:
            payload = jwt.decode(token, self.jwt_secret, algorithms=["HS256"])
            if payload.get("purpose") == purpose:
                return payload.get("email")
        except jwt.ExpiredSignatureError:
            raise HTTPException(status_code=400, detail="Liên kết xác thực đã hết hạn.")
        except jwt.InvalidTokenError:
            raise HTTPException(status_code=400, detail="Liên kết xác thực không hợp lệ.")
        return None

    async def store_verification_code(self, email: str, code: str, purpose: str):
        """Store verification code temporarily (in production, use Redis or database)"""
        # For now, we'll store in memory. In production, use Redis or database
        key = f"{email}:{purpose}"
        self.verification_codes[key] = {
            "code": code,
            "created_at": datetime.now(timezone.utc),
            "purpose": purpose
        }
        return True

    async def verify_code(self, email: str, code: str, purpose: str) -> bool:
        """Verify verification code"""
        key = f"{email}:{purpose}"
        print(f"Verifying code for key: {key}")
        print(f"Input code: '{code}'")
        print(f"Available verification codes: {list(self.verification_codes.keys())}")
        
        stored_data = self.verification_codes.get(key)
        
        if not stored_data:
            print(f"No stored data found for key: {key}")
            return False
        
        print(f"Stored code: '{stored_data['code']}'")
        print(f"Code match: {stored_data['code'] == code}")
        
        # Check if code matches
        if stored_data["code"] != code:
            print("Code mismatch!")
            return False
        
        # Check if code is expired (10 minutes)
        time_diff = datetime.now(timezone.utc) - stored_data["created_at"]
        print(f"Time difference: {time_diff}, Max allowed: {timedelta(minutes=10)}")
        
        if time_diff > timedelta(minutes=10):
            print("Code expired!")
            # Remove expired code
            del self.verification_codes[key]
            return False
        
        # Code is valid, remove it
        print("Code verified successfully!")
        del self.verification_codes[key]
        return True
