# Guion Detallado de Presentación — Proyecto Final IngeSoft V (CircleGuard)

> Deck de **22 diapositivas** (~20–30 min). Para cada slide: **Qué poner**, **Qué decir** (guion)
> y **Espacio para video** (si aplica). Al final: guía detallada de cada video + prompt para IA.
> Mapeo a los 9 requisitos del enunciado y a los entregables de la demo (arquitectura, CI/CD,
> app funcionando, dashboards, rendimiento, lecciones aprendidas).

---

## PARTE A — Guion diapositiva por diapositiva

### Slide 1 — Portada
- **Qué poner:** Título "CircleGuard — Arquitectura de Microservicios con DevOps, Seguridad y Observabilidad". Subtítulo "Proyecto Final IngeSoft V". Nombre, fecha, logo/imagen. URL del repo.
- **Qué decir:** "Buenos días. Voy a presentar CircleGuard, una plataforma de microservicios para gestión de salud en campus, implementada con prácticas modernas de DevOps, seguridad y observabilidad sobre Kubernetes."
- **Video:** No.

### Slide 2 — Contexto y agenda
- **Qué poner:** 2–3 líneas del problema (control de aforo/salud en campus con QR y trazabilidad de contactos). Lista de los 9 requisitos a cubrir.
- **Qué decir:** "El sistema valida el estado de salud de las personas vía códigos QR y rastrea contactos. Hoy recorro los 9 requisitos: ágil, IaC, patrones, CI/CD, pruebas, change management, observabilidad, seguridad y documentación."
- **Video:** No.

### Slide 3 — Arquitectura del sistema
- **Qué poner:** Diagrama de arquitectura: gateway + 6 microservicios (auth, identity, form, promotion, notification, dashboard) + infra (Postgres, Neo4j, Kafka/Zookeeper, Redis, LDAP). Flechas de eventos Kafka (survey.submitted, certificate.validated, circle.fenced).
- **Qué decir:** "Arquitectura event-driven: el gateway valida QR con Circuit Breaker; los servicios se comunican por eventos Kafka. Postgres para datos relacionales, Neo4j para el grafo de contactos, Redis para estado en caché."
- **Video:** Opcional — clip corto navegando la estructura del repo (15s).

### Slide 4 — Stack tecnológico
- **Qué poner:** Iconos/lista: Spring Boot 3.2 / Java 21, Gradle, Docker, Kubernetes (kind), Terraform, Jenkins, SonarQube, Trivy, Prometheus/Grafana/Jaeger, ELK, Locust, OWASP ZAP.
- **Qué decir:** "Todo el stack es open-source y reproducible: Java/Spring Boot, contenedores, Kubernetes provisionado con Terraform, y un pipeline de Jenkins que orquesta calidad, seguridad y despliegue."
- **Video:** No.

### Slide 5 — R1: Metodología Ágil y Estrategia de Branching (10%)
- **Qué poner:** Captura del board de GitHub Projects (columnas Todo/In Progress/Done, issues #3–#10). Diagrama GitFlow (feature → develop → main). Mención: 8 historias de usuario con criterios Gherkin, 2 sprints.
- **Qué decir:** "Usé Kanban en GitHub Projects con 8 historias de usuario y criterios de aceptación Gherkin, en 2 sprints. La estrategia de branching es GitFlow: ramas feature hacia develop, y releases a main."
- **Video:** Sí — **Clip 1** (board + `git log --graph`). Espacio: recuadro 16:9 a la derecha.

### Slide 6 — R2: Infraestructura como Código con Terraform (20%)
- **Qué poner:** Árbol de `terraform/modules/` (namespaces, databases, messaging, app-services, monitoring) y `environments/{dev,stage,prod}`. Nota: backend remoto (Terraform Cloud), `terraform validate` limpio. Mini-diagrama de infra.
- **Qué decir:** "Toda la infraestructura es código: módulos parametrizados reutilizables, tres ambientes con distinta configuración (réplicas, recursos), y estado remoto en Terraform Cloud. `terraform validate` pasa limpio en los tres."
- **Video:** Sí — **Clip 3** (`terraform validate` + `kubectl get pods`). Espacio: recuadro 16:9.

### Slide 7 — R3: Patrones de Diseño (10%)
- **Qué poner:** Tabla patrones existentes (API Gateway, Event-Driven/Pub-Sub, Repository, DTO, Externalized Config, Health Check). Destacar los nuevos: **Circuit Breaker** (Resilience4j en gateway) y **Feature Toggle**. Diagrama de estados del circuit breaker (closed/open/half-open).
- **Qué decir:** "Documenté los patrones existentes e implementé tres nuevos: Circuit Breaker con Resilience4j para tolerar caídas de Redis sin tumbar la validación de QR, y Feature Toggle para activar/desactivar la validación estricta sin redeploy."
- **Video:** Sí — **Clip 2** (código + test del circuit breaker en verde). Espacio: recuadro.

### Slide 8 — R4: CI/CD Avanzado — Visión del pipeline (15%) ⭐
- **Qué poner:** Diagrama del pipeline con TODOS los stages: Checkout → Build → Unit Tests → Integration Tests → Coverage → SonarQube → Quality Gate → Docker Build → Trivy → Deploy → (stage) E2E → (master) Promote Prod → Release Notes. Captura del Stage View con los 6 jobs en SUCCESS.
- **Qué decir:** "El pipeline de Jenkins cubre el ciclo completo. Cada commit pasa por build, pruebas, cobertura, análisis estático, escaneo de seguridad y despliegue. Los 6 microservicios tienen su pipeline, todos en verde."
- **Video:** Sí — **Clip 4 (parte 1)**: Stage View completo. Espacio: recuadro grande 16:9.

### Slide 9 — R4: CI/CD — Calidad y Seguridad en el pipeline
- **Qué poner:** Captura SonarQube Quality Gate **Passed** (proyecto `circleguard`, 0 nuevas violaciones). Mención de Trivy (HIGH/CRITICAL, reporte JSON archivado). Nota: el Quality Gate **aborta el build** si no pasa.
- **Qué decir:** "Integré SonarQube con un Quality Gate que bloquea de verdad: si el análisis no pasa, el pipeline se aborta. Trivy escanea cada imagen por vulnerabilidades HIGH y CRITICAL y archiva el reporte."
- **Video:** Sí — **Clip 4 (parte 2)**: SonarQube + artefacto Trivy. Espacio: recuadro.

### Slide 10 — R4: CI/CD — Promoción, aprobación y notificaciones
- **Qué poner:** Diagrama dev → stage → prod. Captura del stage **Promote Prod** con el `input` "¿Aprobar despliegue a producción?". Captura del email de fallo en MailHog. Mención: versionado semántico + CHANGELOG automático en master.
- **Qué decir:** "La promoción a producción exige aprobación manual: el pipeline se pausa hasta que un humano aprueba. Si algo falla, llega una notificación por email automáticamente. En master se generan release notes y el changelog."
- **Video:** Sí — **Clip 4 (parte 3)**: approval gate + email de fallo. Espacio: recuadro.

### Slide 11 — R5: Pruebas — Funcionales y cobertura (15%)
- **Qué poner:** Tabla de cobertura JaCoCo por servicio (dashboard 73%, form 71%, identity 66%, notification 59%, auth 47%, promotion 38%). Conteo de tests (87 total, 0 fallos, 0 skips). Mención: unit + integración (EmbeddedKafka) + E2E (Newman) + Testcontainers (Neo4j en promotion).
- **Qué decir:** "Pruebas en cinco niveles: unitarias, de integración con Kafka embebido, E2E con Newman, y de integración con Neo4j real vía Testcontainers. 87 pruebas en verde, con cobertura real medida por JaCoCo."
- **Video:** Sí — **Clip 6 (parte 1)**: coverage report en Jenkins. Espacio: recuadro.

### Slide 12 — R5: Pruebas — Rendimiento y Seguridad
- **Qué poner:** Resultados Locust: steady (1046 req, p95 79ms) y spike (50k req, 99% éxito, ~278 req/s). Resultados OWASP ZAP: 0 FAIL, 1 WARN, 66 PASS. Gráfica de Locust.
- **Qué decir:** "Con Locust probé carga sostenida y picos: bajo carga normal el p95 es menor a 80 ms; en el pico extremo el sistema mantiene 99% de éxito. El escaneo dinámico con OWASP ZAP no encontró fallos críticos."
- **Video:** Sí — **Clip 6 (parte 2)**: reporte Locust HTML + reporte ZAP. Espacio: recuadro.

### Slide 13 — R6: Change Management y Release Notes (5%)
- **Qué poner:** Esquema del proceso de cambios (solicitud → revisión → aprobación → despliegue → rollback). Captura del stage Release Notes + artefacto CHANGELOG.md. Mención: etiquetado de releases, plan de rollback documentado.
- **Qué decir:** "Definí un proceso formal de gestión de cambios con plan de rollback, y la generación automática de release notes y changelog en cada release a master."
- **Video:** Sí — **Clip 7** (Release Notes stage + docs). Espacio: recuadro pequeño.

### Slide 14 — R7: Observabilidad — Métricas y Dashboards (10%)
- **Qué poner:** Captura del dashboard Grafana "CircleGuard — Overview" (disponibilidad, throughput, latencia p99, JVM, métricas de negocio). Captura de Prometheus targets (6 UP).
- **Qué decir:** "Instrumenté los servicios con Micrometer; Prometheus scrapea los 6 y Grafana los visualiza. El dashboard muestra disponibilidad, throughput, latencia p99 y memoria, además de métricas de negocio."
- **Video:** Sí — **Clip 8 (parte 1)**: Prometheus targets + Grafana dashboard. Espacio: recuadro grande.

### Slide 15 — R7: Observabilidad — Tracing, Alertas y Métricas de Negocio
- **Qué poner:** Captura de Jaeger (traza distribuida con spans). Captura de alerta ServiceDown firing + email en MailHog. Métricas de negocio: `surveys_submitted_total`, `qr_validations_total`, `notifications_sent_total`.
- **Qué decir:** "El tracing distribuido con Jaeger permite seguir una petición a través de los servicios. Configuré alertas con Alertmanager: si un servicio cae, se dispara una alerta y llega un correo. Y medí métricas de negocio, no solo técnicas."
- **Video:** Sí — **Clip 8 (parte 2)**: Jaeger + alerta + email. Espacio: recuadro.

### Slide 16 — R7: Observabilidad — Logging centralizado (ELK)
- **Qué poner:** Captura de Kibana (Discover) con los logs de los servicios (data view "CircleGuard Logs"). Diagrama: contenedores → Filebeat → Elasticsearch → Kibana. Mención: health checks + readiness/liveness probes.
- **Qué decir:** "Los logs de todos los contenedores se centralizan con Filebeat en Elasticsearch y se exploran en Kibana. Junto con los health checks y probes de Kubernetes, tengo visibilidad completa: métricas, trazas y logs."
- **Video:** Sí — **Clip 8 (parte 3)**: Kibana Discover. Espacio: recuadro.

### Slide 17 — R8: Seguridad (5%)
- **Qué poner:** 4 columnas: **Secretos** (Sealed Secrets cifrados en git), **TLS** (cert-manager + Ingress), **RBAC** (least-privilege), **Escaneo continuo** (Trivy en pipeline). Captura de `kubectl get sealedsecrets,certificate,ingress,role`.
- **Qué decir:** "Cuatro frentes de seguridad: secretos cifrados con Sealed Secrets que viven en git de forma segura, TLS emitido por cert-manager en el Ingress, RBAC de mínimo privilegio, y escaneo continuo de vulnerabilidades con Trivy en cada pipeline."
- **Video:** Sí — **Clip 5** (kubectl de seguridad). Espacio: recuadro.

### Slide 18 — R9: Documentación, Costos y Operación (10%)
- **Qué poner:** Índice de documentación (`docs/proyecto-final/`). Tabla de costos AWS (demo 2 días: ~$7 EC2 Spot / ~$18 EKS Spot; por ambiente). Mención: manual de operaciones (runbook).
- **Qué decir:** "Todo está documentado: arquitectura, patrones, runbook de operación e informe de pruebas. Estimé el costo de llevarlo a AWS: un demo de dos días cuesta entre 7 y 40 dólares según la arquitectura, con guardarraíles para no exceder el presupuesto."
- **Video:** No (o navegar docs 10s).

### Slide 19 — Demostración en vivo
- **Qué poner:** Slide de transición "Demo en vivo" + enlace al video completo. Lista de lo que se mostrará (CI/CD, app, dashboards, rendimiento).
- **Qué decir:** "Ahora una demostración del sistema funcionando de extremo a extremo." (Reproduce el video integrado o haz la demo en vivo.)
- **Video:** Sí — **video completo** (concatenación de clips o demo en vivo). Espacio: slide casi full-screen para el video.

### Slide 20 — Lecciones aprendidas
- **Qué poner:** Bullets: race condition de SonarQube (resuelta con lock); Testcontainers en CI (host.docker.internal + Ryuk); restricción de RAM 8GB (despliegue por tandas); cobertura JaCoCo (ruta java vs kotlin).
- **Qué decir:** "Aprendizajes clave: serializar análisis concurrentes para evitar condiciones de carrera, configurar Testcontainers para correr en CI containerizado, y gestionar recursos limitados desplegando por tandas."
- **Video:** No.

### Slide 21 — Cobertura de requisitos y bonificaciones
- **Qué poner:** Tabla de los 9 requisitos con ✅ y el peso (%). Nota de bonificaciones no implementadas (Multi-Cloud, Service Mesh, Chaos, FinOps) como trabajo futuro.
- **Qué decir:** "Los 9 requisitos están cubiertos y verificados en vivo, sumando el 100% de la nota base. Las bonificaciones (service mesh, chaos engineering, multi-cloud, FinOps) quedan como trabajo futuro."
- **Video:** No.

### Slide 22 — Conclusiones y cierre
- **Qué poner:** 3 conclusiones (DevOps de punta a punta, calidad/seguridad como gates reales, observabilidad completa). URL del repo + del video. "Gracias / Preguntas".
- **Qué decir:** "En conclusión, CircleGuard demuestra un ciclo DevOps completo donde la calidad y la seguridad no son opcionales sino gates que bloquean, con observabilidad total. Gracias, quedo atento a preguntas."
- **Video:** No.

---

## PARTE B — Guía detallada de qué grabar en cada video

> Graba en **dos tandas** (8GB RAM no soporta todo a la vez). Resolución 1080p, terminal con fuente grande, sin notificaciones del SO. Narra la intención, no solo lo visible.

### Preparación
**Tanda A (CI/CD + Seguridad):**
```powershell
docker compose -f observability/docker-compose.elk.yml stop
docker compose -f observability/docker-compose.observability.yml stop
docker start circleguard-dev-control-plane           # cluster kind (Seguridad)
docker start circleguard-sonar-db circleguard-sonarqube circleguard-jenkins
```
**Tanda B (Observabilidad):**
```powershell
docker stop circleguard-jenkins circleguard-sonarqube circleguard-sonar-db
docker compose -f docker-compose.dev.yml up -d
docker compose -f observability/docker-compose.observability.yml up -d
# ELK (requiere detener kind):
docker stop circleguard-dev-control-plane
docker compose -f observability/docker-compose.elk.yml up -d
```

### Clip 1 — Ágil (~1.5 min) · Tanda cualquiera
- Abre el board de GitHub Projects; recorre columnas Todo/In Progress/Done y abre 1 issue mostrando criterios de aceptación.
- En terminal: `git log --graph --oneline -15` y `git branch -a` (muestra develop + feature/*).
- Abre `docs/proyecto-final/01-agile/historias-usuario.md`.
- **Narra:** Kanban, 8 HUs, 2 sprints, GitFlow.

### Clip 2 — Patrones (~2 min)
- Abre `QrValidationService.java`: señala `@CircuitBreaker(name="redis-status", fallbackMethod=...)` y el `application.yml` con la config Resilience4j.
- Abre `FeatureFlags.java` (`@ConfigurationProperties`).
- En cámara: `.\gradlew.bat :services:circleguard-gateway-service:test --no-daemon` y muestra BUILD SUCCESSFUL.
- **Narra:** por qué Circuit Breaker (resiliencia ante caída de Redis) y Feature Toggle.

### Clip 3 — Terraform (~2 min)
- Muestra el árbol `terraform/modules/` y `environments/`.
- En cámara: `cd terraform/environments/dev; terraform validate` (limpio) y `terraform plan` (muestra recursos).
- `kubectl get ns` y `kubectl get pods -n circleguard-dev`.
- **Narra:** módulos, multi-ambiente, backend remoto.

### Clip 4 — CI/CD (~5 min) · Tanda A ⭐ (el más importante)
- **Parte 1:** Jenkins dashboard (6 jobs SUCCESS) → entra a `circleguard-auth-service` → Stage View; recorre los stages uno por uno.
- **Parte 2:** SonarQube (`:9000`) → proyecto `circleguard` → Quality Gate **Passed** y métricas. Explica que `waitForQualityGate abortPipeline:true` aborta si falla. Abre un artefacto `trivy-*.json`.
- **Parte 3:** Abre el build `auth-service #45` (ENVIRONMENT=master) → stage Promote Prod con el `input` de aprobación; explica cómo se aprueba. Abre MailHog (`:8025`) y muestra el email "FALLO — dashboard #39".
- **Narra:** el pipeline cubre calidad + seguridad + promoción controlada con gates reales.

### Clip 5 — Seguridad (~2.5 min) · Tanda A (cluster kind activo)
- En terminal, ejecuta y explica:
  ```powershell
  kubectl get sealedsecrets -n circleguard-dev
  kubectl get secret circleguard-db-secret -n circleguard-dev -o jsonpath='{.metadata.ownerReferences[0].kind}'
  kubectl get clusterissuer; kubectl get certificate -n circleguard-dev
  kubectl get ingress -n circleguard-dev
  kubectl get role,rolebinding,serviceaccount -n circleguard-dev
  ```
- Abre `k8s/security/01-sealed-secrets.yaml` (encryptedData ilegible).
- **Narra:** secretos cifrados en git, TLS por cert-manager, RBAC mínimo privilegio, Trivy en pipeline.

### Clip 6 — Pruebas (~3 min)
- **Parte 1:** en Jenkins, "Coverage Report" de 2–3 servicios (muestra los %). Menciona Testcontainers (promotion, skip=0).
- **Parte 2:** abre `tests/performance/reports/locust-steady-*.html` y `locust-spike-*.html` (gráficas). Abre `tests/security/zap-auth-baseline.html` (resumen 0 FAIL/1 WARN/66 PASS). Abre `docs/proyecto-final/05-pruebas/resultados.md`.
- **Narra:** calidad medida en 5 dimensiones, integrada al pipeline.

### Clip 7 — Change Management (~1.5 min) · Tanda A
- Abre `docs/proyecto-final/06-change-management/proceso.md` y `rollback.md`.
- En Jenkins, build master `auth #45` → stage Release Notes → artefacto CHANGELOG.md.
- **Narra:** proceso formal + rollback + release notes automáticas.

### Clip 8 — Observabilidad (~4 min) · Tanda B (el más visual)
- **Parte 1:** Prometheus `:9090/targets` (6 UP). Grafana `:3000` (admin/admin) → dashboard "CircleGuard — Overview".
- **Parte 2 (métrica de negocio en vivo):**
  ```powershell
  curl -X POST http://localhost:8086/api/v1/surveys -H "Content-Type: application/json" -d '{"anonymousId":"33333333-3333-3333-3333-333333333333","hasFever":true}'
  ```
  En Prometheus: query `surveys_submitted_total` (sube). Jaeger `:16686` → selecciona servicio → abre una traza con spans.
- **Parte 3 (alerta):** `docker stop circleguard-notification`; espera ~90s; muestra `:9090/alerts` (ServiceDown) y el email "[FIRING:1] ServiceDown" en MailHog (`:8025`); luego `docker start circleguard-notification`.
- **Parte 4 (ELK):** Kibana `:5601` → Discover → data view "CircleGuard Logs"; filtra por `container.name: circleguard-form`.
- **Narra:** métricas + trazas + logs + alertas = observabilidad completa.

### Clip 9 (opcional) — Cierre (~1 min)
- Navega `docs/proyecto-final/09-documentacion/` y la tabla de costos.
- **Narra:** documentación completa y costo predecible en nube.

### Edición final
- Concatena los clips en orden o enlázalos en las slides. Recorta tiempos muertos. Total objetivo: 20–30 min. Sube a YouTube (no listado) o Drive y enlaza en Slide 19 y 22.

---

## PARTE C — Prompt para generar las diapositivas con IA

> Copia y pega este prompt en una IA generadora de slides (Gamma, Tome, Beautiful.ai, Copilot/PowerPoint, o "genera un .pptx"). Incluye los placeholders de video.

```
Genera una presentación profesional de 22 diapositivas en formato 16:9 para un proyecto
universitario de ingeniería de software titulado "CircleGuard — Arquitectura de Microservicios
con DevOps, Seguridad y Observabilidad" (Proyecto Final IngeSoft V). Estilo: técnico, limpio,
con acentos azul/verde, iconos de tecnología, y buena jerarquía visual. En las diapositivas
marcadas con [VIDEO] deja un recuadro/placeholder 16:9 vacío con la etiqueta "▶ Video: <nombre>"
para insertar un clip después. Usa viñetas cortas, no párrafos largos. Idioma: español.

Genera exactamente estas 22 diapositivas con este contenido:

1. PORTADA: Título "CircleGuard — Microservicios con DevOps, Seguridad y Observabilidad",
   subtítulo "Proyecto Final IngeSoft V", espacio para nombre/fecha y URL del repositorio.
2. CONTEXTO Y AGENDA: problema (control de salud/aforo en campus con QR y trazabilidad de
   contactos) + lista de los 9 requisitos (Ágil, IaC, Patrones, CI/CD, Pruebas, Change Mgmt,
   Observabilidad, Seguridad, Documentación).
3. ARQUITECTURA: diagrama de un gateway + 6 microservicios (auth, identity, form, promotion,
   notification, dashboard) + infraestructura (PostgreSQL, Neo4j, Kafka/Zookeeper, Redis, LDAP),
   con eventos Kafka (survey.submitted, certificate.validated, circle.fenced). [VIDEO opcional]
4. STACK TECNOLÓGICO: Spring Boot 3.2/Java 21, Gradle, Docker, Kubernetes (kind), Terraform,
   Jenkins, SonarQube, Trivy, Prometheus, Grafana, Jaeger, ELK, Locust, OWASP ZAP.
5. R1 METODOLOGÍA ÁGIL Y BRANCHING (10%): Kanban en GitHub Projects, 8 historias de usuario con
   criterios Gherkin, 2 sprints, estrategia GitFlow (feature→develop→main). [VIDEO]
6. R2 IAC CON TERRAFORM (20%): módulos (namespaces, databases, messaging, app-services,
   monitoring), 3 ambientes (dev/stage/prod), backend remoto (Terraform Cloud), validate limpio.
   [VIDEO]
7. R3 PATRONES DE DISEÑO (10%): patrones existentes (API Gateway, Event-Driven, Repository, DTO,
   External Config, Health Check) + nuevos: Circuit Breaker (Resilience4j) y Feature Toggle;
   incluir diagrama de estados del circuit breaker. [VIDEO]
8. R4 CI/CD — PIPELINE (15%): diagrama del pipeline con stages Build→Unit→Integration→Coverage→
   SonarQube→Quality Gate→Docker Build→Trivy→Deploy→E2E→Promote Prod→Release Notes; los 6
   servicios en SUCCESS. [VIDEO]
9. R4 CI/CD — CALIDAD Y SEGURIDAD: SonarQube Quality Gate "Passed" (0 nuevas violaciones), Trivy
   HIGH/CRITICAL, el Quality Gate aborta el build si no pasa. [VIDEO]
10. R4 CI/CD — PROMOCIÓN, APROBACIÓN Y NOTIFICACIONES: flujo dev→stage→prod, aprobación manual
    (input) para producción, email automático de fallo, versionado semántico + CHANGELOG. [VIDEO]
11. R5 PRUEBAS — FUNCIONALES Y COBERTURA (15%): tabla de cobertura JaCoCo (dashboard 73%, form
    71%, identity 66%, notification 59%, auth 47%, promotion 38%); 87 tests, 0 fallos; unit +
    integración (Kafka embebido) + E2E (Newman) + Testcontainers (Neo4j). [VIDEO]
12. R5 PRUEBAS — RENDIMIENTO Y SEGURIDAD: Locust steady (1046 req, p95 79ms) y spike (50k req,
    99% éxito, 278 req/s); OWASP ZAP (0 FAIL, 1 WARN, 66 PASS). [VIDEO]
13. R6 CHANGE MANAGEMENT Y RELEASE NOTES (5%): proceso de cambios (solicitud→revisión→aprobación→
    despliegue→rollback), release notes y CHANGELOG automáticos, etiquetado de releases. [VIDEO]
14. R7 OBSERVABILIDAD — MÉTRICAS Y DASHBOARDS (10%): Prometheus scrapeando 6 servicios (UP),
    dashboard de Grafana "CircleGuard — Overview" (disponibilidad, throughput, p99, JVM, negocio).
    [VIDEO]
15. R7 OBSERVABILIDAD — TRACING, ALERTAS Y NEGOCIO: Jaeger (traza distribuida), Alertmanager
    (alerta ServiceDown → email), métricas de negocio (surveys_submitted_total, qr_validations_
    total, notifications_sent_total). [VIDEO]
16. R7 OBSERVABILIDAD — LOGGING (ELK): diagrama contenedores→Filebeat→Elasticsearch→Kibana,
    Kibana Discover con logs; health checks + readiness/liveness probes. [VIDEO]
17. R8 SEGURIDAD (5%): 4 pilares — Sealed Secrets (cifrado en git), TLS (cert-manager + Ingress),
    RBAC least-privilege, escaneo continuo (Trivy). [VIDEO]
18. R9 DOCUMENTACIÓN Y COSTOS (10%): índice de documentación, tabla de costos AWS (demo 2 días:
    ~$7 EC2 Spot / ~$18 EKS Spot; mensual por ambiente), manual de operaciones (runbook).
19. DEMOSTRACIÓN EN VIVO: slide de transición con un placeholder de video grande (casi full-screen)
    y lista de lo que se mostrará (CI/CD, app, dashboards, rendimiento). [VIDEO grande]
20. LECCIONES APRENDIDAS: race condition de SonarQube (resuelta con lock), Testcontainers en CI
    (host.docker.internal + Ryuk), restricción de RAM 8GB (despliegue por tandas), ruta JaCoCo
    java vs kotlin.
21. COBERTURA DE REQUISITOS: tabla de los 9 requisitos con ✅ y su peso (suma 100% base);
    bonificaciones (Multi-Cloud, Service Mesh, Chaos, FinOps) como trabajo futuro.
22. CONCLUSIONES Y CIERRE: 3 conclusiones (DevOps de punta a punta, calidad/seguridad como gates
    reales, observabilidad completa), URL del repo y del video, "Gracias / Preguntas".

Para cada diapositiva incluye un título claro, 3–6 viñetas concisas, y donde diga [VIDEO] un
recuadro 16:9 etiquetado "▶ Video". Mantén consistencia visual y numeración de requisitos.
```

---

## Resumen de espacios de video por slide
| Slide | Requisito | ¿Video? | Clip |
|---|---|---|---|
| 5 | R1 Ágil | Sí | Clip 1 |
| 6 | R2 Terraform | Sí | Clip 3 |
| 7 | R3 Patrones | Sí | Clip 2 |
| 8–10 | R4 CI/CD | Sí | Clip 4 (3 partes) |
| 11–12 | R5 Pruebas | Sí | Clip 6 (2 partes) |
| 13 | R6 Change Mgmt | Sí | Clip 7 |
| 14–16 | R7 Observabilidad | Sí | Clip 8 (4 partes) |
| 17 | R8 Seguridad | Sí | Clip 5 |
| 19 | Demo | Sí | Video completo |
| 1–4, 18, 20–22 | — | No | — |
