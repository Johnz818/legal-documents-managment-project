# Product

## Goal

Build a legal case management platform for small and medium-sized law firms.

The platform supports the full working context around a legal case: responsible lawyers, parties, court information, documents, reminders, notifications, and permissions.

## MVP Modules

- Case Management
- Document Generation
- Reminder System
- User and Permission Management

## Primary Users

### System Administrator

Manages users, roles, permissions, and system-level configuration.

Example: `王勇（系统管理员）` creates an account for a new legal assistant and assigns the assistant role.

### Lead Lawyer

Owns cases and performs core legal work.

Example: `张伟（主办律师）` manages `张三诉某公司劳动争议案`, prepares documents, and reviews hearing reminders.

### Legal Assistant

Supports assigned cases through information entry, document preparation, and reminder management.

Example: `李娜（助理人员）` uploads case information and prepares evidence-submission reminders.

## Business Use Cases

### Case Management

Users can:

- View a case list.
- Create a case manually.
- Import cases in batches.
- View and maintain case details.
- Assign a lead lawyer and supporting lawyers.
- Track case stages and important dates.
- Organize cases with tags.
- Archive cases instead of deleting them.

Example:

> 案号：`(2026)沪0115民初1001号`
>
> 案件名称：`张三诉某公司劳动争议案`
>
> 法院：`上海市浦东新区人民法院`
>
> 主办律师：`李律师`
>
> 当前状态：`审理中`

### Document Generation

Users can:

- Manage preset and custom document templates.
- Upload an existing Word document or eventually create a template from
  scratch in a browser-based editor.
- Review and adjust reusable boilerplate and suggested variable regions before
  publishing a template.
- Define template fields represented by deterministic placeholders.
- Select a case and template.
- Review values acquired from Case data, defined system values, explicit user
  input, and eventually information extracted from Case documents.
- Generate a document using only reviewed values.
- Review generation inputs and the generated draft, then explicitly finalize
  the reviewed result.

Example:

In the Phase 5 delivery, the controlled ASCII placeholder `{{case_number}}`
represents the field displayed to the user as `案号` and is replaced with
`(2026)沪0115民初1001号`; `{{lead_lawyer_name}}` is displayed as `主办律师` and
is replaced with `李律师`.

#### Final template-authoring target

The intended product is a browser-based legal-template authoring and versioning
system:

```text
Upload Word document or create a blank draft
  -> system suggests possible boilerplate and variable regions
  -> user edits paragraphs, formatting, and placeholders in the browser
  -> user accepts, rejects, or adjusts suggestions
  -> explicit publication creates an immutable template version
  -> later reusable-template edits begin in a new draft
  -> later publication creates the next immutable version
```

Imported templates may express possible variables through `XXXX`, blank lines,
brackets, underlining, color, annotations, or existing controlled markers.
Those signals are suggestions during authoring, not a reliable runtime
contract. A published version must contain a deterministic, human-approved
field contract. Draft autosaves do not create published versions.

Editing one Case-specific generated document is different from editing a
reusable template: it changes only that generated work product and never
creates a new reusable-template version.

The browser editor, legacy-DOC normalization, suggestion workflow, and
structured placeholder representation require a dedicated editor integration
and licensing spike. They are part of the final product direction but are not
implemented by the current Phase 5 milestone.

#### Evidence-assisted generation target

The long-term generation journey uses information already present in Case
files, including text documents, scanned PDFs, and images:

```text
Case files
  -> parse text or run OCR
  -> suggest values for known template fields
  -> show the supporting document and page
  -> human accepts, edits, or rejects each suggestion
  -> deterministic template rendering
  -> document-specific review and editing
  -> explicit finalization
```

For example, a Case may contain a bank statement, chat screenshots, and a
scanned pleading. The system may suggest `借款本金：200000元` for a declared
template field and identify the supporting file and page. A lawyer must confirm
or correct the value before the application generates the draft.

Template-field identification and field-value acquisition are separate
capabilities:

- Template-field identification determines which values a reusable template
  requires. The initial capability uses explicit controlled DOCX placeholders.
- Field-value acquisition supplies resolved Case values, explicit
  user input, or future evidence-extraction suggestions.

The complete target value-acquisition model is:

- **Case values:** approved structured facts already stored on the selected
  Case, such as its case number or court.
- **System values:** narrowly defined deterministic values such as the current
  date.
- **Explicit user input:** a typed value or a user correction for this one
  generation.
- **Evidence suggestions:** candidate values extracted from Word documents,
  text PDFs, scanned PDFs, or approved images associated with the Case.

Evidence suggestions do not outrank Case data or manual input automatically.
When sources disagree, the application presents the alternatives and their
provenance so a human can choose or correct the value. A file uploaded during
generation should normally become a Case Document so the resulting suggestion
can retain a stable source-document and page reference.

The application supports system-provided and user-created reusable templates.
When document-specific editing is introduced, editing a generated document for
one Case will change that document only; it will not modify the reusable
template or publish a new template version.

Phase 5 is a production-shaped vertical slice and an expandable foundation,
not the complete authoring and evidence-assisted product. It requires
controlled DOCX publication, Case-backed and system defaults, manual input,
deterministic DOCX generation, and human finalization. It deliberately reaches
an end-to-end deployable workflow before browser authoring, OCR, or AI-assisted
extraction is implemented.

Future authoring feeds the existing publication boundary; future extraction
feeds reviewed generation values. Neither capability should replace immutable
Template Versions or deterministic rendering. AI output is never treated as
an approved legal fact without human review.

#### Current Phase 5 deterministic vertical-slice workflow

The current milestone completes this production-shaped vertical slice:

```text
User prepares a DOCX with controlled ASCII placeholders
        |
        v
Upload and validate CUSTOM template content and field definitions
        |
        v
Explicitly publish immutable Template Version
        |
        v
User selects Case and exact Template Version
        |
        v
Application prefills Case and system values
        |
        v
User reviews defaults and enters or corrects remaining values
        |
        v
Application validates required Generation Values
        |
        v
Deterministic renderer creates DOCX draft
        |
        v
Store generated Case Document and generation traceability
        |
        v
User downloads, reviews, and explicitly finalizes the result
```

Phase 5 does not create a template version for an editor autosave or for an
edit made to one Case-specific generated document. It creates a version only
through explicit publication of reusable template content.

#### Eventual product workflow

The final product expands both sides of the same immutable core:

```text
TEMPLATE AUTHORING

Upload DOC/DOCX or create blank browser draft
        |
        v
System suggests variable regions from markers, formatting, context, or AI
        |
        v
User edits boilerplate and accepts, rejects, or creates placeholders
        |
        v
Explicit publication
        |
        v
Immutable Template Version + exact field contract

CASE-SPECIFIC GENERATION

Choose Case and exact Template Version
        |
        v
Collect candidate values from Case, system, manual input, and Case evidence
        |
        v
Show conflicts, confidence, and source-document/page provenance
        |
        v
Human accepts, edits, or rejects each required value
        |
        v
Deterministic rendering of approved values only
        |
        v
Browser review/edit of the Case-specific generated draft
        |
        v
Explicit finalization and retained traceability
```

#### Expandable product model

```text
TemplateSuggestion ───────────────────────> TemplateDraft
[future infrastructure candidate]
browser editor integration/session ──────> TemplateDraft
                                                |
                                                | publish
                                        v
                                       DocumentTemplate
                                                |
                                                v
                                       DocumentTemplateVersion
                                                |
                                                +── owns ──> DocumentTemplateField
                                                |
                                                v
Case ─────────────────────────────────> DocumentGeneration
                                                |
Case values ───────────────────────────┐         |
System values ─────────────────────────┼────────> GenerationValue
User input ────────────────────────────┤              |
EvidenceCandidate ─ review/approve ────┘              v
                                           deterministic renderer
                                                       |
                                                       v
                                            Generated CaseDocument
```

The future models attach before publication or before final value approval.
They do not mutate a published Template Version and do not bypass the
deterministic renderer.

### Reminder and Notification Management

Users can:

- Create reminders for key case dates.
- Create custom case tasks.
- Select system or email notification methods.
- Mark reminders as completed.
- View reminders in a calendar.
- Review case-related notifications.

Example:

> 提醒事项：`证据材料提交截止日期`
>
> 关联案件：`张三诉某公司劳动争议案`
>
> 提醒时间：`2026-08-15 17:00`
>
> 通知方式：`系统通知、邮件通知`

### User and Permission Management

Administrators can:

- Create and update users.
- Enable or disable accounts.
- Assign roles.
- Define permissions by business entity and action.

Example:

A legal assistant may view assigned cases and enter supporting information but may not manage users or system permissions.

## Conceptual Business Data Model

### Case

Represents a legal matter managed by the firm.

Key information:

- Case ID
- Case number
- Case name or cause
- Court
- Plaintiff or applicant
- Defendant or respondent
- Current stage or status
- Lead lawyer
- Supporting lawyers
- Filing, hearing, and judgment dates
- Tags
- Description
- Created and updated timestamps
- Archived state

Current backend status values:

- `待立案`
- `审理准备`
- `审理中`
- `已判决(上诉期内)`
- `上诉审理中`
- `已判决(生效)`
- `执行中`
- `已结案`

`已结案` remains supported for existing and generally closed cases. Archival is a separate state and is not a Case status.

### Case Tag

Classifies or highlights cases.

Examples:

- `紧急`
- `重大`
- `再审中`
- `合同类`
- `已归档`

A case may have multiple tags, and a tag may be associated with multiple cases.

### User

Represents a member of the law firm.

Key information:

- User ID
- Name
- Email
- Role
- Account status
- Team or department
- Creation date

Current roles:

- `系统管理员`
- `主办律师`
- `助理人员`

A user may lead multiple cases or support multiple cases.

### Role and Permission

A role groups permissions for users.

A permission identifies an action against a business entity.

Examples:

- Manage users.
- Manage roles and permissions.
- View or edit cases.
- Manage document templates.
- Review audit logs.

### Document Template

Defines reusable content for generating legal documents.

Key information:

- Template ID
- Name
- Template type
- Description
- Optional creator

A template has a stable identity and one or more immutable published DOCX
versions. Each published version owns the exact reusable DOCX content and field
contract used for generation. The final product also has mutable, unpublished
working drafts for browser authoring. Draft saves are not published versions;
explicit reusable-template publication creates a version. Ordinary edits to a
generated Case document do not create template versions.

Template types:

- `系统预设`
- `用户自定义`

### Template Field

Defines one structured value required or accepted by an exact published
template version. A controlled placeholder is the marker for that field in the
DOCX content.

Each field has a stable key, Chinese display label, scalar value type, required
state, deterministic default source, and display order. Future extracted values
also carry provenance, confidence, and human-review state; those facts belong
to the generation result rather than the template definition.

Phase 5 marker examples use ASCII machine keys with localized display labels:

- `{{case_number}}` — `案号`
- `{{court_name}}` — `法院`
- `{{plaintiff}}` — `原告`
- `{{defendant}}` — `被告`
- `{{lead_lawyer_name}}` — `主办律师`
- `{{current_date}}` — `当前日期`

The field contract is intentionally independent of the long-term Word marker
technology. A future browser editor may publish tagged content controls while
older textual-placeholder versions remain renderable through their supported
strategy.

### Generated Document

Represents a document generated for a case.

Key information:

- Document ID
- Case ID
- Exact template version used
- Document name
- Case stage at generation time
- Generation date
- Generation lifecycle state

A case may have multiple generated documents. Each generated document uses one
exact published template version.

### Reminder

Represents a case deadline, key date, or custom task.

Key information:

- Reminder ID
- Case ID
- Reminder type
- Title
- Target date
- Completion state
- Linked case stage
- Notification methods

Reminder types:

- `关键日期提醒`
- `自定义事项`
- `系统预警提醒`

Notification methods:

- `系统通知`
- `邮件通知`
- `系统与邮件`

### Notification

Represents a message shown to a user.

A notification may reference a case, reminder, document-template change, or batch operation.

Example:

> 案件 `(2026)沪0115民初1001号` 的开庭日期即将到来，请确认准备进度。

### Calendar Event

Provides a calendar representation of an incomplete reminder.

Calendar events are currently derived from reminder data rather than maintained as an independent business record.

### Case Import Result

Summarizes a batch case-import operation.

Key information:

- Total rows
- Successful rows
- Failed rows
- Failure reasons

Example failure:

> 第12行：主办律师工号不存在。

## Business Relationships

```text
User
 ├─ leads/supports → Case
 └─ creates → Custom Document Template

Case
 ├─ has → Case Tags
 ├─ has → Case Documents
 ├─ schedules → Reminders
 └─ produces → Notifications

Document Generation
 ├─ uses → Template Version
 └─ produces → Generated Document

Reminder
 ├─ produces → Calendar Event
 └─ produces → Notification

Role
 └─ grants → Permissions
```

## Current Implementation Boundary

The original frontend primarily uses mock models under `frontend/src/data`.

The Case List and Case Detail are the first frontend vertical slices using the backend API contract:

```text
CaseListTable.vue
  → caseService.ts
  → caseApi.ts
  → GET /api/cases

CaseDetailContent.vue
  → caseService.ts
  → caseApi.ts
  → GET /api/cases/{id}
  → PUT /api/cases/{id}

CaseDetailDocuments.vue
  → documentService.ts
  → documentApi.ts
  → GET /api/cases/{caseId}/documents
  → POST /api/cases/{caseId}/documents
  → GET /api/cases/{caseId}/documents/{documentId}/content
  → DELETE /api/cases/{caseId}/documents/{documentId}
```

Case List displays live scalar case information and supports API-backed filtering by case-number prefix, case-name prefix, status, and exact lead-lawyer snapshot name. Its bulk-action controls remain frontend discovery data and do not change backend state.

Case Detail displays and updates live scalar case information. Updates use optimistic locking so an older edit cannot silently overwrite a newer change. Users can archive and restore cases, and archived cases are discoverable through the Case List archive-state selector. Its Case file section lists, uploads, and downloads live PDF and Word files. Reminders remain mock-backed, and supporting members are shown as not yet available. Tags and supporting members are not synthesized from legacy case mocks.

The backend now persists stable Document Template identities, immutable version
metadata, and version-owned field definitions. Template publication APIs,
binary template storage, rendering, generation, and preview/editing are not yet
implemented, so the related frontend journeys continue to use legacy mock
models. Generated mock documents are not presented as persisted Case files.

The manual Case creation page submits the approved scalar model to the backend Case creation API. Lead lawyers still come from the temporary frontend user dataset and are persisted as name snapshots until the User domain is implemented.

Legacy mock models must not automatically be treated as backend entities. Each feature should define and approve its persistence and API contract during its own vertical-slice ticket.

## Product Roadmap

### Completed capabilities

- Persist core case information.
- View the case list using live case data.
- View case details using live case data.
- Find cases using approved lookup criteria and list filters.
- Create and persist a new case through the manual creation workflow.
- Edit and persist scalar case details.
- Archive, discover, and restore cases.
- Upload, list, download, and remove files for an existing case.
- Persist stable template identities, immutable version metadata, and
  version-owned structured field definitions.

### Planned user journeys

1. Assign user-backed supporting members and organize cases with tags.
2. Work with case-related reminders using live data.
3. Generate and review persisted documents from structured templates.
4. Access case capabilities through authenticated and authorized user accounts.

## Product Rules

- The MVP supports one organization.
- Cases are archived instead of permanently deleted.
- Flyway owns backend schema evolution.
- Backend entities are not exposed directly through APIs.
- Permissions should be enforced by the backend when authentication and authorization are introduced.

## Known Gaps

- Case List bulk actions are not connected to backend capabilities.
- Case tags and supporting members are not yet represented by backend relationship models or APIs.
- Case-related reminders remain mock-backed; document-generation persistence is
  limited to the completed template foundation and has not yet reached live
  publication or generation APIs.
- Authentication and authorization are not implemented.
- Audit-log behavior is represented in navigation and permissions but is not implemented.
