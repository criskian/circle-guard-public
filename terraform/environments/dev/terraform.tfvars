# Dev environment — non-sensitive defaults only
# Sensitive variables (db_password, jwt_secret, etc.) must be provided via:
#   - TF_VAR_db_password environment variable, or
#   - terraform.tfvars.local (gitignored), or
#   - -var flag at plan/apply time

kube_context = "kind-circleguard-dev"

db_password     = "password"
neo4j_password  = "password"
jwt_secret      = "circleguard-jwt-secret-minimum-256-bits-this-is-for-dev-only"
vault_hash_salt = "circleguard-salt-dev"
