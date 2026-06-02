# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in InTrackerOne, please report it by
opening a GitHub Security Advisory or contacting the project maintainers
directly.

Do not report security vulnerabilities through public GitHub issues.

## Authentication

- **Vaadin UI:** Form-based login with Spring Security
- **REST API:** Bearer token authentication with SHA-256 hashing
- Tokens use the `itr_` prefix and are stored as SHA-256 hashes
- The raw token value is returned only once at creation time

## Authorization

- Role-based access control: ADMIN, MODERATOR, USER
- `AccessControl` utility enforces ownership checks
- The "admin" user is protected from deletion/disable

## Best Practices

- Change default passwords immediately in production
- Use PostgreSQL with strong credentials in production
- Keep dependencies updated
- Enable HTTPS in production deployments
