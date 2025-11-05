#!/bin/bash

# Finance Tracker - Docker Compose Startup Script
# This script builds and starts all services using Docker Compose

set -e

echo "🚀 Starting Finance Tracker Application..."
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ Error: docker-compose is not installed. Please install Docker Compose."
    exit 1
fi

# Use docker compose (v2) if available, otherwise fall back to docker-compose (v1)
if docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="docker-compose"
fi

echo "📦 Building and starting containers..."
echo ""

# Build and start services
$COMPOSE_CMD up --build

echo ""
echo "✅ Finance Tracker is running!"
echo ""
echo "🌐 Frontend: http://localhost"
echo "🔧 Backend API: http://localhost:8080"
echo "📚 API Docs: http://localhost:8080/swagger-ui.html"
echo ""
echo "Press Ctrl+C to stop all services."

