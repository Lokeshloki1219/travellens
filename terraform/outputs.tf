# ---------------------------------------------------------------------------
# Values needed outside Terraform.
#
# The two WIF outputs go straight into GitHub repository secrets. Neither is a
# credential - they are resource names, and both would be harmless in a public
# repository. They live in GitHub secrets only to keep the workflow file free
# of project-specific identifiers.
# ---------------------------------------------------------------------------

output "load_balancer_ip" {
  description = "Static IP the DuckDNS A record must point at. Change this and the certificate has to be reissued."
  value       = google_compute_global_address.travellens_ip.address
}

output "cluster_name" {
  value = google_container_cluster.primary.name
}

output "cluster_location" {
  value = google_container_cluster.primary.location
}

output "get_credentials_command" {
  description = "Copy-paste to point kubectl at the cluster."
  value       = "gcloud container clusters get-credentials ${google_container_cluster.primary.name} --zone=${google_container_cluster.primary.location} --project=${var.project_id}"
}

output "wif_provider" {
  description = "Set as the GitHub repository secret WIF_PROVIDER."
  value       = google_iam_workload_identity_pool_provider.github.name
}

output "wif_service_account" {
  description = "Set as the GitHub repository secret WIF_SERVICE_ACCOUNT."
  value       = google_service_account.deployer.email
}

output "log_metric_name" {
  description = "The log-based metric the alert policy watches."
  value       = google_logging_metric.http_5xx.name
}

output "alert_policy_name" {
  value = google_monitoring_alert_policy.http_5xx.name
}
