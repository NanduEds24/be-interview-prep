# be-interview-prep

Five Spring Boot REST features, each delivered as its own branch, pull request and merge.
Scope and design decisions are in [SPEC.md](SPEC.md).

**Stack:** Java 21+ (built on JDK 25), Spring Boot 3.5, Spring Web, Spring Data JPA, H2 (in memory),
Bean Validation, springdoc-openapi (Swagger UI). Maven wrapper included.

## Run the app
```powershell
.\mvnw.cmd spring-boot:run
```
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- H2 console (off by default; local debugging only): set `H2_CONSOLE_ENABLED=true`, then open
  http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:appdb`, user `sa`, no password). It is public
  and gives full SQL access, so never enable it on a shared machine.

### Authentication (Q3)
All `/api/**` endpoints except `/api/auth/**` need `Authorization: Bearer <token>`. Secrets come from
environment variables; none are in the source:

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | HMAC signing key, at least 32 bytes. If unset, a random key is generated on each start (dev only; tokens stop working after a restart). |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Optional. When both are set, an ADMIN account is created at startup. |

```powershell
$env:JWT_SECRET = "<at least 32 random characters>"
$env:ADMIN_EMAIL = "admin@demo.com"; $env:ADMIN_PASSWORD = "<choose one>"
.\mvnw.cmd spring-boot:run
```
In Swagger UI: call `POST /api/auth/login`, copy `token`, click **Authorize** and paste it.

## Run the tests
```powershell
.\mvnw.cmd test
```

## Questions
| # | Question | PR link |
|---|---|---|
| 1 | Task Manager API | [#1](https://github.com/NanduEds24/be-interview-prep/pull/1) |
| 2 | URL Shortener | [#2](https://github.com/NanduEds24/be-interview-prep/pull/2) |
| 3 | Authentication & Roles | [#3](https://github.com/NanduEds24/be-interview-prep/pull/3) |
| 4 | Product Catalog | [#5](https://github.com/NanduEds24/be-interview-prep/pull/5) |
| 5 | Order Service | |
