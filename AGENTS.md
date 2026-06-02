# InTrackerOne — Project Conventions

## Tech Stack
- Java 25, Spring Boot 4.0.x, Vaadin 25.1.x (free core)
- Maven, Lombok, MapStruct, Flyway
- H2 (default profile), PostgreSQL (postgres profile)

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
├── domain/{user,company,project,recruiter,job,interview,compensation,history,status}/
│   ├── {Entity}.java              # JPA entity (@Entity, @Getter/@Setter, @Builder)
│   ├── {Entity}Repository.java    # extends JpaRepository
│   ├── {Entity}Service.java       # business logic, DTOs, uses AccessControl
│   ├── {Entity}Dto.java           # public record (read-only display)
│   ├── {Entity}Form.java          # @Data POJO (for form input)
│   └── {Entity}Mapper.java        # @Mapper(componentModel = "spring")
├── infrastructure/
│   ├── config/                    # SecurityConfig extends VaadinWebSecurity
│   ├── security/                  # SecurityService, TokenAuthenticationFilter, AccessControl
│   ├── exception/                 # ResourceNotFoundException, UnauthorizedException
│   ├── data/                      # DataInitializer
│   └── api/v1/                    # REST controllers (no @RestControllerAdvice yet)
└── ui/
    ├── views/                     # Vaadin @Route views
    └── components/                # Shared UI fragments
```

## Coding Rules
- Method length < 20 lines
- No null returns: use Optional<T> or custom exceptions (throw `ResourceNotFoundException`, `UnauthorizedException`)
- No System.out.println: use @Slf4j logger
- Comments only for "why", never for "what"
- No wildcard imports
- Lombok on JPA: use @Getter/@Setter, never @Data
- Logging: INFO default, DEBUG for com.intrackerone

## Testing
- Unit tests: `@ExtendWith(MockitoExtension.class)` with Mockito mocks
- Repository tests: `@DataJpaTest` with `TestEntityManager`
- Controller tests: `@ExtendWith(MockitoExtension.class)` + MockMvc standalone setup, no `@SpringBootTest`
- Service tests: Mock all dependencies, use `@InjectMocks` for the service under test
- Use `ArgumentCaptor` and `verify()` to assert side effects, not just mock return values

## Security & Authorization
- `AccessControl` utility: static methods `hasGlobalDataAccess(role)`, `requireOwnerOrGlobal()`, `requireSameOwner()`, `isOwner()`
- `SecurityService.getCurrentUser()` retrieves authenticated user from `SecurityContextHolder`
- Bearer token auth via `TokenAuthenticationFilter` (added before `UsernamePasswordAuthenticationFilter`)
- Form validation: use `@Valid` on controller method parameters, throw exceptions for violations

## Database
- Flyway migrations in src/main/resources/db/migration/
- Hibernate ddl-auto: validate (Flyway owns schema)
- H2 console enabled in dev profile
- Tables in snake_case, plural: companies, job_applications

## REST API
- Bearer token auth only (Authorization: Bearer <token>)
- Tokens created by authenticated users
- All endpoints under /api/v1/
- Custom exceptions: 
  - `ResourceNotFoundException(resource, id)` for missing entities (auto-caught as 404)
  - `UnauthorizedException(message)` for access control violations (auto-caught as 403)
  - Throw these from services; Spring's default exception handlers catch them

## Roles
- ADMIN: full access, manage users/roles/statuses, API tokens
- MODERATOR: all data CRUD, API tokens, no user management
- USER: own API tokens and data only

## Build Commands
- `mvn clean install -DskipTests` — build JAR/WAR
- `mvn test` — run all tests (unit + integration)
- `mvn spring-boot:run` — run app locally (H2 profile, default)
- `SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run` — run with PostgreSQL (must set DB_USER, DB_PASS env vars)
- `mvn clean test -Dtest=CompanyServiceTest` — run single test class

## Statuses (11 default stages)
Active: Preparation, Applied, Phone Screen, Technical Interview, On-site/Final, Offer, Negotiation
Terminal: Accepted, Rejected, Withdrawn, Ghosted
