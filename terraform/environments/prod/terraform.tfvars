# Production environment tfvars
# All sensitive values MUST be set as masked workspace variables in Terraform Cloud.
# This file only contains non-sensitive configuration.

kube_context      = "kind-circleguard-stage"
replicas          = 2
enable_monitoring = true
