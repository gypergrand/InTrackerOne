# InTrackerOne

Job application tracking system built with Spring Boot and Vaadin.

## Features

- **Job Application Tracking:** Track applications through 11 pipeline stages
- **Company & Project Management:** Organize applications by company and project
- **Interview Scheduling:** Log interview events with dates and notes
- **Compensation Tracking:** Record salary, bonus, equity, and other compensation
- **REST API:** Full API access with bearer token authentication
- **Role-Based Access:** ADMIN, MODERATOR, and USER roles with granular permissions

## Tech Stack

- Java 25, Spring Boot 4.0.6, Vaadin 25.1.6
- Maven, Lombok, MapStruct, Flyway
- H2 (default), PostgreSQL (production)
- SHA-256 token hashing

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

## Default Users

| Username | Password | Role |
|----------|----------|------|
| admin    | admin    | ADMIN |
| user     | user     | USER |

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

## Project Structure

```
src/main/java/com/intrackerone/
├── domain/          # Business logic and entities
├── infrastructure/  # Security, API, configuration
└── ui/              # Vaadin views and components
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed domain model and [API.md](API.md) for REST API documentation.

## Development

### Build
```bash
mvn clean install -DskipTests
```

### Run Tests
```bash
mvn test
```

### Single Test
```bash
mvn clean test -Dtest=CompanyServiceTest
```

## Database

- H2 console: http://localhost:8080/h2-console (dev profile only)
- Flyway manages schema with 15 migrations (V1–V15)
- Hibernate validates schema against migrations

## Deployment

### Build WAR
```bash
mvn clean package -DskipTests
```

### Docker
```bash
docker build -t intrackerone .
docker run -p 8080:8080 intrackerone
```

## License

Private project.