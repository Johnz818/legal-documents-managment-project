# Local Container Environment

Docker Compose runs the frontend, backend, and MySQL as one reproducible local
environment. MySQL data and uploaded document content live in separate named
volumes so containers can be replaced without losing application data.

## Prerequisites

- Docker with Docker Compose
- Host ports 4321 and 8080 available

The Compose environment does not use the host MySQL server or its database.

## Configure credentials

Copy the non-secret template:

```bash
cp .env.example .env
```

Set local-only values in `.env`:

```dotenv
LEGAL_DB_USERNAME=case_management
LEGAL_DB_PASSWORD=replace-with-local-application-password
LEGAL_DB_ROOT_PASSWORD=replace-with-local-root-password
```

The root `.env` file is ignored by Git. These credentials are for the local
Compose MySQL instance only. Compose creates the application database and user
when the MySQL volume is initialized for the first time.

Changing credentials in `.env` does not update accounts in an existing MySQL
volume. Resetting the volume or changing accounts inside MySQL is required.

## Start the environment

Validate configuration without printing expanded values:

```bash
docker compose config --quiet
```

Build and start all services:

```bash
docker compose up --build --detach
docker compose ps
```

Application endpoints:

- Frontend: `http://localhost:4321/case-list-view.html`
- Backend API: `http://localhost:8080/api/cases`
- Frontend-proxied API: `http://localhost:4321/api/cases`

MySQL is available only on the internal Compose network. It is intentionally
not published on a host port.

## Inspect the environment

Follow all service logs:

```bash
docker compose logs --follow
```

Follow one service:

```bash
docker compose logs --follow backend
```

Rebuild one application after source changes:

```bash
docker compose up --build --detach backend
docker compose up --build --detach frontend
```

## Stop or reset

Stop and remove containers while preserving data:

```bash
docker compose down
```

The next `docker compose up` reuses the `mysql-data` and `document-data`
volumes.

Permanently delete the Compose database and uploaded documents:

```bash
docker compose down --volumes
```

This command is destructive. Use it only when a complete local reset is
intended.

## Persistence boundary

The local environment uses:

- `mysql-data` for MySQL tables and Flyway history;
- `document-data` for uploaded PDF, DOC, and DOCX content.

Named volumes support local development and single-host verification. They are
not the planned production storage architecture. A future deployment will use
managed MySQL and an S3-compatible implementation of `DocumentStorage`.

## Troubleshooting

If startup reports that port 4321 or 8080 is already in use, stop the locally
running Astro or Spring Boot process before starting Compose.

If MySQL becomes healthy but the backend does not start, inspect:

```bash
docker compose logs backend
```

Flyway must apply the repository migrations before Hibernate validates the JPA
mapping. Do not bypass failures by enabling Hibernate schema generation.
