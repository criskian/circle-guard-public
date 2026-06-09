# ── Secrets ─────────────────────────────────────────────────────────────────

resource "kubernetes_secret" "db" {
  metadata {
    name      = "circleguard-db-secret"
    namespace = var.namespace
    labels = {
      "app.kubernetes.io/managed-by" = "terraform"
      "environment"                  = var.environment
    }
  }

  data = {
    postgres-user     = base64encode(var.db_user)
    postgres-password = base64encode(var.db_password)
    neo4j-password    = base64encode(var.neo4j_password)
  }

  type = "Opaque"
}

resource "kubernetes_secret" "app" {
  metadata {
    name      = "circleguard-app-secret"
    namespace = var.namespace
    labels = {
      "app.kubernetes.io/managed-by" = "terraform"
      "environment"                  = var.environment
    }
  }

  data = {
    jwt-secret      = base64encode(var.jwt_secret)
    vault-hash-salt = base64encode(var.vault_hash_salt)
  }

  type = "Opaque"
}

# ── PostgreSQL init script ConfigMap ────────────────────────────────────────

resource "kubernetes_config_map" "postgres_init" {
  metadata {
    name      = "postgres-init-script"
    namespace = var.namespace
  }

  data = {
    "init-db.sql" = <<-SQL
      CREATE DATABASE circleguard_auth;
      CREATE DATABASE circleguard_identity;
      CREATE DATABASE circleguard_form;
      CREATE DATABASE circleguard_promotion;
      CREATE DATABASE circleguard_dashboard;
    SQL
  }
}

# ── PostgreSQL ───────────────────────────────────────────────────────────────

resource "kubernetes_deployment" "postgres" {
  metadata {
    name      = "postgres"
    namespace = var.namespace
    labels = {
      app         = "postgres"
      environment = var.environment
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = { app = "postgres" }
    }

    template {
      metadata {
        labels = { app = "postgres" }
      }

      spec {
        container {
          name  = "postgres"
          image = var.postgres_image

          port { container_port = 5432 }

          env {
            name = "POSTGRES_USER"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.db.metadata[0].name
                key  = "postgres-user"
              }
            }
          }

          env {
            name = "POSTGRES_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.db.metadata[0].name
                key  = "postgres-password"
              }
            }
          }

          env {
            name  = "POSTGRES_DB"
            value = "circleguard"
          }

          volume_mount {
            mount_path = "/var/lib/postgresql/data"
            name       = "pgdata"
          }

          volume_mount {
            mount_path = "/docker-entrypoint-initdb.d/init-db.sql"
            name       = "init-script"
            sub_path   = "init-db.sql"
          }

          readiness_probe {
            exec {
              command = ["pg_isready", "-U", var.db_user, "-d", "circleguard"]
            }
            initial_delay_seconds = 10
            period_seconds        = 5
          }

          resources {
            requests = {
              memory = "128Mi"
              cpu    = "100m"
            }
            limits = {
              memory = "256Mi"
              cpu    = "500m"
            }
          }
        }

        volume {
          name = "pgdata"
          empty_dir {}
        }

        volume {
          name = "init-script"
          config_map {
            name = kubernetes_config_map.postgres_init.metadata[0].name
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "postgres" {
  metadata {
    name      = "postgres"
    namespace = var.namespace
  }

  spec {
    selector = { app = "postgres" }
    port {
      port        = 5432
      target_port = 5432
    }
  }
}

# ── Redis ────────────────────────────────────────────────────────────────────

resource "kubernetes_deployment" "redis" {
  metadata {
    name      = "redis"
    namespace = var.namespace
    labels = {
      app         = "redis"
      environment = var.environment
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = { app = "redis" }
    }

    template {
      metadata {
        labels = { app = "redis" }
      }

      spec {
        container {
          name  = "redis"
          image = var.redis_image

          port { container_port = 6379 }

          readiness_probe {
            exec {
              command = ["redis-cli", "ping"]
            }
            initial_delay_seconds = 5
            period_seconds        = 5
          }

          resources {
            requests = {
              memory = "64Mi"
              cpu    = "50m"
            }
            limits = {
              memory = "128Mi"
              cpu    = "200m"
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "redis" {
  metadata {
    name      = "redis"
    namespace = var.namespace
  }

  spec {
    selector = { app = "redis" }
    port {
      port        = 6379
      target_port = 6379
    }
  }
}

# ── Neo4j ────────────────────────────────────────────────────────────────────

resource "kubernetes_deployment" "neo4j" {
  metadata {
    name      = "neo4j"
    namespace = var.namespace
    labels = {
      app         = "neo4j"
      environment = var.environment
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = { app = "neo4j" }
    }

    template {
      metadata {
        labels = { app = "neo4j" }
      }

      spec {
        enable_service_links = false

        container {
          name  = "neo4j"
          image = var.neo4j_image

          port { container_port = 7474 }
          port { container_port = 7687 }

          env {
            name  = "NEO4J_AUTH"
            value = "neo4j/${var.neo4j_password}"
          }

          env {
            name  = "NEO4J_PLUGINS"
            value = "[\"apoc\"]"
          }

          volume_mount {
            mount_path = "/data"
            name       = "neo4jdata"
          }

          readiness_probe {
            http_get {
              path = "/"
              port = 7474
            }
            initial_delay_seconds = 30
            period_seconds        = 10
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

        volume {
          name = "neo4jdata"
          empty_dir {}
        }
      }
    }
  }
}

resource "kubernetes_service" "neo4j" {
  metadata {
    name      = "neo4j"
    namespace = var.namespace
  }

  spec {
    selector = { app = "neo4j" }
    port {
      name        = "http"
      port        = 7474
      target_port = 7474
    }
    port {
      name        = "bolt"
      port        = 7687
      target_port = 7687
    }
  }
}
