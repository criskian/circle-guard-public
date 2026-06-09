# Documentación de Sprints — CircleGuard

**Proyecto:** CircleGuard — Proyecto Final IngeSoft V  
**Metodología:** Scrum  
**Duración de sprint:** 1 semana  
**Tablero:** [GitHub Projects](https://github.com/users/criskian/projects/1)

---

## Sprint 1 — Estabilización CI/CD y Funcionalidad Core

**Período:** Semana 1  
**Objetivo del sprint:** Lograr que los 6 microservicios pasen todos los stages del pipeline CI/CD (Unit Tests, Integration Tests, Docker Build, Deploy Stage, E2E Tests, Promote Prod, Release Notes) con ENVIRONMENT=master.

### Backlog Comprometido

| Historia | SP | Resultado |
|----------|----|-----------|
| HU-01: Login LDAP | 3 | ✅ Completado |
| HU-02: Encuesta diaria | 5 | ✅ Completado |
| HU-03: Validación QR | 5 | ✅ Completado |
| HU-04: Notificaciones Kafka | 3 | ✅ Completado |
| Spike: K8s liveness probes calibration | 2 | ✅ Completado |
| **Total** | **18 SP** | ✅ 18/18 completados |

### Velocity

- **Comprometido:** 18 SP
- **Completado:** 18 SP
- **Velocity:** 18 SP/sprint

### Entregables del Sprint

1. ✅ 6 pipelines Jenkins corriendo con todas las stages en verde (ENVIRONMENT=master)
2. ✅ K8s namespace `circleguard-stage` con 6 pods 1/1 Running
3. ✅ K8s namespace `circleguard-prod` con 6 pods 1/1 Running
4. ✅ E2E tests (Newman) pasando 25/25 assertions
5. ✅ Release Notes generadas automáticamente vía Conventional Commits
6. ✅ Fixes críticos: SPRING_NEO4J_URI configmap, liveness delays, KAFKA_PORT removal

### Impedimentos Encontrados

| Impedimento | Impacto | Solución |
|-------------|---------|----------|
| Docker Desktop OOM con 6 JVMs en paralelo | 3 builds crashes | Limitar a 2 pipelines concurrentes vía Groovy API |
| Neo4j CrashLoopBackOff por env var inválida | Pod no arrancaba | Remover `NEO4J_server_config_strict__validation__enabled` |
| Spring Boot ignoraba `NEO4J_URI` | Servicio conectaba a localhost | Cambiar a `SPRING_NEO4J_URI` (relaxed binding) |
| Liveness probe mataba pods antes de startup | Pods reiniciando en bucle | Aumentar `initialDelaySeconds` a 180s |
| Kafka en prod con KAFKA_PORT crasheaba | Kafka no arrancaba | Remover `KAFKA_PORT` del deployment |

### Retrospectiva Sprint 1

**¿Qué salió bien?**
- La estructura de Jenkinsfiles con parámetro `ENVIRONMENT` permitió reutilizar el mismo pipeline para dev/stage/master
- Los tests de integración con TestContainers/EmbeddedKafka son robustos y no requieren infra externa
- La secuenciación de pipelines (de a 2) resolvió el problema de OOM sin degradar la funcionalidad

**¿Qué mejorar?**
- Los `initialDelaySeconds` deberían derivarse dinámicamente del tiempo real de startup medido
- El script de release notes podría publicar el artefacto directamente a GitHub Releases
- Falta monitoreo: no hay visibilidad de por qué los pods tardan tanto en arrancar

**Acciones de mejora para Sprint 2:**
- [ ] Implementar Prometheus + Grafana para visibilidad de startup time
- [ ] Agregar SonarQube al pipeline para no acumular deuda técnica
- [ ] Crear módulos Terraform para reproducir el ambiente sin pasos manuales

---

## Sprint 2 — Observabilidad, IaC y Seguridad

**Período:** Semana 2  
**Objetivo del sprint:** Implementar el stack de observabilidad (Prometheus/Grafana/ELK/Jaeger), Infraestructura como Código con Terraform, SonarQube+Trivy en pipelines, y RBAC/TLS de seguridad.

### Backlog Comprometido

| Historia | SP | Estado |
|----------|----|--------|
| HU-05: Dashboard monitoreo (Prometheus+Grafana) | 8 | 🔄 In Progress |
| HU-06: Aprobación manual a producción | 5 | 🔄 In Progress |
| HU-07: SonarQube en CI + JaCoCo | 3 | 📋 Planned |
| HU-08: Terraform IaC multi-ambiente | 8 | 📋 Planned |
| Circuit Breaker (Resilience4j) | 5 | 🔄 In Progress |
| Feature Toggle + External Config | 3 | 🔄 In Progress |
| RBAC + TLS + Sealed Secrets | 5 | 📋 Planned |
| ELK Stack + Jaeger tracing | 5 | 📋 Planned |
| OWASP ZAP + cobertura JaCoCo | 3 | 📋 Planned |
| **Total** | **45 SP** | |

### Definición de Hecho (DoD)

Una historia se considera "Done" cuando:
1. El código está mergeado a `develop` vía PR con review
2. El pipeline de CI corre en verde (incluyendo SonarQube quality gate)
3. Los tests relevantes existen y pasan (cobertura ≥ 60%)
4. La documentación asociada está actualizada en `docs/proyecto-final/`
5. La funcionalidad está demostrable en el ambiente `circleguard-stage`

---

## Métricas de Proceso

| Métrica | Sprint 1 | Sprint 2 (estimado) |
|---------|----------|---------------------|
| Stories completadas | 5/5 (100%) | — |
| SP completados | 18/18 | — |
| Bugs escapados a stage | 5 (K8s config) | 0 (objetivo) |
| Tiempo promedio de pipeline | ~12 min | ~15 min (SonarQube +3 min) |
| Cobertura de pruebas | ~45% | ≥ 60% (objetivo) |
