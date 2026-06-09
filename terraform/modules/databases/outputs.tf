output "db_secret_name" {
  description = "Name of the database Kubernetes secret"
  value       = kubernetes_secret.db.metadata[0].name
}

output "app_secret_name" {
  description = "Name of the application Kubernetes secret"
  value       = kubernetes_secret.app.metadata[0].name
}

output "postgres_service" {
  description = "PostgreSQL service name (internal DNS)"
  value       = kubernetes_service.postgres.metadata[0].name
}

output "redis_service" {
  description = "Redis service name (internal DNS)"
  value       = kubernetes_service.redis.metadata[0].name
}

output "neo4j_service" {
  description = "Neo4j service name (internal DNS)"
  value       = kubernetes_service.neo4j.metadata[0].name
}
