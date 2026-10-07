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

### Quality checks
`.\mvnw.cmd verify` runs everything CI runs on each pull request:

| Check | Tool | Fix or report |
|---|---|---|
| Tests + coverage floor (90% line, 75% branch) | JUnit, JaCoCo | `target/site/jacoco/index.html` |
| Formatting of changed files | Spotless (palantir-java-format) | `.\mvnw.cmd spotless:apply` |
| Bugs and security issues | SpotBugs + FindSecBugs | false positives go in `spotbugs-exclude.xml`, with a reason |
| Secrets in git history | gitleaks (CI only) | |

Optional pre-commit hook that checks formatting: `git config core.hooksPath .githooks`.

## Questions
| # | Question | PR link |
|---|---|---|
| 1 | Task Manager API | [#1](https://github.com/NanduEds24/be-interview-prep/pull/1) |
| 2 | URL Shortener | [#2](https://github.com/NanduEds24/be-interview-prep/pull/2) |
| 3 | Authentication & Roles | [#3](https://github.com/NanduEds24/be-interview-prep/pull/3) |
| 4 | Product Catalog | [#5](https://github.com/NanduEds24/be-interview-prep/pull/5) |
| 5 | Order Service | [#7](https://github.com/NanduEds24/be-interview-prep/pull/7) |

### Follow-up PRs
How PRs were checked changed during the build:
- #1–#3 were merged with no review and no CI. They were reviewed with `/code-review` afterwards; each has a
  comment listing the findings and how they were resolved (fixes in #4 and #10).
- From #4 on, the project rule (CLAUDE.md) is to run `/code-review` before merging. CI exists from #6 on;
  #4 and #5 were merged before it.
- From #7 on, PRs were merged only with green CI. This is a project rule, not a GitHub branch protection
  setting.

| PR | What it does |
|---|---|
| [#4](https://github.com/NanduEds24/be-interview-prep/pull/4) | Fixes from a first review of Q1–Q3 (H2 console off by default, BCrypt 72-byte limit, `.claude/` skills restored to the repo) and the review-before-merge rule |
| [#6](https://github.com/NanduEds24/be-interview-prep/pull/6) | CI on every PR (build and tests on JDK 21 and 25, gitleaks secret scan) and Dependabot |
| [#10](https://github.com/NanduEds24/be-interview-prep/pull/10) | Fixes from the `/code-review` of Q1–Q3 (#1, #2, #3) |
