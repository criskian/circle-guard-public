resource "kubernetes_namespace" "monitoring" {
  metadata {
    name = var.namespace
    labels = {
      "app.kubernetes.io/managed-by" = "terraform"
      "environment"                  = var.environment
    }
  }
}

# ── Prometheus (kube-prometheus-stack via Helm) ───────────────────────────────

resource "helm_release" "prometheus_stack" {
  name       = "prometheus-stack"
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "kube-prometheus-stack"
  version    = "65.1.1"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name

  timeout = 600

  set {
    name  = "grafana.adminPassword"
    value = var.grafana_admin_password
  }

  set {
    name  = "grafana.service.type"
    value = "NodePort"
  }

  set {
    name  = "grafana.service.nodePort"
    value = "30300"
  }

  set {
    name  = "prometheus.prometheusSpec.retention"
    value = var.prometheus_retention
  }

  set {
    name  = "prometheus.service.type"
    value = "NodePort"
  }

  set {
    name  = "prometheus.service.nodePort"
    value = "30090"
  }

  set {
    name  = "alertmanager.enabled"
    value = tostring(var.enable_alertmanager)
  }

  # Scrape configs for CircleGuard microservices
  set {
    name  = "prometheus.prometheusSpec.additionalScrapeConfigs[0].job_name"
    value = "circleguard-services"
  }

  set {
    name  = "prometheus.prometheusSpec.additionalScrapeConfigs[0].static_configs[0].targets[0]"
    value = "auth-service.circleguard-stage.svc.cluster.local:8180"
  }

  values = [
    yamlencode({
      grafana = {
        dashboardProviders = {
          "dashboardproviders.yaml" = {
            apiVersion = 1
            providers = [{
              name            = "circleguard"
              orgId           = 1
              folder          = "CircleGuard"
              type            = "file"
              disableDeletion = false
              options = {
                path = "/var/lib/grafana/dashboards/circleguard"
              }
            }]
          }
        }
      }
    })
  ]
}
