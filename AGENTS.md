# InTrackerOne — Project Conventions

## Tech Stack
- Java 25, Spring Boot 4.0.6, Vaadin 25.1.6 (free core)
- Maven, Lombok, MapStruct, Flyway
- H2 (default profile), PostgreSQL (postgres profile)
- Token hashing: SHA-256 (user-level API tokens)

## Architecture: Layered + DTOs
```
Vaadin Views → Services (DTOs) ←→ Repositories (Entities)
                    ↑
              MapStruct mappers
```
- Entities never leave the service layer
- Display DTOs = Java Records (immutable)
- Form DTOs = `@Data` classes (mutable, Vaadin Binder compatible)
- MapStruct mappers: `@Mapper(componentModel = "spring")`, use `@Mapping(target = "...", ignore = true)` for id/timestamps/relationships
- Mapper methods: `toDto(Entity)` and `toEntity(Form)` for each domain object

## Package Structure
```
com.intrackerone/
├── domain/
│   ├── user/         User, ApiToken
│   ├── company/      Company
│   ├── project/      Project, Industry
│   ├── recruiter/    Recruiter
│   ├── job/          JobApplication
│   ├── interview/    InterviewEvent
│   ├── compensation/ CompensationEntry
│   ├── history/      StatusChange
│   └── status/       PipelineStatus
│   Each domain: {Entity}.java, {Entity}Repository.java, {Entity}Service.java,
│                {Entity}Dto.java (record), {Entity}Form.java (@Data),
│                {Entity}Mapper.java (@Mapper)
├── infrastructure/
│   ├── config/       SecurityConfig extends VaadinWebSecurity
│   ├── security/     SecurityService, TokenAuthenticationFilter, AccessControl, JpaUserDetailsService
│   ├── api/          ApiExceptionHandler, 11 REST controllers (api/v1/)
│   ├── exception/    ResourceNotFoundException, UnauthorizedException
│   └── data/         DataInitializer
└── ui/
    ├── views/        DashboardView, PipelineView, CompanyListView, ProjectListView,
    │                 RecruiterListView, JobListView, UserAdminView,
    │                 DefaultStatusView, DefaultIndustryView, TokenView
    └── components/   Shared UI fragments
```

## Coding Rules
- Method length < 20 lines
- No null returns: use Optional<T> or custom exceptions (throw `ResourceNotFoundException`, `UnauthorizedException`)
- No System.out.println: use @Slf4j logger
- Comments only for "why", never for "what"
- No wildcard imports
- Lombok on JPA: use @Getter/@Setter, never @Data
- Logging: INFO default, DEBUG for com.intrackerone
- Admin user (username "admin") is protected from deletion/disable at service layer

## Testing
- Service tests: `@ExtendWith(MockitoExtension.class)` with Mockito mocks
- Repository tests: `@DataJpaTest` with `TestEntityManager`, in-memory H2 (`create-drop`)
- Controller tests: `@ExtendWith(MockitoExtension.class)` + MockMvc standalone setup, no `@SpringBootTest`
- E2E tests: Full context, in-memory H2 via `application-h2.yml` override
- Security/utility tests: Pure unit tests (no Spring context)
- Use `@InjectMocks`, mock all dependencies
- Use `ArgumentCaptor` and `verify()` to assert side effects, not just mock return values
- 186 tests total across 33 test files, all passing

## Security & Authorization
- Two filter chains: one for Vaadin (form login), one for REST API (Bearer token)
- `AccessControl` utility: static methods `hasGlobalDataAccess(role)`, `requireOwnerOrGlobal()`, `requireSameOwner()`, `isOwner()`
- `requireSameOwner()` allows null `actualOwner` (treats as system-level default, bypasses matching)
- `SecurityService.getCurrentUser()` retrieves authenticated user from `SecurityContextHolder`
- Bearer token auth via `TokenAuthenticationFilter` (added before `UsernamePasswordAuthenticationFilter`)
- Token hashing: SHA-256 via `MessageDigest`; tokens use `itr_` prefix
- Form validation: use `@Valid` on controller method parameters, throw exceptions for violations
- `ApiExceptionHandler` handles: `ResourceNotFoundException` → 404, `UnauthorizedException` → 403, `SecurityException` → 403, `IllegalArgumentException` → 400, `Exception` → 500

## Database
- 11 JPA entities: User, ApiToken, Company, Project, Recruiter, Industry, PipelineStatus, JobApplication, StatusChange, InterviewEvent, CompensationEntry
- 19 Flyway migrations (V1–V19)
- Hibernate ddl-auto: validate (Flyway owns schema)
- H2 console enabled in dev profile
- Tables in snake_case, plural: companies, job_applications
- N+1 fix: use `JOIN FETCH` in `@Query` for collections (not `@EntityGraph` with custom method names)

## REST API
- Bearer token auth only (Authorization: Bearer <token>)
- Tokens created by authenticated users
- All endpoints under /api/v1/
- Custom exceptions:
  - `ResourceNotFoundException(resource, id)` for missing entities (auto-caught as 404)
  - `UnauthorizedException(message)` for access control violations (auto-caught as 403)
  - Throw these from services; `ApiExceptionHandler` catches them
- UI-only features (no REST): Default Statuses, Default Industries, Change Password, Pipeline Kanban

## Roles
- ADMIN: full access, manage users/roles/default statuses/default industries, API tokens
- MODERATOR: all data CRUD, API tokens, no user management
- USER: own API tokens and data only

## Build Commands
- `mvn clean install -DskipTests` — build JAR/WAR
- `mvn test` — run all tests (186 tests)
- `mvn spring-boot:run` — run app locally (H2 profile, default)
- `SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run` — run with PostgreSQL (must set DB_USER, DB_PASS env vars)
- `mvn clean test -Dtest=CompanyServiceTest` — run single test class

## Statuses (14 default stages)
ACTIVE:  Preparation, Applied, Phone Screen, Behavioral Interview, DSA Interview, PO Interview, Technical Interview, On-site/Final, Offer, Negotiation
TERMINAL: Accepted, Rejected, Withdrawn, Ghosted
- Managed via DefaultStatusView (ADMIN only) or per-user via PipelineStatusService
- Default templates stored as `is_default=true, owner_id=NULL`
- Per-user copies snapshotted at creation — future default edits don't retroactively change existing users
