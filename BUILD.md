# InTrackerOne — Build Plan

## Implementation Order (19 steps)

| # | Step | Deliverable |
|---|---|---|
| 1 | **Scaffold** | pom.xml, application.yml/profiles, Flyway config, main class, .gitignore |
| 2 | **Flyway migrations** | V1–V13 SQL files: all tables + seed data |
| 3 | **Entities + Repositories** | All 10 JPA entities + Spring Data repositories |
| 4 | **MapStruct mappers** | All entity↔DTO mapper interfaces |
| 5 | **Services: Company/Project/Recruiter** | CRUD with DTOs, ownership filtering |
| 6 | **Services: JobApplication** | CRUD with status, filtering |
| 7 | **Services: InterviewEvent/Compensation** | CRUD, linked to job |
| 8 | **Services: StatusChange/Dashboard** | History tracking, summary stats |
| 9 | **Services: User/ApiToken** | User CRUD, token generation/hashing/validation |
| 10 | **Security infrastructure** | SecurityConfig, SecurityService, TokenFilter, DataInitializer |
| 11 | **REST exception handling** | ApiExceptionHandler (@RestControllerAdvice) |
| 12 | **REST controllers (all)** | All v1 endpoints |
| 13 | **Vaadin: MainLayout + LoginView** | App shell with nav sidebar + login form |
| 14 | **Vaadin: Company views** | Grid list + create/edit dialog |
| 15 | **Vaadin: Project views** | Grid list (per company) + create/edit dialog |
| 16 | **Vaadin: Recruiter views** | Grid list (per company) + form with project multi-select |
| 17 | **Vaadin: Job views** | Grid list (filterable) + form with compensation/events tabs |
| 18 | **Vaadin: Dashboard** | Summary cards + recent activity timeline |
| 19 | **Vaadin: Pipeline Kanban** | Column-based board with status cards |

## Database Profiles

### Default (H2) — application-h2.yml
```yaml
spring:
  datasource:
    url: jdbc:h2:file:~/intrackerone/data;AUTO_SERVER=TRUE
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
```

### PostgreSQL — application-postgres.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/intrackerone
    driver-class-name: org.postgresql.Driver
    username: ${DB_USER}
    password: ${DB_PASS}
```

### application.yml (shared)
```yaml
spring:
  profiles:
    active: h2
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    locations: classpath:db/migration
```

## Vaadin Key Dependencies (pom.xml)

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.vaadin</groupId>
      <artifactId>vaadin-bom</artifactId>
      <version>24.9.3</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>vaadin-spring-boot-starter</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
  </dependency>
  <dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
  </dependency>
  <dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
  </dependency>
  <dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
  </dependency>
  <dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>

<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <source>17</source>
        <target>17</target>
        <annotationProcessorPaths>
          <path>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
          </path>
          <path>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
            <version>1.5.5.Final</version>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>
```
