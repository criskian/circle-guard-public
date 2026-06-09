output "service_names" {
  description = "Map of service name to Kubernetes service name"
  value       = { for k, v in kubernetes_service.service : k => v.metadata[0].name }
}

output "config_map_name" {
  description = "Name of the application ConfigMap"
  value       = kubernetes_config_map.app.metadata[0].name
}
