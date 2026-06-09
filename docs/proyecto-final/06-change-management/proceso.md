# Proceso de Change Management — CircleGuard

## Objetivo

Garantizar que todos los cambios al sistema CircleGuard sean evaluados, aprobados y desplegados de forma controlada, minimizando el riesgo de interrupciones en el servicio.

---

## Tipos de Cambio

| Tipo | Descripción | Riesgo | Aprobación requerida | Tiempo de implementación |
|------|-------------|--------|----------------------|--------------------------|
| **Standard** | Cambios rutinarios y de bajo riesgo (actualizaciones de config, parches de dependencias, nuevas features en dev) | Bajo | CAB pre-aprobación automática vía pipeline | Inmediato tras pipeline verde |
| **Normal** | Cambios planificados con impacto moderado (nuevas funcionalidades a stage/prod, cambios de infraestructura) | Medio | Revisión de PR + aprobación manual en pipeline | 24–48h (ventana de despliegue) |
| **Emergency** | Hotfixes críticos que afectan disponibilidad o seguridad en producción | Alto | Aprobación verbal inmediata del Tech Lead + registro post-hecho | Inmediato con rollback plan listo |

---

## Flujo de Aprobación

```
Developer → PR (feature → develop)
         ↓
  [Pipeline CI verde]
  [SonarQube Quality Gate]
  [Trivy: sin CRITICAL]
         ↓
  Code Review (1 aprobador)
         ↓
  Merge a develop → Deploy automático a STAGE
         ↓
  [E2E tests en stage]
         ↓
  Stage "Promote Prod" en Jenkins
         ↓
  ⏸️  INPUT: Aprobación manual (30 min timeout)
         ↓
  Deploy a PROD → Tag vX.Y.Z → Release Notes
```

---

## RFC (Request for Change) Template

Para cambios **Normal** y **Emergency**, se debe completar un RFC como issue de GitHub con la etiqueta `change-request`:

```markdown
## RFC: [Título del cambio]

**Tipo:** Normal / Emergency
**Solicitante:** @usuario
**Fecha propuesta:** YYYY-MM-DD
**Servicio(s) afectado(s):** circleguard-*

### Descripción del cambio
Qué se cambia y por qué.

### Impacto esperado
Usuarios afectados, downtime esperado, dependencias.

### Plan de implementación
1. Paso 1
2. Paso 2

### Plan de rollback
Ver: [rollback.md](rollback.md)

### Checklist pre-despliegue
- [ ] Pipeline CI verde en develop
- [ ] E2E tests pasando en stage
- [ ] Backup de base de datos realizado
- [ ] Equipo de soporte notificado
- [ ] Ventana de mantenimiento comunicada a usuarios
```

---

## Roles del CAB (Change Advisory Board)

| Rol | Responsabilidad |
|-----|-----------------|
| **Tech Lead** | Aprobación final para cambios Normal y Emergency |
| **Developer** | Autor del cambio, responsable del RFC y del rollback |
| **QA** | Valida que E2E tests pasen antes de aprobar |
| **Ops** | Monitorea el despliegue y confirma estabilidad post-deploy |

---

## Ventana de Despliegue

- **Producción:** Martes y Jueves entre 22:00–23:00 (fuera de horario pico)
- **Stage:** Cualquier momento, previa notificación al equipo
- **Emergency:** Cualquier momento, con notificación inmediata post-deploy

---

## Sistema de Etiquetado de Releases

CircleGuard usa **Versionado Semántico** (`vMAJOR.MINOR.PATCH`):

| Evento | Acción |
|--------|--------|
| Commit `BREAKING CHANGE` mergeado a main | Bump MAJOR → `v2.0.0` |
| Commit `feat:` mergeado a main | Bump MINOR → `v1.1.0` |
| Commit `fix:` o `docs:` mergeado a main | Bump PATCH → `v1.0.1` |

El script `ci/scripts/generate-release-notes.ps1` ejecutado en el stage **Release Notes** del pipeline:
1. Detecta el último tag `vX.Y.Z`
2. Agrupa commits desde ese tag por tipo
3. Genera `RELEASE_NOTES_vX.Y.Z.md`
4. Actualiza `CHANGELOG.md`
5. Crea el tag git automáticamente

---

## Registro de Cambios (Change Log)

Ver [CHANGELOG.md](../../../CHANGELOG.md) en la raíz del repositorio para el historial completo de versiones.
