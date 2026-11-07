# ---------------------------------------------------------------------------
# The GKE cluster.
#
# Created by Terraform rather than imported. The Milestone 5 cluster was deleted
# after that demo, which is a small piece of luck: importing a GKE cluster means
# reconciling dozens of computed defaults before `plan` goes quiet, whereas a
# cluster Terraform created is clean by construction.
#
# Everything stateful survived that teardown and is imported instead - see
# imports.tf. The cluster holds nothing, which is exactly the property
# Milestone 4 was about.
# ---------------------------------------------------------------------------

resource "google_container_cluster" "primary" {
  name     = var.cluster_name
  location = var.zone

  # Terraform's recommended pattern: the cluster is created with a throwaway
  # default pool which is immediately removed, and the real pool is a separate
  # resource below. Without this, node settings live inside the cluster
  # resource and changing any of them replaces the entire cluster.
  remove_default_node_pool = true
  initial_node_count       = 1

  network    = "default"
  subnetwork = "default"

  # VPC-native. Pod IPs come from a secondary range on the subnet, and subnet
  # routes - secondary ranges included - are exchanged across the Private
  # Service Access peering. That is the mechanism that lets a pod reach Cloud
  # SQL on 10.7.0.3 at all. An empty block lets GKE allocate the ranges.
  networking_mode = "VPC_NATIVE"
  ip_allocation_policy {}

  # Pods get a Google identity without a key file.
  workload_identity_config {
    workload_pool = "${var.project_id}.svc.id.goog"
  }

  release_channel {
    channel = "REGULAR"
  }

  addons_config {
    # Required for Ingress and for Google-managed certificates.
    http_load_balancing {
      disabled = false
    }
    horizontal_pod_autoscaling {
      disabled = false
    }
  }

  # This project is torn down and rebuilt between working sessions to keep the
  # bill down, so the guard is off deliberately. It is left on for Cloud SQL,
  # where a mistaken destroy would take the data with it.
  deletion_protection = false

  depends_on = [google_project_service.required]
}

resource "google_container_node_pool" "primary" {
  name     = "travellens-pool"
  location = var.zone
  cluster  = google_container_cluster.primary.name

  initial_node_count = var.node_min_count

  autoscaling {
    min_node_count = var.node_min_count
    max_node_count = var.node_max_count
  }

  management {
    auto_repair  = true
    auto_upgrade = true
  }

  node_config {
    machine_type = var.node_machine_type
    disk_size_gb = 50
    disk_type    = "pd-balanced"

    # Nodes run as the narrow Milestone 4 identity, not the Editor-privileged
    # default compute service account.
    service_account = google_service_account.travellens_vm.email
    oauth_scopes    = ["https://www.googleapis.com/auth/cloud-platform"]

    # Required for Workload Identity to actually work on these nodes. gcloud
    # sets this implicitly when --workload-pool is passed; Terraform does not,
    # and without it every pod's call to the metadata server returns the node's
    # identity instead of the bound one - or nothing at all.
    workload_metadata_config {
      mode = "GKE_METADATA"
    }

    labels = {
      app = "travellens"
    }
  }

  lifecycle {
    # The node pool's running size is owned by the cluster autoscaler, not by
    # Terraform. Without this, every plan after a scale-up would propose
    # shrinking the pool back to the initial count.
    ignore_changes = [initial_node_count]
  }
}
