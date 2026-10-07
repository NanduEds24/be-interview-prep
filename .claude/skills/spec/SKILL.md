---
name: spec
description: Turn a brief into SPEC.md for a REST API, with a ranked feature list. Use at the start of a timed build or when the brief changes.
---

1. Read the brief. Ask at most 3 questions, only where the answer changes what gets built.
2. Write SPEC.md with:
   - Goal: one sentence, the user and the problem.
   - Must-haves: 3 to 5 features, each with a one-line "done when", including what happens on
     invalid input, missing items and conflicts.
   - Nice-to-haves: ranked, each small (for example filtering, pagination, sorting, search).
   - Out of scope: what we deliberately won't build.
   - Data model: each entity with its fields, types, constraints and relationships.
   - Endpoints: a table of method, path, request body, success status, error statuses.
   - Business rules: the checks the service layer must enforce.
   - Build order: the must-haves as slices, smallest first.
   - Demo script: the Swagger UI calls that show it off, in order.
3. Stop and wait for approval. Don't write code.
