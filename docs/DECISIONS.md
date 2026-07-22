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

## Future considerations (TODO)

- Evaluate Flyway compatibility with MySQL 9.7.
- Define an index strategy based on future query patterns.
- Define an optimistic locking strategy before supporting concurrent editing.
- Define a timezone policy for persistence timestamps.
