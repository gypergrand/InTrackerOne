# Contributing to InTrackerOne

## Getting Started

1. Ensure you have Java 25+ and Maven 3.9+
2. Fork the repository and clone locally
3. Run `mvn spring-boot:run` to verify the app starts

## Development Workflow

1. Create a feature branch from `main`
2. Make your changes following the project conventions (see [AGENTS.md](AGENTS.md))
3. Run `mvn test` to verify all 186 tests pass
4. Run `mvn clean install -DskipTests` to verify the build succeeds
5. Submit a pull request

## Code Style

- Method length < 20 lines
- No null returns — use `Optional<T>` or throw `ResourceNotFoundException`/`UnauthorizedException`
- No `System.out.println` — use `@Slf4j` logger
- Comments only for "why", never for "what"
- No wildcard imports
- Lombok on JPA: use `@Getter`/`@Setter`, never `@Data`

## Testing

All new code must include tests. The project has 186 tests across 33 files:

- **Service tests:** `@ExtendWith(MockitoExtension.class)` with Mockito mocks
- **Repository tests:** `@DataJpaTest` with `TestEntityManager`
- **Controller tests:** `@ExtendWith(MockitoExtension.class)` + MockMvc (no `@SpringBootTest`)
- **E2E tests:** Full context with in-memory H2
- Use `ArgumentCaptor` and `verify()` to assert side effects

Run a single test class:
```bash
mvn clean test -Dtest=ServiceClassNameTest
```

## Security

- The "admin" user is protected from deletion/disable at the service layer
- All REST endpoints require Bearer token authentication
- API tokens use `itr_` prefix with SHA-256 hashing
- Access control uses `AccessControl` utility class static methods

## Pull Request Checklist

- [ ] Tests pass (`mvn test`)
- [ ] Build succeeds (`mvn clean install -DskipTests`)
- [ ] No new warnings introduced
- [ ] Follows project coding conventions
- [ ] Includes tests for new functionality
- [ ] Documentation updated if applicable (README, ARCHITECTURE, API, AGENTS)
