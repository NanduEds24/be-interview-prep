---
name: verify
description: Check that the current API feature really works. Use after finishing each feature, before committing.
---

1. Run `.\mvnw.cmd test` and read the summary. Fix failures first. Each endpoint of the feature should
   have tests for success, 400 and 404 (and 409 where it applies).
2. Make sure the app is running on 8080. If the port is busy, check what holds it before starting
   another server; it may be the user's own.
3. Call each endpoint of the feature on the running app (Invoke-RestMethod or curl against
   http://localhost:8080/api/...): the happy path, one invalid request and one missing id. Check the
   status codes and that error bodies have `status`, `title` and `detail`.
4. Confirm the endpoints appear in http://localhost:8080/v3/api-docs (what Swagger UI shows).
5. Report what you checked and what you saw in 3 lines or fewer. Say plainly if anything failed or
   could not be checked.
