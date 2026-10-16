#!/bin/sh
# ---------------------------------------------------------------------------
# TravelLens backend entrypoint - Milestone 4
#
# Reads secrets from Secret Manager at container start and hands them to the
# JVM as environment variables, then replaces itself with the Java process.
#
# The point of doing it here rather than in docker-compose.yml is that no
# secret value is ever written to disk: there is no .env file on the VM, no
# secret baked into the image, and nothing in the compose file but names. The
# only copy exists in this process's memory for the life of the container.
#
# Authentication uses the instance metadata server, which hands out a token for
# the service account attached to the VM. That means no key file to store,
# leak, or rotate - the identity belongs to the machine, not to a file.
# ---------------------------------------------------------------------------
set -eu

METADATA_URL="http://metadata.google.internal/computeMetadata/v1"
METADATA_HEADER="Metadata-Flavor: Google"

log() { echo "[entrypoint] $*"; }

log "Requesting access token from the instance metadata server..."
TOKEN_RESPONSE="$(curl -s --fail --max-time 10 \
  -H "${METADATA_HEADER}" \
  "${METADATA_URL}/instance/service-accounts/default/token" || true)"

if [ -z "${TOKEN_RESPONSE}" ]; then
  echo "[entrypoint] ERROR: could not reach the metadata server." >&2
  echo "[entrypoint] This container must run on a GCE VM with a service" >&2
  echo "[entrypoint] account attached and the cloud-platform scope." >&2
  exit 1
fi

ACCESS_TOKEN="$(echo "${TOKEN_RESPONSE}" | jq -r '.access_token')"
PROJECT_ID="$(curl -s --fail -H "${METADATA_HEADER}" "${METADATA_URL}/project/project-id")"
SERVICE_ACCOUNT="$(curl -s --fail -H "${METADATA_HEADER}" \
  "${METADATA_URL}/instance/service-accounts/default/email")"

log "Project: ${PROJECT_ID}"
log "Identity: ${SERVICE_ACCOUNT}"

# Fetches the latest version of one secret. Prints the value; exits non-zero
# with a readable message if the call fails or the secret is empty.
fetch_secret() {
  secret_name="$1"
  response="$(curl -s --fail --max-time 15 \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "https://secretmanager.googleapis.com/v1/projects/${PROJECT_ID}/secrets/${secret_name}/versions/latest:access" \
    || true)"

  if [ -z "${response}" ]; then
    echo "[entrypoint] ERROR: could not read secret '${secret_name}'." >&2
    echo "[entrypoint] Check that the secret exists and that ${SERVICE_ACCOUNT}" >&2
    echo "[entrypoint] holds roles/secretmanager.secretAccessor." >&2
    exit 1
  fi

  value="$(echo "${response}" | jq -r '.payload.data' | base64 -d)"

  if [ -z "${value}" ]; then
    echo "[entrypoint] ERROR: secret '${secret_name}' resolved to an empty value." >&2
    exit 1
  fi

  printf '%s' "${value}"
}

log "Reading secrets from Secret Manager..."
SPRING_DATASOURCE_PASSWORD="$(fetch_secret "${DB_PASSWORD_SECRET:-travellens-db-password}")"
APP_JWT_SECRET="$(fetch_secret "${JWT_SECRET_NAME:-travellens-jwt-secret}")"
export SPRING_DATASOURCE_PASSWORD APP_JWT_SECRET

# Lengths only - never the values themselves.
log "Loaded db password (${#SPRING_DATASOURCE_PASSWORD} chars) and JWT key (${#APP_JWT_SECRET} chars)."
log "Starting the application."

exec java -XX:MaxRAMPercentage=70 -jar /app/backend.jar
