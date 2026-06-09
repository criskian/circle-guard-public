variable "namespace" {
  description = "Target Kubernetes namespace for monitoring stack"
  type        = string
  default     = "circleguard-monitoring"
}

variable "environment" {
  description = "Environment label (dev, stage, prod)"
  type        = string
}

variable "grafana_admin_password" {
  description = "Grafana admin password"
  type        = string
  sensitive   = true
  default     = "admin"
}

variable "prometheus_retention" {
  description = "Prometheus data retention period"
  type        = string
  default     = "7d"
}

variable "enable_alertmanager" {
  description = "Enable Prometheus Alertmanager"
  type        = bool
  default     = false
}
