# Historias de Usuario — CircleGuard

**Proyecto:** CircleGuard — Sistema de Gestión de Salud Universitaria  
**Metodología:** Scrum  
**Herramienta de gestión:** GitHub Projects ([ver tablero](https://github.com/users/criskian/projects/1))

---

## Épicas

| ID | Épica | Descripción |
|----|-------|-------------|
| E1 | Autenticación y Acceso | Gestión de identidad, login y control de acceso |
| E2 | Registro de Salud | Formularios de encuesta y seguimiento del estado de salud |
| E3 | Control de Acceso Físico | Validación QR en puntos de ingreso al campus |
| E4 | Notificaciones | Alertas automáticas por cambios de estado |
| E5 | Observabilidad y Operaciones | Monitoreo, logs y dashboards del sistema |
| E6 | Infraestructura y CI/CD | Pipelines, despliegue y gestión de ambientes |

---

## Sprint 1 — Historias

### HU-01: Inicio de sesión con credenciales universitarias

**Como** estudiante universitario,  
**quiero** iniciar sesión en CircleGuard con mis credenciales LDAP institucionales,  
**para** acceder a mi perfil de salud de forma segura sin crear una cuenta nueva.

**Criterios de aceptación (Gherkin):**
```gherkin
Escenario: Login exitoso con credenciales LDAP válidas
  Dado que soy un estudiante registrado en el directorio LDAP
  Cuando ingreso mi usuario y contraseña correctos en /api/v1/auth/login
  Entonces recibo un JWT válido con expiración de 1 hora
  Y puedo usar ese token para acceder a endpoints protegidos

Escenario: Login fallido con credenciales incorrectas
  Dado que ingreso una contraseña incorrecta
  Cuando envío la petición de login
  Entonces recibo HTTP 401 con mensaje "Credenciales inválidas"
  Y no se genera ningún token

Escenario: Cuenta bloqueada después de 5 intentos fallidos
  Dado que he fallado el login 5 veces consecutivas
  Cuando intento iniciar sesión nuevamente
  Entonces recibo HTTP 423 con mensaje "Cuenta bloqueada temporalmente"
```

**Estimación:** 3 SP | **Prioridad:** Alta | **Épica:** E1

---

### HU-02: Registro de encuesta de salud diaria

**Como** estudiante,  
**quiero** completar un formulario de salud con mis síntomas del día,  
**para** que el sistema determine mi estado de salud y si puedo ingresar al campus.

**Criterios de aceptación:**
```gherkin
Escenario: Envío de encuesta sin síntomas
  Dado que soy un estudiante autenticado
  Cuando envío una encuesta indicando que no tengo síntomas
  Entonces mi estado de salud se actualiza a HEALTHY
  Y recibo un código QR verde válido para ese día

Escenario: Envío de encuesta con síntomas
  Dado que indico fiebre y tos en mi encuesta
  Cuando envío el formulario
  Entonces mi estado se actualiza a SUSPECT
  Y se emite un evento Kafka "survey.submitted"
  Y el servicio de promotion-service procesa el evento y genera QR rojo

Escenario: No se puede enviar más de una encuesta por día
  Dado que ya envié mi encuesta hoy
  Cuando intento enviar otra encuesta el mismo día
  Entonces recibo HTTP 409 "Ya existe una encuesta para hoy"
```

**Estimación:** 5 SP | **Prioridad:** Alta | **Épica:** E2

---

### HU-03: Validación de QR en punto de control

**Como** guardia de seguridad en la entrada del campus,  
**quiero** escanear el QR de un estudiante y ver su estado de salud inmediatamente,  
**para** permitir o denegar el ingreso según las políticas sanitarias.

**Criterios de aceptación:**
```gherkin
Escenario: Validación de QR con estado HEALTHY
  Dado que el estudiante tiene estado HEALTHY vigente
  Cuando el guardia escanea el QR en el gate-service
  Entonces el sistema muestra "ACCESO PERMITIDO" en verde
  Y registra el evento de entrada

Escenario: Validación de QR con estado SUSPECT o INFECTED
  Dado que el estudiante tiene estado SUSPECT
  Cuando el guardia escanea el QR
  Entonces el sistema muestra "ACCESO DENEGADO" en rojo
  Y activa la alerta de notificación al área de salud

Escenario: QR expirado (más de 24h)
  Dado que el QR fue generado hace más de 24 horas
  Cuando se intenta validar
  Entonces el sistema muestra "QR EXPIRADO - Requiere nueva encuesta"
```

**Estimación:** 5 SP | **Prioridad:** Alta | **Épica:** E3

---

### HU-04: Notificación automática de cambio de estado

**Como** estudiante,  
**quiero** recibir un email cuando mi estado de salud cambie,  
**para** estar informado de las acciones que debo tomar.

**Criterios de aceptación:**
```gherkin
Escenario: Notificación al pasar a estado SUSPECT
  Dado que mi estado cambia a SUSPECT
  Cuando el servicio de promotion-service publica el evento de cambio
  Entonces el notification-service envía un email en menos de 30 segundos
  Y el email contiene las instrucciones de aislamiento

Escenario: Notificación al recuperar estado HEALTHY
  Dado que mi estado cambia de SUSPECT a HEALTHY (certificado médico)
  Cuando se procesa la corrección administrativa
  Entonces recibo un email de confirmación de estado limpio
```

**Estimación:** 3 SP | **Prioridad:** Media | **Épica:** E4

---

## Sprint 2 — Historias

### HU-05: Dashboard de monitoreo operacional

**Como** administrador del sistema,  
**quiero** ver un dashboard con el estado de todos los microservicios en tiempo real,  
**para** detectar problemas de disponibilidad o rendimiento antes de que afecten a usuarios.

**Criterios de aceptación:**
```gherkin
Escenario: Dashboard muestra todos los servicios en verde
  Dado que todos los microservicios están corriendo
  Cuando accedo al dashboard de Grafana
  Entonces veo un panel por servicio con: latencia p99, tasa de errores, uso de CPU/RAM

Escenario: Alerta cuando un servicio cae
  Dado que el promotion-service falla
  Cuando Prometheus detecta que el health check falla por 2 minutos
  Entonces se dispara una alerta en Alertmanager
  Y se envía notificación al canal de operaciones

Escenario: Circuit breaker visible en dashboard
  Dado que el gateway-service tiene el circuit breaker abierto
  Cuando accedo al panel de Grafana
  Entonces veo el estado del circuito (OPEN/HALF-OPEN/CLOSED) en tiempo real
```

**Estimación:** 8 SP | **Prioridad:** Alta | **Épica:** E5

---

### HU-06: Despliegue automatizado con aprobación a producción

**Como** ingeniero de operaciones,  
**quiero** que el pipeline CI/CD despliegue automáticamente a stage pero requiera aprobación manual para producción,  
**para** mantener control sobre los cambios que llegan a usuarios reales.

**Criterios de aceptación:**
```gherkin
Escenario: Pipeline despliega automáticamente a stage
  Dado que un PR se mergea a develop
  Cuando el pipeline de Jenkins se ejecuta
  Entonces el código se despliega automáticamente a circleguard-stage
  Y se ejecutan E2E tests contra stage

Escenario: Aprobación requerida para producción
  Dado que los tests de stage pasaron
  Cuando el pipeline llega al stage "Promote Prod"
  Entonces el pipeline se pausa esperando aprobación
  Y se envía notificación al aprobador designado
  Y solo procede si alguien aprueba en menos de 30 minutos
```

**Estimación:** 5 SP | **Prioridad:** Alta | **Épica:** E6

---

### HU-07: Análisis estático de código en CI

**Como** desarrollador,  
**quiero** que el pipeline ejecute SonarQube en cada PR,  
**para** detectar code smells, vulnerabilidades y mantener la cobertura de pruebas por encima del 60%.

**Criterios de aceptación:**
```gherkin
Escenario: Quality Gate pasa con código limpio
  Dado que el código tiene cobertura > 60% y sin vulnerabilidades críticas
  Cuando el stage SonarQube Analysis se ejecuta
  Entonces el Quality Gate pasa y el pipeline continúa

Escenario: Quality Gate falla con cobertura insuficiente
  Dado que la cobertura de tests es 45%
  Cuando se ejecuta el análisis de SonarQube
  Entonces el stage falla y el pipeline se detiene
  Y se muestra el enlace al reporte de SonarQube en los logs
```

**Estimación:** 3 SP | **Prioridad:** Media | **Épica:** E6

---

### HU-08: Infraestructura reproducible con Terraform

**Como** ingeniero de infraestructura,  
**quiero** poder reproducir cualquier ambiente (dev/stage/prod) ejecutando `terraform apply`,  
**para** eliminar la configuración manual y garantizar ambientes consistentes.

**Criterios de aceptación:**
```gherkin
Escenario: Crear ambiente dev desde cero con Terraform
  Dado que tengo Terraform instalado y el backend remoto configurado
  Cuando ejecuto: terraform -chdir=terraform/environments/dev apply
  Entonces se crean los namespaces, deployments y servicios de dev en Kubernetes
  Y todos los pods están en estado Running en menos de 5 minutos

Escenario: Diferencia entre ambientes reflejada en variables
  Dado que dev tiene replicas=1 y stage tiene replicas=2
  Cuando ejecuto terraform plan en cada ambiente
  Entonces el plan refleja las diferencias de configuración correctamente
```

**Estimación:** 8 SP | **Prioridad:** Alta | **Épica:** E6

---

## Resumen del Backlog

| Historia | Épica | Sprint | SP | Estado |
|----------|-------|--------|----|--------|
| HU-01: Login LDAP | E1 | 1 | 3 | ✅ Done |
| HU-02: Encuesta diaria | E2 | 1 | 5 | ✅ Done |
| HU-03: Validación QR | E3 | 1 | 5 | ✅ Done |
| HU-04: Notificaciones | E4 | 1 | 3 | ✅ Done |
| HU-05: Dashboard monitoreo | E5 | 2 | 8 | 🔄 In Progress |
| HU-06: Aprobación a producción | E6 | 2 | 5 | 🔄 In Progress |
| HU-07: SonarQube en CI | E6 | 2 | 3 | 📋 Backlog |
| HU-08: Terraform IaC | E6 | 2 | 8 | 📋 Backlog |
| **Total** | | | **40 SP** | |
