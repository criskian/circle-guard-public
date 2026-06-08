# Planes de Rollback — CircleGuard

## Principio General

Todo despliegue a producción debe tener un plan de rollback ejecutable en menos de 5 minutos. Antes de cualquier deploy, el operador responsable verifica que el plan aplica y tiene los comandos listos.

---

## Rollback de Kubernetes (Deployments)

### Rollback inmediato de un servicio

```bash
# Ver historial de revisiones
kubectl rollout history deployment/<servicio> -n circleguard-prod

# Rollback a la revisión anterior
kubectl rollout undo deployment/<servicio> -n circleguard-prod

# Rollback a una revisión específica
kubectl rollout undo deployment/<servicio> -n circleguard-prod --to-revision=2

# Verificar estado tras el rollback
kubectl rollout status deployment/<servicio> -n circleguard-prod
kubectl get pods -n circleguard-prod -l app=<servicio>
```

### Rollback de todos los servicios

```bash
SERVICES="auth-service identity-service form-service notification-service promotion-service dashboard-service"
for svc in $SERVICES; do
  kubectl rollout undo deployment/$svc -n circleguard-prod
done
kubectl get pods -n circleguard-prod
```

---

## Rollback de Release Git (Tags)

Si el problema es de código y no de configuración, se hace rollback al tag anterior:

```bash
# Identificar el último tag estable
git tag --sort=-version:refname | head -5

# Crear rama hotfix desde el tag estable
git checkout -b hotfix/rollback-v1.0.5 v1.0.5

# Re-deployar imagen del tag anterior en K8s (sin necesidad de rebuild)
kubectl set image deployment/auth-service auth-service=circleguard-auth-service:auth-<build-anterior> -n circleguard-prod
```

---

## Rollback de Base de Datos

### PostgreSQL (auth, identity, form, dashboard)

CircleGuard usa Flyway / scripts SQL versionados. Para rollback de schema:

```bash
# 1. Identificar la migración problemática
kubectl exec -n circleguard-prod deployment/postgres -- psql -U circleguard -d circleguard_auth \
  -c "SELECT version, installed_on, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# 2. Ejecutar script de rollback (si existe V{n}__rollback.sql)
kubectl exec -n circleguard-prod deployment/postgres -- psql -U circleguard -d circleguard_auth \
  -f /scripts/rollback/V{n}__rollback.sql

# NOTA: Los rollbacks de schema deben estar pre-escritos junto con cada migración forward.
```

### Neo4j (promotion-service)

```bash
# Restaurar desde backup previo al deploy
kubectl exec -n circleguard-prod deployment/neo4j -- neo4j-admin database restore \
  --from-path=/backups/neo4j-pre-deploy-$(date +%Y%m%d).dump \
  --database=neo4j --overwrite-destination=true
```

### Redis (promotion-service cache)

Redis es solo caché — el rollback es simplemente limpiar la caché:

```bash
kubectl exec -n circleguard-prod deployment/redis -- redis-cli FLUSHALL
```

---

## Rollback de Configuración (ConfigMaps / Secrets)

```bash
# Ver historial de configurmap
kubectl rollout history configmap/circleguard-config -n circleguard-prod
# (ConfigMaps no tienen rollout history nativo — usar git para recuperar versión anterior)

# Recuperar configmap de la versión anterior desde git
git show HEAD~1:k8s/base/02-configmaps.yaml | kubectl apply -f - -n circleguard-prod

# Reiniciar deployments para que tomen la nueva config
kubectl rollout restart deployment -n circleguard-prod
```

---

## Rollback de Infraestructura (Terraform)

```bash
# Ver el state anterior
terraform -chdir=terraform/environments/prod state list

# Destruir recursos nuevos y restaurar estado anterior
terraform -chdir=terraform/environments/prod plan -destroy -target=<recurso>
terraform -chdir=terraform/environments/prod apply

# O restaurar desde backup del state file en el backend remoto
# (el backend S3/TF Cloud mantiene versiones del state)
```

---

## Checklist Post-Rollback

Después de ejecutar cualquier rollback:

- [ ] Todos los pods están en estado `1/1 Running`
- [ ] Health checks responden en `/actuator/health`
- [ ] E2E tests manuales de los flujos críticos pasan
- [ ] Logs no muestran errores nuevos (`kubectl logs`)
- [ ] Alertas de Prometheus están en verde
- [ ] Notificar al equipo del rollback y registrar incidente en GitHub Issues con etiqueta `incident`

---

## Tiempo Objetivo de Recuperación (RTO)

| Componente | Tiempo máximo de rollback |
|------------|--------------------------|
| Deployment K8s | < 2 minutos |
| Configuración (ConfigMap) | < 3 minutos |
| Base de datos | < 15 minutos (con backup pre-deploy) |
| Infraestructura Terraform | < 10 minutos |
