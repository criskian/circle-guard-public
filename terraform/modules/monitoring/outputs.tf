output "monitoring_namespace" {
  description = "Namespace where monitoring stack is deployed"
  value       = kubernetes_namespace.monitoring.metadata[0].name
}

output "grafana_nodeport" {
  description = "NodePort for accessing Grafana UI"
  value       = 30300
}

output "prometheus_nodeport" {
  description = "NodePort for accessing Prometheus UI"
  value       = 30090
}
