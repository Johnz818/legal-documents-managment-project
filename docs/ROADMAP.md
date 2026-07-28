# Engineering Roadmap

The project is evolving from a local Case Management MVP into a reproducible,
deployable, and operationally credible full-stack application.

The roadmap prioritizes completing the engineering lifecycle before expanding
into unrelated business modules.

## Current State

Completed:

- Astro and Vue frontend integrated with a Spring Boot REST API.
- MySQL schema managed exclusively by Flyway.
- Case list, detail, search, creation, update, archive, and restore workflows.
- Optimistic locking for Case updates and archive-state changes.
- Backend integration tests against a dedicated MySQL test database.

Current engineering gaps:

- Case-related documents remain mock-backed.
- Runtime endpoints and allowed origins are not fully environment-driven.
- Containers, CI/CD, deployment, authentication, and operational monitoring are
  not implemented.
- Template generation and reminders remain mock-backed.

## Progress Snapshot

| Phase | Status | Repository evidence | Next action |
| --- | --- | --- | --- |
| Phase 1 — Engineering Baseline | Complete for the approved local-development scope | E1–E4 are committed; frontend checks/tests/build pass and the backend enforces its 90% line-coverage gate. E5 was deliberately reassigned to deployment configuration. | Begin Case document management. |
| Phase 2 — Case Document Management | In progress | D1–D5 define and persist the document boundary, provide local binary storage, and accept validated case document uploads. | Implement D6, the document read APIs. |
| Phase 3 — Containerization | Not started | No Dockerfiles or Compose environment exist. | Start after the local upload/download journey works. |
| Phase 4 — Continuous Integration | Not started | No repository CI workflow exists. | Start after reproducible local containers exist. |
| Phase 5 — Minimal Document Generation | Planned | D-019 defines the deliberately limited generation boundary. | Start after containerization and CI. |
| Phase 6 — Minimum Security | Not started | User, authentication, and backend authorization remain unimplemented. | Complete before a customer-data deployment. |
| Phase 7 — Cloud Deployment | Not started | Hosting and production architecture are not selected. | Begin after CI and minimum security decisions. |
| Phase 8 — Reliability and Performance | Not started | No operational baseline exists. | Begin after a staging deployment is reproducible. |

## Delivery Sequence

### Phase 1 — Engineering Baseline

- Align product, decision, and roadmap documentation with the repository.
- Resolve existing frontend build and type diagnostics.
- Enforce meaningful backend line coverage of at least 90%.
- Introduce frontend unit and component testing.

Runtime endpoint and CORS externalization is intentionally deferred until
containerization and deployment configuration require it.

### Phase 2 — Case Document Management

- Define the Case document contract and storage boundary.
- Persist document metadata in MySQL.
- Store file content outside MySQL.
- Support local storage for development.
- Add upload, list, and download APIs for PDF and Word documents.
- Integrate documents into Case Detail without migrating reminders.
- Defer image uploads and template generation.

### Phase 3 — Containerization

- Add independently buildable backend and frontend images.
- Add a Compose environment for the frontend, backend, MySQL, and object
  storage.
- Preserve environment-based secret configuration.

### Phase 4 — Continuous Integration

- Run backend tests against MySQL and enforce the coverage gate.
- Run frontend tests, type checks, and production builds.
- Verify container image builds.
- Publish versioned images only after a deployment target is selected.

### Phase 5 — Minimal Document Generation

- Persist reusable templates with immutable DOCX versions.
- Define structured scalar template fields.
- Generate DOCX drafts from approved Case and user-provided values.
- Require explicit human finalization.
- Keep PDF conversion, browser editing, images, OCR, and AI deferred.

### Phase 6 — Minimum Security

- Implement the User and role domain.
- Add authentication.
- Enforce backend authorization for Case mutations and document operations.
- Integrate authentication into the frontend.

Minimum security precedes public deployment. An earlier demonstration
environment may use synthetic data only and must restrict or disable unauthenticated
mutation operations.

### Phase 7 — Cloud Deployment

- Select and document hosting, managed MySQL, object storage, domains, TLS, and
  secret-management choices.
- Add production configuration.
- Externalize frontend API endpoints and backend allowed origins.
- Add an S3-compatible document-storage implementation.
- Deploy and verify a staging environment.
- Document deployment, migration, rollback, and recovery procedures.

### Phase 8 — Reliability and Performance

- Add health checks, metrics, and structured logging.
- Define and rehearse database and object-storage recovery.
- Establish k6 performance baselines using realistic synthetic data.
- Optimize queries and indexes only from measured evidence.
- Add operational limits, monitoring, and incident documentation.

## Phase Verification Strategy

| Phase | Required testing strategy |
| --- | --- |
| Phase 1 — Engineering Baseline | Run the Java 21 Maven verification suite against the Flyway-managed MySQL test database; enforce backend coverage; run frontend unit/component tests, Astro type checking, and the production build. |
| Phase 2 — Case Document Management | Unit-test storage contracts and validation; run metadata repository tests against MySQL; test local storage with temporary directories and path-safety cases; exercise multipart upload, compensation, list, and streaming-download behavior through HTTP integration tests; cover frontend loading, empty, error, upload, and download states plus a manual Case Detail journey. |
| Phase 3 — Containerization | Build each image from a clean context; verify non-root backend execution; start a fresh Compose environment; confirm Flyway startup, service health, frontend-to-backend routing, persistent database/file volumes, and restart behavior. |
| Phase 4 — Continuous Integration | Run all backend, frontend, database, coverage, and image checks from a clean checkout; verify dependency caching does not hide missing setup; confirm failures block merging and successful builds produce reproducible results. |
| Phase 5 — Minimal Document Generation | Test template/version persistence against MySQL; use representative DOCX fixtures for placeholders split across formatting runs and tables; verify missing/extra input handling, formatting preservation, generation compensation, immutable version traceability, draft/finalized transitions, and frontend review/error states. |
| Phase 6 — Minimum Security | Test valid and invalid login, disabled users, expiry/logout, and password handling; exercise every protected operation as allowed, unauthenticated, and forbidden roles; verify frontend protected navigation without treating UI visibility as authorization. |
| Phase 7 — Cloud Deployment | Validate production configuration without exposing secrets; run migrations against staging; verify TLS, CORS/routing, object storage, upload/download, synthetic end-to-end journeys, restart behavior, and a rehearsed rollback. |
| Phase 8 — Reliability and Performance | Verify health/readiness and metrics during dependency failures; rehearse database and object-storage restoration; run repeatable k6 workloads with realistic synthetic volume; compare query plans and latency before and after any optimization. |

## Planned Tickets

Ticket order is intentional. A ticket may be refined after repository analysis,
but combining ticket boundaries requires a separate scope review.

### Phase 1 — Engineering Baseline

| Ticket | Outcome | Verification | Suggested commit summary |
| --- | --- | --- | --- |
| E1 — Roadmap alignment | Align repository documentation with completed Case capabilities and the production-readiness sequence. | Documentation diff and consistency review. | `docs: define production readiness roadmap` |
| E2 — Frontend verification cleanup | Resolve project-owned Astro, TypeScript, and static-build diagnostics without changing application behavior. | Clean frontend type check and production build; smoke-test affected pages. | `fix: clean frontend build diagnostics` |
| E3 — Backend coverage enforcement | Add a JaCoCo gate requiring at least 90% line coverage for application-owned backend code. | Java 21 Maven tests against the MySQL test database and a passing coverage check. | `test: enforce backend coverage threshold` |
| E4 — Frontend test foundation | Introduce unit and component testing for API, service, and critical Case UI behavior. | Frontend test suite, type check, and production build. | `test: establish frontend test foundation` |
| E5 — Runtime configuration | Deferred to containerization/deployment so endpoint and origin configuration follows the selected routing architecture. | Local integration under default and overridden environment settings. | `chore: externalize runtime endpoints` |

### Phase 2 — Case Document Management

| Ticket | Outcome | Verification | Suggested commit summary |
| --- | --- | --- | --- |
| D1 — Document architecture (complete) | Record Case ownership, format support, storage, lifecycle, template, generation, and deferred AI boundaries. | Documentation diff and decision review. | `docs: define document workflow architecture` |
| D2 — Document storage contract (complete) | Define minimal store, open, and compensating-remove operations without framework or provider coupling. | Contract-focused unit tests and backend coverage check. | `feat: define document storage contract` |
| D3 — Document metadata persistence (complete) | Add the Flyway schema, JPA mapping, repository, and required Case relationship. | Flyway and repository integration tests against MySQL. | `feat: persist case document metadata` |
| D4 — Local storage adapter (complete) | Store development files outside public directories using generated safe keys. | Temporary-directory integration tests including path-safety cases. | `feat: add local document storage` |
| D5 — Document upload API (complete) | Upload validated PDF, DOC, and DOCX files up to 5 MB against an existing Case with compensating cleanup. | API tests for success, missing Case, invalid type, oversize file, and cleanup failure paths. | `feat: add case document upload API` |
| D6 — Document read APIs | List Case documents and download content with safe response metadata. | API tests for list, download, empty result, and missing resources. | `feat: add case document read APIs` |
| D7 — Frontend document integration | Replace only the Case Detail document mock flow with live upload, list, and download behavior. | Frontend tests and manual loading, empty, error, upload, and download checks. | `feat: integrate case documents` |

### Phase 3 — Containerization

| Ticket | Outcome | Verification | Suggested commit summary |
| --- | --- | --- | --- |
| C1 — Backend image | Build and run the Java 21 backend as a non-root container. | Image build, startup, database migration, and health check. | `build: containerize backend` |
| C2 — Frontend image | Build and serve the frontend through a reproducible production image. | Image build and browser smoke test with configured API routing. | `build: containerize frontend` |
| C3 — Compose environment | Start frontend, backend, MySQL, and object storage as one local environment. | Fresh `docker compose up` and end-to-end smoke test. | `build: add local compose environment` |

### Phase 4 — Continuous Integration

| Ticket | Outcome | Verification | Suggested commit summary |
| --- | --- | --- | --- |
| CI1 — Verification workflow | Run backend MySQL tests and coverage plus frontend tests, checks, and build on GitHub Actions. | Passing workflow from a clean checkout. | `ci: add application verification workflow` |
| CI2 — Container verification | Build backend and frontend container images on pull requests without publishing. | Passing image-build workflow. | `ci: verify container images` |
| CI3 — Image publishing | Publish immutable, versioned images after release and hosting conventions are approved. | Dry run and test release with traceable tags. | `ci: publish release images` |

### Phase 5 — Minimal Document Generation

| Ticket | Outcome | Verification | Suggested commit summary |
| --- | --- | --- | --- |
| G1 — Template persistence | Persist template identity, immutable DOCX versions, and structured scalar field definitions. | Flyway and repository integration tests against MySQL. | `feat: persist versioned document templates` |
| G2 — Template API | Upload, list, and retrieve DOCX templates and their field definitions. | API tests for valid templates, validation failures, missing resources, and immutable-version behavior. | `feat: add document template API` |
| G3 — DOCX renderer | Replace approved scalar placeholders while preserving supported basic formatting. | Renderer fixture tests for replacement, missing values, unsupported constructs, and unchanged formatting. | `feat: render docx templates` |
| G4 — Draft generation | Coordinate Case values, user input, rendering, storage, metadata, compensation, and generation records. | Service and API tests for success, validation, storage failure, and persistence cleanup. | `feat: generate document drafts` |
| G5 — Human finalization | Download drafts and explicitly finalize reviewed generation records without automatic approval. | Tests for draft retrieval, valid finalization, repeated finalization, and stale or missing records. | `feat: finalize generated documents` |
| G6 — Frontend integration | Replace only the relevant template and generation mock flow with draft generation and explicit finalization. | Frontend tests and manual template-selection, input, generation, download, error, and finalization checks. | `feat: integrate document generation` |

Deferred generation work includes reviewed-document replacement, DOCX-to-PDF
conversion, advanced template syntax, image uploads, OCR, and AI-assisted
extraction.

### Phase 6 — Minimum Security

| Ticket | Outcome | Verification | Suggested commit summary |
| --- | --- | --- | --- |
| S1 — User and role domain | Persist active users, secure password hashes, and a minimal role model; finalize lawyer relationships. | Flyway, repository, and domain tests against MySQL. | `feat: add user and role domain` |
| S2 — Authentication | Authenticate users using the approved session or token design. | Successful, invalid, disabled-user, and expiry integration tests. | `feat: add user authentication` |
| S3 — Backend authorization | Protect Case mutations and document operations with backend-enforced permissions. | Allowed, unauthenticated, and forbidden API tests. | `feat: authorize case operations` |
| S4 — Frontend authentication | Add login, session handling, protected navigation, and unauthorized-state handling. | Frontend tests and authenticated browser journey. | `feat: integrate frontend authentication` |

### Phase 7 — Cloud Deployment

| Ticket | Outcome | Verification | Suggested commit summary |
| --- | --- | --- | --- |
| P1 — Deployment architecture | Select and document hosting, database, storage, domains, TLS, secret management, and expected cost. | Architecture and security review. | `docs: define cloud deployment architecture` |
| P2 — Production configuration | Add production-safe runtime profiles and secret injection without committed credentials. | Staging configuration validation and startup. | `chore: add production configuration` |
| P3 — Staging deployment | Deploy the complete system with synthetic data to the selected staging environment. | End-to-end smoke test and migration verification. | `ops: deploy staging environment` |
| P4 — Release workflow | Document and automate controlled deployment, migration, rollback, and recovery. | Deployment and rollback rehearsal. | `ci: add production release workflow` |

### Phase 8 — Reliability and Performance

| Ticket | Outcome | Verification | Suggested commit summary |
| --- | --- | --- | --- |
| O1 — Health and observability | Add readiness, health, structured logging, and application metrics. | Failure/recovery checks and metric inspection. | `ops: add application observability` |
| O2 — Backup and recovery | Define and rehearse MySQL and document-storage backup and restoration. | Recorded restore rehearsal using non-production data. | `ops: document backup and recovery` |
| O3 — Performance baseline | Add k6 scenarios for Case list, search, and document upload using realistic synthetic volume. | Reproducible baseline report with stated thresholds. | `test: add performance baseline` |
| O4 — Evidence-based optimization | Apply measured database or storage improvements without speculative indexes. | Before-and-after query and load-test measurements. | `perf: optimize measured bottlenecks` |
| O5 — Operational hardening | Add justified limits, timeouts, error monitoring, dependency maintenance, and incident guidance. | Failure-mode tests and operational review. | `ops: harden production operations` |

## Ticket Rules

Every ticket must:

1. Start with repository and documentation analysis.
2. Deliver one small, logically complete capability.
3. Identify affected files, tests, risks, and one focused commit boundary.
4. Keep business changes separate from infrastructure changes.
5. Update product or decision documentation when behavior or architecture
   changes.
6. Wait for approval before implementation.
