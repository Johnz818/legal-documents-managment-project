# Decisions

## 2026-07-19

### D-001

Single organization for MVP.

Reserve organization_id for future multi-tenancy.

---

### D-002

Case stages

- `PENDING_FILING` — 待立案
- `PRE_TRIAL_PREPARATION` — 审理准备
- `IN_TRIAL` — 审理中
- `JUDGMENT_PENDING_APPEAL` — 已判决(上诉期内)
- `APPEAL_IN_PROGRESS` — 上诉审理中
- `FINAL_JUDGMENT` — 已判决(生效)
- `IN_ENFORCEMENT` — 执行中
- `CLOSED` — 已结案

`CLOSED` remains available for existing records and cases closed without a more specific lifecycle classification. Existing `CLOSED` records are not rewritten speculatively.

Archived is not a Case status. It remains the independent state defined by D-003.

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

Case number is stored as a string and remains exactly unique. Its lookup and future structured-filtering strategy is defined in D-005.

Participant snapshot storage and the deferred supporting-member relationship are defined in D-006.

Tags are also collections and require a separate relationship model in a future ticket.

---

## 2026-07-24

### D-005

Case number remains the original, exactly unique business identifier.

The business format is:

```text
(收案年度) + 法院代字 + 类型代字 + 案件编号 + 号
```

Examples:

- `(2016)最高法刑123号`
- `(2016)浙01民初1号`

Do not prematurely split the year, court code, case type, or sequence number into separate database columns. Current requirements need the official case number for display and basic lookup, and the complete value is the business identifier.

For current lookup use cases, prefer:

- exact matching;
- prefix matching;
- structured filtering when structured fields exist.

Avoid arbitrary contains queries such as:

```sql
LIKE '%keyword%'
```

If filtering by filing year, court code, or case type becomes a requirement:

1. Add explicit structured fields.
2. Backfill existing case records.
3. Add indexes based on demonstrated query patterns.

Elasticsearch is deferred. Evaluate it only if future requirements introduce full-text or fuzzy search needs.

---

### D-006

The MVP Case participant model uses scalar snapshots:

- lead lawyer is stored as a snapshot string;
- plaintiff or applicant is stored as one required Case field;
- defendant or respondent is stored as one required Case field.

Do not add a `supportingMembers` column or store supporting members as JSON, comma-separated text, or another delimited scalar value.

After the User domain is finalized, supporting members must be modeled through a separate relationship table.

---

### D-007

MySQL owns current Case filtering and sorting.

Introduce indexes only in response to actual query patterns. Do not introduce Elasticsearch for:

- basic filtering;
- sorting;
- exact or prefix lookup.

Consider Elasticsearch in the future only for requirements such as:

- full-text search;
- fuzzy matching;
- relevance ranking;
- large-scale document search.

---

### D-008

Case List and Case Detail are independent read-only API capabilities.

- Case List returns the latest non-archived cases for reusable list use cases.
- Case Detail retrieves one case by its current numeric identifier.
- Both APIs return dedicated response models rather than exposing the JPA entity.
- The frontend uses API contract types that remain separate from legacy mock models.

The current Case APIs expose scalar case data only. Tags, supporting members, related documents, and related reminders must not be inferred from legacy Case mock data.

Documents and reminders may remain mock-backed in their existing frontend sections until their domains are implemented. Tags and supporting members remain deferred until their relationship models are defined.

Authorization for Case Detail access will be introduced with the future authentication and authorization capability.

---

### D-009

Deliver the remaining Case capabilities as incremental user-journey slices.

The planned sequence is:

1. Add Case lookup and filtering using the rules in D-005 and D-007, then connect the Case List filters to that capability.
2. Add the Case creation workflow using the approved core Case model in D-004.
3. Finalize the User domain before introducing supporting-member relationships, then introduce Case tags through a separate relationship model.
4. Replace mock-backed Case documents and reminders after their respective domain and API contracts are approved.
5. Introduce authentication and authorization before treating Case access as customer-secured.

Each slice must preserve the separation between API contract models, persistence entities, and legacy frontend mock models.

---

### D-010

Case creation and case-document upload are separate backend capabilities.

- Create the scalar Case record first.
- Upload PDF or Word documents only after the Case has a persistent identifier.
- Do not combine Case JSON fields and binary document content into one persistence transaction.

The frontend may later present both operations as one user journey. File storage, metadata, size limits, content validation, malware scanning, and download authorization must be decided in the future case-document ticket.

---

## Future considerations (TODO)

- Evaluate Flyway compatibility with MySQL 9.7.
- Define an index strategy based on future query patterns.
- Define an optimistic locking strategy before supporting concurrent editing.
- Define a timezone policy for persistence timestamps.
