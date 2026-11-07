# __generated__ by Terraform
# Please review these resources and move them into your main configuration files.

# __generated__ by Terraform from "travellens-ccp/travellens-uploads-travellens-ccp"
resource "google_storage_bucket" "uploads" {
  default_event_based_hold    = false
  enable_object_retention     = false
  force_destroy               = false
  labels                      = {}
  location                    = "US-CENTRAL1"
  name                        = "travellens-uploads-travellens-ccp"
  project                     = "travellens-ccp"
  public_access_prevention    = "enforced"
  requester_pays              = false
  storage_class               = "STANDARD"
  uniform_bucket_level_access = true
  hierarchical_namespace {
    enabled = false
  }
  soft_delete_policy {
    retention_duration_seconds = 604800
  }
}

# __generated__ by Terraform from "projects/travellens-ccp/secrets/travellens-jwt-secret"
resource "google_secret_manager_secret" "jwt_secret" {
  annotations         = {}
  deletion_protection = false
  labels              = {}
  project             = "travellens-ccp"
  secret_id           = "travellens-jwt-secret"
  tags                = null
  ttl                 = null
  version_aliases     = {}
  version_destroy_ttl = null
  replication {
    auto {
    }
  }
}

# __generated__ by Terraform from "projects/travellens-ccp/secrets/travellens-db-password"
resource "google_secret_manager_secret" "db_password" {
  annotations         = {}
  deletion_protection = false
  labels              = {}
  project             = "travellens-ccp"
  secret_id           = "travellens-db-password"
  tags                = null
  ttl                 = null
  version_aliases     = {}
  version_destroy_ttl = null
  replication {
    auto {
    }
  }
}

# __generated__ by Terraform from "travellens-ccp/travellens-db"
resource "google_sql_database_instance" "travellens" {
  database_version    = "POSTGRES_16"
  deletion_protection = true
  instance_type       = "CLOUD_SQL_INSTANCE"
  maintenance_version = "POSTGRES_16_15.R20260712.01_44"
  name                = "travellens-db"
  node_count          = 0
  project             = "travellens-ccp"
  region              = "us-central1"
  replica_names       = []
  root_password       = null # sensitive
  replication_cluster {
    failover_dr_replica_name = null
    psa_write_endpoint       = null
  }
  settings {
    activation_policy            = "ALWAYS"
    availability_type            = "ZONAL"
    collation                    = null
    connector_enforcement        = "NOT_REQUIRED"
    deletion_protection_enabled  = false
    disk_autoresize              = true
    disk_autoresize_limit        = 0
    disk_size                    = 10
    disk_type                    = "PD_SSD"
    edition                      = "ENTERPRISE"
    enable_dataplex_integration  = true
    enable_google_ml_integration = false
    pricing_plan                 = "PER_USE"
    retain_backups_on_delete     = false
    tier                         = "db-f1-micro"
    time_zone                    = null
    user_labels                  = {}
    backup_configuration {
      binary_log_enabled             = false
      enabled                        = true
      location                       = null
      point_in_time_recovery_enabled = true
      start_time                     = "08:00"
      transaction_log_retention_days = 7
      backup_retention_settings {
        retained_backups = 7
        retention_unit   = "COUNT"
      }
    }
    data_cache_config {
      data_cache_enabled = false
    }
    ip_configuration {
      allocated_ip_range                            = null
      custom_subject_alternative_names              = []
      enable_private_path_for_google_cloud_services = false
      ipv4_enabled                                  = false
      private_network                               = "projects/travellens-ccp/global/networks/default"
      server_ca_mode                                = "GOOGLE_MANAGED_INTERNAL_CA"
      server_ca_pool                                = null
      ssl_mode                                      = "ALLOW_UNENCRYPTED_AND_ENCRYPTED"
    }
    location_preference {
      follow_gae_application = null
      secondary_zone         = null
      zone                   = "us-central1-c"
    }
  }
}

# __generated__ by Terraform from "projects/travellens-ccp/locations/us-central1/repositories/travellens"
resource "google_artifact_registry_repository" "travellens" {
  cleanup_policy_dry_run = false
  description            = null
  format                 = "DOCKER"
  kms_key_name           = null
  labels                 = {}
  location               = "us-central1"
  mode                   = "STANDARD_REPOSITORY"
  project                = "travellens-ccp"
  repository_id          = "travellens"
  vulnerability_scanning_config {
    enablement_config = null
  }
}

# __generated__ by Terraform from "projects/travellens-ccp/global/addresses/google-managed-services-default"
resource "google_compute_global_address" "psa_range" {
  address       = "10.7.0.0"
  address_type  = "INTERNAL"
  description   = null
  ip_version    = null
  labels        = {}
  name          = "google-managed-services-default"
  network       = "https://www.googleapis.com/compute/v1/projects/travellens-ccp/global/networks/default"
  prefix_length = 16
  project       = "travellens-ccp"
  purpose       = "VPC_PEERING"
}

# __generated__ by Terraform from "projects/travellens-ccp/instances/travellens-db/databases/springboot"
resource "google_sql_database" "springboot" {
  charset         = "UTF8"
  collation       = "en_US.UTF8"
  deletion_policy = "DELETE"
  instance        = "travellens-db"
  name            = "springboot"
  project         = "travellens-ccp"
}

# __generated__ by Terraform from "projects/travellens-ccp/global/networks/default:servicenetworking.googleapis.com"
resource "google_service_networking_connection" "psa" {
  deletion_policy         = null
  network                 = "projects/travellens-ccp/global/networks/default"
  reserved_peering_ranges = ["google-managed-services-default"]
  service                 = "servicenetworking.googleapis.com"
  update_on_creation_fail = null
}
