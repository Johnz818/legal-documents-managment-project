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

- Frontend build and type diagnostics are not yet clean.
- Backend coverage is measured informally and has no enforced threshold.
- The frontend has no automated unit or component test foundation.
- Runtime endpoints and allowed origins are not fully environment-driven.
- Containers, CI/CD, deployment, authentication, and operational monitoring are
  not implemented.
- Case documents and reminders remain mock-backed.

## Delivery Sequence

### Phase 1 — Engineering Baseline

- Align product, decision, and roadmap documentation with the repository.
- Resolve existing frontend build and type diagnostics.
- Enforce meaningful backend line coverage of at least 90%.
- Introduce frontend unit and component testing.
- Externalize frontend API and backend CORS configuration.

### Phase 2 — Case Document Management

- Define the Case document contract and storage boundary.
- Persist document metadata in MySQL.
- Store file content outside MySQL.
- Support local storage for development.
- Add upload, list, and download APIs for PDF and Word documents.
- Integrate documents into Case Detail without migrating reminders.
- Add an S3-compatible storage implementation.

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

### Phase 5 — Minimum Security

- Implement the User and role domain.
- Add authentication.
- Enforce backend authorization for Case mutations and document operations.
- Integrate authentication into the frontend.

Minimum security precedes public deployment. An earlier demonstration
environment may use synthetic data only and must restrict or disable unauthenticated
mutation operations.

### Phase 6 — Cloud Deployment

- Select and document hosting, managed MySQL, object storage, domains, TLS, and
  secret-management choices.
- Add production configuration.
- Deploy and verify a staging environment.
- Document deployment, migration, rollback, and recovery procedures.

### Phase 7 — Reliability and Performance

- Add health checks, metrics, and structured logging.
- Define and rehearse database and object-storage recovery.
- Establish k6 performance baselines using realistic synthetic data.
- Optimize queries and indexes only from measured evidence.
- Add operational limits, monitoring, and incident documentation.

## Ticket Rules

Every ticket must:

1. Start with repository and documentation analysis.
2. Deliver one small, logically complete capability.
3. Identify affected files, tests, risks, and one focused commit boundary.
4. Keep business changes separate from infrastructure changes.
5. Update product or decision documentation when behavior or architecture
   changes.
6. Wait for approval before implementation.
