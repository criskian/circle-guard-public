# Informe Consolidado de Pruebas — CircleGuard

> Consolida cobertura (JaCoCo), calidad estática (SonarQube), pruebas funcionales,
> rendimiento (Locust) y seguridad dinámica (OWASP ZAP).
> **Fecha de verificación:** 2026-06-09 / 2026-06-11.

## 1. Cobertura de código (JaCoCo)

Cobertura de línea **real** por servicio (medida en los pipelines de Jenkins; el plugin
quedó corregido para apuntar a `build/classes/java/main`):

| Servicio | Cobertura de línea | Líneas cubiertas |
|---|---|---|
| dashboard-service | **73 %** | 56 / 77 |
| form-service | **71 %** | 105 / 147 |
| identity-service | **66 %** | 92 / 140 |
| notification-service | **59 %** | 112 / 189 |
| auth-service | **47 %** | 92 / 197 |
| promotion-service | **38 %** | 320 / 851 |

> El umbral JaCoCo configurado es 60 % (modo reporte, no bloqueante). promotion tiene
> mayor base de código (graph/Neo4j) y concentra la deuda de cobertura.

## 2. Calidad estática (SonarQube)

- **Proyecto:** `circleguard` (SonarQube 10.6 community).
- **Quality Gate:** ✅ **Passed** (`status = OK`).
- **Condiciones:** `new_violations = 0` (umbral 0), `new_duplicated_lines_density = 0 %` (umbral 3 %).
- Integrado en el pipeline con `waitForQualityGate abortPipeline: true` — **bloquea el build** si no pasa (verificado: con SonarQube caído el stage falla y aborta el pipeline).

## 3. Pruebas funcionales (unitarias + integración)

Todas en verde en los pipelines (vía Jenkins JUnit):

| Servicio | Tests | Fallos | Notas |
|---|---|---|---|
| auth-service | 9 | 0 | unit + integración (WireMock) |
| dashboard-service | 10 | 0 | |
| form-service | 13 | 0 | EmbeddedKafka integración |
| identity-service | 17 | 0 | EmbeddedKafka |
| notification-service | 13 | 0 | EmbeddedKafka |
| promotion-service | 25 | 0 | **7 de integración con Neo4j real (Testcontainers), skip=0** |

- **E2E (Newman/Postman):** colección de flujos extremo a extremo (ver pipeline master).
- Las pruebas de Testcontainers requieren Docker accesible al runtime de test; se habilitaron en CI con `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` + Ryuk desactivado.

## 4. Rendimiento (Locust)

Reportes en `tests/performance/reports/` (HTML + CSV).

**Prueba sostenida (steady):**
- Solicitudes: 1 046 · Fallos: 48 (~4.6 %)
- Mediana: 48 ms · p95: 79 ms · p99: 820 ms
- Throughput: ~8.75 req/s

**Prueba de pico (spike):**
- Solicitudes: 50 030 · Fallos: 445 (~0.9 %)
- Mediana: 52 ms · p95: 250 ms · p99: 16 s (degradación bajo pico extremo)
- Throughput: ~278 req/s · pico máx: 31 s (timeouts puntuales al saturar)

> Bajo carga sostenida la latencia es estable (p95 < 80 ms). En el pico extremo el
> sistema sigue respondiendo la mayoría de peticiones (99 % éxito) con degradación de cola.

## 5. Seguridad dinámica (OWASP ZAP)

Baseline scan (`zap-baseline.py`) contra `auth-service`. Reporte: `tests/security/zap-auth-baseline.html`.

- **FAIL: 0** · **WARN: 1** · **PASS: 66**
- Único warning: *Non-Storable Content / Cache-Control* (cabecera informativa, riesgo bajo).
- Sin vulnerabilidades de severidad alta/crítica en el baseline.

> Complementa el escaneo **estático de contenedores con Trivy** (HIGH/CRITICAL) que corre
> en cada pipeline y archiva el reporte JSON.

## 6. Resumen

| Dimensión | Resultado |
|---|---|
| Cobertura | 38–73 % por servicio (real, medida) |
| Quality Gate | ✅ Passed (0 nuevas violaciones) |
| Funcionales | 87 tests, 0 fallos, 0 skips |
| Rendimiento | p95 < 80 ms sostenido; 99 % éxito en pico |
| Seguridad dinámica | 0 FAIL ZAP; Trivy HIGH/CRITICAL en pipeline |

**Conclusión:** la calidad está **medida y verificada** en las cinco dimensiones, integrada en
el pipeline (cobertura + Sonar + Trivy gatillan/bloquean), y respaldada con reportes reproducibles.
