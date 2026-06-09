# Dev environment uses a local backend for simplicity.
# For shared team environments, migrate to Terraform Cloud:
#
#   terraform {
#     cloud {
#       organization = "circleguard"
#       workspaces {
#         name = "circleguard-dev"
#       }
#     }
#   }
#
# To migrate: run `terraform init -migrate-state` after updating this file.

terraform {
  backend "local" {
    path = "terraform.tfstate"
  }
}
