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

# Run as an unprivileged user rather than root.
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

# Create the upload directory owned by the app user BEFORE the named volume is
# attached, so the non-root process can write uploaded photos into it.
RUN mkdir -p /app/uploads && chown -R app:app /app

COPY --from=build --chown=app:app /build/target/backend.jar /app/backend.jar

USER app
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=45s --retries=12 \
  CMD wget -q --spider http://localhost:8080/api/destinations || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/backend.jar"]
