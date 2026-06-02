@echo off
cd /d "%~dp0.."

if "%1"=="" set action=up
set action=%1

if "%action%"=="up" (
    echo Building JAR...
    mvn clean package -DskipTests -q
    echo Starting Docker Compose...
    docker compose up -d
    echo.
    echo App:        http://localhost:8080
    echo PostgreSQL: localhost:5432
    echo.
    echo Default users: admin/admin, user/user
) else if "%action%"=="down" (
    echo Stopping...
    docker compose down
) else if "%action%"=="restart" (
    echo Restarting...
    docker compose restart
) else if "%action%"=="logs" (
    docker compose logs -f app
) else if "%action%"=="status" (
    docker compose ps
) else (
    echo Usage: docker.bat {up^|down^|restart^|logs^|status}
    exit /b 1
)