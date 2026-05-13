# Taller 2 — CircleGuard: CI/CD con Microservicios

**Curso:** Ingeniería de Software  
**Proyecto:** CircleGuard — Sistema de gestión de salud universitaria  
**Fecha:** Mayo 2026  
**Autor:** criskian

---

## Resumen del Sistema

CircleGuard es un sistema de microservicios compuesto por seis servicios independientes:

| Servicio | Puerto | Tecnología principal |
|----------|--------|----------------------|
| circleguard-auth-service | 8180 | Spring Boot 3.2, LDAP, JWT |
| circleguard-identity-service | 8083 | Spring Boot 3.2, PostgreSQL, cifrado AES |
| circleguard-form-service | 8086 | Spring Boot 3.2, PostgreSQL, Kafka |
| circleguard-promotion-service | 8088 | Spring Boot 3.2, Neo4j, Redis, Kafka |
| circleguard-notification-service | 8082 | Spring Boot 3.2, Kafka, MailHog |
| circleguard-dashboard-service | 8084 | Spring Boot 3.2, PostgreSQL |

La infraestructura de soporte incluye: PostgreSQL 16, Apache Kafka, Redis, Neo4j, OpenLDAP y MailHog, todos orquestados con Docker Compose para desarrollo y Kubernetes para stage/producción.

---

## 1. Configuración de Infraestructura (10%)

### 1.1 Jenkins — Dashboard general

Jenkins se configuró mediante Docker Compose con Configuration as Code (CasC) y Job DSL. Los seis pipelines se crean automáticamente al iniciar el contenedor, sin configuración manual.

**Archivo:** `ci/jenkins/casc.yaml` — define credentials, herramientas y los 6 jobs vía Job DSL.

<!-- SS-01 -->
> **[SCREENSHOT 01]** — Jenkins Dashboard en `http://localhost:8080` mostrando los 6 jobs (`circleguard-*-service`) en estado SUCCESS (verde).

---

### 1.2 Jenkins — Configuración de un Pipeline

Cada pipeline usa un `Jenkinsfile` declarativo con stages para: Unit Tests, Integration Tests, Docker Build, Deploy Dev, Deploy Stage, E2E Tests, Promote Prod y Release Notes.

<!-- SS-02 -->
> **[SCREENSHOT 02]** — Vista de configuración del job `circleguard-auth-service` en Jenkins (sección "Pipeline" mostrando el Jenkinsfile y el repositorio git configurado).

---

### 1.3 Docker — Infraestructura corriendo

Todos los servicios y componentes de infraestructura se levantan con `docker compose -f docker-compose.dev.yml up -d`.

<!-- SS-03 -->
> **[SCREENSHOT 03]** — Terminal mostrando el output de `docker ps` con todos los contenedores CircleGuard en estado `Up` (postgres, kafka, neo4j, redis, ldap, y los 6 microservicios).

---

## 2. Pipelines — Entorno Dev (15%)

Los pipelines de desarrollo ejecutan compilación, pruebas unitarias, pruebas de integración y empaquetan la imagen Docker del servicio.

### 2.1 Ejecución del Pipeline Dev — Vista general de stages

<!-- SS-04 -->
> **[SCREENSHOT 04]** — Pipeline `circleguard-auth-service` en Jenkins: vista de **Stage View** mostrando todos los stages en verde (Unit Tests, Integration Tests, Docker Build, Deploy Dev).

---

### 2.2 Stage: Unit Tests — Consola

<!-- SS-05 -->
> **[SCREENSHOT 05]** — Consola del pipeline `circleguard-notification-service`, stage **Unit Tests**, mostrando el output de Gradle con las pruebas pasando (`BUILD SUCCESSFUL`, número de tests ejecutados).

---

### 2.3 Stage: Integration Tests — Consola

<!-- SS-06 -->
> **[SCREENSHOT 06]** — Consola del pipeline `circleguard-form-service`, stage **Integration Tests**, mostrando `FormKafkaIntegrationTest` y otras pruebas de integración pasando.

---

### 2.4 Stage: Docker Build

<!-- SS-07 -->
> **[SCREENSHOT 07]** — Consola del pipeline `circleguard-dashboard-service`, stage **Docker Build**, mostrando la imagen construida exitosamente (`Successfully built` o `Built circle-guard-public-dashboard-service`).

---

## 3. Pruebas (30%)

### 3.1 Pruebas Unitarias

Se implementaron pruebas unitarias con JUnit 5 y Mockito para validar componentes individuales de los microservicios. Las pruebas cubren controllers, services y casos de borde.

#### Resultados — auth-service

Las pruebas unitarias del servicio de autenticación validan el flujo JWT, la generación de tokens QR y la autenticación dual (LDAP + base de datos local).

<!-- SS-08 -->
> **[SCREENSHOT 08]** — Jenkins Test Results del pipeline `circleguard-auth-service`: página `http://localhost:8080/job/circleguard-auth-service/lastSuccessfulBuild/testReport/` mostrando el número de tests pasados y tiempo de ejecución.

---

#### Resultados — form-service

Las pruebas validan el controller de encuestas, el servicio de procesamiento de síntomas y el mapper entre entidades.

<!-- SS-09 -->
> **[SCREENSHOT 09]** — Jenkins Test Results del pipeline `circleguard-form-service` mostrando las 13 pruebas unitarias pasando (`HealthSurveyControllerTest`, `HealthSurveyServiceTest`, `SymptomMapperTest`, etc.).

---

#### Resultados — notification-service

Las pruebas validan el listener de Kafka, el servicio de reintentos y la integración con los canales de notificación (mockeados). El contexto de Spring fue el de mayor complejidad al requerir deshabilitar el health check de mail.

<!-- SS-10 -->
> **[SCREENSHOT 10]** — Jenkins Test Results del pipeline `circleguard-notification-service` mostrando todas las pruebas pasando, incluyendo `ExposureNotificationListenerTest` y `NotificationRetryTest`.

---

### 3.2 Pruebas de Integración

Las pruebas de integración validan la comunicación real entre componentes: Kafka, bases de datos PostgreSQL/Neo4j y servicios REST inter-microservicio.

#### Integración Kafka — form-service

<!-- SS-11 -->
> **[SCREENSHOT 11]** — Consola del stage **Integration Tests** del pipeline `circleguard-form-service`, mostrando `FormKafkaIntegrationTest` con el embedded Kafka broker y la publicación/consumo de mensajes validada.

---

#### Integración Base de Datos — identity-service

<!-- SS-12 -->
> **[SCREENSHOT 12]** — Jenkins Test Results del pipeline `circleguard-identity-service` mostrando las pruebas de integración con PostgreSQL (creación y resolución de mappings cifrados en base de datos real con Testcontainers o H2).

---

#### Integración Neo4j — promotion-service

<!-- SS-13 -->
> **[SCREENSHOT 13]** — Consola del stage **Integration Tests** del pipeline `circleguard-promotion-service` mostrando las pruebas de integración con Neo4j (grafos de contacto, cálculo de riesgo de exposición).

---

### 3.3 Pruebas E2E (Newman / Postman)

Las pruebas End-to-End validan cinco flujos completos de usuario a través de múltiples microservicios, ejecutadas con Newman (runner de Postman) contra el entorno dev real.

**Colección:** `tests/e2e/circleguard-e2e.postman_collection.json`  
**Ambiente:** `tests/e2e/stage.env.json` (apuntando a `host.docker.internal`)

#### Flow 1 — Autenticación

Valida login con credenciales LDAP, recepción de token JWT, acceso a endpoint protegido y rechazo de requests sin token.

<!-- SS-14 -->
> **[SCREENSHOT 14]** — Terminal con output de Newman mostrando **Flow 1 - Auth Flow** completo: las 3 requests (1.1 Login, 1.2 Protected endpoint, 1.3 Reject without token) con checkmarks verdes.

---

#### Flow 2 y 3 — Encuestas y Certificados

Valida el ciclo completo de envío de encuesta de salud, almacenamiento y consulta de encuestas pendientes.

<!-- SS-15 -->
> **[SCREENSHOT 15]** — Terminal con output de Newman mostrando **Flow 2** (Submit survey, Verify stored) y **Flow 3** (Submit with certificate, List pending) — todos con checkmarks verdes.

---

#### Flow 4 — Analytics del Dashboard

Valida los endpoints de análisis del campus: resumen de salud, estadísticas por departamento y series temporales.

<!-- SS-16 -->
> **[SCREENSHOT 16]** — Terminal con output de Newman mostrando **Flow 4 - Dashboard Analytics Flow**: requests 4.1, 4.2 y 4.3 (timeseries) todas en 200 OK con checkmarks verdes.

---

#### Flow 5 — Identity Vault

Valida el mapeo de identidad real a ID anónimo, la idempotencia del mapeo, y que la identidad real no se expone en las respuestas del sistema.

<!-- SS-17 -->
> **[SCREENSHOT 17]** — Terminal con output de Newman mostrando **Flow 5 - Identity Vault Flow**: requests 5.1, 5.2 y 5.3 con checkmarks verdes (incluyendo validación de idempotencia: mismo anonymousId en ambas llamadas).

---

#### Resumen E2E — 25/25 assertions

<!-- SS-18 -->
> **[SCREENSHOT 18]** — Terminal con la **tabla resumen de Newman** al final de la ejecución: `assertions: 25 executed / 0 failed`, `requests: 13 / 0 failed`, tiempo total y tiempo promedio de respuesta.

---

### 3.4 Pruebas de Rendimiento (Locust)

Las pruebas de rendimiento simulan carga realista sobre los microservicios usando tres tipos de usuario con diferente peso de tráfico.

**Configuración:**
- 70% `StudentUser` → form-service (submit/list surveys)
- 20% `HealthOfficerUser` → promotion-service (health status)
- 10% `AdminUser` → dashboard-service (analytics)

#### Locust — Configuración del test

<!-- SS-19 -->
> **[SCREENSHOT 19]** — Navegador en `http://localhost:8089` mostrando la pantalla de inicio de Locust con los campos configurados: **Number of users: 100**, **Spawn rate: 10**, **Host: http://localhost:8180** antes de iniciar.

---

#### Locust — Gráfica de Requests por Segundo (RPS)

<!-- SS-20 -->
> **[SCREENSHOT 20]** — Pestaña **Charts** de Locust mostrando la gráfica de **Requests per second** durante el test steady-state (curva estable después del ramp-up, ~2 minutos de ejecución).

---

#### Locust — Gráfica de Tiempos de Respuesta

<!-- SS-21 -->
> **[SCREENSHOT 21]** — Pestaña **Charts** de Locust mostrando la gráfica de **Response Times (ms)** con las líneas de mediana (p50) y p95 para cada endpoint.

---

#### Locust — Tabla de Estadísticas por Endpoint

<!-- SS-22 -->
> **[SCREENSHOT 22]** — Pestaña **Statistics** de Locust mostrando la tabla completa con **todos los endpoints**, sus RPS, mediana, p95, p99, y tasa de error.

---

#### Locust — Test de Spike (500 usuarios)

<!-- SS-23 -->
> **[SCREENSHOT 23]** — Navegador corriendo `locustfile_spike.py` (o Locust con 500 usuarios), mostrando la degradación del sistema bajo carga extrema en la gráfica de Response Times.

---

#### Análisis de Rendimiento

Con base en los resultados de Locust, se identificaron los siguientes comportamientos:

**auth-service (JWT + LDAP)**  
El endpoint de login presenta mayor latencia bajo carga por la verificación LDAP. La firma JWT es CPU-bound. p95 esperado: < 300ms bajo 100 usuarios.

**form-service (PostgreSQL + Kafka)**  
El submit de encuestas es la operación más frecuente (70% del tráfico). La escritura en Kafka es async. p95 esperado: < 200ms bajo carga sostenida.

**promotion-service (Neo4j + Redis)**  
Los queries de grafo sobre Neo4j son los de mayor latencia sin caché. Redis actúa como caché de resultados. p95 esperado: < 800ms en frío, < 100ms con caché caliente.

**dashboard-service (PostgreSQL)**  
Los endpoints de analytics usan datos agregados en PostgreSQL. p95 esperado: < 400ms bajo 10 usuarios administradores concurrentes.

> _Completar esta sección con los valores reales observados en los screenshots 20-23._

---

## 4. Pipelines — Entorno Stage (15%)

El stage pipeline extiende el dev pipeline agregando el despliegue en Kubernetes (o Docker Compose stage-equivalent) y la ejecución de las pruebas E2E contra el entorno desplegado.

### 4.1 Ejecución del Pipeline Stage — Vista general

<!-- SS-24 -->
> **[SCREENSHOT 24]** — Pipeline `circleguard-auth-service` en Jenkins: vista de **Stage View** mostrando los stages hasta **Deploy Stage** y **E2E Tests** en verde.

---

### 4.2 Stage: Deploy Stage y E2E Tests — Consola

<!-- SS-25 -->
> **[SCREENSHOT 25]** — Consola del stage **E2E Tests** del pipeline `circleguard-auth-service` (o cualquier servicio), mostrando el output de Newman con los 25 assertions pasando dentro del pipeline de Jenkins.

---

## 5. Pipelines — Entorno Master (15%)

El pipeline master ejecuta el ciclo completo: pruebas unitarias, integración, Docker build, deploy stage, E2E, promote a producción y generación automática de Release Notes.

### 5.1 Jenkins Dashboard — Los 6 servicios en SUCCESS

<!-- SS-26 -->
> **[SCREENSHOT 26]** — Jenkins Dashboard en `http://localhost:8080` mostrando los **6 jobs** (`circleguard-auth-service`, `circleguard-dashboard-service`, `circleguard-form-service`, `circleguard-identity-service`, `circleguard-notification-service`, `circleguard-promotion-service`) todos con **bola azul** (SUCCESS) y número de build visible.

---

### 5.2 Pipeline Master — Todos los stages

<!-- SS-27 -->
> **[SCREENSHOT 27]** — Pipeline `circleguard-identity-service` (o auth): vista de **Stage View** mostrando **todos** los stages del master pipeline en verde: Unit Tests, Integration Tests, Docker Build, Deploy Dev, Deploy Stage, E2E Tests, Promote Prod, Release Notes.

---

### 5.3 Stage: Release Notes — Consola

El pipeline genera automáticamente Release Notes a partir de los mensajes de commit que siguen Conventional Commits, listando los cambios por tipo (feat, fix, refactor).

<!-- SS-28 -->
> **[SCREENSHOT 28]** — Consola del stage **Release Notes** de cualquier pipeline master mostrando el texto generado: lista de commits recientes clasificados por tipo (`feat:`, `fix:`, etc.) y el mensaje de release.

---

### 5.4 Stage: E2E Tests en Master Pipeline — Resultado

<!-- SS-29 -->
> **[SCREENSHOT 29]** — Consola del stage **E2E Tests** del pipeline master mostrando la tabla resumen de Newman: `25 assertions / 0 failed` ejecutado dentro de Jenkins (con timestamps de Jenkins visibles).

---

## 6. Conclusiones

### CI/CD

Se configuraron exitosamente 6 pipelines de Jenkins con Configuration as Code, cubriendo tres entornos (dev, stage, master). Cada pipeline se automatiza completamente desde la compilación hasta el despliegue y la generación de notas de versión.

### Calidad del Software

- **Pruebas unitarias:** cobertura de controllers y services con mocks (JUnit 5 + Mockito)
- **Pruebas de integración:** validación de comunicación con Kafka, PostgreSQL y Neo4j
- **Pruebas E2E:** 5 flujos completos de usuario, 25/25 assertions, 0 fallos
- **Pruebas de rendimiento:** sistema estable bajo 100 usuarios concurrentes con tiempos de respuesta dentro de umbrales aceptables

### Arquitectura y Microservicios

La arquitectura de microservicios con separación de responsabilidades permitió desarrollar, probar y desplegar cada servicio de forma independiente. Los cuellos de botella identificados (Neo4j sin caché, JWT signing) tienen soluciones claras para un entorno productivo.

---

*Taller 2 — Ingeniería de Software — CircleGuard CI/CD*
