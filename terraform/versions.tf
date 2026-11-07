# ---------------------------------------------------------------------------
# Provider and state configuration.
#
# State lives in a GCS bucket, never in the repository. Terraform state holds
# every attribute of every managed resource in plain text, including things
# that are sensitive even when they are not called secrets - connection
# strings, IAM members, instance names. Committing it would undo the work of
# Milestone 4.
#
# The state bucket is itself NOT managed by Terraform. That is unavoidable:
# Terraform has to read its state before it can create anything, so the bucket
# must exist before the first init. It is created once with a single gcloud
# command and documented as an exception in the written analysis.
# ---------------------------------------------------------------------------
terraform {
  required_version = ">= 1.9.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }

  backend "gcs" {
    bucket = "travellens-tfstate-travellens-ccp"
    prefix = "travellens/state"
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
  zone    = var.zone
}
