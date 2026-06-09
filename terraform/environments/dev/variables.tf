variable "kubeconfig_path" {
  description = "Path to kubeconfig file"
  type        = string
  default     = "~/.kube/config"
}

variable "kube_context" {
  description = "Kubernetes context to use from kubeconfig"
  type        = string
  default     = "kind-circleguard-dev"
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
  description = "JWT signing secret (min 32 chars)"
  type        = string
  sensitive   = true
}

variable "vault_hash_salt" {
  description = "Hash salt for identity vault"
  type        = string
  sensitive   = true
  default     = "circleguard-salt-dev"
}
