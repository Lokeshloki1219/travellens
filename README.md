# TravelLens

Destination discovery, advisor booking with double-booking prevention, and a
traveler experience feed with photo uploads.

Spring Boot 4.1 (Java 17) + React 18 (Vite) + PostgreSQL 16, running as three
containers on a private Docker network.

## Architecture

| Service  | Image                         | Exposed       |
|----------|-------------------------------|---------------|
| frontend | nginx:1.27-alpine             | host port 80  |
| backend  | eclipse-temurin:17-jre-alpine | internal only |
| db       | postgres:16-alpine            | internal only |

nginx serves the built React bundle and reverse-proxies /api and /uploads to
the backend. Only port 80 is published; the API and database are reachable
only from inside the travellens network.

Two named volumes hold all state: pgdata for the database and uploads for
traveler photos. The images themselves are stateless.

## Architecture (Milestone 4)

No application state lives on the compute layer.

- **Database** - Cloud SQL PostgreSQL 16, private IP only, reached over VPC peering
- **Uploads** - Cloud Storage bucket with public access prevention, written and
  read using the VM's service account via Application Default Credentials
- **Secrets** - Secret Manager, read by the container entrypoint at startup
- **Schema** - created by `db/migration/V1__initial_schema.sql` (Flyway);
  Hibernate runs with `ddl-auto=validate` and verifies rather than alters

## Running

Requires a GCE VM with a service account holding secretmanager.secretAccessor
and storage.objectAdmin, plus a reachable Cloud SQL instance.

    docker compose -f docker-compose.prod.yml up -d

There is no .env file. The entrypoint reads the database password and JWT
signing key from Secret Manager using the instance metadata server, so no
credential is stored on disk, in the image, or in this repository.

Note: on Spring Boot 4, `flyway-core` alone is not enough - the
`spring-boot-flyway` auto-configuration module must also be present, or
migrations silently never run.