#!/bin/bash
set -e

cd "$(dirname "$0")/.."

case "${1:-local}" in
  local)
    echo "Starting with H2..."
    mvn spring-boot:run
    ;;
  docker)
    echo "Starting with Docker Compose..."
    docker compose up -d
    echo "App: http://localhost:8080"
    echo "PostgreSQL: localhost:5432"
    ;;
  build)
    echo "Building..."
    mvn clean package -DskipTests
    echo "JAR: target/*.jar"
    ;;
  test)
    echo "Running tests..."
    mvn test
    ;;
  stop)
    echo "Stopping Docker..."
    docker compose down
    ;;
  logs)
    docker compose logs -f app
    ;;
  *)
    echo "Usage: $0 {local|docker|build|test|stop|logs}"
    exit 1
    ;;
esac