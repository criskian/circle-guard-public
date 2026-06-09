variable "namespace" {
  description = "Target Kubernetes namespace"
  type        = string
}

variable "environment" {
  description = "Environment label (dev, stage, prod)"
  type        = string
}

variable "postgres_image" {
  description = "PostgreSQL Docker image"
  type        = string
  default     = "postgres:16"
}

variable "redis_image" {
  description = "Redis Docker image"
  type        = string
  default     = "redis:7.2"
}

variable "neo4j_image" {
  description = "Neo4j Docker image"
  type        = string
  default     = "neo4j:5.26"
}

variable "db_user" {
  description = "PostgreSQL admin username"
  type        = string
  sensitive   = true
  default     = "admin"
}

variable "db_password" {
  description = "PostgreSQL admin password"
  type        = string
  sensitive   = true
}

variable "neo4j_password" {
  description = "Neo4j admin password"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT signing secret"
  type        = string
  sensitive   = true
}

variable "vault_hash_salt" {
  description = "Hash salt for identity vault"
  type        = string
  sensitive   = true
}
