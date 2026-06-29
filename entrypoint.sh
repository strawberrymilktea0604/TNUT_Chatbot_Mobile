#!/bin/bash

# Check if backend_auth/.env exists
if [ ! -f /app/backend_auth/.env ]; then
  echo "Error: Missing /app/backend_auth/.env"
  exit 1
fi

# Check if backend_chatbot/.env exists
if [ ! -f /app/backend_chatbot/.env ]; then
  echo "Error: Missing /app/backend_chatbot/.env"
  exit 1
fi

echo "All required .env files are present."

echo "Starting TNUT AI ChatBox services..."
echo "Chatbot will be available on port 5000"
echo "Auth service will be available on port 8000"

# Start supervisor to run both services
exec /usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf