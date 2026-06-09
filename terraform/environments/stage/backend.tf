# Stage uses Terraform Cloud as remote backend.
# Prerequisites:
#   1. Create a free account at https://app.terraform.io
#   2. Create organization "circleguard" (or update below)
#   3. Run: terraform login
#   4. Run: terraform init
#
# State locking is handled automatically by Terraform Cloud.

terraform {
  cloud {
    organization = "circleguard"

    workspaces {
      name = "circleguard-stage"
    }
  }
}
