variable "namespace" {
  description = "Target Kubernetes namespace"
  type        = string
}

variable "environment" {
  description = "Environment label (dev, stage, prod)"
  type        = string
}

variable "image_tag" {
  description = "Docker image tag for all services (e.g. dev-latest, stage-latest, prod-latest)"
  type        = string
  default     = "latest"
}

variable "image_pull_policy" {
  description = "Image pull policy (Never for local kind clusters, Always/IfNotPresent for remote)"
  type        = string
  default     = "Never"
}

variable "replicas" {
  description = "Number of replicas per service"
  type        = number
  default     = 1
}

variable "db_secret_name" {
  description = "Name of the database Kubernetes secret"
  type        = string
  default     = "circleguard-db-secret"
}

variable "app_secret_name" {
  description = "Name of the app Kubernetes secret"
  type        = string
  default     = "circleguard-app-secret"
}

variable "config_map_name" {
  description = "Name of the application ConfigMap"
  type        = string
  default     = "circleguard-config"
}

variable "services" {
  description = "Map of microservice configurations"
  type = map(object({
    port                   = number
    db_name                = optional(string)
    readiness_initial_delay = optional(number, 30)
    liveness_initial_delay  = optional(number, 120)
    memory_request         = optional(string, "256Mi")
    memory_limit           = optional(string, "512Mi")
    cpu_request            = optional(string, "100m")
    cpu_limit              = optional(string, "500m")
    node_port              = optional(bool, false)
    extra_env              = optional(map(string), {})
  }))

  default = {
    "auth-service" = {
      port     = 8180
      db_name  = "circleguard_auth"
      node_port = true
      extra_env = {
        IDENTITY_SERVICE_URL = "http://identity-service:8083"
      }
    }
    "identity-service" = {
      port                   = 8083
      db_name                = "circleguard_identity"
      liveness_initial_delay = 180
    }
    "form-service" = {
      port                   = 8086
      db_name                = "circleguard_form"
      liveness_initial_delay = 180
      node_port              = true
    }
    "promotion-service" = {
      port           = 8088
      db_name        = "circleguard_promotion"
      memory_request = "512Mi"
      memory_limit   = "1Gi"
      cpu_request    = "200m"
      cpu_limit      = "1000m"
      node_port      = true
      readiness_initial_delay = 45
    }
    "notification-service" = {
      port                   = 8082
      liveness_initial_delay = 180
      extra_env = {
        AUTH_API_URL = "http://auth-service:8180"
      }
    }
    "dashboard-service" = {
      port                   = 8084
      db_name                = "circleguard_dashboard"
      liveness_initial_delay = 180
      node_port              = true
      extra_env = {
        CIRCLEGUARD_PROMOTION_SERVICE_URL = "http://promotion-service:8088"
      }
    }
  }
}
