# Documentación — Proyecto Final IngeSoft V (CircleGuard)

Índice de la documentación del proyecto final y runbook para levantar/verificar todo.

## Índice por requisito

| # | Requisito | Documentación / Evidencia |
|---|---|---|
| 1 | Metodología Ágil y Branching | `01-agile/` (historias, sprints, branching) |
| 2 | Patrones de Diseño | `03-patrones/` (Circuit Breaker, Feature Toggle) |
| 3 | IaC con Terraform | `02-terraform/` + `terraform/` |
| 4 | CI/CD Avanzado | `services/*/Jenkinsfile`, `ci/jenkins/`, `ci/sonarqube/` |
| 5 | Pruebas Completas | `05-pruebas/resultados.md`, `tests/` |
| 6 | Change Management | `06-change-management/` |
| 7 | Observabilidad | `observability/`, este directorio |
| 8 | Seguridad | `k8s/security/` |
| 9 | Documentación y Costos | este directorio (`09-documentacion/`) |

## Artefactos de esta carpeta
- `video-script.md` — guía detallada de grabación del video por requisito.
- `presentacion-outline.md` — outline de diapositivas.
- `costos-infraestructura.md` — estimación de costos AWS.

## Arquitectura (resumen)

```
            ┌──────────────┐
  cliente → │  gateway-svc │ (Circuit Breaker, Feature Toggle, valida QR)
            └──────┬───────┘
   ┌───────┬───────┼────────┬───────────┬─────────────┐
   ▼       ▼       ▼        ▼           ▼             ▼
 auth   identity form   promotion  notification  dashboard
   │       │       │        │           │             │
   └─ Postgres ─┐  └─ Kafka ─┴── Neo4j ──┘     Redis / LDAP / MailHog
                └─ eventos: survey.submitted, certificate.validated, circle.fenced
```

- **Stack:** Spring Boot 3.2 / Java 21, Gradle multi-módulo, Docker, Kubernetes (kind), Jenkins, Terraform, SonarQube, Trivy.
- **Mensajería:** Kafka (event-driven entre form → promotion → notification).
- **Datos:** Postgres (relacional), Neo4j (grafo de contactos), Redis (estado/cache).

## Runbook — levantar y verificar

> **Importante (8 GB RAM):** no todo corre simultáneamente. Usar tandas.

### CI/CD (Jenkins + SonarQube)
```powershell
docker compose -f ci/sonarqube/docker-compose.yml up -d
docker compose -f ci/jenkins/docker-compose.yml up -d
# Jenkins http://localhost:8080 (admin/admin123) · SonarQube http://localhost:9000
```

### Aplicación (dev)
```powershell
docker compose -f docker-compose.dev.yml up -d --build
```

### Observabilidad
```powershell
docker compose -f observability/docker-compose.observability.yml up -d
# Prometheus :9090 · Grafana :3000 (admin/admin) · Jaeger :16686 · Alertmanager :9093
```

### Seguridad (cluster kind)
```powershell
# Sealed Secrets
kubectl apply -f k8s/security/sealed-secrets-controller.yaml
kubectl apply -f k8s/security/01-sealed-secrets.yaml
# cert-manager + TLS
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.4/cert-manager.yaml
kubectl apply -f k8s/security/02-tls-issuer.yaml
# NGINX Ingress + ingress TLS
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.2/deploy/static/provider/kind/deploy.yaml
kubectl label node circleguard-dev-control-plane ingress-ready=true --overwrite
kubectl apply -f k8s/security/03-gateway-ingress.yaml
```

### Pruebas
```powershell
# Cobertura + tests:   ver pipelines Jenkins
# Locust:              tests/performance/  (reportes en tests/performance/reports/)
# OWASP ZAP baseline:
docker run --rm -v "${PWD}/tests/security:/zap/wrk:rw" ghcr.io/zaproxy/zaproxy:stable `
  zap-baseline.py -t http://host.docker.internal:8180 -r zap-auth-baseline.html -I
```

## Verificación rápida (smoke)
```powershell
kubectl get sealedsecrets,certificate,ingress,role,rolebinding -n circleguard-dev
curl http://localhost:9090/api/v1/targets        # Prometheus: 6 servicios up
curl http://localhost:16686/api/services         # Jaeger: trazas de los 6
```
