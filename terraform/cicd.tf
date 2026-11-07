# ---------------------------------------------------------------------------
# Keyless CI/CD: Workload Identity Federation for GitHub Actions.
#
# The usual way to let a pipeline deploy to GCP is to create a service account
# key, download the JSON, and paste it into a GitHub secret. That key is
# permanent, works from anywhere on the internet, and sits in a system whose
# access list is not the same as the project's. If it leaks it is valid until
# someone notices and revokes it.
#
# This does none of that. GitHub signs a short-lived OIDC token asserting which
# repository and which ref is running; Google verifies the signature against
# GitHub's public keys, checks the attribute condition below, and issues an
# access token good for an hour. Nothing long-lived exists to leak, and the
# trust is scoped to one repository rather than to whoever holds a file.
# ---------------------------------------------------------------------------

resource "google_iam_workload_identity_pool" "github" {
  workload_identity_pool_id = "github-actions-pool"
  display_name              = "GitHub Actions"
  description               = "Federated identities for the TravelLens deployment pipeline"

  depends_on = [google_project_service.required]
}

resource "google_iam_workload_identity_pool_provider" "github" {
  workload_identity_pool_id          = google_iam_workload_identity_pool.github.workload_identity_pool_id
  workload_identity_pool_provider_id = "github-provider"
  display_name                       = "GitHub OIDC"

  attribute_mapping = {
    "google.subject"       = "assertion.sub"
    "attribute.repository" = "assertion.repository"
    "attribute.ref"        = "assertion.ref"
  }

  # Without this, ANY GitHub repository in the world could present a token and
  # be accepted. Google now refuses to create a GitHub provider that has no
  # condition, which is a good default. This restricts it to one repository.
  attribute_condition = "assertion.repository == '${var.github_repository}'"

  oidc {
    issuer_uri = "https://token.actions.githubusercontent.com"
  }
}

# A separate identity for deployments, not the runtime one.
#
# travellens-vm needs to read secrets and write to the media bucket; the
# pipeline needs neither, and must never be able to read a production secret
# just because it can deploy. Two accounts, two jobs.
resource "google_service_account" "deployer" {
  account_id   = "travellens-deployer"
  display_name = "TravelLens CI/CD deployer"
  description  = "Impersonated by GitHub Actions via Workload Identity Federation"
}

# Only workflows running in the named repository may impersonate the deployer.
# Narrowing further to a single branch is possible - append /attribute.ref/... -
# but pull request runs would then fail to authenticate at all rather than
# failing a permission check, which is a confusing error to debug.
resource "google_service_account_iam_member" "github_impersonation" {
  service_account_id = google_service_account.deployer.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.github.name}/attribute.repository/${var.github_repository}"
}

# What the pipeline is allowed to do: push images, and change workloads in the
# cluster. Note what is absent - no secretmanager.secretAccessor, no
# storage.objectAdmin, no cloudsql roles. A compromised pipeline can deploy a
# bad image, which rollback handles, but it cannot read a password or touch
# the database.
locals {
  deployer_roles = {
    "roles/artifactregistry.writer" = "push built images"
    "roles/container.developer"     = "kubectl set image, rollout status, rollout undo"
  }
}

resource "google_project_iam_member" "deployer" {
  for_each = local.deployer_roles

  project = var.project_id
  role    = each.key
  member  = "serviceAccount:${google_service_account.deployer.email}"
}
