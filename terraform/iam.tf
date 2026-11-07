# ---------------------------------------------------------------------------
# IAM for the runtime identity.
#
# travellens-vm is the single Google identity the application runs as. It was
# created in Milestone 4 for a VM; in Milestone 5 the same account was bound to
# a Kubernetes ServiceAccount through Workload Identity, so pods inherit it
# without a key file existing anywhere.
#
# These bindings are declared rather than imported. google_project_iam_member
# is additive and idempotent - applying one that already exists changes
# nothing at the API level - so Terraform can adopt them simply by creating
# them. Only the first plan shows them as additions.
#
# Note what is NOT here: no roles/editor, no roles/owner, nothing that can
# change infrastructure. Six narrow roles, each traceable to one thing the
# application or the node actually does.
# ---------------------------------------------------------------------------
locals {
  travellens_vm_roles = {
    "roles/artifactregistry.reader"            = "pull container images"
    "roles/logging.logWriter"                  = "write pod logs to Cloud Logging"
    "roles/monitoring.metricWriter"            = "node telemetry"
    "roles/stackdriver.resourceMetadata.writer" = "node telemetry"
    "roles/secretmanager.secretAccessor"       = "read the db password and JWT key at container start"
    "roles/storage.objectAdmin"                = "read and write traveler media in the bucket"
  }
}

resource "google_project_iam_member" "travellens_vm" {
  for_each = local.travellens_vm_roles

  project = var.project_id
  role    = each.key
  member  = "serviceAccount:${google_service_account.travellens_vm.email}"
}

# Workload Identity: lets the Kubernetes ServiceAccount default/travellens-sa
# obtain tokens for the Google service account above. This single binding is
# what removes the need for a key file in the cluster.
#
# The member string is a fixed GKE format: PROJECT.svc.id.goog[NAMESPACE/KSA].
resource "google_service_account_iam_member" "pod_workload_identity" {
  service_account_id = google_service_account.travellens_vm.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.project_id}.svc.id.goog[default/travellens-sa]"
}
