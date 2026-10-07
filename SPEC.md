# SPEC: be-interview-prep

## Goal
Show an interviewer five small, well-tested Spring Boot REST features (tasks, URL shortener, JWT auth,
cached product catalog, concurrency-safe orders), each delivered as its own branch, PR and merge.

## Ground rules (from the brief, these override CLAUDE.md where they differ)
- Spring Boot **3.5.x** (downgraded from 4.1.1), Java 21+ release level. Test imports go back to
  `org.springframework.boot.test.autoconfigure.web.servlet.*`; Jackson 2.
- Public GitHub repo `be-interview-prep`. `main` gets the base project + README first.
- One branch per question, created from the latest `main`, in order:
  `feature/q1-task-api`, `feature/q2-urlshortener`, `feature/q3-auth`, `feature/q4-product-catalog`,
  `feature/q5-order-service`. Small meaningful commits. PR body uses the template
  (Problem / Approach / Decisions & trade-offs / How to test). Merge, pull `main`, next.
- README: how to run the app and tests, table of question -> PR link (updated after each merge).
- Approved new dependencies: `spring-boot-starter-security`, `io.jsonwebtoken:jjwt-*`,
  `spring-boot-starter-cache` (default in-memory cache).
- Time box: Q1 15m, Q2 15m, Q3 25m, Q4 25m, Q5 40m. Video is out of scope.

## Must-haves
1. **Q1 Task Manager API**. Done when: CRUD + `?status=` filter work; invalid input -> 400 with
   per-field messages; unknown id -> 404; any error (incl. unexpected 500) uses the same problem JSON.
2. **Q2 URL Shortener**. Done when: POST returns code + short URL; `/r/{code}` redirects (302) and
   increments the count atomically; stats endpoint works; invalid URL -> 400, unknown code -> 404,
   expired code -> 410.
3. **Q3 Authentication & Roles**. Done when: register/login return a 15-minute JWT; BCrypt passwords;
   no session; `/api/users/me` for any user, `/api/users` ADMIN only; missing/invalid token -> 401
   JSON, wrong role -> 403 JSON; duplicate email -> 409; JWT secret and admin credentials come from
   environment variables.
4. **Q4 Product Catalog**. Done when: 100 seeded products; paged, sortable, combinable filters;
   size capped at 100; single lookups cached and evicted on update/delete; a test proves the second
   lookup does not hit the repository.
5. **Q5 Order Service**. Done when: multi-item orders are all-or-nothing; 50 concurrent orders on
   stock 10 -> exactly 10 succeed and stock is 0; same `Idempotency-Key` -> one order; insufficient
   stock -> 409 naming the product; cancel restores stock (cancel twice -> 409).

## Nice-to-haves (ranked, only after all five are merged)
1. Q1: Swagger UI is already there via springdoc; add `@Operation` summaries.
2. Q2: optional custom short code (409 if taken).
3. Q5: second concurrency approach (pessimistic `SELECT ... FOR UPDATE`) + short comparison in README.
4. Q3: refresh token + logout that revokes it.
5. Q4: shared cache (Redis) for multiple instances.

## Out of scope
Video/YouTube, frontend, email verification, password reset, payment, real database (H2 only),
Docker, rate limiting, editing orders.

## Data model
**Task**: id Long PK; title String not blank, max 100; description String nullable, max 1000;
status enum TODO / IN_PROGRESS / DONE (default TODO); dueDate LocalDate nullable, today or later;
createdAt Instant set by server.

**ShortLink**: id Long PK; code String unique, max 8, `[A-Za-z0-9]`; originalUrl String max 2048,
http/https only; expiresAt Instant nullable, must be in the future; visitCount long default 0;
createdAt Instant.

**AppUser**: id Long PK; email String unique, valid email; passwordHash String (BCrypt);
role enum USER / ADMIN; createdAt Instant.

**Product** (shared by Q4 and Q5): id Long PK; name String not blank max 100; category String not
blank; price BigDecimal >= 0.01; stock int >= 0; rating double 0..5; createdAt Instant.

**CustomerOrder**: id Long PK; idempotencyKey String unique, max 64; status enum PLACED / CANCELLED;
createdAt Instant; items one-to-many **OrderItem** (id, product many-to-one, quantity int >= 1).

## Endpoints
| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| POST | /api/tasks | title, description, status, dueDate | 201 | 400 |
| GET | /api/tasks?status= | | 200 | 400 (bad status) |
| GET | /api/tasks/{id} | | 200 | 404 |
| PUT | /api/tasks/{id} | title, description, status, dueDate | 200 | 400, 404 |
| DELETE | /api/tasks/{id} | | 204 | 404 |
| POST | /api/links | url, expiresAt? | 201 {code, shortUrl, ...} | 400 |
| GET | /r/{code} | | 302 Location | 404, 410 |
| GET | /api/links/{code}/stats | | 200 {originalUrl, visitCount, createdAt, expiresAt} | 404 |
| POST | /api/auth/register | email, password (min 8) | 201 user | 400, 409 |
| POST | /api/auth/login | email, password | 200 {token, expiresIn: 900} | 400, 401 |
| GET | /api/users/me | | 200 | 401 |
| GET | /api/users | | 200 list | 401, 403 |
| GET | /api/products?page&size&sort&category&minPrice&maxPrice&inStock&q | | 200 {content, page, size, totalElements, totalPages} | 400 (bad sort field, minPrice > maxPrice) |
| GET | /api/products/{id} | | 200 | 404 |
| POST | /api/products | name, category, price, stock, rating | 201 | 400 |
| PUT | /api/products/{id} | same | 200 | 400, 404 |
| DELETE | /api/products/{id} | | 204 | 404 |
| POST | /api/orders (header `Idempotency-Key`) | items: [{productId, quantity}] | 201 new / 200 replay | 400, 404 (product), 409 (stock) |
| GET | /api/orders/{id} | | 200 | 404 |
| POST | /api/orders/{id}/cancel | | 200 | 404, 409 (already cancelled) |

After Q3, every `/api/**` endpoint except `/api/auth/**` requires a token; `/r/**`, Swagger UI and the
H2 console stay public. The H2 console is off unless `H2_CONSOLE_ENABLED=true` (it would let anyone
edit the database, including roles). Earlier tests get `@WithMockUser`.

## Business rules
- Errors: always `{"status","title","detail"}` (+ `errors` per field for validation), including a
  catch-all 500 that hides internals.
- Q1: dueDate cannot be before today (`@FutureOrPresent`), checked on create and update.
- Q2: shortening the same URL twice creates **two different codes**: each link has its own expiry
  and stats, and there is no find-then-insert race. Codes: 7 random Base62 chars from SecureRandom
  (62^7 ≈ 3.5e12), retry on the rare collision (unique constraint). Visit count uses one atomic
  `UPDATE ... SET visit_count = visit_count + 1 WHERE code = ?` so concurrent visits never lose
  increments. Expired = `expiresAt <= now` -> 410 Gone, no count.
- Q3: register always creates USER; an ADMIN is seeded at startup only if `ADMIN_EMAIL` and
  `ADMIN_PASSWORD` env vars are set. JWT (HS256) signed with `JWT_SECRET` env var, `exp` = 15 min,
  subject = email, role claim. Login with wrong credentials -> 401 with no hint which part was wrong.
  Custom `AuthenticationEntryPoint` (401) and `AccessDeniedHandler` (403) write problem JSON.
- Q4: page size > 100 is clamped to 100; sort field must be one of the Product fields, else 400.
  Filters built with JPA Specifications so any combination is one query. `@Cacheable("products")`
  on get-by-id, `@CacheEvict` on update and delete (evict, not `@CachePut`, so a rolled-back update can't
  leave an uncommitted value in the cache; the cache proxy runs outside the transaction so eviction
  happens after commit), and on stock changes from Q5. Category filter is an exact match so its index
  can be used.
- Q5: in one transaction, for each item (sorted by productId to avoid deadlocks) run
  `UPDATE product SET stock = stock - :q WHERE id = :id AND stock >= :q`; 0 rows updated -> throw
  409, which rolls back every earlier decrement. Idempotency: client sends `Idempotency-Key` header
  (required, 400 if missing); key stored with a unique constraint; a repeat returns the existing
  order with 200; a concurrent duplicate that hits the constraint is caught and the existing order
  returned. Cancel adds each quantity back and sets CANCELLED; cancelling a cancelled order -> 409.

## Build order
0. `main`: downgrade to Boot 3.5.x, README, create public repo, push.
1. Q1 tasks -> PR -> merge.
2. Q2 links -> PR -> merge.
3. Q3 auth (security config, JWT filter, users) + `@WithMockUser` on earlier tests -> PR -> merge.
4. Q4 products (entity, seeder, specs, paging, cache) -> PR -> merge.
5. Q5 orders (atomic decrement, idempotency, cancel, 50-thread test) -> PR -> merge.

## Demo script (Swagger UI)
1. `POST /api/auth/register`, then `POST /api/auth/login`; paste the token into **Authorize**.
2. `GET /api/users` -> 403 as USER; log in as the env-seeded admin -> 200.
3. `POST /api/tasks` with a past dueDate and empty title -> 400 with two field errors; valid -> 201;
   `GET /api/tasks?status=TODO`; `GET /api/tasks/999` -> 404.
4. `POST /api/links` with `https://spring.io` -> open `/r/{code}` in the browser twice ->
   `GET /api/links/{code}/stats` shows 2.
5. `GET /api/products?category=Books&minPrice=10&inStock=true&q=pro&sort=price,desc&size=500`
   -> size 100, totals shown. `GET /api/products/1` twice: SQL log shows one select.
6. `POST /api/orders` with `Idempotency-Key: abc` -> 201; same call again -> 200, same id;
   order more than the stock -> 409; `POST /api/orders/{id}/cancel` -> stock restored.
