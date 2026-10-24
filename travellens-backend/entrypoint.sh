#!/bin/sh
# ---------------------------------------------------------------------------
# TravelLens backend entrypoint - Milestone 5
#
# Reads secrets from Secret Manager at container start and hands them to the
# JVM as environment variables, then replaces itself with the Java process.
#
# No secret value is ever written to disk: there is no .env file, nothing baked
# into the image, and nothing in the Kubernetes manifests but secret *names*.
# The only copy exists in this process's memory for the life of the container.
# Note in particular that there is no Kubernetes Secret object either - those
# are stored base64-encoded, not encrypted, and are readable by anyone with get
# access to the namespace.
#
# What changed from Milestone 4
# -----------------------------
# Functionally, nothing. The identity is still obtained from
# metadata.google.internal, which is the point worth making: in Milestone 4
# that endpoint was served by the GCE metadata server and returned a token for
# the service account attached to travellens-vm. On GKE it is served by the GKE
# metadata server and returns a token for the Google service account that
# Workload Identity has bound to this pod's Kubernetes ServiceAccount. Same
# call, same contract, no key file in either case.
#
# What did change is the retry loop below. Under Compose, Docker started this
# container on a VM whose metadata server had been answering for minutes. In a
# pod, the GKE metadata server is itself a DaemonSet, and on a freshly created
# node this container can start a second or two before it is ready to answer.
# Without a retry the container exits, Kubernetes backs off, and the pod spends
# its first minute in CrashLoopBackOff for no real reason - alarming in a
# kubectl get pods output, and completely avoidable.
# ---------------------------------------------------------------------------
set -eu

METADATA_URL="http://metadata.google.internal/computeMetadata/v1"
METADATA_HEADER="Metadata-Flavor: Google"

# 15 attempts, 2s apart: 30 seconds of patience, well inside the three-minute
# window the startup probe allows.
METADATA_MAX_ATTEMPTS=15
METADATA_RETRY_DELAY=2

log() { echo "[entrypoint] $*"; }

# -------------------------------------------------------------------------
# Wait for the metadata server, then take a token from it.
# -------------------------------------------------------------------------
ACCESS_TOKEN=""
attempt=1

while [ "${attempt}" -le "${METADATA_MAX_ATTEMPTS}" ]; do
  TOKEN_RESPONSE="$(curl -s --fail --max-time 10 \
    -H "${METADATA_HEADER}" \
    "${METADATA_URL}/instance/service-accounts/default/token" || true)"

  if [ -n "${TOKEN_RESPONSE}" ]; then
    ACCESS_TOKEN="$(echo "${TOKEN_RESPONSE}" | jq -r '.access_token')"
    if [ -n "${ACCESS_TOKEN}" ] && [ "${ACCESS_TOKEN}" != "null" ]; then
      log "Metadata server answered on attempt ${attempt}."
      break
    fi
    ACCESS_TOKEN=""
  fi

  log "Metadata server not ready yet (attempt ${attempt}/${METADATA_MAX_ATTEMPTS}); retrying in ${METADATA_RETRY_DELAY}s."
  attempt=$((attempt + 1))
  sleep "${METADATA_RETRY_DELAY}"
done

if [ -z "${ACCESS_TOKEN}" ]; then
  echo "[entrypoint] ERROR: could not obtain a token from the metadata server." >&2
  echo "[entrypoint] On GKE this almost always means Workload Identity is not" >&2
  echo "[entrypoint] wired up. Check three things:" >&2
  echo "[entrypoint]   1. the cluster has --workload-pool set" >&2
  echo "[entrypoint]   2. the pod sets serviceAccountName: travellens-sa" >&2
  echo "[entrypoint]   3. travellens-sa carries the iam.gke.io/gcp-service-account" >&2
  echo "[entrypoint]      annotation AND the Google service account grants that" >&2
  echo "[entrypoint]      KSA roles/iam.workloadIdentityUser" >&2
  exit 1
fi

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

# MaxRAMPercentage rather than a fixed -Xmx, so the heap is sized from the pod's
# memory limit (1Gi in 01-backend-deployment.yaml) instead of from the node's
# total RAM. Getting this wrong is the classic way to have a container
# OOM-killed by Kubernetes while the JVM still believes it has room to grow.
exec java -XX:MaxRAMPercentage=70 -jar /app/backend.jar
