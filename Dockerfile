# Multi-stage Dockerfile for TNUT AI ChatBox App
# Stage 1: Convert PDFs to Markdown
FROM python:3.11-slim as convert-stage

WORKDIR /app

# Install dependencies for PDF conversion
COPY dataset/requirements.txt ./dataset/requirements.txt
RUN pip install --no-cache-dir -r ./dataset/requirements.txt

# Copy dataset and convert PDFs
COPY dataset/ ./dataset/
WORKDIR /app/dataset
RUN python convert_pdfs.py

# Stage 2: Setup Chatbot dependencies
FROM python:3.11-slim as chatbot-stage

WORKDIR /app

# Copy chatbot requirements and install
COPY backend_chatbot/requirements.txt ./backend_chatbot/requirements.txt
RUN pip install --no-cache-dir -r ./backend_chatbot/requirements.txt

# Stage 3: Setup Auth dependencies
FROM python:3.11-slim as auth-stage

WORKDIR /app

# Copy auth requirements and install
COPY backend_auth/requirements.txt ./backend_auth/requirements.txt
RUN pip install --no-cache-dir -r ./backend_auth/requirements.txt

# Final stage: Combine everything
FROM python:3.11-slim

# Install supervisor for running multiple services
RUN apt-get update && apt-get install -y supervisor && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy installed packages from stages (to avoid re-installing)
COPY --from=chatbot-stage /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=auth-stage /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages

# Copy converted markdown files
COPY --from=convert-stage /app/dataset/ ./dataset/

# Copy source code
COPY backend_chatbot/ ./backend_chatbot/
COPY backend_auth/ ./backend_auth/

# Copy supervisor config
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# Copy entrypoint script
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Expose ports
EXPOSE 5000 8000

# Set entrypoint
ENTRYPOINT ["/app/entrypoint.sh"]