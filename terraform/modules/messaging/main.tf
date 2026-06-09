# ── Zookeeper ────────────────────────────────────────────────────────────────

resource "kubernetes_deployment" "zookeeper" {
  metadata {
    name      = "zookeeper"
    namespace = var.namespace
    labels = {
      app         = "zookeeper"
      environment = var.environment
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = { app = "zookeeper" }
    }

    template {
      metadata {
        labels = { app = "zookeeper" }
      }

      spec {
        container {
          name  = "zookeeper"
          image = var.zookeeper_image

          port { container_port = 2181 }

          env {
            name  = "ZOOKEEPER_CLIENT_PORT"
            value = "2181"
          }

          env {
            name  = "ZOOKEEPER_TICK_TIME"
            value = "2000"
          }

          resources {
            requests = {
              memory = "256Mi"
              cpu    = "100m"
            }
            limits = {
              memory = "512Mi"
              cpu    = "500m"
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "zookeeper" {
  metadata {
    name      = "zookeeper"
    namespace = var.namespace
  }

  spec {
    selector = { app = "zookeeper" }
    port {
      port        = 2181
      target_port = 2181
    }
  }
}

# ── Kafka ─────────────────────────────────────────────────────────────────────

resource "kubernetes_deployment" "kafka" {
  metadata {
    name      = "kafka"
    namespace = var.namespace
    labels = {
      app         = "kafka"
      environment = var.environment
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = { app = "kafka" }
    }

    template {
      metadata {
        labels = { app = "kafka" }
      }

      spec {
        # Prevent K8s from injecting KAFKA_PORT env var (conflicts with Confluent image)
        enable_service_links = false

        container {
          name  = "kafka"
          image = var.kafka_image

          port { container_port = 29092 }

          env {
            name  = "KAFKA_BROKER_ID"
            value = "1"
          }

          env {
            name  = "KAFKA_ZOOKEEPER_CONNECT"
            value = "zookeeper:2181"
          }

          env {
            name  = "KAFKA_ADVERTISED_LISTENERS"
            value = "PLAINTEXT://kafka:29092"
          }

          env {
            name  = "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP"
            value = "PLAINTEXT:PLAINTEXT"
          }

          env {
            name  = "KAFKA_INTER_BROKER_LISTENER_NAME"
            value = "PLAINTEXT"
          }

          env {
            name  = "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR"
            value = "1"
          }

          env {
            name  = "KAFKA_AUTO_CREATE_TOPICS_ENABLE"
            value = "true"
          }

          resources {
            requests = {
              memory = "512Mi"
              cpu    = "200m"
            }
            limits = {
              memory = "1Gi"
              cpu    = "1000m"
            }
          }
        }
      }
    }
  }

  depends_on = [kubernetes_deployment.zookeeper]
}

resource "kubernetes_service" "kafka" {
  metadata {
    name      = "kafka"
    namespace = var.namespace
  }

  spec {
    selector = { app = "kafka" }
    port {
      port        = 29092
      target_port = 29092
    }
  }
}
