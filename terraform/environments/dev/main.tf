locals {
  environment = "dev"
  namespace   = "circleguard-dev"
  image_tag   = "dev-latest"
}

module "namespace" {
  source      = "../../modules/namespaces"
  namespace   = local.namespace
  environment = local.environment
}

module "databases" {
  source      = "../../modules/databases"
  namespace   = module.namespace.namespace
  environment = local.environment

  db_password    = var.db_password
  neo4j_password = var.neo4j_password
  jwt_secret     = var.jwt_secret
  vault_hash_salt = var.vault_hash_salt
}

module "messaging" {
  source      = "../../modules/messaging"
  namespace   = module.namespace.namespace
  environment = local.environment
}

module "app_services" {
  source      = "../../modules/app-services"
  namespace   = module.namespace.namespace
  environment = local.environment
  image_tag   = local.image_tag
  replicas    = 1

  db_secret_name  = module.databases.db_secret_name
  app_secret_name = module.databases.app_secret_name
}
