# Continuous Integration

GitHub Actions runs the repository's established application verification on
pull requests and pushes to `main`. Backend and frontend jobs run independently
so one does not delay or hide failures in the other.

## Backend verification

The backend job uses:

- Eclipse Temurin Java 21;
- Maven dependency caching based on `backend/pom.xml`;
- an ephemeral MySQL 9.7.1 service;
- database `legal_case_management_test`;
- `mvn --batch-mode --no-transfer-progress verify`.

Maven verification runs unit and integration tests, applies the Flyway
migrations to the fresh database, validates the JPA mapping, and enforces the
90% JaCoCo line-coverage gate.

The MySQL username and passwords in the workflow are intentionally disposable
test values. The database exists only for the job, contains synthetic test
records, and is destroyed with the runner. Local, staging, and production
credentials must not be added to this workflow.

The generated JaCoCo HTML report is uploaded for seven days as the
`backend-jacoco-report` workflow artifact.

## Frontend verification

The frontend job uses Node 22 and the pnpm version declared in
`frontend/package.json`. It runs:

```bash
pnpm install --frozen-lockfile
pnpm test
pnpm exec astro check
pnpm build
```

The pnpm package store is cached using `frontend/pnpm-lock.yaml`. The workflow
does not cache `node_modules`, Astro output, or build output; every run must
still perform installation and build steps successfully.

## Workflow security

The workflow has read-only repository-content permission. Third-party and
GitHub-maintained actions are pinned to reviewed commit SHAs, with release
versions recorded in comments for maintainability.

CI1 does not use:

- repository secrets;
- local `.env` files;
- production database credentials;
- cloud credentials;
- registry credentials.

## Local equivalents

Run backend verification with Java 21 and the dedicated local MySQL test
database:

```bash
cd backend
mvn --batch-mode --no-transfer-progress verify
```

Run frontend verification:

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm test
pnpm exec astro check
pnpm build
```

## Hosted verification

The workflow becomes active after this repository is pushed to GitHub. The
first hosted run must confirm both jobs pass and that the JaCoCo artifact can
be downloaded before CI1 is considered fully operational.

Branch protection or a repository ruleset should require the Backend and
Frontend checks only after GitHub has registered their names through a
successful workflow run.
