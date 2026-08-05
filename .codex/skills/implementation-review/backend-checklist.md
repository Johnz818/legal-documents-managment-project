# Backend Implementation Review Checklist

## Purpose

Review completed backend implementation after coding.

The goal is to verify:

- The implementation matches the approved implementation plan.
- Backend behavior is correct and reliable.
- Existing domain rules and architecture decisions are respected.
- The implementation is safe to merge.

This review is not for redesigning the architecture.

---

# Backend Review Scope

Apply this checklist when backend code, API, database, migration, or backend configuration is modified.

---

# 1. API Contract Review

Review:

- Endpoint path correctness.
- HTTP method correctness.
- Request and response schema consistency.
- Request validation behavior.
- HTTP status code correctness.
- Error response consistency.

Verify:

- The API behavior matches the approved contract.
- Frontend assumptions and backend behavior are aligned.
- Invalid requests cannot silently succeed.
- Error cases provide actionable information without exposing internal details.

---

# 2. Business Logic Review

Review:

- Domain rule implementation.
- Business validation.
- Entity lifecycle handling.
- State transition correctness.
- Domain invariant preservation.

Verify:

- Invalid domain states cannot be created.
- Business rules are enforced in backend rather than only frontend.
- The implementation follows DOCUMENT_DOMAIN.md and DECISIONS.md.

Examples of concerns:

- An immutable entity can still be modified.
- A published object can return to an invalid state.
- Required relationships are not enforced.

---

# 3. Database and Persistence Review

Review:

- Database migrations.
- Entity mapping.
- Repository implementation.
- Constraints.
- Indexes.
- Data access patterns.

Verify:

- Database schema matches domain requirements.
- Constraints prevent invalid data.
- Migration is safe for existing environments.
- Queries are reasonable for expected usage.

Check for:

- Missing unique constraints.
- Incorrect nullability.
- Missing foreign key relationships.
- Unnecessary indexes.
- Data migration risks.

---

# 4. Transaction and Consistency Review

Review:

- @Transactional usage.
- Transaction boundaries.
- Rollback behavior.
- Multi-step operations.

Verify:

- Related database operations are atomic when required.
- Failure scenarios do not leave inconsistent database state.
- External systems are not incorrectly assumed to participate in the transaction.

Consider:

- Database writes.
- File storage operations.
- External API calls.
- Message publishing.

Confirm whether compensation logic is required.

---

# 5. Concurrency Review

Review:

- Race condition risks.
- Concurrent updates.
- Locking strategy.
- Idempotency.

Verify:

- Important invariants are protected under concurrent requests.
- Unique constraints or locking mechanisms exist where required.
- Implementation does not depend on timing assumptions.

Consider:

- Duplicate requests.
- Concurrent updates.
- Retry behavior.

---

# 6. Security Review

Review:

- Authentication.
- Authorization.
- Input validation.
- File handling.
- Sensitive data handling.

Verify:

- Users cannot access unauthorized resources.
- User input is validated.
- Uploaded content is handled safely.
- Credentials and secrets are not exposed.
- Error messages do not leak internal information.

Check for:

- SQL injection risks.
- Path traversal risks.
- Missing permission checks.
- Unsafe deserialization.

---

# 7. Error Handling and Reliability Review

Review:

- Exception handling.
- Failure recovery.
- Logging behavior.
- Retry behavior.

Verify:

- Expected failures return meaningful errors.
- Unexpected failures do not silently corrupt state.
- Logs provide enough information for debugging.
- Internal implementation details are not exposed to clients.

---

# 8. Backend Testing Review

Review:

- Unit tests.
- Integration tests.
- API tests.
- Migration tests.

Verify:

- Main success path is tested.
- Important failure scenarios are tested.
- Tests validate behavior rather than only increasing coverage.
- Tests cover domain invariants.

Required verification should include:

- Happy path.
- Invalid input.
- Missing resources.
- Permission failures when applicable.
- Persistence behavior.
- Transaction failure scenarios when applicable.

---

# Backend Review Output

Provide:

## Verdict

Choose:

- APPROVED
- APPROVED WITH MINOR CHANGES
- NEEDS FIXES

## Summary

Explain:

- What was implemented.
- Whether it matches the approved plan.

## Findings

Classify each finding:

### Blocking

Issues that must be fixed before merge.

Include:

- Problem.
- Failure scenario.
- Required fix.

### Non-blocking

Improvements that do not prevent merge.

### Follow-up

Future improvements outside current scope.

## Verification

Include:

- Tests executed.
- Build status.
- Manual verification performed.

## Merge Recommendation

State whether the implementation is ready to merge.