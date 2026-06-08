# Patrones Nuevos Implementados — CircleGuard

## 1. Circuit Breaker (Resilience4j)

**Servicio:** `circleguard-gateway-service`  
**Archivo:** `src/main/java/com/circleguard/gateway/service/QrValidationService.java`  
**Dependencia:** `io.github.resilience4j:resilience4j-spring-boot3:2.2.0`

### Propósito

Cuando Redis está indisponible, el gateway-service no debe fallar con excepción ni quedar bloqueado esperando timeouts. El Circuit Breaker detecta fallos consecutivos en la consulta a Redis y "abre el circuito" para devolver inmediatamente una respuesta de fallback, protegiendo tanto al servicio como al usuario.

### Estados del Circuito

```
CLOSED ──(≥50% fallos en 10 llamadas)──► OPEN
  ▲                                         │
  │                                     (10 seg)
  │                                         ▼
  └──(≥3 llamadas exitosas)────── HALF-OPEN
```

### Configuración (`application.yml`)

```yaml
resilience4j:
  circuitbreaker:
    instances:
      redis-status:
        sliding-window-size: 10          # evalúa las últimas 10 llamadas
        failure-rate-threshold: 50       # abre si ≥50% fallan
        wait-duration-in-open-state: 10s # espera 10s antes de HALF-OPEN
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
        register-health-indicator: true  # expone estado en /actuator/health
```

### Implementación

```java
@CircuitBreaker(name = "redis-status", fallbackMethod = "fetchStatusFallback")
public String fetchStatusFromRedis(String anonymousId) {
    return redisTemplate.opsForValue().get(STATUS_KEY_PREFIX + anonymousId);
}

public String fetchStatusFallback(String anonymousId, Throwable t) {
    if (featureFlags.isStrictQrValidation()) {
        return "POTENTIAL"; // denegar en modo estricto
    }
    return null; // permitir en modo no estricto
}
```

### Observabilidad

El estado del circuito es visible en:
- `GET /actuator/health` → incluye `circuitBreakers.redis-status`
- `GET /actuator/circuitbreakers` → estado detallado (CLOSED/OPEN/HALF_OPEN + métricas)

### Beneficios

- **Fail-fast:** evita acumulación de threads bloqueados esperando Redis
- **Auto-recuperación:** transición automática a HALF-OPEN cuando Redis vuelve
- **Configurable por ambiente:** umbral de fallos ajustable en ConfigMap de K8s
- **Comportamiento seguro:** el fallback con Feature Toggle permite diferentes políticas por ambiente (negar en prod, permitir en dev)

---

## 2. Feature Toggle (External Configuration)

**Servicio:** `circleguard-gateway-service`  
**Archivo:** `src/main/java/com/circleguard/gateway/config/FeatureFlags.java`

### Propósito

Permite activar o desactivar funcionalidades en tiempo de ejecución sin redesplegar código. Las flags se leen del `application.yml` y pueden sobreescribirse por ambiente mediante Kubernetes ConfigMaps.

### Implementación

```java
@Component
@ConfigurationProperties(prefix = "circleguard.features")
public class FeatureFlags {
    private boolean strictQrValidation = true;
    private boolean emailNotificationsEnabled = true;
    private boolean qrExpirationEnabled = true;
    // getters y setters...
}
```

### Uso en código

```java
if (featureFlags.isStrictQrValidation()) {
    return "POTENTIAL"; // denegar acceso cuando Redis no disponible
}
return null; // permitir acceso
```

### Configuración por ambiente

**Dev** (`application.yml`):
```yaml
circleguard:
  features:
    strict-qr-validation: false       # más permisivo en desarrollo
    email-notifications-enabled: false # no enviar emails reales en dev
    qr-expiration-enabled: false       # QRs no expiran en demos
```

**Stage/Prod** (Kubernetes ConfigMap):
```yaml
# k8s/base/02-configmaps.yaml
CIRCLEGUARD_FEATURES_STRICT_QR_VALIDATION: "true"
CIRCLEGUARD_FEATURES_EMAIL_NOTIFICATIONS_ENABLED: "true"
CIRCLEGUARD_FEATURES_QR_EXPIRATION_ENABLED: "true"
```

### Beneficios

- **Sin downtime:** cambiar una feature no requiere redespliegue — solo actualizar el ConfigMap y reiniciar el pod
- **Rollback instantáneo:** si una feature causa problemas, se desactiva en segundos
- **Diferenciación de ambientes:** dev puede correr con flags permisivas sin afectar producción
- **A/B testing:** base para implementar feature flags más avanzados (por usuario, por porcentaje, etc.)

---

## Resumen de Patrones

| Patrón | Categoría | Servicio | Archivo clave |
|--------|-----------|----------|---------------|
| API Gateway | Structural | gateway-service | `GateController.java` |
| Event-Driven | Behavioral | form / promotion / notification | Kafka topics |
| Repository | Data Access | todos | `*Repository.java` |
| DTO | Structural | todos | `*Request/Response.java` |
| Externalized Config | Configuration | todos | `02-configmaps.yaml` |
| Health Check Probes | Operational | todos | `20-services.yaml` |
| **Circuit Breaker** ✨ | **Resilience** | **gateway-service** | **`QrValidationService.java`** |
| **Feature Toggle** ✨ | **Configuration** | **gateway-service** | **`FeatureFlags.java`** |
