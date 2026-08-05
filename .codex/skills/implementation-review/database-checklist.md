# Database Implementation Review Checklist

## Purpose

Review database-related implementation changes after coding is complete.

The goal is to verify:

- Database changes match the approved implementation plan.
- Schema changes preserve data integrity.
- Persistence behavior is correct.
- Migrations are safe to apply.

This review is not for redesigning the database architecture.

---

# Database Review Scope

Apply this checklist when implementation includes:

- database migrations;
- schema changes;
- entity changes;
- repository changes;
- persistence logic;
- transaction-related database operations.

---

# 1. Migration Review

Review:

- Migration files.
- Migration ordering.
- Schema changes.
- Upgrade behavior.

Verify:

- Migration follows existing migration conventions.
- Migration runs successfully on a clean database.
- Migration can be applied to an existing database when required.
- Migration does not unexpectedly modify existing data.

Check for:

- Incorrect table creation.
- Missing columns.
- Incorrect column types.
- Unsafe column changes.
- Missing rollback consideration when applicable.

---

# 2. Schema Design Review

Review:

- Tables.
- Columns.
- Constraints.
- Relationships.

Verify:

- Schema matches domain requirements.
- Column types represent business meaning correctly.
- Nullability decisions are intentional.
- Default values are intentional.
- Naming follows project conventions.

Check:

- Are required fields enforced?
- Can invalid states be represented?
- Are relationships correctly modeled?

Examples of concerns:

- A required business identifier allows NULL.
- A unique business key has no unique constraint.
- A lifecycle state has no validation protection.

---

# 3. Data Integrity Review

Review:

- Primary keys.
- Foreign keys.
- Unique constraints.
- Check constraints.
- Referential behavior.

Verify:

- Database constraints protect important invariants.
- Invalid data cannot be persisted accidentally.
- Relationships remain consistent.

Check:

- Duplicate records.
- Orphan records.
- Invalid references.
- Incorrect cascade behavior.

---

# 4. Entity and ORM Mapping Review

Review:

- Entity definitions.
- ORM annotations/configuration.
- Repository queries.

Verify:

- Entity mapping matches database schema.
- Relationship mappings are correct.
- Lazy/eager loading behavior is intentional.
- Generated queries are reasonable.

Check:

- Incorrect column mapping.
- Missing relationship configuration.
- Unexpected additional queries.
- N+1 query problems when relevant.

---

# 5. Query and Persistence Review

Review:

- Repository methods.
- Query implementation.
- Data access patterns.

Verify:

- Queries retrieve the intended data.
- Queries handle expected data volume.
- Persistence operations are correct.

Check:

- Missing filters.
- Incorrect joins.
- Inefficient queries.
- Loading unnecessary data.

Do not request optimization without evidence.

---

# 6. Index Review

Review:

- Added indexes.
- Removed indexes.
- Index usage.

Verify:

- Indexes support actual query patterns.
- Indexes have a clear reason.

Avoid:

- Adding indexes speculatively.
- Optimizing without query evidence.

Consider:

- Frequently filtered columns.
- Sorting requirements.
- Unique lookup requirements.

---

# 7. Transaction and Consistency Review

Review:

- Database transaction boundaries.
- Multiple related writes.
- Failure handling.

Verify:

- Related database changes are atomic when required.
- Partial updates cannot create invalid states.
- Transaction boundaries match business operations.

Consider:

- Multiple tables updated together.
- Version creation.
- Status transitions.
- Audit records.

Check:

- Is @Transactional placed at the correct boundary?
- Are external operations incorrectly assumed to rollback?

---

# 8. Concurrency Review

Review:

- Concurrent database updates.
- Race conditions.
- Locking behavior.

Verify:

- Important invariants remain true under concurrent access.
- Duplicate creation is prevented.
- Version numbers or counters are safe.

Consider:

- Unique constraint conflicts.
- Optimistic locking.
- Pessimistic locking.
- Retry behavior.

---

# 9. Data Migration Safety Review

Apply when existing data is affected.

Review:

- Data transformation logic.
- Backfill operations.
- Existing record compatibility.

Verify:

- Existing data remains valid.
- Migration handles large datasets safely.
- Partial migration failure is considered.

Check:

- Long-running migrations.
- Locking impact.
- Data loss risk.

---

# 10. Testing Review

Review:

- Migration tests.
- Repository tests.
- Integration tests.

Verify:

- Schema creation is tested.
- Persistence behavior is verified.
- Important constraints are tested.
- Failure scenarios are covered.

Required scenarios may include:

- Valid insert/update.
- Invalid data rejection.
- Constraint violation.
- Migration execution.
- Transaction rollback behavior.

---

# 11. Database Security Review

Review:

- Sensitive data storage.
- Access patterns.
- Query safety.

Verify:

- Sensitive information is protected.
- Credentials are not stored incorrectly.
- User input cannot manipulate queries.

Check:

- SQL injection risks