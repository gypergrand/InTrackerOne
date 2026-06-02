# InTrackerOne — Architecture

## System Overview

InTrackerOne is a Spring Boot 4.0.6 application for tracking job applications through the hiring pipeline. It provides both a Vaadin web UI and a REST API.

## Domain Model

### Entities

10 JPA entities organized by domain:

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│     User    │────<│   ApiToken  │     │   Company   │
└─────────────┘     └─────────────┘     └─────────────┘
                                                 │
                              ┌──────────────────┼──────────────────┐
                              ▼                  ▼                  ▼
                    ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
                    │   Project   │     │  Recruiter  │     │   Industry  │
                    └─────────────┘     └─────────────┘     └─────────────┘
                              │
                              ▼
                    ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
                    │    Job      │────<│   Status    │     │   Pipeline  │
                    │ Application │     │   Change    │     │   Status    │
                    └─────────────┘     └─────────────┘     └─────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
    ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
    │  Interview  │     │ Compensation│     │   Status    │
    │    Event    │     │   Entry     │     │   Change    │
    └─────────────┘     └─────────────┘     └─────────────┘
```

### Entity Details

#### User
- Fields: id, username, passwordHash, email, role, enabled, createdAt, updatedAt
- Relationships: 1:N → ApiToken, 1:N → JobApplication (owner)
- Role enum: ADMIN, MODERATOR, USER

#### ApiToken
- Fields: id, tokenIdentifier, tokenHash, name, user, createdAt, lastUsedAt
- Relationships: N:1 → User
- Token format: `itr_` prefix + token identifier
- Hashing: SHA-256 via `MessageDigest`

#### Company
- Fields: id, name, website, industry, notes, owner, createdAt, updatedAt
- Relationships: N:1 → User (owner), 1:N → Project, 1:N → Recruiter, 1:N → JobApplication

#### Project
- Fields: id, name, description, company, owner, createdAt, updatedAt
- Relationships: N:1 → Company, N:1 → User (owner), 1:N → JobApplication

#### Recruiter
- Fields: id, name, email, phone, company, owner, createdAt, updatedAt
- Relationships: N:1 → Company, N:1 → User (owner)

#### PipelineStatus
- Fields: id, name, color, sortOrder, owner, createdAt
- Relationships: N:1 → User (owner)
- 11 default statuses provided by DataInitializer

#### JobApplication
- Fields: id, title, url, company, project, recruiter, currentStatus, owner, createdAt, updatedAt
- Relationships: N:1 → Company, N:1 → Project, N:1 → Recruiter, N:1 → PipelineStatus (currentStatus), N:1 → User (owner)
- 1:N → StatusChange, 1:N → InterviewEvent, 1:N → CompensationEntry

#### StatusChange
- Fields: id, jobApplication, status, decidedBy, notes, owner, createdAt
- Relationships: N:1 → JobApplication, N:1 → PipelineStatus (status), N:1 → User (owner)
- decidedBy: "COMPANY" or "CANDIDATE" (max 50 chars)

#### InterviewEvent
- Fields: id, jobApplication, eventType, date, notes, owner, createdAt
- Relationships: N:1 → JobApplication, N:1 → User (owner)
- EventType enum: PHONE_SCREEN, TECHNICAL, ONSITE, BEHAVIORAL, OTHER

#### CompensationEntry
- Fields: id, jobApplication, type, baseSalary, bonus, equity, currency, notes, owner, createdAt
- Relationships: N:1 → JobApplication, N:1 → User (owner)
- Type enum: BASE, BONUS, EQUITY, SIGNING, RELOCATION

## Security Model

### Authentication
- **Vaadin UI:** Form login (Spring Security default)
- **REST API:** Bearer token authentication via `TokenAuthenticationFilter`
- Token creation: `ApiTokenService.generateToken()` returns `itr_` prefixed value once
- Token verification: SHA-256 hash comparison via `MessageDigest`

### Authorization
- **AccessControl utility:** Static methods for ownership checks
  - `hasGlobalDataAccess(role)`: ADMIN/MODERATOR get global access
  - `requireOwnerOrGlobal(resourceOwnerId)`: Ensures user owns resource or has global access
  - `requireSameOwner(ownerId)`: Ensures user owns the resource
  - `isOwner(ownerId)`: Simple boolean check
- **SecurityService:** Provides `getCurrentUser()` from `SecurityContextHolder`

### Roles
- **ADMIN:** Full access to all resources, can manage users/roles/statuses
- **MODERATOR:** All data CRUD, API tokens, no user management
- **USER:** Own API tokens and data only

## Database Schema

### Migrations
- 15 Flyway migrations (V1–V15)
- Tables in snake_case, plural: `companies`, `job_applications`
- Hibernate ddl-auto: validate (Flyway owns schema)

### Key Relationships
```
users (1) ──< api_tokens (N)
users (1) ──< job_applications (N)
companies (1) ──< projects (N)
companies (1) ──< recruiters (N)
companies (1) ──< job_applications (N)
projects (1) ──< job_applications (N)
job_applications (1) ──< status_changes (N)
job_applications (1) ──< interview_events (N)
job_applications (1) ──< compensation_entries (N)
pipeline_statuses (1) ──< job_applications (N)
pipeline_statuses (1) ──< status_changes (N)
```

## Performance Optimizations

### N+1 Query Prevention
- **JOIN FETCH in Repository Queries:** Used for `JobApplication` collections to avoid N+1 when loading related entities (`company`, `project`, `recruiter`, `currentStatus`, `owner`)
- **Aggregate Queries:** Dashboard uses `GROUP BY` for status counts instead of per-status COUNT queries
- **ReadOnly Transactions:** Repository queries marked with `@Transactional(readOnly = true)` for optimization

### Query Examples
```java
// JobApplicationRepository - JOIN FETCH for collection loading
@Query("SELECT DISTINCT j FROM JobApplication j " +
       "LEFT JOIN FETCH j.company " +
       "LEFT JOIN FETCH j.project " +
       "LEFT JOIN FETCH j.recruiter " +
       "LEFT JOIN FETCH j.currentStatus " +
       "LEFT JOIN FETCH j.owner")
List<JobApplication> findAllWithRelations();

// JobService - Aggregate query for dashboard
@Query("SELECT j.currentStatus.id, COUNT(j) FROM JobApplication j " +
       "WHERE j.owner.id = :ownerId GROUP BY j.currentStatus.id")
List<Object[]> countByOwnerGroupByStatusId(@Param("ownerId") Long ownerId);
```

## Exception Handling

### Custom Exceptions
- `ResourceNotFoundException`: Thrown when entity not found (maps to 404)
- `UnauthorizedException`: Thrown for access control violations (maps to 403)
- `SecurityException`: Caught by `ApiExceptionHandler` (maps to 403)

### Global Exception Handler
`ApiExceptionHandler` provides consistent error responses:
```json
{
  "message": "Resource not found: JobApplication with id 42",
  "status": 404,
  "timestamp": "2025-01-15T10:30:00Z"
}
```

## Testing Strategy

### Unit Tests
- Service tests: Mock all dependencies, use `@InjectMocks`
- Controller tests: MockMvc standalone setup, no `@SpringBootTest`
- Repository tests: `@DataJpaTest` with `TestEntityManager`

### Test Coverage
- 27 test files (17 unit tests, 2 repository tests, 3 e2e tests)
- Focus on business logic and API contract verification
- Use `ArgumentCaptor` and `verify()` for side effect assertions

## Configuration

### Profiles
- **default:** H2 in-memory database (development)
- **postgres:** PostgreSQL (production)

### Key Properties
- `spring.jpa.hibernate.ddl-auto=validate` (Flyway manages schema)
- `spring.h2.console.enabled=true` (dev profile only)
- `server.port=8080` (default)

## Deployment

### Build
```bash
mvn clean install -DskipTests  # Build JAR/WAR
mvn test                       # Run all tests
```

### Run
```bash
mvn spring-boot:run  # H2 (default profile)
SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run  # PostgreSQL
```