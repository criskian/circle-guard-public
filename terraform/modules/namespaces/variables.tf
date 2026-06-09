variable "namespace" {
  description = "Kubernetes namespace name"
  type        = string
}

variable "environment" {
  description = "Environment label (dev, stage, prod)"
  type        = string
}

variable "labels" {
  description = "Additional labels to apply to the namespace"
  type        = map(string)
  default     = {}
}
