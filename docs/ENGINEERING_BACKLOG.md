# Engineering Backlog

## Purpose

This file tracks accepted debugging tickets and product improvements that are outside the current implementation scope. It is a delivery-tracking aid and does not override `PRODUCT.md`, `DOCUMENT_DOMAIN.md`, `DECISIONS.md`, or `ROADMAP.md`.

## Priority

- `P0` — production emergency, security vulnerability, data corruption, or outage;
- `P1` — major broken workflow or materially misleading behavior;
- `P2` — important correctness or usability improvement with a workaround;
- `P3` — low-impact enhancement or test improvement.

## Open Items

### BUG-001 — Case Detail fields appear empty after tab switching

- Priority: `P1`
- Type: Bug
- Status: Open
- Area: Case Detail
- Evidence: During G5 stale-value testing, switching Case Detail tabs caused loaded fields to show empty/default values until the page was reloaded.
- Impact: A user may believe Case information was lost or begin editing an incorrect empty form.
- Boundary: The behavior predates and is outside the document-generation component changes.
- Acceptance criteria:
  - reproduce the issue deterministically;
  - verify whether tab mounting/unmounting unregisters `vee-validate` fields;
  - preserve loaded and unsaved values across tab changes;
  - add a component regression test;
  - verify a page reload is no longer required.
- Related work: Case Detail frontend integration.

### INV-001 — Migrate the complete system to a consistent UTC instant contract

- Priority: `P1`
- Type: Cross-cutting migration / bug
- Status: Open
- Area: Cross-cutting API and frontend
- Evidence: G5 manual testing observed a generated-document time resembling GMT rather than the browser's `Asia/Shanghai` time. The current Docker backend and MySQL run in UTC, but most APIs expose timezone-less Java `LocalDateTime` values. G5 now gives document generation its own explicit UTC-instant response contract; other timestamp consumers remain inconsistent.
- Impact: Event times outside the scoped document-generation contract may be interpreted as browser-local even when produced in another timezone, resulting in factually misleading displays.
- Current containment: Generated-document event time is serialized as an explicit UTC instant and rendered in the browser timezone. The Case-document ID remains a stable secondary identifier. This containment does not establish a system-wide policy.
- Acceptance criteria:
  - deliver the migration as one dedicated, independently reviewed commit rather than mixing partial timestamp changes into feature work;
  - document the database, Hibernate/JDBC, persistence, and API timestamp policy;
  - audit historical Case, uploaded-document, template/version, reminder, notification, user, and other event timestamps before assigning UTC semantics;
  - persist event times as UTC instants and serialize them with an explicit offset or UTC `Z`;
  - preserve date-only legal and calendar values without accidental timezone conversion;
  - render user-visible event times in the intended user/browser timezone;
  - add backend persistence/serialization and frontend conversion tests;
  - review API compatibility and ordering/pagination behavior for every existing timestamp consumer.
- Related decision: `DECISIONS.md` future timezone-policy consideration.

### FEAT-001 — User-selected generated DOCX filename

- Priority: `P2`
- Type: Product feature
- Status: Open
- Area: Document generation
- Summary: Allow a user to specify the generated filename before generation, with a server-generated default when blank.
- Boundary: G5 uses the existing backend-generated filename contract.
- Acceptance criteria:
  - define allowed length and characters;
  - reject path characters and unsafe names;
  - define `.docx` extension behavior;
  - define duplicate-name behavior;
  - update the API and persisted Case-document filename contract;
  - test default and user-selected names.

### FEAT-002 — Semantic phone-number validation

- Priority: `P3`
- Type: Product feature
- Status: Open
- Area: Template fields and generation validation
- Summary: Support optional phone-number semantics without hardcoding validation to a template-local key such as `contact_phone`.
- Boundary: Phase 5 intentionally supports generic `TEXT`, `DATE`, `DECIMAL`, and `BOOLEAN` scalars.
- Acceptance criteria:
  - decide between validation metadata and an additional semantic type;
  - define supported mobile, landline, country-code, spacing, and extension formats;
  - validate consistently at publication and generation boundaries;
  - retain generic text fields for templates that do not request phone semantics.

### UX-001 — Expanded read-only Case and template preview

- Priority: `P3`
- Type: UX improvement
- Status: Open
- Area: Document generation
- Summary: Show additional read-only Case context and template metadata during selection.
- Boundary: This item must not introduce browser DOCX editing or imply that field-contract inspection renders Word content.
- Acceptance criteria:
  - define the additional Case fields useful during selection;
  - keep exact template-version selection explicit;
  - distinguish field-contract details from document-content preview.

### TEST-001 — Deterministic duplicate-click browser verification

- Priority: `P3`
- Type: Test improvement
- Status: Open
- Area: Document generation
- Summary: Add a repeatable browser-level assertion that rapid duplicate activation produces one generation request and one Case document.
- Boundary: Component-level duplicate-submit protection remains required in G5.
- Acceptance criteria:
  - exercise rapid repeated activation under network throttling;
  - assert one generation POST;
  - assert one successful result and one persisted Case document.
