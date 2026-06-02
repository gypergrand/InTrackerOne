# InTrackerOne — Architecture

## System Overview

InTrackerOne is a Spring Boot 4.0.6 application for tracking job applications through the hiring pipeline. It provides both a Vaadin web UI and a REST API.

## Domain Model

### Entities

11 JPA entities organized by domain:

```
┌─────────────┐     ┌─────────────┐
│     User    │────<│   ApiToken  │
└─────────────┘     └─────────────┘
                                          ┌─────────────┐
                                          │   Company   │
                                          └─────────────┘
                                      ┌───────┼────────┬──────────────┐
                                      ▼       ▼        ▼              ▼
                              ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
                              │  Project │ │ Recruiter│ │ Industry │ │    Job   │
                              └──────────┘ └──────────┘ └──────────┘ │Applicat'n│
                                      │                               └──────────┘
                                      ▼                                    │
                              ┌──────────┐                    ┌────────────┼────────────┐
                              │ Pipeline │                    ▼            ▼            ▼
                              │  Status  │              ┌──────────┐ ┌──────────┐ ┌──────────┐
                              └──────────┘              │  Status  │ │Interview │ │Compensat'n│
                              (abstract)                │  Change  │ │  Event   │ │  Entry   │
                                                        └──────────┘ └──────────┘ └──────────┘
```

### Entity Details

#### User
- Fields: id, username, passwordHash, email, role, enabled, createdAt, updatedAt
- Relationships: 1:N → ApiToken, 1:N → JobApplication (owner)
- Role enum: ADMIN, MODERATOR, USER
- Special: "admin" user protected from deletion/disable at service layer

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
- Relationships: N:1 → Company, N:1 → User (owner), 1:N → JobApplication, M:N → Recruiter (via join table)

#### Recruiter
- Fields: id, name, email, phone, company, owner, createdAt, updatedAt
- Relationships: N:1 → Company, N:1 → User (owner), M:N → Project (via join table)

#### Industry
- Fields: id, name, createdAt
- Relationships: shared across all users (no owner)
- Admin-managed via `DefaultIndustryView` Vaadin UI
- Users can add custom industries ad-hoc from Project form

#### PipelineStatus
- Fields: id, name, color, sortOrder, isDefault, owner, createdAt
- Relationships: N:1 → User (owner, nullable — default templates have null owner)
- **Default statuses** (`isDefault=true, owner_id=NULL`): 14 templates seeded by `PipelineStatusService.seedDefaultStatusesIfNeeded()`
- **User statuses** (`isDefault=false, owner_id=<userId>`): per-user copies created at registration via `seedDefaultsForUser()`

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

## Default Statuses (14 stages)

Seeded by `PipelineStatusService.seedDefaultStatusesIfNeeded()` on first run:

| Sort | Name                | Type     |
|------|---------------------|----------|
| 1    | Preparation         | ACTIVE   |
| 2    | Applied             | ACTIVE   |
| 3    | Phone Screen        | ACTIVE   |
| 4    | Behavioral Interview| ACTIVE   |
| 5    | DSA Interview       | ACTIVE   |
| 6    | PO Interview        | ACTIVE   |
| 7    | Technical Interview | ACTIVE   |
| 8    | On-site / Final     | ACTIVE   |
| 9    | Offer               | ACTIVE   |
| 10   | Negotiation         | ACTIVE   |
| 11   | Accepted            | TERMINAL |
| 12   | Rejected            | TERMINAL |
| 13   | Withdrawn           | TERMINAL |
| 14   | Ghosted             | TERMINAL |

Admin can add/edit/delete/reorder these via `DefaultStatusView` (admin-only Vaadin view).
Per-user copies are snapshots — future default changes do not retroactively modify existing user statuses.

## Default Industries

Managed in the shared `industries` table via `DefaultIndustryView` (admin-only Vaadin view).
Users can create new industries on-the-fly from the Project form via `ProjectService.resolveIndustries()`.

## Security Model

### Authentication
- **Vaadin UI:** Form login (Spring Security form-based authentication)
- **REST API:** Bearer token authentication via `TokenAuthenticationFilter` (added before `UsernamePasswordAuthenticationFilter`)
- Token creation: `ApiTokenService.generateToken()` returns `itr_` prefixed value exactly once (never stored in plaintext)
- Token verification: SHA-256 hash comparison via `MessageDigest`

### Authorization
- **AccessControl utility:** Static methods for ownership checks
  - `hasGlobalDataAccess(role)`: ADMIN/MODERATOR get global access
  - `requireOwnerOrGlobal(resourceOwnerId)`: Ensures user owns resource or has global access
  - `requireSameOwner(ownerId)`: Ensures user owns the resource (treats null as system-level bypass)
  - `isOwner(ownerId)`: Simple boolean check
- **SecurityService:** Provides `getCurrentUser()` from `SecurityContextHolder`

### Roles
- **ADMIN:** Full access to all resources, can manage users/roles/default statuses/default industries/API tokens
- **MODERATOR:** All data CRUD, API tokens, no user/status/industry management
- **USER:** Own API tokens and data only

## Database Schema

### Migrations
- 19 Flyway migrations (V1–V19), history:
  - V1: users, V2: api_tokens, V3: pipeline_statuses, V4: companies
  - V5: recruiters, V6: projects, V7: recruiter_projects (join)
  - V8: job_applications, V9: status_changes, V10: interview_events
  - V11: compensation_entries, V12: email constraints, V13: work_type
  - V14: industries, V15: job details columns
  - V16: clear tokens, V17: work_type NOT NULL
  - V18: rename user_id to owner_id, V19: add is_default + nullable owner
- Tables in snake_case, plural: `companies`, `job_applications`
- Hibernate ddl-auto: validate (Flyway owns schema)

### Key Relationships
```
users (1) ──< api_tokens (N)
users (1) ──< job_applications (N)
companies (1) ──< projects (N)
companies (1) ──< recruiters (N)
companies (1) ──< job_applications (N)
recruiters (M) ──< recruiter_projects >── projects (M)
projects (1) ──< job_applications (N)
industries (1) ──< ... (shared, no owner FK)
pipeline_statuses (1) ──< job_applications (N)  [current_status_id]
pipeline_statuses (1) ──< status_changes (N)     [status_id]
job_applications (1) ──< status_changes (N)
job_applications (1) ──< interview_events (N)
job_applications (1) ──< compensation_entries (N)
```

## UI Features

### Vaadin Views
| View | Route | Access | Description |
|------|-------|--------|-------------|
| Dashboard | `/` | Authenticated | Job stats, recent changes |
| Pipeline | `/pipeline` | Authenticated | Kanban board with ComboBox status picker |
| Companies | `/companies` | Authenticated | CRUD grid with edit dialog + delete |
| Projects | `/projects` | Authenticated | CRUD grid with industry tag badges + delete |
| Recruiters | `/recruiters` | Authenticated | CRUD grid + delete |
| Jobs | `/jobs` | Authenticated | CRUD grid with filters + delete |
| Users | `/users` | ADMIN only | User management, admin user protection |
| Default Statuses | `/default-statuses` | ADMIN only | Manage 14 default status templates |
| Default Industries | `/default-industries` | ADMIN only | Manage shared industries list |
| API Tokens | `/tokens` | Authenticated | Create/revoke own tokens |

## Performance Optimizations

### N+1 Query Prevention
- **JOIN FETCH in Repository Queries:** Used for `JobApplication` collections to avoid N+1 when loading related entities (`company`, `project`, `recruiter`, `currentStatus`, `owner`)
- **JOIN FETCH in ApiTokenRepository:** Used for token lookup to eagerly load user relationship
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
- `ResourceNotFoundException(resource, id)`: Thrown when entity not found (maps to 404)
- `UnauthorizedException(message)`: Thrown for access control violations (maps to 403)
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

Additional handlers: `IllegalArgumentException` → 400, general `Exception` → 500.

## Testing Strategy

### Test Types
- **Service tests:** `@ExtendWith(MockitoExtension.class)`, mock all dependencies, use `@InjectMocks`
- **Controller tests:** `@ExtendWith(MockitoExtension.class)`, MockMvc standalone setup, no `@SpringBootTest`
- **Repository tests:** `@DataJpaTest` with `TestEntityManager`, in-memory H2 (`create-drop`)
- **Security tests:** Pure unit tests for `AccessControl`, `SecurityService`, `JpaUserDetailsService`
- **E2E tests:** Full application context with in-memory H2 (`application-h2.yml` override)

### Test Coverage
- 33 test files: 12 service, 11 controller, 5 repository, 3 security/utility, 3 E2E
- 186 total tests (all passing)
- Every service (including IndustryService) and every repository with custom queries is tested
- `AccessControl` — all 6 static methods tested exhaustively
- `JpaUserDetailsService` — load by username (found/not-found/disabled)
- Uses `ArgumentCaptor` and `verify()` for side effect assertions

## Configuration

### Profiles
- **default:** H2 file-based database (development)
- **postgres:** PostgreSQL (production)
- **test:** In-memory H2 (automatically active for tests via `application-h2.yml`)

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

### Docker
```bash
docker build -t intrackerone .
docker compose up -d
```