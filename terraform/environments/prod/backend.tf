# Production uses Terraform Cloud as remote backend with state locking.
# Prerequisites:
#   1. Create a free account at https://app.terraform.io
#   2. Create organization "circleguard"
#   3. Run: terraform login
#   4. Run: terraform init
#
# IMPORTANT: Production workspace variables must be set as "Sensitive" in TF Cloud UI.
# Never commit production secrets to this file.

terraform {
  cloud {
    organization = "circleguard"

    workspaces {
      name = "circleguard-prod"
    }
  }
}
