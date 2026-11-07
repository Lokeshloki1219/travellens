variable "project_id" {
  description = "GCP project that holds every TravelLens resource."
  type        = string
  default     = "travellens-ccp"
}

variable "region" {
  description = "Region for regional resources: Artifact Registry, Cloud SQL, the GKE node pool."
  type        = string
  default     = "us-central1"
}

variable "zone" {
  description = "Zone for the GKE cluster. Zonal rather than regional so it falls under the one-free-cluster tier."
  type        = string
  default     = "us-central1-a"
}

variable "sql_zone" {
  description = <<-EOT
    Zone of the existing Cloud SQL instance. Deliberately separate from var.zone:
    the instance was created in us-central1-c before the cluster existed, and
    changing it would force the instance to be replaced - taking the database
    with it. Matching reality here is what keeps `terraform plan` clean.
  EOT
  type        = string
  default     = "us-central1-c"
}

variable "cluster_name" {
  type    = string
  default = "travellens-cluster"
}

variable "hostname" {
  description = "Public hostname the managed certificate is issued for."
  type        = string
  default     = "travellens-uh.duckdns.org"
}

variable "bucket_name" {
  description = "Existing Cloud Storage bucket holding traveler media. Imported, not created."
  type        = string
  default     = "travellens-uploads-travellens-ccp"
}

variable "github_repository" {
  description = "owner/repo allowed to deploy via Workload Identity Federation. Nothing else can authenticate."
  type        = string
  default     = "Lokeshloki1219/travellens"
}

variable "deploy_branch" {
  description = "Branch the pipeline deploys from."
  type        = string
  default     = "deploy"
}

variable "alert_email" {
  description = "Address that receives alert notifications. Set this in terraform.tfvars."
  type        = string
}

variable "node_machine_type" {
  type    = string
  default = "e2-medium"
}

variable "node_min_count" {
  type    = number
  default = 2
}

variable "node_max_count" {
  type    = number
  default = 4
}
