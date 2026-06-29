import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
from contextlib import asynccontextmanager
from auth_routes import router as auth_router
from chat_routes import router as chat_router
from database import DatabaseService

# Load environment variables
# Construct the path to the .env file relative to this script's location
dotenv_path = os.path.join(os.path.dirname(__file__), '.env')
load_dotenv(dotenv_path=dotenv_path)

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Asynchronous context manager to handle startup and shutdown events.
    """
    print("Auth Server starting up...")

    # Initialize database
    db_service = DatabaseService()
    await db_service.init_database()
    print("Database initialized.")
    
    yield
    # Clean up resources on shutdown
    print("Auth Server shutting down...")

# Initialize FastAPI app with the lifespan context manager
app = FastAPI(
    title="TNUT Auth & Database Service",
    description="Authentication and database service for TNUT chatbot.",
    version="1.0.0",
    lifespan=lifespan,
)

# Add CORS middleware to allow requests from the frontend and other services
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost",
        "http://localhost:3000",
        "http://localhost:5173",  # Default Vite port
        "http://127.0.0.1:5173",
        "http://localhost:8001",  # Chatbot backend
        "http://127.0.0.1:8001",
    ],
    allow_credentials=True,
    allow_methods=["*"],  # Allow all methods
    allow_headers=["*"],  # Allow all headers
)

# Include routers
app.include_router(auth_router)
app.include_router(chat_router)

@app.get("/")
def read_root():
    return {"message": "Welcome to the TNUT Auth & Database Service API"}

@app.get("/health")
def health_check():
    return {"status": "healthy", "service": "auth_database"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)