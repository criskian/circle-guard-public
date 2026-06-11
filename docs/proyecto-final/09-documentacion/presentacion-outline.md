# Outline de Presentación — Proyecto Final IngeSoft V (CircleGuard)

> **Duración objetivo:** 15–20 min de exposición + demo (video o en vivo).
> **Formato:** ~18–22 diapositivas. Cada requisito = 1–2 slides + clip de video embebido/enlazado.
> **Regla de oro:** una idea por slide, evidencia visual (captura o clip), y una frase de "por qué importa".

---

## Estructura de diapositivas

### 1. Portada
- Título: **CircleGuard — Plataforma de Microservicios para Gestión de Salud en Campus**
- Subtítulo: Proyecto Final IngeSoft V · Autor · Fecha
- Logo / imagen del sistema.

### 2. Agenda
- Los 9 requisitos del taller + demo.

### 3. Visión general del sistema
- Diagrama de arquitectura (6 microservicios + gateway + infra: Postgres, Neo4j, Kafka, Redis, LDAP).
- Stack: Spring Boot 3.2 / Java 21, Gradle, Docker, Kubernetes (kind), Jenkins, Terraform.

### 4. Requisito 1 — Metodología Ágil y Branching (10%)
- Captura del board de GitHub Projects (Sprint 1 Done, Sprint 2 en curso).
- Diagrama GitFlow (feature → develop → main).
- "Por qué importa": trazabilidad de historias a commits/PRs.

### 5. Requisito 2 — Patrones de Diseño (10%)
- Circuit Breaker (Resilience4j) en gateway + Feature Toggle.
- Snippet de código + diagrama de estados del circuit breaker.
- "Por qué importa": resiliencia ante caída de Redis sin tumbar el sistema.

### 6. Requisito 3 — IaC con Terraform (20%)
- Estructura modular + multi-ambiente (dev/stage/prod).
- Captura de `terraform validate` limpio + `kubectl get pods` del cluster.
- "Por qué importa": infraestructura reproducible y versionada.

### 7–8. Requisito 4 — CI/CD Avanzado (15%)  ⭐
- **Slide 7:** diagrama del pipeline (todos los stages) + captura del Stage View en SUCCESS.
- **Slide 8:** SonarQube Quality Gate (Passed) + Trivy + aprobación manual de producción + email de fallo (MailHog).
- "Por qué importa": los gates **bloquean de verdad** (calidad y seguridad antes de prod).

### 9. Requisito 8 — Seguridad (5%)
- Sealed Secrets (cifrado en git) + cert-manager TLS + Ingress TLS + RBAC least-privilege + Trivy.
- Captura de `kubectl get sealedsecrets,certificate,ingress,role`.
- "Por qué importa": secretos cifrados, TLS extremo a extremo, mínimo privilegio.

### 10. Requisito 5 — Pruebas Completas (15%)
- Cobertura JaCoCo (tabla por servicio) + Testcontainers (Neo4j) + Locust + OWASP ZAP.
- Captura del informe consolidado.
- "Por qué importa": calidad medida y verificada, no asumida.

### 11. Requisito 6 — Change Management y Release Notes (5%)
- Proceso de cambios + rollback + CHANGELOG automático en master.

### 12–13. Requisito 7 — Observabilidad (10%)
- **Slide 12:** Grafana dashboard "CircleGuard — Overview" (disponibilidad, p99, throughput, JVM, negocio).
- **Slide 13:** Jaeger (traza distribuida) + alerta ServiceDown → email.
- "Por qué importa": visibilidad total (métricas, trazas, logs, alertas).

### 14. Requisito 9 — Documentación y Costos (10%)
- Índice de documentación + tabla de costos AWS (estimación 2 días: ~$6–40 según arquitectura).
- "Por qué importa": el proyecto está documentado y su costo es predecible.

### 15. Demo en vivo / video
- Enlace o clip embebido (ver `video-script.md`).

### 16. Retos y aprendizajes
- Race condition de SonarQube (resuelta con lock), Testcontainers en CI (host.docker.internal), restricción de RAM 8GB.

### 17. Conclusiones
- Los 9 requisitos cubiertos y verificados en vivo.
- Próximos pasos: ELK completo, despliegue EKS.

### 18. Cierre / Q&A
- Repo + contacto.

---

## Mapa requisito → evidencia → clip de video
| Requisito | Slide | Clip (video-script) |
|---|---|---|
| 1 Ágil | 4 | Clip 1 |
| 2 Patrones | 5 | Clip 2 |
| 3 Terraform | 6 | Clip 3 |
| 4 CI/CD | 7–8 | Clip 4 |
| 8 Seguridad | 9 | Clip 5 |
| 5 Pruebas | 10 | Clip 6 |
| 6 Change Mgmt | 11 | Clip 7 |
| 7 Observabilidad | 12–13 | Clip 8 |
| 9 Docs/Costos | 14 | Clip 9 |

## Consejos de presentación
- Abre fuerte: en 30s di qué es CircleGuard y qué vas a demostrar.
- No leas las slides; cuenta la historia y deja que la captura/clip sea la prueba.
- Cada requisito cierra con "por qué importa" — eso es lo que evalúan.
- Ten el video como respaldo por si la demo en vivo falla.
- Tiempo: ~1.5–2 min por requisito; ensaya con cronómetro.
