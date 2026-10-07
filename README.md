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
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:appdb`, user `sa`, no password)

## Run the tests
```powershell
.\mvnw.cmd test
```

## Questions
| # | Question | PR link |
|---|---|---|
| 1 | Task Manager API | [#1](https://github.com/NanduEds24/be-interview-prep/pull/1) |
| 2 | URL Shortener | [#2](https://github.com/NanduEds24/be-interview-prep/pull/2) |
| 3 | Authentication & Roles | |
| 4 | Product Catalog | |
| 5 | Order Service | |
