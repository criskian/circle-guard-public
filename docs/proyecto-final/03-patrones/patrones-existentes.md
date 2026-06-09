# Patrones de Diseño Existentes — CircleGuard

## 1. API Gateway

**Servicio:** `circleguard-gateway-service` (puerto 8087)

**Propósito:** Punto de entrada único para validación de QR en el control de acceso físico. Centraliza la autenticación de tokens, la consulta al caché de estado de salud y la decisión de acceso.

**Implementación:**
- `GateController` expone `POST /api/v1/gate/validate`
- `QrValidationService` valida el JWT del QR y consulta Redis para el estado de salud
- Los clientes (guardias de seguridad) solo interactúan con este servicio

**Beneficio:** Desacopla la lógica de acceso físico del resto de microservicios. Si la lógica de validación cambia, solo se modifica el gateway.

---

## 2. Event-Driven / Publish-Subscribe (Apache Kafka)

**Servicios:** `form-service` (producer) → `promotion-service` y `notification-service` (consumers)

**Propósito:** Desacopla el registro de encuestas de salud del procesamiento posterior (cambio de estado, envío de notificaciones).

**Implementación:**
- `form-service` publica en el topic `survey.submitted` cuando un usuario completa una encuesta
- `promotion-service` consume el evento y actualiza el grafo de estado en Neo4j
- `notification-service` consume el mismo evento y envía emails vía MailHog

**Beneficio:** Los servicios son independientes — `form-service` no necesita conocer a `promotion-service` ni a `notification-service`. Se pueden añadir nuevos consumidores sin modificar el productor.

---

## 3. Repository Pattern (Spring Data)

**Servicios:** `auth-service`, `identity-service`, `form-service`, `dashboard-service`, `promotion-service`

**Propósito:** Abstrae el acceso a datos detrás de interfaces Java, independizando la lógica de negocio del motor de base de datos.

**Implementación:**
- `JpaRepository<Entity, ID>` para PostgreSQL (auth, identity, form, dashboard)
- `Neo4jRepository<Entity, ID>` para grafos de contacto (promotion)
- Tests usan H2 o Testcontainers sin cambiar el código de producción

**Beneficio:** Se puede cambiar de PostgreSQL a otro motor sin tocar la lógica de negocio.

---

## 4. DTO (Data Transfer Object)

**Servicios:** Todos

**Propósito:** Separa el modelo de dominio interno de los contratos de la API REST.

**Implementación:**
- Records Java (`record LoginRequest(String username, String password)`) como DTOs inmutables
- Mapeo explícito entre entidades JPA y DTOs en el servicio
- Evita exponer IDs internos o campos sensibles (ej: hash de contraseña)

---

## 5. Externalized Configuration

**Implementación:** Kubernetes ConfigMaps (`k8s/base/02-configmaps.yaml`) + Spring Boot relaxed binding

**Propósito:** La configuración (URLs de servicios, puertos, secretos) está fuera del código y varía por ambiente.

**Ambientes:**
- Dev: `docker-compose.dev.yml` con variables de entorno
- Stage/Prod: ConfigMap `circleguard-config` montado en todos los pods

---

## 6. Health Check / Readiness-Liveness Probes

**Implementación:** Spring Boot Actuator (`/actuator/health/liveness`, `/actuator/health/readiness`) + K8s probes en `k8s/base/20-services.yaml`

**Propósito:** Kubernetes solo envía tráfico a pods listos (`readiness`) y reinicia pods degradados (`liveness`).

**Beneficio:** Alta disponibilidad automática — pods que fallen en startup o en ejecución son reemplazados sin intervención manual.
