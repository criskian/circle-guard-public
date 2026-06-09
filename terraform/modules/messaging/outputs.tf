output "kafka_bootstrap_servers" {
  description = "Kafka bootstrap servers address (internal DNS)"
  value       = "kafka:29092"
}

output "zookeeper_connect" {
  description = "Zookeeper connect string"
  value       = "zookeeper:2181"
}
