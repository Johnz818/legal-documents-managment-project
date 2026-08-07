# Document Domain Model

## 1. Domain Overview

Document generation exists to reduce the manual work required to transfer Case
information and information from supporting materials into recurring legal
documents. The initial workflow lets a user choose a reusable template, review
resolved Case/system values and explicit input, and generate a completed DOCX.
Future browser editing inserts a Case-specific draft and explicit finalization
before that completed output.

The longer-term business goal is to assist this process by extracting possible
values from Case materials. Such assistance remains advisory: a human must
review the supporting evidence and approve the values used in a legal document.

The final authoring target also lets a user upload a Word document or create a
blank template, edit reusable boilerplate in a browser, review suggested
variable regions, and explicitly publish an immutable version. Mutable
authoring drafts exist upstream of published versions; evidence suggestions
exist upstream of approved generation values.

Phase 5 is a production-shaped vertical slice and an expandable foundation,
not the complete authoring and evidence-assisted product. It completes
controlled publication, deterministic Case/system/manual value resolution,
DOCX generation, and successful traceability end to end. Future authoring and
extraction extend this core instead of replacing it.

Case Document upload, download, management, and removal are already established
parts of the Document domain. This document describes those concepts where they
intersect with document generation, while focusing primarily on the template
and generation extension introduced in Phase 5.

### Domain boundaries

The **Case domain** owns the identity and lifecycle of a legal Case. It is the
source of Case facts that may be used during generation. It does not own
document binaries, template definitions, storage references, or generation
lifecycle.

The **Document domain** owns Case-related document metadata, reusable template
concepts, template versions and field contracts, generation traceability, and
the document-generation lifecycle. It associates documents with a Case without
moving Case identity or Case lifecycle into the Document domain.

The **storage infrastructure** is responsible for storing and retrieving binary
content. It does not decide what a template means, which values are valid,
whether a document may be finalized, or which Case owns a document.

```text
Case domain                  Document domain                 Storage

Case identity ─────────────> Case document metadata ──────> binary content
Case values ────────────────> Generation workflow
                              Template and version contract
```

## 2. Core Domain Concepts

### 2.1 Case Document

A **Case Document** represents the business metadata for one file associated
with one Case. It exists so the system can manage a Case's uploaded materials
and generated outputs without storing their binary content inside the Case
model.

It owns information that describes and locates the file in the business
workflow, including its Case association, original name, technical format,
content type, size, source, storage reference, and lifecycle timestamps. Its
source distinguishes uploaded material from generated output.

It does not own the Case itself, reusable template definitions, generation
rules, or the storage-provider implementation.

The current design does not require a separate universal `Document` entity
above Case Document. “Document” is an umbrella business term; the justified
Case-related concept is Case Document. Templates and generation records remain
separate concepts because they have different identities and lifecycles.

### 2.2 Document Template

A **Document Template** is the stable identity of reusable legal-document
content, such as `授权委托书` or `民事起诉状`. It exists so users can refer to one
business template even as its reusable content changes over time.

It owns the template's stable business identity and whether it is
system-provided or user-created. It groups the template's published versions.
A user-created template may have a creator, but the Document domain does not
own User identity. The exact creator relationship remains open until the User
domain is defined.

It does not own Case-specific values, generated output, or edits made to one
generated document. Editing generated output for a Case does not change the
reusable template.

### 2.3 Template Version

A **Template Version** is an immutable published snapshot of a Document
Template. It owns one exact DOCX source and the field contract required by that
source.

Template Version exists to preserve generation traceability. A generation must
be associated with the exact reusable content and field definitions used at
that time, even if a later version of the same template is published.

A version does not represent an output format. The Phase 5 deterministic
vertical slice has one intended
template and generated format: DOCX. A version also does not represent edits to
one Case-specific generated document.

### 2.4 Document Field Definition

A **Document Field Definition**, also called a **Template Field**, declares one
structured scalar value required or accepted by one exact Template Version. It
exists to turn placeholders in reusable content into an explicit,
human-approved contract.

The field definition owns its stable key within the version, display meaning,
scalar value type, required state, deterministic default-source information,
and display ordering. Field definitions belong to the published version. Their
repetition across versions is intentional because each version must remain
self-contained and immutable.

A field definition does not own the value used for a particular generation.
Generation values belong to the generation context. Future evidence provenance,
confidence, and human-review state also belong to generation values rather than
to reusable field definitions.

The initial scalar types are `TEXT`, `DATE`, `DECIMAL`, and `BOOLEAN`. The
deterministic source categories are `CASE_FIELD`, `SYSTEM_VALUE`, and
`USER_INPUT`. Case and system sources require a source key; user input does not
have one. D-029 defines the approved G2 source-key vocabulary, exact source/type
compatibility, and the prohibition on implicit type conversion during
publication.

These categories describe a field's deterministic default. A future
evidence-derived candidate belongs to one generation's value-acquisition and
review workflow; it is not mutable provenance stored on the reusable field
definition.

### 2.5 Template Draft — target capability

A **Template Draft** is a mutable, unpublished working document used to author
new reusable content or prepare a change to a published template. It may begin
from an uploaded Word document, a blank browser document, or a copy of one exact
published Template Version.

Draft saves, editor autosaves, and suggestion decisions do not create published
versions. Explicit publication validates the finalized document and field
contract and creates a new immutable Template Version. A draft based on a
published version does not mutate that version.

Template Draft is part of the final product model but is not persisted in the
current Phase 5 vertical slice. Its exact schema must follow a browser-editor
integration and licensing spike because selection identities, save callbacks,
and editor-native annotations affect the correct persistence boundary.

### 2.6 Template Suggestion — target capability

A **Template Suggestion** is an advisory proposal that some imported content
represents a variable region. Signals may include brackets, `XXXX`, blank
lines, color, underlining, prose annotations, matching Case values, or later
semantic analysis.

A suggestion is not a Template Field and is never a published runtime contract
until a user accepts or adjusts it. The user may reject it, change the selected
text, choose an ASCII field key, change its source binding, or create a field
manually. Published placeholders must use the deterministic representation
supported by that Template Version.

### 2.7 Document Generation

**Document Generation** is the coordinated business process that creates a
Case-related document from one exact Template Version and a set of resolved input
values. It exists to preserve the relationship between the Case, the template
contract, the values used, and the resulting output.

The process requires durable generation traceability. In Phase 5, the user
reviews the complete value set before requesting generation, and the successful
command stores the completed DOCX without an intermediate persisted draft. A
generation record provides the durable representation of that successful
business event; it is not an attempt log or the generation process itself.

Document Generation does not define template structure, implement binary
storage, or approve legal facts automatically.

### 2.8 Generation Value

A **Generation Value** is the final scalar value used for one Template Field in
one Document Generation. It is separate from the reusable field definition so
the system can retain what was actually rendered and how it was acquired.

Phase 5 values are acquired from a declared Case field, a defined system value,
or explicit user input. Future evidence assistance may propose one or more
candidate values from Case Documents. A candidate retains its source document,
page or location, confidence where applicable, and human-review decision. Only
the approved final value is passed to the deterministic renderer.

Generation Values should have stable relational identities so future evidence,
candidate, provenance, and review records can refer to them additively. Phase 5
must not add unused OCR or AI columns before those capabilities are designed.

### 2.9 Generated Document

A **Generated Document** is the Case-related output produced by rendering an
exact Template Version with resolved input values. In the document collection,
it is represented as a Case Document whose source is generated; generation
traceability records why and how it was produced.

It exists as a Case-specific work product. It is not a reusable template and is
not a Template Version. Changes made for one Case must not mutate the reusable
template or create a template version.

### 2.10 Document Storage

**Document Storage** is an infrastructure capability, not a domain entity. It
stores, opens, and removes binary content through storage references.
The Document domain depends on this capability without depending on a local
filesystem or S3-specific model.

Storage is responsible for binary durability and provider-level operations. It does not own
Case associations, filenames as business metadata, template contracts,
generation state, authorization rules, or finalization decisions.

Every successful `store` operation allocates a new, exclusively owned opaque
storage key, even when another object contains identical bytes. This ownership
rule makes individual removal and compensating cleanup safe. Content-addressed
deduplication would require a separately designed shared-reference lifecycle and
is not part of the current storage contract.

## 3. Domain Relationships

### 3.1 Confirmed business relationships

```text
Case 1 ─── owns ─── * Case Document

Document Template 1 ─── has ─── * Template Version

Template Version 1 ─── defines ─── * Document Field Definition

Document Generation ─── uses ─── one exact Template Version
Document Generation ─── occurs for ─── one Case
Document Generation 1 ─── resolves ─── * Generation Value

Template Version ─── provides structure for ─── Generated Document
```

- One Case may own many Case Documents.
- Every Case Document belongs to exactly one Case.
- Sharing one Case Document across multiple Cases is not supported.
- A Document Template groups one or more immutable published Template
  Versions.
- Each Template Version owns its exact field definitions.
- Document Generation uses one exact Template Version rather than only the
  stable template identity.
- A Generated Document is structured by the exact Template Version used for
  its generation. The Template Version does not own the Generated Document.

Document Generation belongs to one Case and one exact Template Version, owns
one value per used Template Field, and optionally references its resulting Case
Document. The optional link preserves traceability if that individual file is
later removed.

### 3.2 Infrastructure relationships

```text
Case Document metadata ─── references ───> stored binary
Template Version metadata ─── references ─> stored DOCX source
Document Generation ─── stores output through ─> Document Storage
```

These are infrastructure relationships because storage provides binary
durability rather than business ownership. The storage reference connects
business metadata to content, but it does not make the storage system the owner
of the Case, template, or generation lifecycle.

Template and generated-document binaries are stored separately from their
business metadata. The selected storage technology does not change the domain
relationships.

### 3.3 Current and expandable model

Status legend:

- `[implemented]` exists in the repository now;
- `[implemented acquisition channel]` supplies a value but is not a separate domain
  entity;
- `[implemented capability]` is application behavior rather than a domain
  record;
- `[future conceptual model]` is a likely future business concept whose schema
  is not yet accepted;
- `[future infrastructure candidate]` is an unselected technical integration.

```text
AUTHORING SIDE

[future conceptual model] TemplateDraft
    | may start from upload, blank document, or published version
    |
    +── has ──> [future conceptual model] TemplateSuggestion
    |
    +── edited through ──> [future infrastructure candidate]
                           browser editor integration/session
    |
    +── explicit publication ───────────────────────────────┐
                                                            v
PUBLISHED REUSABLE CORE

                                         [implemented] DocumentTemplate
                                                    1
                                                    |
                                                    *
                                 [implemented] DocumentTemplateVersion
                                                    1
                                                    |
                                                    *
                                   [implemented] DocumentTemplateField
                                                    |
                                                    | required contract
                                                    v
GENERATION SIDE

[implemented] Case ──────────────────────> [implemented capability]
                                           Document Generation process
                                                   |
[implemented acquisition channel] Case field ──────────┤
[implemented acquisition channel] System value ────────┼──> [implemented]
[implemented acquisition channel] Explicit user input ─┘    GenerationValue
                                                             ^
                                                             |
[future conceptual model] EvidenceCandidate ────────────────┘
    | source CaseDocument
    | page/confidence
    | human review

  [implemented capability] DocumentGenerationService
      |
      +── renderer ──> generated DOCX binary ──> [implemented] DocumentStorage
      |
      +── file metadata ──────────────────────> [implemented]
      |                                         generated instance of the
      |                                         implemented CaseDocument model
      |                                             |
      |                                             +── references ──>
      |                                                 stored DOCX binary
      |
      +── generation metadata ────────────────> [implemented]
                                                GenerationRecord
                                                    |
                                                    +── result ──>
                                                    |   generated CaseDocument
                                                    |
                                                    +── completed Phase 5 output

Future browser editing inserts DRAFT -> review/edit -> revisions -> FINALIZED
between rendering and the completed generated CaseDocument.
```

Expansion follows two rules:

1. Future authoring terminates at the existing explicit publication boundary;
   it does not mutate published versions.
2. Future extraction terminates at a reviewed Generation Value; it does not
   write directly into the template or renderer.

## 4. Document Lifecycle

### 4.1 Template creation and publication

A user-created Document Template receives a stable identity. For initial
onboarding, Phase 5 accepts reusable DOCX content prepared in Word/WPS with
controlled Chinese brace markers such as `{{案号}}`. A later-version candidate
derived from published content may retain canonical ASCII placeholders such as
`{{case_number}}`, and Chinese and canonical markers may coexist. Inspection
identifies each unique marker and presents it for human review. The user may
group markers such as `案号` and `案件编号` when they should receive the same value,
then confirms one canonical ASCII field key, display name, type, required state,
and value-source binding for the group. A canonical marker must retain its own
key. Publication deterministically normalizes the confirmed Chinese marker
groups, rescans and validates the canonical field contract, and creates an
immutable Template Version. A template with no placeholders and an empty field
contract is valid.

D-029 defines the exact Chinese and canonical marker grammars, field-key rules,
finite supported and inspected locations, split-run behavior, repeated-marker
counting, and unsupported-location validation. These are deliberate G2 input
constraints rather than a general definition of every future authoring syntax.

Detected Chinese marker text and grouping are transient publication input, not
additional persistent field attributes. The immutable version stores the
canonical DOCX and the existing version-owned `fieldKey` and `displayName`.
Different versions may use different display names for the same conventional
field key. Field-key uniqueness remains scoped to one version rather than
forming a global semantic-field catalog.

Existing visual documents that use blank lines, `XXXX`, formatting, or prose
annotations are not deterministic template contracts. They must be normalized
before publication. In Phase 5 that normalization happens outside the
application. The final authoring product imports those signals as suggestions,
lets a user normalize them in the browser, and publishes only the approved
result.

Phase 5 public creation produces `CUSTOM` templates. `PRESET` authoring is
deferred until administrator identity and authorization exist. The final
browser-authoring workflow uses editable Template Drafts, but that draft
lifecycle is not implemented in the current vertical slice.

### 4.2 Template versioning

A new version is created only by explicit publication of changed reusable
template content or its field contract. Ordinary edits to a generated document
do not create a template version.

Published versions are immutable so an existing generation can continue to
identify the exact source and field contract it used. Similar field definitions
may therefore appear in several versions; this duplication preserves history
rather than making old versions depend on mutable central definitions.

### 4.3 Phase 5 generation

The user selects a Case and a published Template Version. Required values are
resolved from Case information and explicit user input. Defined system
values may also participate. Preparation exposes valid deterministic defaults
without storing business records. The user reviews every value and its declared
source, then explicitly requests generation. After validation, deterministic
rendering produces a DOCX that is stored as a generated Case Document together
with successful generation traceability.

The user may correct a resolved value for this generation without silently
changing the underlying Case. Future evidence assistance may present candidate
values from existing Case Documents or files uploaded through the generation
journey. Such uploads should normally become Case Documents so provenance can
use a stable source identity.

### 4.4 Human review and future finalization

Phase 5 human review occurs before rendering. Because the application cannot yet
edit or replace a generated DOCX, a separate post-download finalization action
would not represent a meaningful product decision and is not persisted.

The future browser-authoring phase inserts a persisted Case-specific draft,
human review/edit, revisions, and explicit finalization between rendering and
the completed generated Case Document. Finalization will record a human
workflow decision; it will not mean the application independently verifies
legal correctness. Editing a Case-specific output never mutates its reusable
Template Version. If a lifecycle state is introduced later, successful Phase 5
generations may be backfilled as finalized because they already represent the
completed output of the short-circuited workflow.

### 4.5 Historical integrity

Immutable published Template Versions preserve which reusable source and field
contract were used. Generation traceability preserves the Case-specific
generation event and resulting output.

The accepted requirement is immutable template-version history and traceable
generation. Whether a finalized generated binary is itself immutable, and how
later revisions or corrections are represented, remain open questions rather
than confirmed lifecycle rules.

## 5. Template Domain

A template represents reusable legal-document structure, not one Case's final
work product. Its stable identity provides continuity, while immutable published
versions preserve the exact reusable content used for generation.

The Phase 5 deterministic vertical slice uses:

- DOCX template sources;
- one intended generated format, DOCX;
- controlled scalar placeholders;
- structured field definitions owned by a Template Version;
- deterministic values from Case fields, defined system values, and
  explicit user input;
- basic replacement rendering that preserves supported formatting.

Controlled Chinese `{{...}}` markers form the initial Phase 5 onboarding input,
while canonical ASCII placeholders form the published runtime contract and may
also be accepted when canonical content is used to prepare a later version.
Chinese and canonical markers may coexist. Publication scans the DOCX, presents
detected markers, lets the user explicitly group equivalent markers and
configure their fields, and uses one user confirmation to authorize
normalization and publication. A canonical marker must map to its own key. A
second scan after normalization is an internal validation step, not another
human approval. Field keys are machine-oriented while display labels remain
localized.

The relational field contract is independent of the eventual Word marker
technology. A future editor may publish tagged content controls or another
explicit structured marker. Older published textual-placeholder versions must
remain renderable; a second rendering strategy is introduced only when a real
second representation exists.

The Phase 5 deterministic vertical slice does not use arbitrary expressions,
conditions, formulas, repeating
collections, or AI discovery as the runtime template contract. Word content
controls, merge fields, and bookmarks are not the Phase 5 marker mechanism,
but structured content controls remain a candidate for future browser
authoring.

Template definition, value acquisition, and rendering remain separate:

```text
Template definition -> declares required values
Value acquisition   -> supplies resolved input values
Rendering           -> applies those values deterministically to DOCX
```

The Phase 5 deterministic vertical slice does not introduce a global
semantic-field catalog. Unambiguous Case or
system bindings may supply deterministic defaults, while context-dependent
values may remain local user-input fields. A narrowly governed semantic catalog
for precise evidence-derived facts is only a future decision gate before
AI-assisted extraction.

## 6. Generation Workflow

### 6.1 Current Phase 5 deterministic vertical-slice workflow

```text
Publish controlled CUSTOM DOCX template
        -> select Case and exact Template Version
        -> resolve Case/system defaults
        -> collect and review explicit user input or corrections
        -> validate required Generation Values
        -> render deterministic DOCX
        -> persist completed CaseDocument and successful generation traceability
```

### 6.2 Eventual product workflow

```text
Upload Word or create blank TemplateDraft
        -> suggest and review variable regions
        -> edit boilerplate and structured placeholders in browser
        -> explicitly publish immutable Template Version
        -> select Case and version
        -> acquire candidates from Case/system/manual/evidence sources
        -> show conflicts and evidence provenance
        -> human approves final Generation Values
        -> render deterministically
        -> edit Case-specific draft in browser without changing the template
        -> explicitly finalize
```

### 6.3 Inputs

- a Case;
- one exact published Template Version;
- resolved values from the Case and defined system sources;
- explicit user-provided values required by the template contract.

Future evidence suggestions are candidate inputs, not automatically approved
values. When Case data, evidence, and manual entry disagree, the application
must show the alternatives rather than enforce a hidden source precedence.

### 6.4 Process

```text
Load Case and Template Version
        │
        v
Resolve and validate required values
        │
        v
Render deterministic DOCX
        │
        v
Store generated binary
        │
        v
Persist Case Document metadata and generation traceability
        │
        v
Return completed generation metadata; content remains available through the CaseDocument API
```

### 6.5 Output

The Phase 5 output is a completed DOCX associated with the Case, represented in
the Case's document collection as generated output and linked to its exact
Template Version and reviewed values through generation traceability.

### 6.6 Responsibility and failure boundaries

The **application workflow** coordinates Case and template loading, value
resolution, validation, rendering, storage, metadata persistence, and
compensation. It owns the order of these operations but does not implement the
storage provider or rendering format internals.

The **rendering responsibility** transforms one template source and resolved
values into DOCX output. It does not load Cases, authorize users, persist
metadata, or access provider-specific storage APIs.

The **storage responsibility** stores and retrieves binary content through
storage references. It does not validate the template's business field contract
or persist generation metadata.

The **metadata persistence responsibility** preserves Case-document metadata,
template and generation traceability, and domain constraints. It does not store
binary content.

Binary storage and metadata persistence do not share one atomic transaction.
The application workflow is responsible for reporting generation failure and
coordinating best-effort compensation when only part of the operation
succeeds. The accepted operation order, rejection of two-phase commit, and
possible future reconciliation are architecture decisions recorded in
`DECISIONS.md`, not additional domain concepts.

Generation is synchronous in Phase 5: validate, verify and open the immutable
template, render in memory, store the output, then persist CaseDocument,
Generation, and Generation Values in one database transaction. Rendering and
storage do not hold a database transaction open. If persistence fails after
storage, the workflow attempts idempotent removal and preserves the original
failure; cleanup failure is logged without legal values. A process crash can
still leave an unreferenced object, which is an accepted residual risk until
operational evidence justifies reconciliation.

A Generation exists only after storage and the database transaction succeed.
Failures are logs and metrics rather than incomplete Generation rows. Every
successful Generation identifies the Case, exact Template Version, resulting
CaseDocument when retained, Case-status snapshot, reviewed Generation Values,
idempotency key, request fingerprint, and creation time. Removing an individual
CaseDocument sets the Generation's optional document reference to null while
retaining traceability. Case archival never deletes documents; future Case
purge must coordinate retention, binary removal, and relational metadata rather
than relying on database cascade deletion.

Each Generation Value refers to one exact Template Field and preserves the
accepted scalar string and its explicit source category. Phase 5 does not
normalize a reviewed value before storage or rendering. `DATE` accepts strict
ISO `uuuu-MM-dd` and Chinese legal forms `uuuu年M月d日` (padded or unpadded),
validates the calendar date, and preserves the submitted spelling. Comparing a
Case/system date default uses its semantic date so an ISO default may be
reviewed and rendered in Chinese form without becoming a manual override.

The client supplies one validated IANA timezone to preparation and generation.
Defined system dates are resolved with an injected Clock in that zone rather
than the backend host's default timezone. The timezone participates in the
request fingerprint together with Case, exact Template Version, field keys,
exact lexical values, and explicit sources.

Concurrent idempotent requests are serialized by the Generation's unique key.
If persistence fails, winner classification occurs only after the losing
transaction has rolled back. A matching winner returns the existing result; a
different fingerprint returns a conflict; absence of a winner remains a genuine
persistence failure. In every case, compensation removes only the losing
request's exclusively owned object.

Generated-output availability is derived from the nullable CaseDocument
reference. It is true exactly when that reference is present and is never
stored as an independent flag that could drift from the relationship.

## 7. Domain Boundary and Responsibility

### The Document domain owns

- Case Document metadata concepts and their Case association;
- the distinction between uploaded and generated Case documents;
- stable Document Template identity;
- immutable published Template Versions;
- version-owned field definitions;
- value validation and generation coordination;
- generation traceability;
- draft and finalization concepts.

### The Document domain does not own

- Case identity, lifecycle, or the meaning of Case facts;
- local-filesystem implementation details;
- S3 or another storage-provider implementation;
- relational database implementation details;
- authentication or user identity;
- authorization policy enforcement design;
- frontend state, rendering, or navigation;
- OCR engines, AI providers, or PDF-conversion providers.

The Document domain may depend on abstract capabilities for storage and
rendering, but provider-specific concerns remain outside its business model.

## 8. Future Evolution

The following extensions preserve the long-term product goal but do not expand
the Phase 5 deterministic vertical slice.

### Browser template authoring

The final authoring workflow supports Word upload or blank-draft creation,
browser editing of boilerplate and placeholders, advisory variable-region
suggestions, and explicit immutable publication. Updating reusable content
forks a mutable draft from a published version; editor saves do not create
versions.

The editor is infrastructure rather than the owner of template identity,
publication rules, or field meaning. Select it through an integration and
licensing spike that verifies Chinese DOCX fidelity, legacy-DOC normalization,
tagged placeholders, save callbacks, side-panel integration, security, and
deployment cost. Do not finalize draft or suggestion persistence before that
spike.

### Evidence text extraction

Future processing may extract page-aware text from supported PDF and Word Case
files. It is deferred because extraction accuracy, malformed files, page
mapping, and privacy require separate evaluation.

### OCR and image processing

Scanned PDFs, screenshots, and supported image formats may later be processed by
OCR. This is deferred because format validation, Chinese-language accuracy,
layout handling, background execution, cost, privacy, and retry behavior remain
unresolved.

### AI-assisted suggestions

Future AI processing may map normalized evidence to known field schemas and
suggest values with document/page provenance and confidence. Suggestions must
remain subject to human acceptance, editing, or rejection. Provider choice,
structured-output reliability, false assertions, prompt-injection resistance,
data policy, latency, and cost require separate investigation.

AI may also assist with onboarding legacy visual templates, but it must not
replace the deterministic, human-approved runtime field contract.

### PDF conversion and advanced templates

DOCX-to-PDF conversion, more advanced template syntax, and alternative marker
mechanisms may be evaluated after the basic DOCX workflow works end to end.
They are deferred because the Phase 5 vertical slice needs a small, testable
generation contract
before supporting more formats or expressive template behavior.

## 9. Open Questions

The following questions are unresolved and must be decided in their relevant
future tickets.

### Template fields and persistence details

- How should a custom template's creator be represented after the User domain
  is defined?

### Template publication and parsing

G2 uses docx4j behind application-owned scanner/normalizer contracts. It accepts
DOCX only, controlled Chinese onboarding markers, and canonical ASCII
placeholders used in later-version candidates. It supports repeated and
split-run markers in body and table paragraphs and requires explicit human
grouping and field configuration. It inspects the finite unsupported locations
defined by D-029 for marker mistakes rather than claiming exhaustive discovery
across all OOXML parts. A marker inside a content control is rejected with its
unsupported location rather than silently omitted. Package preflight rejects
embedded files, ActiveX, macros, and external relationships while allowing
ordinary images. Template `name` and `description` remain metadata of the stable
Template identity; later-version publication accepts only the new immutable
DOCX field contract and does not silently accept Template metadata. Alternative
marker forms, legacy DOC conversion, semantic-similarity suggestions, and
browser editing remain deferred. Exact HTTP error payload details remain an
implementation-level G2 contract.

### Future generated-document editing and lifecycle

- How are persisted Case-specific drafts, revisions, and final output related?
- What business commitment does future finalization represent, and is it
  reversible?
- Is a finalized generated binary immutable, and how are later corrections
  represented?
- Which editor and reviewed-document replacement contracts preserve generated
  history without changing the reusable Template Version?

### Future document formats

- If future output formats are introduced, how do they relate to the stable
  Document Template, its versions, and generated output?

The Phase 5 decision remains one DOCX source and DOCX output per published
Template Version. This question does not expand the current scope.

### Production governance

- Which permissions protect template publication, generation, finalization,
  download, and removal after the User domain exists?
- What actor and audit information must be retained?
- What malware-scanning, retention, legal-hold, purge, and reconciliation rules
  apply before customer legal materials are used in production?

### Evidence-assisted generation

- Which text extraction and Chinese OCR approaches meet the required accuracy,
  privacy, provenance, retry, latency, and cost constraints?
- Is a narrow governed semantic-field catalog justified before structured AI
  extraction, and how would precise reusable definitions be governed?
- What provider-neutral AI boundary and evaluation criteria are required?
- How should confidence, conflicting evidence, false assertions, and human
  review be represented?
