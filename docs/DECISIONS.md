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

## 2026-07-25

### D-011

Case updates use full replacement of the editable scalar Case fields through `PUT /api/cases/{id}`.

- The client must send the version returned by Case Detail.
- JPA optimistic locking owns version increments.
- An update based on a stale version returns a conflict instead of silently overwriting newer changes.
- The Case ID, creation and update timestamps, and archived state remain server-owned.
- Case-number corrections are allowed but exact uniqueness remains enforced.

Authorization for Case updates is deferred until the authentication and authorization capability is introduced. The backend, rather than frontend visibility alone, must ultimately enforce update permission.

---

### D-012

Customer-facing Case removal uses a reversible archive/restore workflow.

- No general physical-delete endpoint is exposed.
- Active cases remain the default Case List.
- Archived cases are explicitly discoverable and restorable.
- Archive and restore operations participate in optimistic locking.
- Permanent purge is deferred until legal retention, audit, related-document cleanup, and administrative authorization requirements are defined.

---

## 2026-07-26

### D-013

Backend line coverage must be at least 90% before CI accepts a change.

The coverage gate applies to application-owned production code. Tests, generated
code, and framework bootstrap or declarative configuration with no meaningful
branching may be excluded only through an explicit, reviewed build configuration.
Packages must not be excluded merely to make the threshold pass.

Coverage is a regression guard rather than a substitute for behavior-focused
tests. Persistence and HTTP behavior that depends on MySQL must continue to be
verified against the dedicated Flyway-managed MySQL test database.

The frontend must gain unit and component coverage before a percentage threshold
is selected. Its initial gate is a clean type check, test run, and production
build.

---

### D-014

Case document metadata is stored in MySQL, while file content is stored through
a storage abstraction outside the relational database.

The initial upload capability supports PDF, DOC, and DOCX. Validation must use:

- an allowlist of supported extensions;
- an allowlist of accepted media types;
- server-side file-signature or content inspection.

The original filename and client-provided media type are untrusted metadata.
Storage keys are generated by the backend and must not contain user-controlled
paths. Configured upload-size limits must be enforced by the backend.

Local development may use filesystem storage outside publicly served
directories. Production storage must use an S3-compatible implementation behind
the same application-owned storage contract.

---

### D-015

Document binary storage and metadata persistence cannot form one atomic database
transaction.

For uploads:

1. Validate the request and referenced Case.
2. Store the binary under a backend-generated key.
3. Persist its metadata.
4. If metadata persistence fails, make a best-effort removal of the newly stored
   binary and report the upload as failed.

Cleanup failure must be logged without exposing credentials or sensitive file
content. A future reconciliation process may detect and remove orphaned objects
if operational evidence shows it is necessary.

---

### D-016

Minimum authentication and backend authorization must precede a publicly
accessible deployment containing customer or realistic legal-case data.

An earlier public demonstration is allowed only with synthetic data and with
unauthenticated mutation operations disabled or otherwise access-restricted.
Frontend control visibility is not an authorization boundary.

Authorization must ultimately protect Case creation, update, archive, restore,
and document upload/download operations according to the approved role model.

---

### D-017

Malware scanning is not part of the first document-upload slice, but it is a
known production-readiness requirement. Until scanning is implemented, uploaded
documents must not be treated as trusted content and must not be executed or
rendered by the backend.

General retention-controlled document purge remains deferred. Its design
depends on legal retention, audit, authorization, Case purge, and object-storage
lifecycle rules. D-020 separately defines customer removal of an individual
Case document. Archive behavior must not silently delete associated document
binaries.

---

## 2026-07-28

### D-018

Status: Accepted

The Document domain owns Case-related file metadata and binary-storage
coordination. The Case domain owns Case identity and lifecycle but does not own
document binaries, storage keys, templates, or generation workflow state.

For the current model:

- one Case may own many Case documents;
- every Case document belongs to exactly one Case;
- sharing one document across multiple Cases is not supported;
- the relationship is represented by a required `case_id`, not a many-to-many
  join table or a bidirectional Case entity collection;
- document templates are independent reusable definitions and are not Case
  documents.

The initial Case file-management capability supports uploading PDF, DOC, and
DOCX. Image uploads are deferred until their exact formats, validation, preview,
and security requirements justify the additional scope.

Case file management includes upload, metadata persistence, listing, download,
and Case Detail integration. It does not include template-based generation,
browser editing, PDF conversion, OCR, or AI processing.

---

### D-019

Status: Accepted

The initial template-generation model uses one DOCX source file per immutable
template version. A template has one intended generated format in this phase:
DOCX. Versioning preserves the exact template used by existing generation
records; it does not represent multiple output formats.

`DocumentGenerationService` owns the application workflow:

1. Load the Case and selected template version.
2. Resolve approved Case values and explicit user input.
3. Validate required structured fields.
4. Invoke `DocumentTemplateRenderer`.
5. Store the generated binary through `DocumentStorage`.
6. Persist the Case document metadata and generation record.
7. Perform best-effort binary cleanup if later persistence fails.

`DocumentTemplateRenderer` performs rendering only. It must not load Cases,
authorize users, persist metadata, or access local-filesystem or S3-specific
APIs.

The first generation capability supports structured scalar placeholders, DOCX
draft output, and explicit human finalization. It does not support arbitrary
expressions, conditions, repeating collections, browser-based Word editing, or
automatic finalization.

DOCX-to-PDF conversion, reviewed-document replacement, image support, OCR, and
AI-assisted extraction are deferred. Future AI output is advisory and must
remain subject to explicit human review before document finalization.

---

## 2026-07-29

### D-020

Status: Accepted

A user who is authorized to edit a Case may permanently remove an individual
document belonging to that Case. Permission is based on Case edit authority,
not whether the document source is `UPLOADED` or `GENERATED`. Until
authentication and authorization are implemented, this endpoint has the same
temporary unauthenticated local-development boundary as the existing Case and
document mutation APIs. Backend authorization is required before customer-data
deployment.

Removal deletes both document metadata and binary content. The application uses
transactional best-effort coordination:

1. Find the document by both Case ID and document ID.
2. Delete and flush its metadata inside a MySQL transaction.
3. Remove its binary through the idempotent `DocumentStorage` contract.
4. Propagate storage failure so the metadata transaction rolls back.

MySQL and file or object storage do not share an atomic transaction. A database
commit failure or process crash after binary removal can still leave restored
metadata whose binary is absent. Do not introduce two-phase commit. Before
production storage contains customer legal files, evaluate a durable,
retryable deletion workflow using explicit deletion state and a transactional
outbox or reconciliation process.

Document-removal audit history is deferred until the User domain provides a
real actor identity. The future audit record should identify the actor, Case,
document, original filename, source, removal time, and optional reason without
requiring retention of the deleted binary.

---

### D-021

Status: Accepted

The backend is packaged as a multi-stage container image. A Maven and Java 21
builder compiles the Spring Boot executable JAR; the final image contains a
Java 21 JRE and the JAR but not Maven, source code, test reports, local
credentials, or document content. Backend tests and the coverage gate run
outside the image build against the dedicated MySQL test database.

The runtime process uses numeric UID and GID `10001:10001` rather than root.
The image documents internal port 8080, while host, Compose, load-balancer, and
cloud routing remain runtime concerns. Runtime environments may override the
Spring port without rebuilding the image.

Database credentials and endpoints remain runtime-injected configuration.
Local filesystem document storage uses `/app/data/documents`, which must be
backed by a persistent volume whenever local storage is used. This local volume
supports development and single-instance verification only; production and
horizontally scaled deployments still require the planned S3-compatible
storage adapter.

The Docker build uses a BuildKit cache for Maven dependencies. Base images are
selected from the official Maven and Eclipse Temurin Java 21 image families.
Digest pinning, automated base-image updates, image vulnerability scanning, and
publishing are deferred to the CI and release-image tickets.

---

## Future considerations (TODO)

- Evaluate Flyway compatibility with MySQL 9.7.
- Define an index strategy based on future query patterns.
- Define a timezone policy for persistence timestamps.
- After the User domain is finalized, replace the temporary lead-lawyer dropdown with a searchable system-user selector. The backend must validate that the selected user exists, is active, and is eligible to lead cases; decide the user relationship and retained lawyer-name snapshot together with the supporting-member model.
- Define a legally appropriate retention and restricted permanent-purge policy, including related-document cleanup.
- Select a malware-scanning approach and failure/quarantine policy before accepting untrusted production uploads.
- Define document-object reconciliation and orphan cleanup if operational evidence requires it.
- During the runtime-configuration/deployment phase, replace the frontend's
  hardcoded local backend URL with environment-based API configuration and
  evaluate same-origin `/api` proxying. Until then, the localhost URL remains an
  intentional local-development constraint; public frontend environment values
  must never contain secrets.
- During the runtime-configuration/deployment phase, replace the backend's
  hardcoded `http://localhost:4321` CORS origin with environment-based allowed
  origins. Keep CORS handling centralized in one early servlet filter, validate
  configured origins at startup, and do not use a permissive wildcard origin.
  When authentication is introduced, review allowed origins, credentials,
  cookies, and CSRF protection together. Until then, the hardcoded origin
  remains an intentional local-development setting.
