# Terraform IaC — CircleGuard

Infraestructura como Código para el sistema CircleGuard. Gestiona namespaces, secretos, bases de datos, mensajería, microservicios y monitoreo en Kubernetes.

## Estructura

```
terraform/
├── modules/
│   ├── namespaces/      # Namespace + ServiceAccount + RBAC
│   ├── databases/       # Secrets + PostgreSQL + Redis + Neo4j
│   ├── messaging/       # Zookeeper + Kafka
│   ├── app-services/    # ConfigMap + 6 microservicios (for_each)
│   └── monitoring/      # kube-prometheus-stack (Helm)
└── environments/
    ├── dev/             # Backend local, réplicas=1, dev-latest
    ├── stage/           # Backend TF Cloud, réplicas=1, stage-latest
    └── prod/            # Backend TF Cloud, réplicas=2, prod-latest
```

## Prerequisitos

- [Terraform >= 1.6](https://developer.hashicorp.com/terraform/install)
- [kubectl](https://kubernetes.io/docs/tasks/tools/) configurado
- [kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation) para entornos locales
- [helm](https://helm.sh/docs/intro/install/) (usado internamente por el provider)

## Uso rápido — Dev

```bash
# 1. Crear cluster local
kind create cluster --name circleguard-dev
kubectl config use-context kind-circleguard-dev

# 2. Inicializar
cd terraform/environments/dev
terraform init

# 3. Revisar cambios
terraform plan -var-file="terraform.tfvars"

# 4. Aplicar
terraform apply -var-file="terraform.tfvars"

# 5. Destruir (cleanup)
terraform destroy -var-file="terraform.tfvars"
```

## Uso — Stage / Prod (Terraform Cloud)

```bash
# 1. Autenticarse en Terraform Cloud
terraform login

# 2. Inicializar (descarga módulos + configura backend remoto)
cd terraform/environments/stage   # o prod
terraform init

# 3. Configurar variables sensitivas en TF Cloud UI
#    (db_password, jwt_secret, neo4j_password, vault_hash_salt)
#    Marcarlas como "Sensitive" para que no aparezcan en logs

# 4. Plan y apply
terraform plan
terraform apply
```

## Variables por ambiente

| Variable | Dev | Stage | Prod |
|----------|-----|-------|------|
| `kube_context` | `kind-circleguard-dev` | `kind-circleguard-stage` | `kind-circleguard-stage` |
| `replicas` | 1 | 1 | **2** |
| `image_tag` | `dev-latest` | `stage-latest` | `prod-latest` |
| `enable_monitoring` | false | true | true |
| `enable_alertmanager` | false | false | **true** |

## Módulos

### `namespaces/`

Crea el namespace Kubernetes con etiquetas `app.kubernetes.io/managed-by: terraform` y configura RBAC con mínimo privilegio (solo lectura de pods, services, configmaps).

### `databases/`

Crea los dos Secrets (`circleguard-db-secret`, `circleguard-app-secret`) y despliega PostgreSQL, Redis y Neo4j con sus Services correspondientes.

### `messaging/`

Despliega Zookeeper y Kafka. Kafka tiene `enableServiceLinks: false` para evitar el conflicto con la variable de entorno `KAFKA_PORT` que inyecta Kubernetes automáticamente.

### `app-services/`

Usa `for_each` sobre un mapa de configuraciones para crear los 6 Deployments y Services de forma DRY. Crea el ConfigMap `circleguard-config` con los Feature Toggles ajustados por ambiente.

### `monitoring/`

Despliega el chart `kube-prometheus-stack` via Helm con Prometheus (NodePort 30090) y Grafana (NodePort 30300). En prod activa Alertmanager y extiende la retención a 30 días.

## Gestión del estado

| Ambiente | Backend | State locking |
|----------|---------|---------------|
| dev | Local (`terraform.tfstate`) | No (solo un usuario) |
| stage | Terraform Cloud `circleguard/circleguard-stage` | Sí (automático) |
| prod | Terraform Cloud `circleguard/circleguard-prod` | Sí (automático) |

Para migrar dev a Terraform Cloud: actualizar `backend.tf` y ejecutar `terraform init -migrate-state`.

## Diagrama completo

Ver [docs/proyecto-final/02-terraform/arquitectura-infra.md](../docs/proyecto-final/02-terraform/arquitectura-infra.md).
