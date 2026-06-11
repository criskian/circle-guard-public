# Guía de Grabación de Video — Proyecto Final IngeSoft V (CircleGuard)

> **Objetivo:** grabar un video demostrativo (20–30 min) que evidencie cada requisito del taller.
> **Formato sugerido:** screen recording (OBS Studio / Xbox Game Bar `Win+G` / Loom) a 1080p, con narración por voz.
> **Estructura:** un clip por requisito; al final se concatenan o se enlazan en la presentación.

## Antes de grabar — preparación del entorno

Por la RAM de 8 GB **no todo corre a la vez**. Graba en **dos tandas**:

**Tanda A — CI/CD + Seguridad (Jenkins/SonarQube/cluster):**
```powershell
# Detener observabilidad para liberar RAM
docker compose -f observability/docker-compose.observability.yml stop
# Levantar Jenkins + SonarQube
docker start circleguard-sonar-db circleguard-sonarqube circleguard-jenkins
```
Espera ~2 min a que SonarQube esté UP (`http://localhost:9000`).

**Tanda B — Observabilidad (Prometheus/Grafana/Jaeger):**
```powershell
docker stop circleguard-jenkins circleguard-sonarqube circleguard-sonar-db
docker compose -f docker-compose.dev.yml up -d
docker compose -f observability/docker-compose.observability.yml up -d
```

Ten abiertas las pestañas del navegador antes de grabar para que no se vea tiempo muerto.

---

## Clip 0 — Introducción (1–2 min)
- **Mostrar:** el README del repo y el diagrama de arquitectura (`docs/proyecto-final/02-terraform/arquitectura-infra.md`).
- **Narrar:** "CircleGuard es un sistema de microservicios (6 servicios Spring Boot + gateway) para gestión de salud en campus. Este video demuestra los 9 requisitos del proyecto final."
- **Tip:** muestra el tablero de GitHub Projects y la estructura de carpetas del repo.

---

## Clip 1 — Metodología Ágil y Branching (10%) · ~2 min
- **Mostrar en pantalla:**
  1. GitHub Projects board: https://github.com/users/criskian/projects/1 (columnas Todo/In Progress/Done con issues #3–#10).
  2. `docs/proyecto-final/01-agile/historias-usuario.md` (8 HUs con Gherkin).
  3. `git log --oneline --graph --all` mostrando ramas `develop` + `feature/*` y PRs mergeados.
- **Narrar:** explica GitFlow (feature → develop → main), los 2 sprints, y los criterios de aceptación Gherkin.
- **Comando en cámara:** `git branch -a` y `git log --graph --oneline -15`.

---

## Clip 2 — Patrones de Diseño (10%) · ~3 min
- **Mostrar:**
  1. `docs/proyecto-final/03-patrones/patrones-nuevos.md`.
  2. **Circuit Breaker:** `services/circleguard-gateway-service/.../QrValidationService.java` — la anotación `@CircuitBreaker(name="redis-status", fallbackMethod=...)` y el `application.yml` con la config Resilience4j.
  3. **Feature Toggle:** `FeatureFlags.java` con `@ConfigurationProperties(prefix="circleguard.features")`.
  4. Los tests del circuit breaker en verde: en cámara corre
     ```powershell
     .\gradlew.bat :services:circleguard-gateway-service:test --no-daemon
     ```
- **Narrar:** explica por qué Circuit Breaker (resiliencia ante caída de Redis) y Feature Toggle (activar/desactivar validación estricta de QR sin redeploy).

---

## Clip 3 — IaC con Terraform (20%) · ~3 min
- **Mostrar:**
  1. Estructura `terraform/modules/` (namespaces, databases, messaging, app-services, monitoring) y `terraform/environments/{dev,stage,prod}`.
  2. En cámara:
     ```powershell
     cd terraform/environments/dev
     terraform validate
     terraform plan          # mostrar el plan (recursos a crear)
     ```
  3. El cluster real: `kubectl get ns` y `kubectl get pods -n circleguard-dev`.
- **Narrar:** módulos parametrizados, multi-ambiente (réplicas dev=1/prod=2), backend remoto (Terraform Cloud para stage/prod), provider kubernetes+helm.
- **Captura clave:** `terraform validate` limpio en los 3 ambientes.

---

## Clip 4 — CI/CD Avanzado (15%) · ~5 min  ⭐ (núcleo del taller)
> Tanda A (Jenkins + SonarQube arriba).
- **Mostrar y narrar paso a paso:**
  1. **Jenkins dashboard** (`http://localhost:8080`): los 6 jobs en SUCCESS.
  2. Entra a `circleguard-auth-service` → **Stage View**: recorre los stages
     `Build → Unit Tests → Integration Tests → Coverage → SonarQube Analysis → Quality Gate → Docker Build → Trivy Scan`.
  3. **SonarQube** (`http://localhost:9000` → proyecto `circleguard`): muestra el **Quality Gate = Passed** y las métricas.
  4. **Quality Gate que bloquea:** explica (o muestra el build dashboard #38/#39) que cuando el análisis no pasa, `waitForQualityGate abortPipeline:true` **aborta** el pipeline.
  5. **Trivy:** abre un artefacto `trivy-*.json` de un build (Artifacts).
  6. **Aprobación manual de producción:** muestra el build `auth-service #45` (ENVIRONMENT=master) → stage **Promote Prod** con el `input` "¿Aprobar despliegue a producción?" y cómo se aprobó → SUCCESS.
  7. **Notificación de fallo:** abre MailHog (`http://localhost:8025`) y muestra el email *"FALLO — circleguard-dashboard-service Build #39"*.
- **Tip de edición:** este clip es el más importante; tómate tiempo en el Stage View y el Quality Gate.

---

## Clip 5 — Seguridad (5%) · ~3 min
> Tanda A (cluster kind activo).
- **Mostrar en terminal (en cámara):**
  ```powershell
  # Sealed Secrets: el SealedSecret cifrado y el Secret desencriptado por el controlador
  kubectl get sealedsecrets -n circleguard-dev
  kubectl get secret circleguard-db-secret -n circleguard-dev -o jsonpath='{.metadata.ownerReferences[0].kind}'
  # TLS con cert-manager
  kubectl get clusterissuer; kubectl get certificate -n circleguard-dev    # Ready=True
  # Ingress con TLS
  kubectl get ingress -n circleguard-dev
  # RBAC least-privilege
  kubectl get role,rolebinding,serviceaccount -n circleguard-dev
  ```
- **Narrar:** los secretos viven cifrados en git (Sealed Secrets), el certificado TLS lo emite cert-manager, el Ingress termina TLS, y el RBAC limita el ServiceAccount a solo lectura.
- **Mostrar también:** `k8s/security/01-sealed-secrets.yaml` (datos `encryptedData` ilegibles) + Trivy en el pipeline (Clip 4).

---

## Clip 6 — Pruebas Completas (15%) · ~3 min
- **Mostrar:**
  1. **Cobertura JaCoCo:** en Jenkins, "Coverage Report" de cada servicio (auth 47%, dashboard 73%, form 71%, identity 66%, notification 59%, promotion 38%).
  2. **Pruebas unitarias + integración** en verde (Stage View o `build/reports/tests`).
  3. **Testcontainers:** menciona que promotion corre 7 tests de integración con Neo4j real (Testcontainers), skip=0.
  4. **Locust:** abre `tests/performance/reports/locust-steady-*.html` y `locust-spike-*.html`.
  5. **OWASP ZAP:** abre `tests/security/zap-auth-baseline.html`.
  6. **Informe consolidado:** `docs/proyecto-final/05-pruebas/resultados.md`.

---

## Clip 7 — Change Management y Release Notes (5%) · ~1.5 min
- **Mostrar:**
  1. `docs/proyecto-final/06-change-management/proceso.md` y `rollback.md`.
  2. El stage **Release Notes** del pipeline master y el artefacto `CHANGELOG.md` generado (build auth #45).
- **Narrar:** proceso formal de cambios + plan de rollback + generación automática de release notes en master.

---

## Clip 8 — Observabilidad y Monitoreo (10%) · ~4 min
> Tanda B (Prometheus/Grafana/Jaeger arriba).
- **Mostrar y narrar:**
  1. **Prometheus targets** (`http://localhost:9090/targets`): los 6 servicios `UP`.
  2. **Grafana** (`http://localhost:3000`, admin/admin) → dashboard **"CircleGuard — Overview"**: paneles de disponibilidad, throughput, latencia p99, JVM y **métricas de negocio**.
  3. **Métrica de negocio en vivo:** en otra pestaña envía una survey y muestra cómo sube `surveys_submitted_total`:
     ```powershell
     curl -X POST http://localhost:8086/api/v1/surveys -H "Content-Type: application/json" -d '{"anonymousId":"22222222-2222-2222-2222-222222222222","hasFever":true}'
     ```
     Luego en Prometheus: query `surveys_submitted_total`.
  4. **Jaeger** (`http://localhost:16686`): selecciona un servicio → Find Traces → abre una traza y muestra los spans.
  5. **Alertas:** en cámara detén un servicio y muestra la alerta + el email en MailHog:
     ```powershell
     docker stop circleguard-notification    # esperar ~90s
     ```
     Muestra `http://localhost:9090/alerts` (ServiceDown firing) y el email *"[FIRING:1] ServiceDown"* en MailHog. Luego `docker start circleguard-notification`.
- **Tip:** este clip impacta visualmente; deja que el dashboard de Grafana se vea poblado.

---

## Clip 9 — Documentación, Costos y Cierre (10%) · ~2 min
- **Mostrar:**
  1. `docs/proyecto-final/09-documentacion/` (este directorio): arquitectura, runbook, costos.
  2. Tabla de **costos de infraestructura** (`costos-infraestructura.md`): estimación AWS por ambiente.
  3. README actualizado con el índice del proyecto final.
- **Narrar:** cierra con el resumen de los 9 requisitos cubiertos y el costo estimado de llevarlo a la nube.

---

## Checklist de grabación
- [ ] Clip 0 Intro
- [ ] Clip 1 Ágil
- [ ] Clip 2 Patrones
- [ ] Clip 3 Terraform
- [ ] Clip 4 CI/CD (Tanda A)
- [ ] Clip 5 Seguridad (Tanda A)
- [ ] Clip 6 Pruebas
- [ ] Clip 7 Change Management (Tanda A)
- [ ] Clip 8 Observabilidad (Tanda B)
- [ ] Clip 9 Docs/Costos

## Consejos de producción
- Resolución 1080p, fuente de terminal grande (zoom Ctrl++ en VS Code/navegador).
- Cierra notificaciones del SO. Usa una sola ventana a pantalla completa por clip.
- Narra la **intención** ("esto demuestra que el Quality Gate bloquea de verdad"), no solo lo que se ve.
- Si algo tarda en cargar, córtalo en edición; no dejes tiempos muertos.
- Sube el video a YouTube (no listado) o Drive y enlázalo en la última diapositiva.
