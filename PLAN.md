# InTrackerOne

Personal job interview tracking and management application.

## Tech Stack

| Component | Version / Choice |
|---|---|
| Java | 17 |
| Framework | Spring Boot 3.5.x |
| Frontend | Vaadin 24.9.x (free core) |
| Database (dev) | H2 (file-based, default profile) |
| Database (prod) | PostgreSQL (via `postgres` profile) |
| Schema management | Flyway |
| Build | Maven |
| Boilerplate | Lombok |
| Mapping | MapStruct |
| Auth (browser) | Spring Security, form-based login |
| Auth (API) | Bearer token |

## Architecture: Layered + DTOs

```
Vaadin Views → Services (DTOs) ←→ Repositories (Entities)
                    ↑
              MapStruct mappers
```

- Entities never leave the service layer
- Display DTOs = Java Records (immutable)
- Form DTOs = `@Data` classes (mutable, Vaadin Binder compatible)

## Roles & Permissions

| Role | Data Access | Can Manage Users/Roles | Can Manage Pipeline | API Token Access |
|---|---|---|---|---|
| ADMIN | All users' data | Yes | Yes | Yes |
| MODERATOR | All users' data | No | No | Yes |
| USER | Own data only | No | Own pipeline only | No |

## Authentication

- **Browser**: Spring Security form login (Vaadin LoginOverlay)
- **REST API**: Bearer token only (`Authorization: Bearer <token>`)
- Tokens created by ADMIN/MODERATOR only, hashed in DB, shown once

## Coding Conventions

| Rule | Value |
|---|---|
| Java features | Records, Optional, var (where clear) |
| Method length | < 20 lines |
| Null handling | `Optional<T>` or custom exceptions (`CompanyNotFoundException`) |
| Logging | `@Slf4j`, no `System.out` |
| Comments | "why" only, never "what" |
| Imports | No wildcards |
| Lombok on JPA | `@Getter/@Setter` (not `@Data`) |
| Package structure | `com.intrackerone.domain.*`, `com.intrackerone.infrastructure.*`, `com.intrackerone.ui.*` |
| Error display | In-view error sections (not notifications) |
| i18n | English only |
| Logging levels | `INFO` default, `DEBUG` for `com.intrackerone` services |

## Testing

| Layer | Tool |
|---|---|
| Services | JUnit 5 + Mockito |
| Repositories | `@DataJpaTest` |
| Controllers | `@WebMvcTest` |
| Security | `@SpringBootTest` |

## Project Structure

```
intrackerone/
├── pom.xml
├── src/main/java/com/intrackerone/
│   ├── InTrackerOneApplication.java
│   ├── domain/
│   │   ├── user/           # User entity, repo, service, DTOs, mapper
│   │   ├── company/        # Company entity, repo, service, DTOs, mapper
│   │   ├── project/        # Project entity, repo, service, DTOs, mapper
│   │   ├── recruiter/      # Recruiter entity, repo, service, DTOs, mapper
│   │   ├── job/            # JobApplication entity, repo, service, DTOs, mapper
│   │   ├── interview/      # InterviewEvent entity, repo, service, DTOs, mapper
│   │   ├── compensation/   # CompensationEntry entity, repo, service, DTOs, mapper
│   │   ├── history/        # StatusChange entity, repo, service, DTOs, mapper
│   │   └── status/         # PipelineStatus entity, repo, service, DTOs, mapper
│   ├── infrastructure/
│   │   ├── config/         # SecurityConfig, WebConfig
│   │   ├── security/       # SecurityService, CurrentUser, TokenFilter
│   │   ├── data/           # DataInitializer
│   │   └── api/            # REST controllers + exception handler
│   │       └── v1/
│   └── ui/
│       ├── views/
│       │   ├── LoginView.java
│       │   ├── DashboardView.java
│       │   ├── companies/
│       │   ├── projects/
│       │   ├── recruiters/
│       │   ├── jobs/
│       │   └── pipeline/
│       └── components/     # Shared UI fragments
├── src/main/resources/
│   ├── application.yml
│   ├── application-h2.yml
│   ├── application-postgres.yml
│   └── db/migration/
├── .gitignore
└── README.md
```
