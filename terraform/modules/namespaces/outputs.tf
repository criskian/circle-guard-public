output "namespace" {
  description = "Name of the created namespace"
  value       = kubernetes_namespace.this.metadata[0].name
}

output "service_account_name" {
  description = "Name of the application service account"
  value       = kubernetes_service_account.app.metadata[0].name
}
