# Costos de Infraestructura — CircleGuard

> Estimaciones en `us-east-1`, precios on-demand (~2026). Son aproximados; el número
> exacto lo fija el [AWS Pricing Calculator](https://calculator.aws).

## Footprint del stack completo

"Absolutamente todo" = 6–8 microservicios + Postgres + Neo4j + Kafka/Zookeeper + Redis +
LDAP + Jenkins + SonarQube(+DB) + Prometheus/Grafana/Jaeger/Alertmanager + ELK + MailHog.
RAM real ≈ **20–24 GB** → en nube se necesita ~32 GB en una máquina o ~48 GB en cluster.

## Opción 1 — Un solo EC2 con docker-compose (más simple y barato)

| Recurso | Detalle | 48 h |
|---|---|---|
| EC2 `m5.2xlarge` (8 vCPU / 32 GB) | $0.384/h | $18.4 |
| EBS gp3 100 GB | prorrateado | $0.5 |
| IP elástica + transferencia | demo | ~$1 |
| **Total on-demand** | | **≈ $20** |
| **Con Spot (−70 %)** | | **≈ $7** |

## Opción 2 — EKS con Terraform (alineado a la IaC del proyecto) ⭐

| Recurso | Detalle | 48 h |
|---|---|---|
| EKS control plane | $0.10/h | $4.8 |
| 3× nodos `m5.xlarge` (48 GB) | $0.192/h c/u | $27.7 |
| ALB (ingress) | $0.0225/h + LCU | ~$2 |
| NAT Gateway | $0.045/h (o $0 con subnets públicas) | ~$3 |
| EBS para PVs (~80 GB) | | ~$0.5 |
| Transferencia | demo | ~$2 |
| **Total on-demand** | | **≈ $40** |
| **Con nodos Spot** | | **≈ $18** |

## Opción 3 — EKS + servicios gestionados (RDS, MSK, ElastiCache, OpenSearch)

Lo más "producción"; MSK y OpenSearch son los caros. **≈ $35–55 / 2 días** + más setup.

## Comparativa por ambiente (mensual estimado, on-demand)

| Ambiente | Réplicas | Cómputo aprox. | Costo/mes aprox. |
|---|---|---|---|
| dev | 1 c/u | 1 nodo m5.xlarge | ~$140 |
| stage | 1 c/u | 2 nodos m5.xlarge + EKS | ~$320 |
| prod | 2 c/u | 3 nodos m5.xlarge + EKS + ALB + NAT | ~$520 |

> Para un **demo de 2 días** el costo es marginal (ver opciones arriba). El costo mensual
> solo aplica si se deja corriendo permanentemente.

## Recomendación

- **Demo 2 días:** Opción 1 (un EC2 Spot) ≈ **$7**, o Opción 2 (EKS Spot) ≈ **$18** si se
  quiere mostrar Kubernetes.
- **Ventana corta (2–4 h) solo para capturas:** ≈ **$1–2** (EC2 Spot) — suficiente para
  evidencia, sin arriesgar créditos.

## Guardarraíles de costo

- Usar **Spot** para workers (tolera interrupciones en demo).
- Crear un **AWS Budget alert** antes de empezar.
- **`terraform destroy` al terminar** y verificar que no queden: EBS huérfanos, EIP sin
  asociar, snapshots, NAT Gateway, load balancers, imágenes en ECR.
- Evitar NAT Gateway/ALB/EKS si el presupuesto es de créditos limitados.
