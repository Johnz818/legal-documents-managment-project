# Phase 5 — Document Generation Handoff

## Purpose of this document

This document preserves the business understanding, accepted architecture,
current repository state, and unresolved decisions for Phase 5. It is a handoff
for continuing the work in a new conversation; it does not replace
`PRODUCT.md`, `DOCUMENT_DOMAIN.md`, `DECISIONS.md`, or `ROADMAP.md`.

Lifecycle: this handoff is active only while Phase 5 is in progress. Before
Phase 5 is declared complete, every lasting product goal, domain rule, accepted
decision, and future roadmap item must be present in `PRODUCT.md`,
`DOCUMENT_DOMAIN.md`, `DECISIONS.md`, or `ROADMAP.md`. At Phase 5 completion,
mark this file deprecated and stop using it as an architecture authority or the
starting context for later phases. Retain it only as historical delivery
context unless the repository's documentation-retention policy later removes
it.

G1 — Template persistence is implemented and committed locally as `72a1c65`.
The next planned ticket is **G2 — Template API**. Template publication,
rendering, generation, finalization, and their frontend journey are not yet
implemented.

The guiding delivery rule is:

> Phase 5 is a production-shaped vertical slice and an expandable foundation,
> not the complete authoring and evidence-assisted product.

The project will complete, secure, and deploy the deterministic vertical slice
before implementing the final browser-authoring and evidence-extraction
experience. Future capabilities must extend the immutable publication and
Generation Value boundaries rather than replace them.

This document uses the following status language deliberately:

- **Implemented:** present in the current repository.
- **Confirmed / accepted:** recorded product scope or architecture decision.
- **Proposed / planned:** part of an approved implementation direction but not
  yet a finalized domain or schema decision.
- **Open:** requires a future ticket-specific decision.

## 1. Business requirements

### 1.1 User problems to solve

The feature's business goal is to reduce the manual work required to transfer Case information and information from supporting materials into recurring legal documents.

The product should eventually let a user:

1. Upload a Word document or create a blank reusable-template draft.
2. Edit boilerplate and placeholders in a browser and review system suggestions.
3. Explicitly publish an immutable reusable-template version.
4. Select a Case and one exact published template version.
5. Review values acquired from Case data, system values, direct user input, and
   eventually Case-document evidence.
6. Generate a DOCX draft while preserving supported formatting.
7. Review or eventually edit the Case-specific draft and explicitly finalize it.

The longer-term business target also uses information contained in uploaded Case files—for example evidence, bank statements, chat screenshots, pleadings, and scanned materials—to suggest field values. A lawyer must be able to inspect the supporting document and page, then accept, edit, or reject each suggestion.

Chinese example:

```text
模板字段：{{case_number}}（显示名称：案号）
案件数据：(2026)沪0115民初1001号
生成结果：在 DOCX 中以案件案号替换 {{case_number}}
```

A future evidence-assisted example is a suggestion such as `借款本金：200000元`, accompanied by its source file and page. The suggestion is not an approved legal fact until a human confirms it.

### 1.2 Confirmed Phase 5 deterministic vertical-slice journey

Phase 5 deliberately delivers a useful, deployable workflow without a browser
Word editor, OCR, or AI:

- upload and manage user-created `CUSTOM` templates;
- publish a version of a template with explicitly defined scalar fields;
- select a Case and a published template version;
- review values supplied from approved Case data, defined system values, and
  explicit user input;
- generate and download a DOCX draft;
- explicitly finalize the reviewed result.

This is the current delivery milestone, not the final product ceiling. The
final authoring workflow adds mutable browser drafts and advisory placeholder
suggestions upstream of publication. The final evidence workflow adds reviewed
candidate values and provenance upstream of deterministic rendering.

Finalization is an explicit workflow action by a human. It does not mean that
the application independently verifies the document's legal correctness.

### 1.3 Confirmed Phase 5 deterministic technical boundary

The accepted technical boundary supporting that user journey is:

- one DOCX source file for each immutable published template version;
- controlled ASCII machine-key placeholders in the DOCX, with localized
  display labels and an allowed empty field contract;
- structured scalar field definitions owned by the published version;
- deterministic value acquisition and DOCX rendering;
- binary content outside MySQL through the existing `DocumentStorage` boundary;
- Case-document metadata and generation traceability in MySQL;
- backend capabilities delivered before the related frontend mock flow is
  migrated.

### 1.4 Deferred capabilities and technical approaches

The following are explicitly outside the initial G1–G6 milestone:

- extracting text from uploaded PDF or Word evidence;
- OCR for scanned PDFs, screenshots, or images;
- image upload and image processing;
- AI-assisted field-value extraction;
- AI-assisted discovery of fields in legacy templates;
- automatic acceptance of AI output or automatic finalization;
- arbitrary template expressions, conditions, formulas, or repeating collections;
- browser-based Word editing;
- mutable template-draft and template-suggestion persistence;
- legacy-DOC normalization inside the application;
- DOCX-to-PDF conversion;
- replacing a draft with a separately reviewed document;
- production malware scanning, authorization, audit history, and
  retention-controlled purge, which remain separate production-readiness work.

The following technical approaches are rejected or deferred for the initial
milestone and are documented more precisely in Section 3:

- Word content controls, merge fields, or bookmarks as the initial marker
  mechanism;
- a global semantic-field catalog in G1;
- a second NoSQL or vector database.

These capabilities are deferred from the current vertical slice, not rejected
as final product goals. Browser template authoring is confirmed as a future
product phase. Begin it with an editor integration and licensing spike; do not
design draft persistence around an unverified editor API.

The deferred evidence-assisted sequence is:

1. **EG1 — Text extraction:** page-aware extraction from supported PDF and Word files.
2. **EG2 — OCR and image extraction:** scanned material processed through durable background jobs.
3. **EG3 — Structured AI suggestions:** normalized evidence mapped to known field schemas.
4. **EG4 — Human evidence review:** provenance, confidence, and accept/edit/reject workflow.
5. **EG5 — Template onboarding assistance:** suggestions for normalizing legacy visual templates, followed by human approval and deterministic publication.

### 1.5 Current and eventual workflow summary

Current Phase 5 deterministic vertical-slice workflow:

```text
Prepare controlled CUSTOM DOCX
        |
        v
Upload, validate fields, and explicitly publish immutable version
        |
        v
Select Case and exact Template Version
        |
        v
Prefill Case/system values and review manual input or corrections
        |
        v
Persist final Generation Values and validate required fields
        |
        v
Render deterministic DOCX and persist generated CaseDocument
        |
        v
Download, review, and explicitly finalize
```

Eventual product workflow:

```text
Upload DOC/DOCX or create blank TemplateDraft
        |
        v
System suggests variable regions from markers, formatting, context, or AI
        |
        v
User edits boilerplate and placeholders in browser
        |
        v
Human-approved immutable publication
        |
        v
Select Case and exact version
        |
        v
Collect Case/system/manual/evidence candidate values
        |
        v
Show conflicts, confidence, and source-document/page provenance
        |
        v
Human approves final Generation Values
        |
        v
Deterministic rendering and browser review of Case-specific draft
        |
        v
Explicit finalization
```

Model connection and expansion:

```text
[future authoring]

TemplateSuggestion ───────────────────────> TemplateDraft
[future infrastructure candidate]
browser editor integration/session ──────> TemplateDraft
                                                |
                                                | explicit publication
                                                v
[implemented reusable foundation]

DocumentTemplate 1 ── * DocumentTemplateVersion 1 ── * DocumentTemplateField
                                                               |
                                                               v
[Phase 5 generation]

Case ──────────────────────────────────────────> DocumentGeneration
System values / User input ────────────────────>       |
                                                        v
                                                 GenerationValue
                                                        ^
                                                        |
[future evidence]                                       |

CaseDocument -> extraction/OCR/AI -> EvidenceCandidate -+
                   source/page/confidence + human review

GenerationValue -> deterministic renderer -> generated CaseDocument -> finalize
```

Future authoring connects only through explicit publication. Future evidence
connects only through human-reviewed Generation Values. This is the central
rule that lets Phase 5 remain small without making its model disposable.

## 2. Domain understanding

### 2.1 Existing entities and boundaries

#### Case

The Case domain owns Case identity and lifecycle. It provides approved scalar values that generation may use, but it does not own template files, generated binaries, storage keys, or generation workflow state.

#### CaseDocument

`CaseDocument` is already implemented in the Document domain. It represents metadata for one file belonging to exactly one Case. Its source distinguishes `UPLOADED` from `GENERATED`; this classification does not determine removal permission.

One Case may own many Case documents. A Case document belongs to one Case only. Sharing one document across multiple Cases is not supported.

The existing metadata includes the Case ID, original filename, backend-generated storage key, source, technical format, content type, size, and timestamps. The binary content is stored outside MySQL.

### 2.2 Implemented publication foundation and planned generation concepts

#### DocumentTemplate

A stable reusable identity representing the business template, such as `授权委托书` or `民事起诉状`. The implemented model can represent `PRESET` and `CUSTOM`, but the Phase 5 public creation API will create only `CUSTOM`. `PRESET` authoring remains unavailable until administrator identity and authorization exist. The stable identity groups its published versions; it is not itself a generated Case document.

#### DocumentTemplateVersion

An immutable published snapshot of one template. Each version owns exactly one DOCX source and its storage metadata, including the content SHA-256 digest. Versioning exists so a generated document can always identify the exact template content and field contract used.

A user's edits to one generated Case document do not alter the reusable template and do not create a new template version. A version is created only through explicit reusable-template publication. The final browser authoring product has separate unpublished working drafts, but draft saves and autosaves are not part of G1–G6 and never create versions.

#### DocumentTemplateField

A definition of one structured scalar value required or accepted by one exact
published template version. A field definition is not the value supplied for a
particular generation. Accepted field metadata includes:

- stable field key within the version;
- Chinese display name;
- optional description;
- scalar value type;
- required state;
- deterministic default source;
- optional source key;
- display order.

G1 uses the scalar types `TEXT`, `DATE`, `DECIMAL`, and `BOOLEAN`, and these
deterministic source categories:

- `CASE_FIELD`: an approved, unambiguous Case value such as `caseNumber`, `courtName`, or `filingDate`;
- `SYSTEM_VALUE`: a narrowly defined system value such as the current date;
- `USER_INPUT`: a value entered for this generation and local to the template contract.

Case and system sources require a non-empty source key; user input must not
have one. The exact approved Case and system source-key vocabularies remain a
G2 publication decision. D-027 records the persistence constraints.

Field definitions belong to a template version. Repeated definitions across versions are intentional: they make each published version self-contained and immutable rather than allowing a later central edit to change an old version's contract.

#### GenerationRecord

A generation record represents the workflow and traceability for generating a
Case-related document from one exact template version. The accepted workflow
persists a generation record and supports draft output followed by explicit
finalization. Its exact schema, relationships, lifecycle representation, and
API contract belong to G4/G5 and have not been finalized.

#### Generated document

The output produced by rendering a published template with approved values. Its
binary and Case-file metadata use the existing document storage and
`CaseDocument` model with source `GENERATED`; the generation record carries
template and workflow traceability. A generated document is not a reusable
template and is not a template version.

#### Generation values

Values used for a specific generation are separate from template field definitions. In the deterministic milestone they come from Case fields, system values, or explicit user input.

Future extracted values—not template fields—must carry the suggested value, source document and page, confidence, and human-review state.

G4 must give Generation Values stable relational identities so those future
candidate, provenance, and review records can attach additively. It must not
add unused OCR or AI columns now. When Case data, evidence, and manual input
disagree, the future review experience shows alternatives rather than applying
hidden source precedence. A file uploaded during generation should normally
become a Case Document so provenance has a stable source identity.

#### TemplateDraft and TemplateSuggestion — confirmed future concepts

The final authoring workflow adds a mutable Template Draft before publication.
A draft may begin from a Word upload, a blank browser document, or a copy of one
published version. Advisory Template Suggestions may be derived from brackets,
`XXXX`, blanks, underlining, color, prose annotations, known Case values, or
later semantic analysis. Users accept, reject, resize, rename, remap, or create
placeholders manually. Only the finalized deterministic field contract is
published.

These concepts are deliberately not persisted in G2–G6. Their schema follows a
future browser-editor integration and licensing spike. Future authoring calls
the same publication boundary used by direct Phase 5 DOCX upload.

#### DocumentGenerationService

The application service responsible for coordinating the complete generation use case:

1. Load the Case and selected template version.
2. Resolve approved Case values and explicit user input.
3. Validate required structured fields.
4. Invoke the renderer.
5. Store the generated binary through `DocumentStorage`.
6. Persist Case-document metadata and the generation record.
7. Perform best-effort binary cleanup if later persistence fails.

#### DocumentTemplateRenderer

A narrow rendering component. It receives template content plus approved values and produces the rendered DOCX. It does not load Cases, authorize users, persist metadata, or depend on local-filesystem or S3-specific APIs.

### 2.3 Relationship overview

```text
DocumentTemplate 1 ─── * DocumentTemplateVersion 1 ─── * DocumentTemplateField
                              │
                              └── one immutable DOCX source in DocumentStorage

Case 1 ─── * CaseDocument

Planned generation traceability:
Case ─── GenerationRecord ─── exact DocumentTemplateVersion
                 │
                 └── generated output stored through CaseDocument/DocumentStorage
```

The template/version/field and Case/CaseDocument cardinalities are accepted.
The exact cardinality and database relationships around `GenerationRecord` are
not yet decided and must be finalized in G4.

### 2.4 Important terminology

- **Template identity:** the stable reusable business template across versions.
- **Published version:** an immutable DOCX source plus its exact field contract.
- **Controlled placeholder:** in Phase 5, an explicit ASCII machine-key token
  such as `{{case_number}}` that the application recognizes deterministically;
  its display label may be localized as `案号`.
- **Template field / field definition:** metadata declaring a value required or accepted by one template version. “Template variable” is legacy frontend terminology; Phase 5 uses “template field” for the planned domain model.
- **Generation value:** the value supplied for one field in one generation; it is not part of the reusable field definition.
- **Value acquisition:** obtaining a value from a Case, the system, user input, or a future extraction suggestion.
- **Rendering:** deterministic replacement of approved values in the DOCX.
- **Draft:** generated output that has not been explicitly finalized.
- **Finalization:** an explicit human workflow action after review; it is not AI approval and does not represent independent legal verification by the application.
- **Provenance:** the source document and page supporting a future extracted suggestion.
- **Semantic field:** a possible future governed definition for a precise reusable evidence fact; it is not part of G1.

## 3. Confirmed architecture decisions

### 3.1 Domain and storage boundaries

**Decision:** The Document domain owns Case-file metadata, template definitions, generated-document metadata, and binary-storage coordination. The Case domain supplies Case identity and approved Case values.

**Reason:** This keeps Case lifecycle separate from file, template, and generation concerns while preserving a required Case relationship for Case documents.

**Decision:** MySQL stores metadata; binary content remains outside MySQL behind the existing application-owned `DocumentStorage` contract.

**Reason:** Relational data needs constraints and queryability, while potentially large binary files belong in file/object storage. Local development uses filesystem storage; production is expected to use an S3-compatible adapter behind the same contract.

**Rejected:** Storing binary content in MySQL or exposing provider-specific filesystem paths, Spring `MultipartFile`, or S3 SDK types through the storage contract.

### 3.2 Template identity and versioning

**Decision:** Use a stable `DocumentTemplate` identity with immutable published `DocumentTemplateVersion` records. Each version owns one DOCX source and one exact field contract.

**Reason:** Generation records must remain reproducible and traceable even if the reusable template changes later. Versioning represents template history, not output-format variants.

**Rejected:** One mutable template row, because editing it would destroy historical traceability.

**Rejected:** Event sourcing for the complete Template aggregate, because append-only events and projections add disproportionate complexity to the current workflow.

### 3.3 Relational version-owned field definitions

**Decision:** Persist template fields as relational child records of the exact published version rather than as a JSON array.

**Reason:** Future generation values and evidence provenance need stable references to exact fields. Relational rows retain uniqueness, enum, and foreign-key guarantees in MySQL. Expected field counts are small, so the indexed one-to-many join is not a material cost.

**Alternative considered:** A MySQL JSON manifest would be reasonable if the manifest were always opaque, immutable, loaded as one unit, and validated only in Java. It was not selected because field-level integrity and future references would become string-key conventions in application code.

**Rejected:** Adding a NoSQL database solely for template manifests. It adds operational and consistency cost without a demonstrated requirement.

The repeated field rows that result when template versions share similar fields are accepted. They preserve version immutability and are expected to remain small.

### 3.4 Controlled field identification

**Decision:** The deterministic milestone identifies fields through controlled DOCX placeholder tokens. Publication scans the tokens, presents the detected fields, requires definitions to match, and then publishes the immutable version.

**Reason:** The application needs an explicit, testable contract before it can render reliably. Existing legal templates commonly use visual blanks, `XXXX`, colored prose annotations, or fixed text; these are ambiguous and must be normalized rather than guessed at runtime.

The scanner and renderer must account for tokens split across Word formatting runs and tokens inside supported tables.

**Deferred as initial marker mechanisms:** Word content controls, merge fields,
and bookmarks.

**Rejected as a runtime contract:** Arbitrary AI field discovery, because the
published template requires a deterministic, human-approved field contract.

### 3.5 Separation of definition, acquisition, and rendering

**Decision:** Keep these responsibilities separate:

```text
Template definition -> declares required values
Value acquisition   -> resolves Case values, system values, user input,
                       or future reviewed extraction suggestions
Rendering           -> applies approved values deterministically to DOCX
```

**Reason:** Templates should not know how Cases are loaded, AI should not define the runtime contract, and the renderer should not coordinate business workflow or persistence.

`DocumentGenerationService` owns orchestration. `DocumentTemplateRenderer` owns rendering only.

### 3.6 Layered semantic strategy

**Decision:** Do not introduce a global semantic-field catalog in G1. The deterministic model uses direct `CASE_FIELD`/`SYSTEM_VALUE` bindings and template-local `USER_INPUT` fields.

**Reason:** Direct bindings already cover unambiguous application data, while legal roles and wording are often context-dependent. A universal ontology now would add governance and mapping complexity before evidence extraction needs it.

**Future decision gate:** Before EG3, evaluate a narrow governed semantic catalog only for precise reusable evidence-derived facts, for example `LOAN_PRINCIPAL_AMOUNT` or `LOAN_TRANSFER_DATE`. Mapping must remain optional. Definitions must be owner- and meaning-qualified where needed.

**Rejected:** Ambiguous global concepts such as generic `PHONE`, `DATE`, or a universal legal-party role. Contextual roles and wording may remain local to a template.

This layered approach may later give AI extraction stable targets without forcing every template field into centralized governance.

### 3.7 Human review and AI boundary

**Decision:** The first workflow produces a DOCX draft and requires explicit human finalization. AI output, when introduced, is advisory only.

**Reason:** Extracted legal facts can be incomplete or wrong. A user must see provenance and confirm or correct every suggestion before it is used for finalization.

**Rejected:** Automatic legal-fact approval and automatic finalization.

### 3.8 Consistency across MySQL and storage

**Decision:** Do not use two-phase commit between MySQL and file/object storage. Generation follows the same best-effort compensation principle already used for uploads: store the binary, persist metadata and the generation record, and try to remove the binary if later persistence fails.

**Reason:** The two systems do not share an atomic transaction, and distributed transactions would add substantial complexity. A future reconciliation or durable retry mechanism should be justified by operational evidence.

## 4. Current implementation status

### 4.1 Implemented and reusable for Phase 5

- Spring Boot 3 / Java 21 backend with Spring Data JPA, Flyway, MySQL, and `ddl-auto=validate`.
- Dedicated MySQL integration-test database and a backend JaCoCo line-coverage gate of at least 90%.
- Case CRUD, search, archive/restore, optimistic locking, and frontend integration.
- `case_documents` schema and `CaseDocumentEntity`.
- `DocumentSource` values `UPLOADED` and `GENERATED`.
- `DocumentFormat` support for PDF, DOC, and DOCX metadata.
- framework/provider-neutral `DocumentStorage` contract with `store`, `open`, and idempotent `remove` operations.
- local filesystem `DocumentStorage` implementation using generated opaque keys.
- validated Case-document upload, list, download, and permanent removal APIs.
- Case Detail frontend integration for live file upload, list, download, and removal.
- best-effort upload compensation and the documented non-atomic storage/database boundary.
- backend and frontend container images, local Compose environment with persistent MySQL and document volumes, GitHub Actions application verification, and container build verification.
- Flyway V8 tables `document_templates`, `document_template_versions`, and
  `document_template_fields`.
- JPA entities and deliberately narrow repositories for stable template
  identity, immutable published version metadata, and immutable version-owned
  field definitions.
- MySQL-verified uniqueness, restrictive foreign keys, scalar/source enums,
  storage metadata, and lowercase SHA-256 persistence constraints from D-027.

### 4.2 Present only as frontend mocks

The frontend currently contains mock-backed pages and models for:

- template management;
- preset-template listing;
- custom-template creation/editing;
- template and Case selection;
- generation entry;
- preview/edit;
- generated documents.

`frontend/src/data/document.ts` currently simulates templates, variables, and text replacement. These models are legacy UI mocks, not approved backend API contracts or persistence models. They must not be reused as backend contracts without a ticket-specific migration review.

### 4.3 Not implemented

- template binary publication and storage;
- placeholder scanning and field-contract validation;
- template REST APIs;
- renderer library selection or `DocumentTemplateRenderer` implementation;
- generation record persistence;
- `DocumentGenerationService`;
- draft-generation or finalization APIs;
- live frontend integration for template or generation screens;
- text extraction, OCR, AI, provenance, confidence, or evidence-review workflow.

## 5. Open design questions

The following decisions are not finalized. They must remain explicit gates rather than being silently assumed during implementation.

### 5.1 Completed G1 persistence boundary

- Creator and ownership relationships remain deferred until the User domain
  provides a real identity.
- Template archival or deletion behavior remains deferred until a business
  lifecycle is approved; G1 preserves published history with restrictive
  foreign keys.
- G1 is complete. Do not redesign its stable identity, immutable version, or
  version-owned field boundaries to implement G2.

### 5.2 Template publication API and validation

- Exact G2 request shape for uploading a DOCX and defining or confirming detected fields.
- The exact grammar and escaping behavior within the approved ASCII
  machine-key placeholder direction.
- Duplicate tokens, unsupported document constructs, missing definitions, extra definitions, and invalid source bindings.
- Exact approved Case and system source-key vocabularies.

Confirmed G2 boundaries are:

- an empty placeholder set and empty field contract are valid;
- the public creation API creates only `CUSTOM` templates;
- list responses are bounded and paginated;
- nested versions are addressed by template-local version number;
- storage keys and persistence entities are not exposed through the API;
- publication rescans the exact uploaded bytes and remains authoritative even
  if a separate scan/preview endpoint is provided.

### 5.3 Renderer selection

Apache POI versus docx4j is intentionally unresolved. Before G3, run a focused spike using representative, non-sensitive DOCX fixtures and compare:

- placeholders split across formatting runs;
- placeholders in supported tables;
- formatting preservation;
- malformed or unsupported constructs;
- dependency weight and maintainability.

The representative source files discussed previously are external legal examples and must not be copied into the repository if they contain sensitive information. Safe synthetic fixtures are required for committed tests.

### 5.4 Generation persistence and lifecycle

- Exact generation-record schema and its relationship to the resulting `CaseDocument`.
- Exact draft/finalized state model and permitted transitions.
- Whether and how values used for a generation are persisted for reproducibility.
- Exact behavior for repeated finalization, stale state, failed storage, failed metadata persistence, and process crashes.
- What “preview/edit” means in G5/G6 given that browser-based Word editing and reviewed-document replacement are deferred.

### 5.5 Security, audit, and retention

- Template-management and generation permissions after the User domain exists.
- Actor attribution and audit records for publishing, generating, finalizing, and removing documents.
- Malware-scanning policy before untrusted templates or generated documents are rendered in a customer-data deployment.
- Retention, legal hold, permanent purge, and object reconciliation policies.

These are production-readiness requirements but are not permission to expand G1–G6 into the full Security phase.

### 5.6 Evidence-assisted generation

- Text extraction libraries and support boundaries for Word and PDF.
- Chinese OCR provider, local versus cloud processing, accuracy, layout support, privacy, retry, cost, and page mapping.
- Whether a narrow governed semantic catalog is justified before EG3 and, if so, its ownership and governance rules.
- Provider-neutral structured AI boundary, model/provider data policy, prompt-injection defenses, accuracy evaluation, latency, and cost.
- Confidence thresholds, conflicting evidence, and how false assertions are handled.
- Durable background processing; Kafka must not be introduced unless measured workload requires it.
- Whether keyword/page filtering is enough before considering vector retrieval or a vector database.
- Sensitive-data transmission, logging, redaction, retention, and deletion rules.

### 5.7 Future browser template authoring

- Select an embedded editor only after a time-boxed integration, fidelity,
  deployment, security, and licensing spike with safe representative Chinese
  Word fixtures.
- Verify legacy `.doc` normalization, blank-document creation, tagged
  placeholder support, save callbacks, side-panel integration, and DOCX
  round-trip fidelity before defining draft persistence.
- Keep mutable Template Drafts and advisory Template Suggestions separate from
  immutable published versions and their exact field contracts.
- Treat brackets, blanks, `XXXX`, formatting, and prose annotations as
  suggestions rather than automatically approved fields.
- Ensure editing a generated Case document remains separate from editing and
  publishing reusable template content.

## 6. Recommended next implementation steps

Continue with the already approved incremental sequence. Do not combine these tickets without a separate scope review.

### G1 — Template persistence (complete)

Implemented by local commit `72a1c65` with Flyway and repository verification
against MySQL. Preserve this boundary while adding publication behavior.

### G2 — Template API

Upload and publish `CUSTOM` DOCX templates and exact version-owned field
definitions. Store template binaries through `DocumentStorage`. Detect
controlled ASCII tokens, allow an empty contract, reject invalid or mismatched
contracts, publish subsequent immutable versions, list with bounded pagination,
retrieve versions by template-local version number, and download exact content.

Suggested commit: `feat: add document template API`

### Renderer-selection spike

Before committing to G3, compare Apache POI and docx4j against safe representative DOCX fixtures. Record the decision and supported/unsupported constructs in `DECISIONS.md`.

### G3 — DOCX renderer

Implement deterministic scalar replacement while preserving the supported basic formatting established by the spike. Keep the renderer independent of Case loading, authorization, persistence, and storage adapters.

Suggested commit: `feat: render docx templates`

### G4 — Draft generation

Introduce generation-record persistence, stable relational Generation Value
records, and `DocumentGenerationService`. Coordinate approved Case and system
values, explicit user input or correction, validation, rendering, storage,
Case-document metadata, and compensation. Keep evidence provenance additive and
deferred rather than adding speculative extraction columns.

Suggested commit: `feat: generate document drafts`

### G5 — Human finalization

Support draft download and explicit finalization with tested lifecycle transitions. Do not treat generation as automatically approved.

Suggested commit: `feat: finalize generated documents`

### G6 — Frontend integration

Replace only the relevant template and generation mock flow with the live APIs. Keep unrelated mock-backed modules unchanged.

Suggested commit: `feat: integrate document generation`

### After G6

Complete minimum security, cloud deployment, and the required reliability
baseline before expanding the Phase 5 vertical slice into browser template
authoring or evidence-assisted generation. This produces a demonstrable
end-to-end system while keeping both future capabilities additive:

```text
Future TemplateDraft ──> existing publication boundary
Future evidence candidate ──> reviewed Generation Value ──> existing renderer
```

The browser-authoring phase begins with the editor spike described in Section
5.7. Evidence assistance follows its separately governed extraction, OCR,
privacy, provenance, and human-review roadmap. Neither is permission to add
speculative infrastructure to G2–G6.

### Verification discipline for every ticket

- Read `AGENTS.md`, `PRODUCT.md`, `DECISIONS.md`, and `ROADMAP.md` first.
- Apply the repository engineering-change-planning skill.
- Inspect current code, propose one bounded plan, and wait for approval before editing.
- Run Java 21 backend tests against the Flyway-managed MySQL test database.
- Preserve `ddl-auto=validate` and Flyway-only schema ownership.
- Keep the backend line-coverage gate at or above 90%, while testing behavior rather than merely covering lines.
- Add frontend tests, type checking, production build verification, and manual workflow checks when frontend integration begins.
- Show the diff before staging or committing.
- Keep documentation and application commits focused, and update accepted decisions when a design gate is resolved.
