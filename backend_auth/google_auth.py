import os
import asyncio
from google.auth.transport import requests
from google.oauth2 import id_token
from pydantic import BaseModel
from typing import Optional

class GoogleTokenVerification(BaseModel):
    id_token: str

class GoogleUserInfo(BaseModel):
    email: str
    name: str
    picture: str
    email_verified: bool

class GoogleAuthService:
    def __init__(self):
        self.google_client_id = os.getenv("GOOGLE_CLIENT_ID")
        
    async def verify_google_token(self, token: str) -> Optional[GoogleUserInfo]:
        """Verify Google ID token and return user info with retry logic"""
        # First attempt with standard clock skew
        user_info = await self._verify_token_attempt(token, 300)
        if user_info:
            return user_info
        
        # If first attempt fails, try with larger clock skew tolerance
        print("First verification failed, retrying with larger clock skew tolerance...")
        await asyncio.sleep(1)  # Small delay before retry
        user_info = await self._verify_token_attempt(token, 600)
        
        return user_info
    
    async def _verify_token_attempt(self, token: str, clock_skew_seconds: int) -> Optional[GoogleUserInfo]:
        """Single attempt to verify Google ID token"""
        try:
            # Verify the token with clock skew tolerance
            idinfo = id_token.verify_oauth2_token(
                token, 
                requests.Request(), 
                self.google_client_id,
                clock_skew_in_seconds=clock_skew_seconds
            )
            
            # Check if token is valid
            if idinfo['iss'] not in ['accounts.google.com', 'https://accounts.google.com']:
                raise ValueError('Wrong issuer.')
            
            # Extract user information
            user_info = GoogleUserInfo(
                email=idinfo.get('email', ''),
                name=idinfo.get('name', ''),
                picture=idinfo.get('picture', ''),
                email_verified=idinfo.get('email_verified', False)
            )
            
            return user_info
            
        except ValueError as e:
            error_msg = str(e)
            print(f"Invalid Google token: {error_msg}")
            
            # Check if it's a clock skew error and provide helpful message
            if "Token used too early" in error_msg or "clock" in error_msg.lower():
                print("Clock skew detected. Consider synchronizing system time or increasing clock skew tolerance.")
            
            return None
        except Exception as e:
            print(f"Error verifying Google token: {e}")
            return None
    
    def is_gmail_account(self, email: str) -> bool:
        """Check if email is from Gmail"""
        return email.endswith('@gmail.com')
    
    def generate_username_from_email(self, email: str, name: str) -> str:
        """Generate unique username from email and name"""
        # Try to use name first
        username = name.lower().replace(' ', '_')
        if len(username) < 3:
            # Fallback to email prefix
            username = email.split('@')[0]
        
        # Remove special characters
        import re
        username = re.sub(r'[^a-zA-Z0-9_]', '', username)
        
        # Ensure minimum length
        if len(username) < 3:
            username = f"user_{username}"
            
        return username[:20]  # Limit length
