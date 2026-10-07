# Project

Timed build: a Java REST API built against the clock. There is no frontend; the API is the product
and Swagger UI is the demo. Scope lives in SPEC.md; read it before starting any feature (it is created
by /spec when the brief arrives).
The team is new to Java: after each change, explain what you did in 3 lines or fewer.

## Stack
- Spring Boot 3.5.16 (brief requires 3.x), Java 21 release on JDK 25, Maven wrapper, Spring Web MVC + Data JPA + H2 (in-memory) +
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
- Format changed files: `.\mvnw.cmd spotless:apply` (palantir-java-format; run before committing)
- All checks, as CI runs them: `.\mvnw.cmd verify` (tests, coverage floor 90% line / 75% branch,
  Spotless, SpotBugs + FindSecBugs). Coverage report: `target/site/jacoco/index.html`
- Pre-commit hook (once per clone): `git config core.hooksPath .githooks`
- Swagger UI (demo and manual testing): http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Database browser: off by default. Start with `$env:H2_CONSOLE_ENABLED="true"` to use
  http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:appdb`, user `sa`). Never enable it by default:
  it is public and gives full SQL access.

## How we work
- One feature at a time: entity, repository, service, controller, then tests.
- Every endpoint gets MockMvc tests for the success case, validation failure (400) and not found (404).
- Use proper REST status codes: 201 for create, 204 for delete, 400 for invalid input, 404 for missing,
  409 for conflicts.
- After each feature: run the tests, call the endpoints on the running app, then commit.
- Simplest thing that works. No new dependencies without asking.
- Git flow follows the brief: one branch per question (feature/qN-...), PR into main, merge, pull main. One-line commit messages. After committing, show `git log --oneline -1` as proof.
- Never merge a PR unreviewed: before merging, run `/code-review` on it, then fix each finding or
  write in the PR why it is accepted. Merge only after the tests pass again and all CI checks
  (Build and test on JDK 21 and 25, Secret scan) are green on the PR.
- If something fails twice, stop and explain instead of trying a third approach.
- Match the existing code style. Spotless is the only formatter: run `spotless:apply`, never another tool.
- A SpotBugs finding is fixed, or added to `spotbugs-exclude.xml` with a reason. Never lower the coverage
  floor to make a build pass.

## Known traps
- Spring Boot 3: test annotations are `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest`
  and `...web.servlet.AutoConfigureMockMvc`. Jackson 2 (`com.fasterxml.jackson.*`).
- In tests, build JSON as strings and read values with `com.jayway.jsonpath.JsonPath`.
- DevTools restarts on recompiled classes only. After changing application.properties, restart the app.
- H2 is in memory: data is gone after every restart. Put demo data in `src/main/resources/data.sql`.
- PATCH requests with optional fields: `@NotBlank` also rejects a missing field. Use
  `@Pattern(regexp = "(?s).*\\S.*")` when a field is optional but must not be blank if sent.
- A loaded entity changed inside a `@Transactional` service method is saved automatically; no save() needed.
- Integration tests: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Transactional` rolls back each test.
