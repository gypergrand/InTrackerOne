# InTrackerOne

Job application tracking system built with Spring Boot and Vaadin.

## Features

- **Job Application Tracking:** Track applications through 14 pipeline stages
- **Company & Project Management:** Organize applications by company and project
- **Recruiter Management:** Track recruiters per company with contact details
- **Interview Scheduling:** Log interview events with dates and notes
- **Compensation Tracking:** Record salary, bonus, equity, and other compensation
- **Pipeline Kanban Board:** Drag-and-drop column management with editable statuses
- **Default Status & Industry Templates:** Admin-managed global defaults for new users
- **Change Password:** Self-service password change for any authenticated user
- **REST API:** Full API access with bearer token authentication
- **Role-Based Access:** ADMIN, MODERATOR, and USER roles with granular permissions
- **Admin User Protection:** The "admin" user cannot be deleted or disabled

## Tech Stack

- Java 25, Spring Boot 4.0.6, Vaadin 25.1.6 (free core)
- Maven, Lombok, MapStruct, Flyway
- H2 (default), PostgreSQL (production)
- SHA-256 token hashing for API tokens

## Project Structure

```
src/main/java/com/intrackerone/
├── domain/          # Business logic (entities, services, repos, DTOs, mappers)
├── infrastructure/  # Security, API, configuration, exception handling
└── ui/              # Vaadin views and shared components
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed domain model and [API.md](API.md) for REST API documentation.

## Quick Start

### Prerequisites
- Java 25+
- Maven 3.9+

### Run Locally (H2)
```bash
mvn spring-boot:run
```
Open http://localhost:8080

### Run with PostgreSQL
```bash
export DB_USER=your_user
export DB_PASS=your_password
SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run
```

### Launch Scripts
```bash
# Linux/Mac
./scripts/run.sh local    # H2 (default)
./scripts/run.sh docker   # PostgreSQL via Docker
./scripts/run.sh build    # Build JAR
./scripts/run.sh test     # Run tests
./scripts/run.sh stop     # Stop Docker
./scripts/run.sh logs     # View logs

# Windows
scripts\run.bat local
scripts\run.bat docker
scripts\run.bat build
scripts\run.bat test
scripts\run.bat stop
scripts\run.bat logs
```

### Manual Commands
```bash
mvn spring-boot:run          # Run with H2
mvn clean package -DskipTests  # Build JAR
mvn test                     # Run all tests (186 total)
mvn clean test -Dtest=CompanyServiceTest  # Single test class
```

## Default Users

| Username | Password | Role     |
|----------|----------|----------|
| admin    | admin    | ADMIN    |
| moderator| moderator| MODERATOR|
| user     | user     | USER     |

The "admin" user is protected — cannot be deleted or disabled from the system.

## Database

- H2 console: http://localhost:8080/h2-console (dev profile only)
- Flyway manages schema with 19 migrations (V1–V19)
- Hibernate validates schema against migrations (`ddl-auto: validate`)
- Tables in snake_case, plural: `companies`, `job_applications`

## API Usage

### Create Token
```bash
curl -X POST http://localhost:8080/api/v1/tokens \
  -H "Content-Type: application/json" \
  -d '{"name": "my-token"}'
```

### Use Token
```bash
curl http://localhost:8080/api/v1/companies \
  -H "Authorization: Bearer itr_xxxxxxxxxxxxxxxx"
```

See [API.md](API.md) for the full REST API reference.

## Deployment

### Build WAR/JAR
```bash
mvn clean package -DskipTests
```

### Docker (Standalone)
```bash
docker build -t intrackerone .
docker run -e SPRING_PROFILES_ACTIVE=postgres \
           -e DB_USER=postgres \
           -e DB_PASS=postgres \
           -p 8080:8080 intrackerone
```

### Docker Compose (with PostgreSQL)
```bash
docker compose up -d
```
App: http://localhost:8080  
PostgreSQL: localhost:5432

## Documentation

| File | Description |
|------|-------------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Domain model, security, database, testing strategy |
| [API.md](API.md) | REST API v1 endpoint reference |
| [AGENTS.md](AGENTS.md) | Project conventions for AI-assisted development |

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) before contributing.

## Security

Report security vulnerabilities to the project maintainers. See [SECURITY.md](SECURITY.md).

## License

Private project. All rights reserved.