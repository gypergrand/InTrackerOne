@echo off
cd /d "%~dp0.."

if "%1"=="" set action=local
set action=%1

if "%action%"=="local" (
    echo Starting with H2...
    mvn spring-boot:run
) else if "%action%"=="docker" (
    echo Starting with Docker Compose...
    docker compose up -d
    echo App: http://localhost:8080
    echo PostgreSQL: localhost:5432
) else if "%action%"=="build" (
    echo Building...
    mvn clean package -DskipTests
    echo JAR: target\*.jar
) else if "%action%"=="test" (
    echo Running tests...
    mvn test
) else if "%action%"=="stop" (
    echo Stopping Docker...
    docker compose down
) else if "%action%"=="logs" (
    docker compose logs -f app
) else (
    echo Usage: run.bat {local^|docker^|build^|test^|stop^|logs}
    exit /b 1
)