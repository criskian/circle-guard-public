locals {
  # Build database URL env var for services that have a db_name
  db_env = {
    for name, svc in var.services : name => svc.db_name != null ? {
      SPRING_DATASOURCE_URL = "jdbc:postgresql://postgres:5432/${svc.db_name}"
    } : {}
  }
}

# ── Application ConfigMap ────────────────────────────────────────────────────

resource "kubernetes_config_map" "app" {
  metadata {
    name      = var.config_map_name
    namespace = var.namespace
    labels = {
      "app.kubernetes.io/managed-by" = "terraform"
      "environment"                  = var.environment
    }
  }

  data = {
    AUTH_SERVICE_URL         = "http://auth-service:8180"
    IDENTITY_SERVICE_URL     = "http://identity-service:8083"
    FORM_SERVICE_URL         = "http://form-service:8086"
    PROMOTION_SERVICE_URL    = "http://promotion-service:8088"
    NOTIFICATION_SERVICE_URL = "http://notification-service:8082"
    DASHBOARD_SERVICE_URL    = "http://dashboard-service:8084"

    SPRING_KAFKA_BOOTSTRAP_SERVERS = "kafka:29092"
    SPRING_DATA_REDIS_HOST         = "redis"
    SPRING_DATA_REDIS_PORT         = "6379"
    SPRING_NEO4J_URI               = "bolt://neo4j:7687"
    SPRING_MAIL_HOST               = "mailhog"
    SPRING_MAIL_PORT               = "1025"

    KAFKA_BOOTSTRAP_SERVERS = "kafka:29092"
    REDIS_HOST              = "redis"
    REDIS_PORT              = "6379"
    NEO4J_URI               = "bolt://neo4j:7687"
    POSTGRES_HOST           = "postgres"
    POSTGRES_PORT           = "5432"
    LDAP_URL                = "ldap://openldap:389"
    MAIL_HOST               = "mailhog"
    MAIL_PORT               = "1025"

    SPRING_PROFILES_ACTIVE = "k8s"

    CIRCLEGUARD_FEATURES_STRICT_QR_VALIDATION        = var.environment == "dev" ? "false" : "true"
    CIRCLEGUARD_FEATURES_EMAIL_NOTIFICATIONS_ENABLED = var.environment == "dev" ? "false" : "true"
    CIRCLEGUARD_FEATURES_QR_EXPIRATION_ENABLED       = var.environment == "dev" ? "false" : "true"
  }
}

# ── Microservice Deployments ──────────────────────────────────────────────────

resource "kubernetes_deployment" "service" {
  for_each = var.services

  wait_for_rollout = false

  metadata {
    name      = each.key
    namespace = var.namespace
    labels = {
      app         = each.key
      environment = var.environment
    }
  }

  spec {
    replicas = var.replicas

    selector {
      match_labels = { app = each.key }
    }

    template {
      metadata {
        labels = {
          app         = each.key
          environment = var.environment
        }
      }

      spec {
        container {
          name              = each.key
          image             = "circleguard-${each.key}:${var.image_tag}"
          image_pull_policy = var.image_pull_policy

          port { container_port = each.value.port }

          env_from {
            config_map_ref { name = kubernetes_config_map.app.metadata[0].name }
          }

          # Datasource URL (only for services with a DB)
          dynamic "env" {
            for_each = local.db_env[each.key]
            content {
              name  = env.key
              value = env.value
            }
          }

          # DB username from secret (only for services with a DB)
          dynamic "env" {
            for_each = each.value.db_name != null ? [1] : []
            content {
              name = "SPRING_DATASOURCE_USERNAME"
              value_from {
                secret_key_ref {
                  name = var.db_secret_name
                  key  = "postgres-user"
                }
              }
            }
          }

          # DB password from secret (only for services with a DB)
          dynamic "env" {
            for_each = each.value.db_name != null ? [1] : []
            content {
              name = "SPRING_DATASOURCE_PASSWORD"
              value_from {
                secret_key_ref {
                  name = var.db_secret_name
                  key  = "postgres-password"
                }
              }
            }
          }

          # JWT secret (auth-service)
          dynamic "env" {
            for_each = each.key == "auth-service" ? [1] : []
            content {
              name = "JWT_SECRET"
              value_from {
                secret_key_ref {
                  name = var.app_secret_name
                  key  = "jwt-secret"
                }
              }
            }
          }

          # Vault hash salt (identity-service)
          dynamic "env" {
            for_each = each.key == "identity-service" ? [1] : []
            content {
              name = "VAULT_HASH_SALT"
              value_from {
                secret_key_ref {
                  name = var.app_secret_name
                  key  = "vault-hash-salt"
                }
              }
            }
          }

          # Neo4j password (promotion-service)
          dynamic "env" {
            for_each = each.key == "promotion-service" ? [1] : []
            content {
              name = "SPRING_NEO4J_AUTHENTICATION_PASSWORD"
              value_from {
                secret_key_ref {
                  name = var.db_secret_name
                  key  = "neo4j-password"
                }
              }
            }
          }

          # Extra per-service env vars
          dynamic "env" {
            for_each = each.value.extra_env
            content {
              name  = env.key
              value = env.value
            }
          }

          readiness_probe {
            http_get {
              path = "/actuator/health/readiness"
              port = each.value.port
            }
            initial_delay_seconds = each.value.readiness_initial_delay
            period_seconds        = 10
          }

          liveness_probe {
            http_get {
              path = "/actuator/health/liveness"
              port = each.value.port
            }
            initial_delay_seconds = each.value.liveness_initial_delay
            period_seconds        = 15
          }

          resources {
            requests = {
              memory = each.value.memory_request
              cpu    = each.value.cpu_request
            }
            limits = {
              memory = each.value.memory_limit
              cpu    = each.value.cpu_limit
            }
          }
        }
      }
    }
  }
}

# ── Microservice Services ─────────────────────────────────────────────────────

resource "kubernetes_service" "service" {
  for_each = var.services

  metadata {
    name      = each.key
    namespace = var.namespace
  }

  spec {
    selector = { app = each.key }
    type     = each.value.node_port ? "NodePort" : "ClusterIP"
    port {
      port        = each.value.port
      target_port = each.value.port
    }
  }
}
