# Document Domain Model

## 1. Domain Overview

Document generation exists to reduce the manual work required to transfer Case
information and information from supporting materials into recurring legal
documents. The initial workflow lets a user choose a reusable template, supply
resolved Case values and explicit input, generate a DOCX draft, review the
result, and explicitly finalize it.

The longer-term business goal is to assist this process by extracting possible
values from Case materials. Such assistance remains advisory: a human must
review the supporting evidence and approve the values used in a legal document.

Case Document upload, download, management, and removal are already established
parts of the Document domain. This document describes those concepts where they
intersect with document generation, while focusing primarily on the template
and generation extension introduced in Phase 5.

### Domain boundaries

The **Case domain** owns the identity and lifecycle of a legal Case. It is the
source of Case facts that may be used during generation. It does not
own document binaries, template definitions, storage references, or generation
lifecycle.

The **Document domain** owns Case-related document metadata, reusable template
concepts, template versions and field contracts, generation traceability, and
the document-generation lifecycle. It associates documents with a Case without
moving Case identity or Case lifecycle into the Document domain.

The **storage infrastructure** is responsible for storing and retrieving binary
content. It does not
decide what a template means, which values are valid, whether a document may be
finalized, or which Case owns a document.

```text
Case domain                  Document domain                 Storage

Case identity ─────────────> Case document metadata ──────> binary content
Approved Case values ──────> Generation workflow
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

A version does not represent an output format. The MVP has one intended
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

The exact scalar-type and source-category members remain part of G1 detailed
design. The confirmed model requires structured scalar fields with a
deterministic default source and optional source key.

### 2.5 Document Generation

**Document Generation** is the coordinated business process that creates a
Case-related draft from one exact Template Version and a set of resolved input
values. It exists to preserve the relationship between the Case, the template
contract, the values used, and the resulting output.

The process requires durable generation traceability and supports draft output
followed by explicit human finalization. A generation record is the possible
durable representation of that traceability; it is not the generation process
itself. Its exact structure, relationships, and state representation remain
open for the generation tickets.

Document Generation does not define template structure, implement binary
storage, or approve legal facts automatically.

### 2.6 Generated Document

A **Generated Document** is the Case-related output produced by rendering an
exact Template Version with resolved and reviewed values. In the document collection it is
represented as a Case Document whose source is generated; generation
traceability records why and how it was produced.

It exists as a Case-specific work product. It is not a reusable template and is
not a Template Version. Changes made for one Case must not mutate the reusable
template or create a template version.

### 2.7 Document Storage

**Document Storage** is an infrastructure capability, not a domain entity. It
stores, opens, and removes binary content through storage references.
The Document domain depends on this capability without depending on a local
filesystem or S3-specific model.

Storage is responsible for binary durability and provider-level operations. It does not own
Case associations, filenames as business metadata, template contracts,
generation state, authorization rules, or finalization decisions.

## 3. Domain Relationships

### 3.1 Confirmed business relationships

```text
Case 1 ─── owns ─── * Case Document

Document Template 1 ─── has ─── * Template Version

Template Version 1 ─── defines ─── * Document Field Definition

Document Generation ─── uses ─── one exact Template Version
Document Generation ─── occurs for ─── one Case

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

The exact cardinality and persistent links between Document Generation and its
resulting Case Document have not yet been finalized.

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

## 4. Document Lifecycle

### 4.1 Template creation and publication

A system-provided or user-created Document Template receives a stable identity.
Reusable DOCX content is prepared with controlled placeholders. Publication
identifies those placeholders, requires matching structured field definitions,
and creates an immutable Template Version.

Existing visual documents that use blank lines, `XXXX`, formatting, or prose
annotations are not deterministic template contracts. They must be normalized
before publication.

Editable template working drafts are a possible future capability and are not
part of the MVP.

### 4.2 Template versioning

A new version is created only by explicit publication of changed reusable
template content or its field contract. Ordinary edits to a generated document
do not create a template version.

Published versions are immutable so an existing generation can continue to
identify the exact source and field contract it used. Similar field definitions
may therefore appear in several versions; this duplication preserves history
rather than making old versions depend on mutable central definitions.

### 4.3 Draft generation

The user selects a Case and a published Template Version. Required values are
resolved from Case information and explicit user input. Defined system
values may also participate. After validation, deterministic rendering produces
a DOCX draft, which is stored as a generated Case Document with generation
traceability.

### 4.4 Human review and finalization

The generated output remains a draft until a human explicitly finalizes the
reviewed result. Finalization records a workflow decision; it does not mean the
application independently verifies legal correctness.

The exact draft/finalized state representation, allowed transitions, and
editing experience remain open. Browser-based Word editing and replacement of
a draft with a separately reviewed document are deferred from the MVP.

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

The MVP uses:

- DOCX template sources;
- one intended generated format, DOCX;
- controlled scalar placeholders;
- structured field definitions owned by a Template Version;
- deterministic values from Case fields, defined system values, and
  explicit user input;
- basic replacement rendering that preserves supported formatting.

Controlled placeholders form the runtime contract. Publication scans the DOCX,
presents detected fields, and requires the field definitions to match before
the version is published. Detailed parsing and supported document structures
belong to the renderer decision rather than the domain model.

The MVP does not use arbitrary expressions, conditions, formulas, repeating
collections, or AI discovery as the runtime template contract. Word content
controls, merge fields, and bookmarks are not the initial marker mechanism.

Template definition, value acquisition, and rendering remain separate:

```text
Template definition -> declares required values
Value acquisition   -> supplies resolved input values
Rendering           -> applies those values deterministically to DOCX
```

The MVP does not introduce a global semantic-field catalog. Unambiguous Case or
system bindings may supply deterministic defaults, while context-dependent
values may remain local user-input fields. A narrowly governed semantic catalog
for precise evidence-derived facts is only a future decision gate before
AI-assisted extraction.

## 6. Generation Workflow

### 6.1 Inputs

- a Case;
- one exact published Template Version;
- resolved values from the Case and defined system sources;
- explicit user-provided values required by the template contract.

### 6.2 Process

```text
Load Case and Template Version
        │
        v
Resolve and validate required values
        │
        v
Render deterministic DOCX draft
        │
        v
Store generated binary
        │
        v
Persist Case Document metadata and generation traceability
        │
        v
Return generated draft for human review
```

### 6.3 Output

The output is a DOCX draft associated with the Case, represented in the Case's
document collection as generated output and linked conceptually to its exact
Template Version through generation traceability.

### 6.4 Responsibility and failure boundaries

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
the deterministic MVP.

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
They are deferred because the MVP needs a small, testable generation contract
before supporting more formats or expressive template behavior.

## 9. Open Questions

The following questions are unresolved and must be decided in their relevant
future tickets.

### Template fields and persistence details

- What are the final scalar value types and deterministic source categories?
- Which combinations of source and source key are valid?
- What exact constraints and metadata are required beyond the confirmed field
  contract?

### Template publication and parsing

- What is the exact controlled-placeholder grammar and escaping behavior?
- How should duplicate tokens, missing or extra definitions, and unsupported
  document constructs be reported?
- Which rendering library should be selected after comparing representative
  DOCX fixtures?

The decision to use controlled DOCX placeholders is confirmed. The unresolved
question is the detailed parsing and rendering strategy, not whether runtime
field discovery should be arbitrary.

### Generation traceability and lifecycle

- Is Document Generation itself a durable business entity, or is only a record
  of its traceability durable?
- What is the exact generation-record model and its relationship to the
  resulting Case Document?
- Which generation values must be retained for reproducibility?
- What are the exact draft/finalized state representation and allowed
  transitions?
- What business commitment does finalization represent, and is it reversible?
- How should repeated finalization, stale state, and later document corrections
  behave?
- Is a finalized generated binary immutable, and how should later revisions be
  represented?
- What review experience is supported while browser-based Word editing and
  reviewed-document replacement remain deferred?

### Future document formats

- If future output formats are introduced, how do they relate to the stable
  Document Template, its versions, and generated output?

The MVP decision remains one DOCX source and DOCX output per published Template
Version. This question does not expand the current scope.

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
