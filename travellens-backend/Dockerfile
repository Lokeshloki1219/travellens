# syntax=docker/dockerfile:1

# ---------- Stage 1: build the jar ----------
# Maven and the ~300 MB .m2 dependency cache live only in this stage and are
# thrown away, so none of it reaches the final image.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy the pom first so Docker can cache the dependency download layer.
COPY pom.xml .
RUN mvn -B dependency:go-offline -DskipTests || true

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: runtime ----------
# JRE only (no compiler, no Maven) on Alpine.
FROM eclipse-temurin:17-jre-alpine AS runtime

# curl and jq are used by the entrypoint to read secrets from Secret Manager at
# container start. Together they add roughly 5 MB.
RUN apk add --no-cache curl jq

# Run as an unprivileged user rather than root.
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=build --chown=app:app /build/target/backend.jar /app/backend.jar
COPY --chown=app:app entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

USER app
EXPOSE 8080

# No uploads directory and no volume. Traveler media goes straight to Cloud
# Storage, so the container filesystem holds nothing but the jar.

# Milestone 5: this now points at the liveness endpoint rather than a business
# route, so a local "docker run" asks the same question Kubernetes will.
#
# Kubernetes ignores Dockerfile HEALTHCHECK entirely - the kubelet runs the
# probes declared in 01-backend-deployment.yaml instead. It is kept because it
# makes the image behave sensibly under plain Docker and Compose, which is
# still how it gets smoke-tested before being pushed.
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=15 \
  CMD wget -q --spider http://127.0.0.1:8080/healthz/live || exit 1

# The entrypoint fetches secrets, exports them, then exec's the JVM so Java
# remains PID 1 and receives signals directly. PID 1 matters here: it is what
# lets Kubernetes deliver SIGTERM straight to the JVM at the end of the preStop
# sleep and get an orderly shutdown instead of a 40-second wait and a SIGKILL.
ENTRYPOINT ["/app/entrypoint.sh"]
