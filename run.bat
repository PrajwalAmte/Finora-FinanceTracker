@echo off
REM Finance Tracker - Docker Compose Startup Script (Windows)
REM This script builds and starts all services using Docker Compose

echo.
echo 🚀 Starting Finance Tracker Application...
echo.

REM Check if Docker is running
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Error: Docker is not running. Please start Docker Desktop and try again.
    pause
    exit /b 1
)

REM Check if docker-compose is available
docker compose version >nul 2>&1
if %errorlevel% equ 0 (
    set COMPOSE_CMD=docker compose
) else (
    docker-compose --version >nul 2>&1
    if %errorlevel% equ 0 (
        set COMPOSE_CMD=docker-compose
    ) else (
        echo ❌ Error: docker-compose is not installed. Please install Docker Compose.
        pause
        exit /b 1
    )
)

echo 📦 Building and starting containers...
echo.

REM Build and start services
%COMPOSE_CMD% up --build

echo.
echo ✅ Finance Tracker is running!
echo.
echo 🌐 Frontend: http://localhost
echo 🔧 Backend API: http://localhost:8080
echo 📚 API Docs: http://localhost:8080/swagger-ui.html
echo.
echo Press Ctrl+C to stop all services.
pause

