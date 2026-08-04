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

Status: Accepted; delivery scope clarified by D-028 and generation lifecycle
superseded by D-030

The initial template-generation model uses one DOCX source file per immutable
template version. A template has one intended generated format in this phase:
DOCX. Versioning preserves the exact template used by existing generation
records; it does not represent multiple output formats.

`DocumentGenerationService` owns the application workflow:

1. Load the Case and selected template version.
2. Resolve Case values and explicit user input.
3. Validate required structured fields.
4. Invoke `DocumentTemplateRenderer`.
5. Store the generated binary through `DocumentStorage`.
6. Persist the Case document metadata and generation record.
7. Perform best-effort binary cleanup if later persistence fails.

`DocumentTemplateRenderer` performs rendering only. It must not load Cases,
authorize users, persist metadata, or access local-filesystem or S3-specific
APIs.

The first generation capability supports structured scalar placeholders and
completed DOCX output from values reviewed before rendering. D-030 removes the
previous separate Phase 5 draft/finalization step because no document-specific
editing capability exists yet. It does not support arbitrary expressions,
conditions, repeating collections, or browser-based Word editing.

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

### D-022

Status: Accepted

The Astro frontend is packaged as a multi-stage container image. A Node and
pnpm builder performs a frozen-lockfile install and generates the static site;
the final image contains only the generated site and an unprivileged Nginx
runtime. Source code, Node, pnpm, dependencies, tests, coverage output, and
local environment files are excluded from the runtime image.

The frontend calls the relative `/api` path rather than embedding a backend
origin in browser JavaScript. During local development, Astro proxies `/api`
to the local backend. In the production-style container, Nginx serves static
files and proxies `/api` to the runtime-injected `LEGAL_API_UPSTREAM`. The same
frontend image can therefore target different backend locations without being
rebuilt.

Nginx runs as a non-root user and listens on internal port 8080. Host ports,
Compose service discovery, TLS termination, and public routing remain runtime
infrastructure concerns. The proxy permits enough multipart request overhead
for document uploads, while Spring remains authoritative for the 5 MB file
limit and document validation.

The same-origin proxy removes the need for CORS in the containerized browser
journey. Existing backend CORS configuration remains available for direct and
local-development access and will be externalized and reviewed with future
deployment and authentication configuration.

---

### D-023

Status: Accepted

The local Compose environment runs the frontend, backend, and MySQL as
replaceable containers on one private Compose network. Service names provide
internal discovery: the frontend proxies `/api` to `backend:8080`, and the
backend connects to `mysql:3306`. Only the frontend and backend publish host
ports; MySQL remains internal and does not conflict with or depend on the host
MySQL installation.

The Compose MySQL version matches local development at 9.7.1. Flyway remains
the only schema-management mechanism, and the backend starts only after the
MySQL container reports healthy.

Container replacement must not remove application data. MySQL stores its data
in the `mysql-data` named volume, and the current local filesystem storage
adapter stores document content in the independent `document-data` named
volume. Normal Compose shutdown preserves both volumes; removing volumes is an
explicit destructive reset.

An object-storage container is intentionally not part of the current Compose
environment because the backend does not yet have an S3-compatible
`DocumentStorage` adapter. Adding an unused MinIO service would not validate an
application path. Managed MySQL, S3-compatible storage, production secret
management, and any optional MinIO-based adapter environment remain deployment
or storage-adapter decisions.

Local Compose credentials are injected through an ignored `.env` file using
the names documented in `.env.example`. They are local-development
configuration, not production secret management.

---

### D-024

Status: Accepted

GitHub Actions reproduces the established backend and frontend verification
gates in independent parallel jobs on pull requests and pushes to `main`.

The backend job uses Java 21 and an ephemeral MySQL 9.7.1 service initialized
with explicit disposable test credentials. Maven verification applies the
Flyway migrations to `legal_case_management_test`, runs the unit and real-MySQL
integration tests, validates JPA mappings, and enforces the 90% JaCoCo line
coverage gate. The JaCoCo HTML report is retained briefly as a workflow
artifact.

The frontend job uses Node 22, the pnpm version declared in
`frontend/package.json`, a frozen-lockfile install, tests, Astro checking, and
the production build. Dependency caches may store Maven artifacts and the pnpm
package store, but must not cache application build output, `node_modules`, test
results, or databases.

The workflow has read-only repository permission and does not consume local,
staging, or production secrets. Actions are pinned to reviewed commit SHAs.
The MySQL credentials committed in the workflow are non-production values
whose authority and lifetime are limited to one disposable CI job.

CI1 became operational after its first hosted Backend and Frontend jobs passed.
The repository ruleset requires those registered checks for changes to `main`.

---

### D-025

Status: Accepted

GitHub Actions verifies the backend and frontend container images as
independent parallel checks on pull requests and pushes to `main`. CI builds
the same Dockerfiles and build contexts used by local development without
publishing images or authenticating to a registry.

Container verification covers only application-owned images. MySQL remains a
pinned vendor image and is already pulled, started, and exercised by CI1's
Flyway-managed integration tests; the repository does not build a custom
MySQL image.

CI2 intentionally verifies packaging rather than runtime orchestration. A
future automated Compose smoke test must be justified as a separate ticket and
must not introduce production credentials or persistent CI data. Build caching
remains deferred until measured build time justifies it.

Image publishing remains tracked as CI3 but is deferred until P1 approves the
deployment platform and container registry. CI3 must not begin without explicit
decisions for registry ownership, image names, immutable tags, deployment
consumers, supported architectures, credential management, retention, and the
required provenance, SBOM, or signing controls.

---

## 2026-08-01

### D-026

Status: Accepted; final-product direction clarified by D-028

The Document Template aggregate uses a stable `DocumentTemplate` identity with
immutable published `DocumentTemplateVersion` records. Each published version
owns the exact DOCX storage metadata, a content SHA-256 digest, and its field
contract. Versioning exists for generation traceability even when templates
change infrequently.

Editing a generated document for one Case does not modify its reusable template
and does not create a template version. A new version is created only by an
explicit reusable-template publication. If browser-based template authoring is
introduced later, editable working drafts remain separate from immutable
published versions.

Template fields are relational child records of one published version rather
than a JSON array. The initial field contract contains a stable key, display
name, optional description, scalar value type, required state, deterministic
default source, optional source key, and display order. This allows future
generation values and extraction provenance to reference an exact field and
keeps uniqueness, enum, and foreign-key guarantees in MySQL. The expected field
count is small, so an indexed one-to-many join is not a material performance
cost.

Do not introduce a second NoSQL database for template manifests. MySQL JSON
would be reasonable for an opaque manifest always loaded and validated as one
unit, but it would move field-level integrity and future references into Java
string-key validation without providing enough benefit for the approved
workflow.

The deterministic milestone identifies fields through explicit controlled DOCX
placeholder tokens. System and custom templates follow the same publication
contract: scan tokens, present detected fields, require their definitions to
match, then publish the immutable version. Existing visual templates that use
blank lines, formatting, `XXXX`, or prose annotations must be normalized before
publication. Scanning and rendering must support placeholders split across Word
formatting runs and placeholders inside supported tables.

AI is not a template-field source. Template definition, value acquisition, and
rendering remain separate responsibilities:

```text
Template definition -> declares the required values
Value acquisition   -> Case value, manual value, or extraction suggestion
Rendering           -> deterministically applies resolved values to DOCX
```

The initial deterministic model does not introduce a global semantic-field
catalog. A template field may bind directly to an unambiguous Case
field through `CASE_FIELD` and a source key such as `caseNumber`, `courtName`,
or `filingDate`; it may bind to a narrowly defined `SYSTEM_VALUE`; or it may
remain a template-local `USER_INPUT`. These bindings describe how the current
application acquires a value and must not be treated as a universal legal
ontology.

Before structured AI extraction begins, evaluate a narrow governed semantic
catalog for precise, reusable evidence-derived facts such as
`LOAN_PRINCIPAL_AMOUNT` or `LOAN_TRANSFER_DATE`. Do not create ambiguous global
definitions such as `PHONE`, `DATE`, or a generic legal-party role. Reusable
definitions must be owner- and meaning-qualified where necessary, while
contextual legal roles and wording may remain local to a template. Mapping a
template field to such a future semantic definition is optional. This layered
approach gives extraction a stable target without forcing every template field
into centralized governance or expanding the G1 persistence scope.

Future extracted generation values must record the suggestion, source document
and page, confidence, and human-review state. OCR or AI may suggest template
definitions during future template onboarding and may suggest values from Case
materials, but a human must approve the deterministic field contract and every
legal fact used for finalization.

The following designs are rejected or deferred for the current milestones:

- one mutable template row that loses historical generation traceability;
- event sourcing for the complete Template aggregate;
- arbitrary AI discovery as the runtime template contract;
- Word content controls, merge fields, and bookmarks as the initial marker
  mechanism;
- conditions, formulas, repeating collections, and automatic finalization.

At the time of this decision, the renderer library remained a decision gate and
required an Apache POI/docx4j comparison using representative DOCX fixtures.
D-029 records the resulting docx4j selection. G3 subsequently completed
automated and representative manual verification of supported rendering,
Chinese text, tables, images, and formatting preservation. Later extraction
milestones must separately evaluate PDFBox/Tika, Chinese
OCR options, background processing, and a provider-neutral structured AI
extraction boundary.

---

### D-027

Status: Accepted

G1 persists the Document Template aggregate in three relational tables:
`document_templates`, `document_template_versions`, and
`document_template_fields`.

Template identities use `PRESET` and `CUSTOM`. Published versions use a
positive number unique within their template, reference one globally unique
opaque storage key, and retain the original filename, content type, size,
lowercase SHA-256 digest, and publication timestamp. The digest verifies the
identity and integrity of the exact published DOCX bytes; it is not unique
because separate publications may intentionally use identical binary content.

The initial scalar field types are `TEXT`, `DATE`, `DECIMAL`, and `BOOLEAN`.
The deterministic source categories are `CASE_FIELD`, `SYSTEM_VALUE`, and
`USER_INPUT`. Case and system sources require a non-empty source key; user input
must not have one. At the time of D-027, the exact Case and system source-key
vocabularies remained a G2 publication-layer decision. D-029 records the
resulting approved vocabulary and source/type compatibility.

Version numbers and field keys are unique only within their parent. Field
display order is also unique within a version. Foreign keys use restrictive
deletion so published history cannot be removed through cascading persistence
operations. Published version and field mappings are ORM-immutable, and their
repositories expose only the persistence and lookup operations needed by the
publication foundation rather than general deletion operations. G1 does not
add template deletion, archival, creator snapshots, seed data, binary upload,
placeholder parsing, or generation behavior.

---

## 2026-08-02

### D-028

Status: Accepted; Phase 5 generation lifecycle refined by D-030

Phase 5 is a production-shaped vertical slice and an expandable foundation,
not the complete authoring and evidence-assisted product. The project will
complete, secure, and deploy a deterministic document-generation journey before
implementing the final browser-authoring and evidence-extraction experience.
Expandability comes from stable domain boundaries, immutable history, narrow
technology interfaces, and relational identities that future records can
reference—not from speculative tables or unused abstractions.

The final template-authoring target lets a user upload a Word document or create
a blank draft, edit boilerplate and placeholders in a browser, review suggested
variable regions, and explicitly publish an immutable version. Imported
brackets, `XXXX`, blanks, underlining, color, and prose annotations are advisory
signals rather than runtime contracts. Template drafts and autosaves remain
mutable and do not create versions. Editing a published reusable template begins
a new draft; only explicit publication creates the next immutable Template
Version.

The current Phase 5 vertical slice does not implement that browser editor,
legacy-DOC normalization, draft persistence, or suggestion workflow. D-029
refines its controlled DOCX authoring-marker and publication contract. It allows
an empty placeholder set and empty field
contract, creates only `CUSTOM` templates through the public creation API, lists
templates through a bounded paginated contract, and addresses nested versions
by their template-local version number. `PRESET` authoring remains unavailable
until administrator identity and authorization exist.

The relational Template Field contract is independent of the eventual Word
marker representation. Future browser authoring may use tagged content controls
or another deterministic structured marker while existing textual-placeholder
versions remain renderable. Do not add a marker-strategy discriminator until a
second real representation is introduced and verified.

Generation acquires approved values from four conceptual channels:

- structured Case values;
- narrowly defined system values;
- explicit user input or correction for one generation;
- future evidence-derived suggestions from Case Documents.

The first three channels form the deterministic Phase 5 implementation.
Evidence-derived values are advisory candidates, not reusable Template Field
provenance and not automatically approved legal facts. When sources disagree,
the application must present alternatives rather than apply hidden precedence.
A file uploaded during generation should normally become a Case Document so
future evidence records can retain a stable source-document and page reference.

Generation Values must be persisted separately from reusable field definitions
and have stable relational identities. This lets future candidate, provenance,
confidence, and review records attach additively. Phase 5 must not add unused
OCR, AI, editor-session, or evidence columns before those capabilities are
designed. The deterministic renderer consumes only final reviewed values and
remains isolated from Case loading, persistence, storage providers, browser
editors, OCR, and AI.

The delivery order is:

1. finish Phase 5 controlled publication, Case/system/manual value resolution,
   deterministic DOCX generation, successful traceability, and frontend integration;
2. implement minimum security and actor-based authorization;
3. complete cloud deployment, object storage, CI publishing, and the required
   reliability and operational baseline;
4. add browser template authoring and evidence-assisted generation as separately
   scoped, additive product phases.

The accepted Phase 5 workflow is:

```text
controlled CUSTOM DOCX publication
  -> immutable Template Version and field contract
  -> Case and version selection
  -> Case/system defaults plus reviewed user input
  -> persisted Generation Values
  -> deterministic DOCX rendering
  -> completed generated Case Document and successful generation traceability
```

The accepted eventual product workflow is:

```text
upload Word or create blank TemplateDraft
  -> browser edit and placeholder suggestions
  -> human-approved immutable publication
  -> select Case and exact version
  -> Case/system/manual/evidence value candidates
  -> conflict and provenance review
  -> approved Generation Values
  -> same deterministic rendering boundary
  -> browser review of Case-specific output
  -> explicit finalization
```

The expansion model is:

```text
TemplateDraft + TemplateSuggestion
                |
                | explicit publication
                v
DocumentTemplate -> DocumentTemplateVersion -> DocumentTemplateField
                                                   |
Case ----------------------------------------------+--> DocumentGeneration
System/User input ---------------------------------+         |
Future EvidenceCandidate -- human approval --------+         v
                                                       GenerationValue
                                                            |
                                                            v
                                                   deterministic renderer
                                                            |
                                                            v
                                                   generated CaseDocument
```

This decision does not reverse D-019, D-026, or D-027. It clarifies that their
minimal mechanisms are the current delivery boundary rather than the final
product experience. G1's stable Template identity, immutable versions,
version-owned fields, storage metadata, and SHA-256 traceability remain the
accepted foundation.

---

### D-029

Status: Accepted

G2 completes a DOCX-only controlled publication slice. For initial onboarding,
a user prepares one DOCX in Word/WPS with explicit Chinese brace markers such as
`{{案号}}`. A later-version candidate derived from published content may already
contain canonical ASCII placeholders such as `{{case_number}}`. Legacy
`.doc`, visual blanks, `XXXX`, color, underlining, prose annotations, alternate
marker syntaxes, browser boilerplate editing, automatic semantic-similarity
decisions, and content controls remain deferred.

The Chinese authoring-marker grammar is exactly `{{名称}}`, where `名称` contains
from 1 through 40 Unicode Han code points. Whitespace, line breaks, nested
braces, punctuation, digits, and ASCII characters are not accepted inside a
Chinese marker. The canonical placeholder grammar is `{{field_key}}`, where the
key matches `[a-z][a-z0-9_]{0,99}`. Invalid keys are rejected rather than
silently lowercased or otherwise normalized. Chinese and canonical markers may
coexist in one DOCX so a user can retain existing canonical placeholders while
adding new Chinese markers to a later version.

A marker may be split across DOCX formatting runs within one supported
paragraph. G2 scans and normalizes ordinary paragraphs in the main document body
and paragraphs inside tables, including nested tables. It also inspects the
finite set of headers, footers, footnotes, endnotes, comments, and text boxes
whose text is exposed through the explicitly implemented docx4j inspection path;
controlled marker syntax found there causes a location-specific validation
error. Content controls, other drawing or diagram content, charts, embedded
documents or objects, custom XML, and arbitrary OOXML extensions are outside the
G2 marker contract. G2 does not promise exhaustive marker discovery across every
OOXML part; bounded package preflight separately rejects unsafe or structurally
unsupported input. Repeated identical markers produce one inspection result
with an occurrence count.

Inspection stores nothing and reports each unique detected marker and occurrence
count. The user reviews the results, may explicitly group different markers such
as `案号` and `案件编号` when they should receive the same value, and configures
one version-owned field for each resulting group: canonical ASCII `fieldKey`,
localized `displayName`, scalar type, required state, default source, and source
key. A Chinese marker requires explicit mapping. A canonical marker already
identifies its key and must map to that same key, although its other field
metadata must still be confirmed for the new version. Multiple Chinese markers
may be explicitly grouped under one key, and Chinese markers may join a matching
canonical marker's group. Two distinct canonical markers cannot be grouped
because each must retain its own key. The application does not infer
legal-semantic equivalence automatically.

Detected Chinese marker names and grouping are transient publication input, not
new persistent field attributes. The existing `displayName` is the chosen
user-facing label for the resulting field; `fieldKey` remains unique only within
one template version and does not create a global semantic catalog. Different
templates may use `案号`, `案件编号`, or another display name with the same
conventional key `case_number`. Value acquisition remains separately identified
by `defaultSource` and a source key such as `caseNumber`.

One user action confirms publication. The application then deterministically
normalizes every confirmed marker group to its ASCII token, for example both
`{{案号}}` and `{{案件编号}}` to `{{case_number}}`; reopens and rescans the
canonical DOCX; validates that its keys exactly match the field contract; hashes
and stores the canonical bytes; and persists the immutable Template Version.
The post-normalization scan is an internal safety check, not a second human
approval. The browser may retain and resubmit the selected file during this
single page session; Phase 5 does not persist an autosaved Template Draft.

G2 uses docx4j behind application-owned scanner and normalizer contracts. The
spike established controlled split-run, body, table, nested-table, Chinese text,
and save/reopen feasibility. It demonstrated exploratory traversal of content
controls but did not establish content-control publication or rendering support.
docx4j/JAXB types must not escape the adapter boundary. G3 subsequently
implemented final-value rendering and completed automated plus representative
manual visual-fidelity verification with safe Chinese DOCX fixtures.

The initial approved deterministic value bindings are:

- `CASE_FIELD` with value type `TEXT`: `caseNumber`, `caseName`, `courtName`,
  `caseCause`, `plaintiff`, `defendant`, `leadLawyerName`, and `description`;
- `CASE_FIELD` with value type `DATE`: `filingDate`, `hearingDate`, and
  `judgmentDate`;
- `SYSTEM_VALUE`: `currentDate` with value type `DATE`;
- `USER_INPUT`: no source key and any approved scalar field type.

Case and system sources require the listed exact value type and a non-empty
source key. `USER_INPUT` must not have a source key. G2 does not perform implicit
conversion between incompatible source and field types. D-030 defines the G4
date input, semantic comparison, and exact rendering contract.

Publication performs bounded DOCX/ZIP package preflight before docx4j parsing,
never resolves external resources, validates supported marker locations, stores
binary content through `DocumentStorage`, and uses best-effort binary cleanup if
metadata persistence fails. Later-version number allocation uses a short
transactional lock on the stable template only after scanning, normalization,
and binary storage have completed. Template and version listing are bounded and
paginated; nested versions are addressed by template-local version number; API
contracts do not expose storage keys or persistence entities.

Known unsupported content fails closed rather than disappearing from the
publication contract. A controlled marker inside a content control returns a
location-specific validation error. Package preflight rejects embedded files,
ActiveX, macro content, and external relationships with a controlled feature
identifier, while ordinary images remain permitted. Detection uses a targeted
semantic denylist of exact OOXML relationship and content types plus
conventional package paths as defense in depth; it is not a general OOXML
allowlist, and an otherwise unknown internal relationship is not rejected only
because G2 does not recognize it. This bounded package policy does not replace
future malware scanning or decide later document-governance policy for digital
signatures, tracked changes, comments, hidden text, or metadata. Template `name` and
`description` belong to the stable Template identity and are accepted only when
that identity is created; later-version publication accepts the version field
contract and rejects unexpected Template metadata. Template pagination uses
`created_at DESC, id DESC` so equal timestamps cannot produce unstable pages.

---

## 2026-08-05

### D-030

Status: Accepted

G4 completes Phase 5 generation as one synchronous, production-shaped command.
The user first retrieves Case/system suggestions, reviews or corrects every
value, and explicitly requests generation. The application then validates the
complete submitted field set, verifies the exact immutable template bytes,
renders in memory, stores the output, and persists the generated CaseDocument,
DocumentGeneration, and Generation Values in one database transaction.
Rendering and storage occur outside that transaction.

Phase 5 does not persist an intermediate draft or require a second finalization
action. Without browser editing or revised-document replacement, that action
would be ceremonial rather than a meaningful business transition. Future
Case-specific browser editing inserts persisted draft, human review/edit,
revisions, and explicit finalization between rendering and the completed
generated CaseDocument. That future workflow must not mutate its reusable
Template Version. A later lifecycle migration may deterministically treat
successful Phase 5 generations as finalized.

`document_generations` represents successful business events, not request
attempts. It identifies the Case, exact Template Version, optional resulting
CaseDocument, Case-status snapshot, creation time, required UUID idempotency key,
and deterministic request SHA-256. The idempotency key is unique. Reusing it
with the same fingerprint returns the existing result; reusing it with a
different Case, template, exact lexical value, or declared source returns a
conflict. The fingerprint also includes the client-supplied IANA timezone used
to resolve system dates.

A concurrent duplicate is resolved by the database uniqueness constraint. The
losing persistence transaction rolls back before the orchestrator looks up the
winner. A matching winner returns the existing result; a different fingerprint
returns a conflict; no winner means the failure was not proven to be an
idempotency race and remains a persistence failure. Do not classify races by
parsing MySQL exception text. The losing request compensates only the binary it
exclusively owns. Failed attempts belong in privacy-safe logs and metrics rather
than incomplete Generation rows.

`generation_values` uses a stable primary key, belongs to one Generation,
references one exact Template Field, stores the exact accepted scalar string
and its explicit `CASE_FIELD`, `SYSTEM_VALUE`, or `USER_INPUT` source, and is
unique per Generation and Template Field. It does not duplicate field metadata
or preallocate OCR, AI, confidence, editor, or evidence columns. Future
candidates and provenance attach through additive child records.

Preparation stores nothing. It returns each field contract, a valid deterministic
suggestion when available, and either `RESOLVED` or `REQUIRES_USER_INPUT`.
Multiline, null, oversized, or type-invalid deterministic defaults are not
offered as usable suggestions. Generation accepts a complete explicit list of
field key, value, and value source. Missing, duplicate, extra, null, multiline,
oversized, or type-invalid values fail before template access. A Case/system
value may retain its declared source only when it still semantically matches the
current deterministic value; otherwise the user must mark the reviewed
correction as `USER_INPUT`. Stale deterministic values return a controlled
conflict without exposing the expected legal value.

Accepted strings are preserved exactly in the Generation Value and rendered
DOCX. Per-value input is bounded to 10,000 Unicode code points and aggregate
input to 100,000 code points. Decimal and Boolean values use strict plain
lexical forms. Dates accept strict ISO `uuuu-MM-dd` and Chinese legal forms
`uuuu年M月d日`, with padded or unpadded month/day, and must be valid calendar
dates. Date comparison for Case/system source validation is semantic, while the
submitted lexical form is preserved; preparation prefers the Chinese legal
display form. Preparation and generation require the client's validated IANA
timezone. An injected Clock resolves `currentDate` in that zone and never uses
the backend host's default timezone.

Before rendering, G4 bounds the immutable template read and requires its actual
length and SHA-256 to match the published version metadata. Any mismatch fails
closed without output or business metadata. If output storage succeeds but the
database transaction fails, the workflow attempts idempotent binary removal and
preserves the original error. Cleanup failures are logged without legal values.
A process crash can still leave an orphaned object; do not introduce two-phase
commit, a queue, worker, workflow engine, attempt table, or reconciliation job
without operational evidence.

Every successful `DocumentStorage.store` operation creates a newly allocated,
exclusively owned object and returns a unique opaque key, including when the
bytes match another object. This is required for safe individual removal and
compensation. Content-addressed deduplication is outside the current contract.
Public generation output availability is derived from whether the Generation's
nullable CaseDocument reference is present; do not persist a separate
`output_available` flag.

Deleting an individual generated CaseDocument sets the Generation's optional
document reference to null while retaining successful traceability. Case
archival does not delete documents. A future permanent Case purge must
coordinate legal retention, binary removal, and relational metadata; database
cascade deletion is not an adequate storage workflow.

---

## Future considerations (TODO)

- Evaluate Flyway compatibility with MySQL 9.7.
- Define an index strategy based on future query patterns.
- Define a timezone policy for persistence timestamps.
- After the User domain is finalized, replace the temporary lead-lawyer dropdown with a searchable system-user selector. The backend must validate that the selected user exists, is active, and is eligible to lead cases; decide the user relationship and retained lawyer-name snapshot together with the supporting-member model.
- Define a legally appropriate retention and restricted permanent-purge policy, including related-document cleanup.
- Select a malware-scanning approach and failure/quarantine policy before accepting untrusted production uploads.
- Define document-object reconciliation and orphan cleanup if operational evidence requires it.
- During the runtime-configuration/deployment phase, replace the backend's
  hardcoded `http://localhost:4321` CORS origin with environment-based allowed
  origins. Keep CORS handling centralized in one early servlet filter, validate
  configured origins at startup, and do not use a permissive wildcard origin.
  When authentication is introduced, review allowed origins, credentials,
  cookies, and CSRF protection together. Until then, the hardcoded origin
  remains an intentional local-development setting.
