# ---------------------------------------------------------------------------
# Two imported resources declared by hand rather than generated.
#
# -generate-config-out only writes configuration for import blocks whose
# resource is not already declared, and it validates the whole config first.
# These two are referenced by gke.tf, iam.tf and outputs.tf, so they have to
# exist before that validation passes. Both are small enough that there is
# nothing to get wrong; everything with dozens of computed attributes is still
# generated from the live resource.
# ---------------------------------------------------------------------------

resource "google_service_account" "travellens_vm" {
  account_id   = "travellens-vm"
  display_name = "TravelLens VM runtime"
}

# The load balancer address. Reserved by hand so DNS could be pointed at it
# immediately; adopted here so Terraform will not try to create a second one.
# No address attribute: it is assigned by Google and is read-only.
resource "google_compute_global_address" "travellens_ip" {
  name = "travellens-ip"
}
