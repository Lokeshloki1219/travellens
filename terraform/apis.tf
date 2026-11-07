# ---------------------------------------------------------------------------
# Project APIs.
#
# These are already enabled - they were turned on by hand across Milestones 2
# to 5. They are declared here anyway because enabling an already-enabled API
# is a no-op, so no import is needed, and because a fresh clone of this
# repository in a new project should stand the whole thing up unaided.
#
# disable_on_destroy = false everywhere: tearing down the cluster should not
# switch off the APIs that other things in the project still depend on.
# ---------------------------------------------------------------------------
locals {
  required_apis = [
    "compute.googleapis.com",
    "container.googleapis.com",
    "sqladmin.googleapis.com",
    "secretmanager.googleapis.com",
    "artifactregistry.googleapis.com",
    "servicenetworking.googleapis.com",
    "storage.googleapis.com",
    "logging.googleapis.com",
    "monitoring.googleapis.com",
    "iamcredentials.googleapis.com",
    "sts.googleapis.com",
  ]
}

resource "google_project_service" "required" {
  for_each = toset(local.required_apis)

  project = var.project_id
  service = each.value

  disable_on_destroy = false
}
