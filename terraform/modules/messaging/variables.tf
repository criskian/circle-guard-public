variable "namespace" {
  description = "Target Kubernetes namespace"
  type        = string
}

variable "environment" {
  description = "Environment label (dev, stage, prod)"
  type        = string
}

variable "zookeeper_image" {
  description = "Zookeeper Docker image"
  type        = string
  default     = "confluentinc/cp-zookeeper:7.6.0"
}

variable "kafka_image" {
  description = "Kafka Docker image"
  type        = string
  default     = "confluentinc/cp-kafka:7.6.0"
}
