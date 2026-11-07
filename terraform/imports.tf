# ---------------------------------------------------------------------------
# Resources that already existed before Terraform did.
#
# Milestones 2 through 5 were built by hand. Everything stateful from that work
# survived the Milestone 5 teardown - the database, the media bucket, both
# secrets, the image registry and the runtime service account - and none of it
# can be recreated without losing something. The database holds the migrated
# rows. The bucket holds traveler photos. The secrets hold values generated
# from /dev/urandom that exist nowhere else on earth.
#
# So Terraform adopts them rather than replacing them. Each block below tells
# Terraform "this resource already exists, here is its ID, put it under
# management without touching it."
#
# How to turn these into configuration
# ------------------------------------
# Do not hand-write the resource blocks. A Cloud SQL instance has roughly forty
# attributes and guessing even one wrong - a disk size, a backup window, a
# maintenance day - shows up as a diff, and a diff is the difference between
# full marks and none on the "plan reports no changes" requirement.
#
# Let Terraform write them instead:
#
#     terraform plan -generate-config-out=imported.tf
#
# That reads the live resources and writes configuration that matches them
# exactly. Review the result, then:
#
#     terraform apply
#
# which performs the imports. From then on `terraform plan` should be clean.
#
# Deliberately NOT imported
# -------------------------
#   google_sql_user           - the database password would be written to
#                               Terraform state in plain text. The whole point
#                               of Milestone 4 was that the password exists
#                               only in Secret Manager. The user stays
#                               unmanaged and is documented as an exception.
#
#   google_secret_manager_secret_version
#                             - same reasoning, one level down. Terraform
#                               manages the secret *containers* below, so their
#                               replication policy and IAM are under version
#                               control, but never the values inside them.
# ---------------------------------------------------------------------------

# The identity every pod runs as, via Workload Identity.
import {
  to = google_service_account.travellens_vm
  id = "projects/travellens-ccp/serviceAccounts/travellens-vm@travellens-ccp.iam.gserviceaccount.com"
}

# Managed PostgreSQL 16, private IP only, no public address.
import {
  to = google_sql_database_instance.travellens
  id = "travellens-ccp/travellens-db"
}

import {
  to = google_sql_database.springboot
  id = "projects/travellens-ccp/instances/travellens-db/databases/springboot"
}

# Traveler media. Uniform bucket-level access, public access prevention on.
import {
  to = google_storage_bucket.uploads
  id = "travellens-ccp/travellens-uploads-travellens-ccp"
}

# Secret containers only - never the versions inside them.
import {
  to = google_secret_manager_secret.db_password
  id = "projects/travellens-ccp/secrets/travellens-db-password"
}

import {
  to = google_secret_manager_secret.jwt_secret
  id = "projects/travellens-ccp/secrets/travellens-jwt-secret"
}

# Container images, including every tag the pipeline has ever pushed.
import {
  to = google_artifact_registry_repository.travellens
  id = "projects/travellens-ccp/locations/us-central1/repositories/travellens"
}

# The /16 reserved for Private Service Access, and the peering that uses it.
# This pair is what lets a pod reach Cloud SQL on 10.7.0.3 without the database
# ever having a public address.
import {
  to = google_compute_global_address.psa_range
  id = "projects/travellens-ccp/global/addresses/google-managed-services-default"
}

import {
  to = google_service_networking_connection.psa
  id = "projects/travellens-ccp/global/networks/default:servicenetworking.googleapis.com"
}

# The load balancer's static IP. Reserved by hand at the start of this
# milestone so that DNS could be pointed at it immediately and the certificate
# could start provisioning, rather than waiting for the first terraform apply.
import {
  to = google_compute_global_address.travellens_ip
  id = "projects/travellens-ccp/global/addresses/travellens-ip"
}
