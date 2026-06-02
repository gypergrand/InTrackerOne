#!/bin/bash
set -e

cd "$(dirname "$0")/.."

case "${1:-up}" in
  up)
    echo "Starting Docker Compose..."
    docker compose up -d
    echo ""
    echo "App:        http://localhost:8080"
    echo "PostgreSQL: localhost:5432"
    echo ""
    echo "Default users: admin/admin, user/user"
    ;;
  down)
    echo "Stopping..."
    docker compose down
    ;;
  restart)
    echo "Restarting..."
    docker compose restart
    ;;
  logs)
    docker compose logs -f app
    ;;
  status)
    docker compose ps
    ;;
  build)
    echo "Building images..."
    docker compose build
    ;;
  *)
    echo "Usage: $0 {up|down|restart|logs|status|build}"
    exit 1
    ;;
esac