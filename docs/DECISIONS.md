# Decisions

## 2026-07-19

### D-001

Single organization for MVP.

Reserve organization_id for future multi-tenancy.

---

### D-002

Case stages

- 待立案
- 审理准备
- 审理中
- 已结案

---

### D-003

No delete.

Archive only.

---

## 2026-07-23

### D-004

The core Case persistence model requires:

- case number
- case name
- status
- one plaintiff or applicant
- one defendant or respondent
- one lead lawyer snapshot
- created and updated timestamps
- archived state

The following core fields are optional so an incomplete or pre-filing case can be recorded:

- court name
- case cause
- filing date
- hearing date
- judgment date
- description

Case number is stored as a string and remains exactly unique. Query use cases may support matching by part of a case number without changing its persisted type or uniqueness rule.

Supporting members are intentionally deferred from the MVP Case model. A future implementation must use a separate relationship table rather than JSON, delimited text, or additional columns in `cases`. The final relationship model depends on the future User domain.

Tags are also collections and require a separate relationship model in a future ticket.

---

## Future considerations (TODO)

- Evaluate Flyway compatibility with MySQL 9.7.
- Define an index strategy based on future query patterns.
- Define an optimistic locking strategy before supporting concurrent editing.
- Define a timezone policy for persistence timestamps.
