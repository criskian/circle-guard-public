resource "kubernetes_namespace" "this" {
  metadata {
    name = var.namespace
    labels = merge({
      "app.kubernetes.io/managed-by" = "terraform"
      "environment"                  = var.environment
    }, var.labels)
  }
}

resource "kubernetes_service_account" "app" {
  metadata {
    name      = "circleguard-sa"
    namespace = kubernetes_namespace.this.metadata[0].name
    labels = {
      "app.kubernetes.io/managed-by" = "terraform"
    }
  }
}

resource "kubernetes_role" "app" {
  metadata {
    name      = "circleguard-role"
    namespace = kubernetes_namespace.this.metadata[0].name
  }

  rule {
    api_groups = [""]
    resources  = ["pods", "pods/log", "services", "endpoints", "configmaps"]
    verbs      = ["get", "list", "watch"]
  }

  rule {
    api_groups = ["apps"]
    resources  = ["deployments", "replicasets"]
    verbs      = ["get", "list", "watch"]
  }
}

resource "kubernetes_role_binding" "app" {
  metadata {
    name      = "circleguard-role-binding"
    namespace = kubernetes_namespace.this.metadata[0].name
  }

  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "Role"
    name      = kubernetes_role.app.metadata[0].name
  }

  subject {
    kind      = "ServiceAccount"
    name      = kubernetes_service_account.app.metadata[0].name
    namespace = kubernetes_namespace.this.metadata[0].name
  }
}
