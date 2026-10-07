# Project

Timed build: a Java REST API built against the clock. There is no frontend; the API is the product
and Swagger UI is the demo. Scope lives in SPEC.md; read it before starting any feature (it is created
by /spec when the brief arrives).
The team is new to Java: after each change, explain what you did in 3 lines or fewer.

## Stack
- Spring Boot 4.1.1, Java 25, Maven wrapper, Spring Web MVC + Data JPA + H2 (in-memory) +
  Validation + DevTools + springdoc-openapi (Swagger UI). The Maven project is at the repository root.
- Package com.example.app, one package per feature (entity, repository, service, controller, DTOs),
  like `health/`. Shared code in `common/`.
- Java records for request and response DTOs, with Bean Validation annotations on requests.
  Never return entities from controllers. No Lombok.
- Errors: `common/ApiExceptionHandler` turns every error into a problem detail
  (`{"status","title","detail"}`; validation errors also list fields under `errors`). For a missing
  item, throw `new ResponseStatusException(HttpStatus.NOT_FOUND, "Task 5 not found")` from the service.

## Commands (Windows, PowerShell)
- Run: `.\mvnw.cmd spring-boot:run`   (port 8080; use `.\mvnw.cmd`, not `./mvnw`)
- Test: `.\mvnw.cmd test`
- Swagger UI (demo and manual testing): http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Database browser: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:appdb`, user `sa`)

## How we work
- One feature at a time: entity, repository, service, controller, then tests.
- Every endpoint gets MockMvc tests for the success case, validation failure (400) and not found (404).
- Use proper REST status codes: 201 for create, 204 for delete, 400 for invalid input, 404 for missing,
  409 for conflicts.
- After each feature: run the tests, call the endpoints on the running app, then commit.
- Simplest thing that works. No new dependencies without asking.
- Work on main. One-line commit messages. After committing, show `git log --oneline -1` as proof.
- If something fails twice, stop and explain instead of trying a third approach.
- Match the existing code style. Don't run formatters or tools that aren't installed in the project.

## Known traps
- Spring Boot 4 moved test annotations: `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`
  and `...webmvc.test.autoconfigure.AutoConfigureMockMvc`. Older imports don't compile.
- Spring Boot 4 uses Jackson 3 (`tools.jackson.*`). In tests, build JSON as strings and read values
  with `com.jayway.jsonpath.JsonPath` rather than an ObjectMapper.
- DevTools restarts on recompiled classes only. After changing application.properties, restart the app.
- H2 is in memory: data is gone after every restart. Put demo data in `src/main/resources/data.sql`.
- PATCH requests with optional fields: `@NotBlank` also rejects a missing field. Use
  `@Pattern(regexp = "(?s).*\\S.*")` when a field is optional but must not be blank if sent.
- A loaded entity changed inside a `@Transactional` service method is saved automatically; no save() needed.
- Integration tests: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Transactional` rolls back each test.
