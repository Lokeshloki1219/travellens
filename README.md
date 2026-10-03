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

## Running locally

Requires Docker Desktop. Copy .env.example to .env, then set a database
password and a JWT signing key. The key must be at least 32 characters -
HMAC-SHA256 rejects anything shorter.

Then run: docker compose up --build

Open http://localhost once all three containers report healthy.

## Running from published images

docker-compose.prod.yml pulls the tagged images from Artifact Registry
instead of building them:

docker compose -f docker-compose.prod.yml up -d

## Configuration

No credentials are stored in this repository. Every environment-specific value
is read from the environment at run time, and the two secrets have no default
so the application fails to start rather than falling back to a known value.

POSTGRES_PASSWORD      - database password
APP_JWT_SECRET         - JWT signing key, 32+ characters
SPRING_DATASOURCE_URL  - JDBC URL
APP_UPLOAD_DIR         - absolute path for media uploads

.env is gitignored and excluded from both Docker build contexts, so these
values never enter an image.
