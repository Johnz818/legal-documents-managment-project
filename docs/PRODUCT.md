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
- Define template variables.
- Select a case and template.
- Generate a document using case and lawyer data.
- Preview and edit generated content.

Example:

The template variable `{{案号}}` is replaced with `(2026)沪0115民初1001号`, and `{{主办律师}}` is replaced with `李律师`.

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
- Field-value acquisition supplies values from approved Case fields, explicit
  user input, or future evidence-extraction suggestions.

The application supports system-provided and user-created reusable templates.
Editing a generated document for one Case changes that document only; it does
not modify the reusable template or publish a new template version.

The initial product milestone requires Case-backed defaults, manual input,
deterministic DOCX generation, and human finalization. OCR and AI-assisted
extraction are future assistance capabilities, not prerequisites for the first
usable generation workflow. AI output is never treated as an approved legal
fact without human review.

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
- Template content
- Last modification date
- Optional creator

A template has a stable identity and one or more immutable published DOCX
versions. Field definitions belong to the exact published version. A future
template editor may maintain an unpublished working draft, but ordinary edits
to a generated Case document do not create template versions.

Template types:

- `系统预设`
- `用户自定义`

### Template Variable

Maps a placeholder in a document template to case, user, or system data.

Each field has a stable key, Chinese display label, scalar value type, required
state, deterministic default source, and display order. Future extracted values
also carry provenance, confidence, and human-review state; those facts belong
to the generation result rather than the template definition.

Examples:

- `{{案号}}`
- `{{法院}}`
- `{{原告}}`
- `{{被告}}`
- `{{主办律师}}`
- `{{当前日期}}`

### Generated Document

Represents a document generated for a case.

Key information:

- Document ID
- Case ID
- Template ID
- Document name
- Case stage at generation time
- Generation date
- Generated or edited content

A case may have multiple generated documents. Each generated document uses one template.

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
 ├─ generates → Documents
 ├─ schedules → Reminders
 └─ produces → Notifications

Document
 └─ uses → Document Template

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

Document templates, generation, and preview/editing continue to use the legacy
mock models. Generated mock documents are not presented as persisted Case files.

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
- Case-related reminders and document generation remain mock-backed.
- Authentication and authorization are not implemented.
- Audit-log behavior is represented in navigation and permissions but is not implemented.
