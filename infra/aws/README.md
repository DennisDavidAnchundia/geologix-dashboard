# GeoLogix en AWS (capa gratuita)

> Objetivo: demo pública con costo **$0**. Todo cabe en el free-tier de 12 meses
> (EC2 t2/t3.micro 750 h/mes + S3 5 GB). Poner **Billing Alert desde el día 1**.

## Opción A (recomendada): todo en una EC2

1. **Billing Alert:** Consola → Billing → Budgets → presupuesto $5 mensual con aviso al 50% y 100%.
2. **EC2:** Amazon Linux 2023, `t3.micro` (free-tier), disco gp3 20 GB, keypair propio.
3. **Security Group:** inbound `22` (tu IP), `80` (0.0.0.0/0). Nada más.
4. **User data:** pegar `infra/aws/user-data.sh` al lanzar (cambia `REPO_URL` si haces fork).
   Sin user-data: clonar el repo y correr el script a mano.
5. Abrir `http://<IP-publica>/` → login `admin / geologix123` (**cambiar claves** o
   crear usuarios propios vía `POST /api/auth/register` con token admin).
6. **Tras arrancar:** editar `/opt/geologix/.env` (claves reales) y `docker-compose restart`.
   Los secretos del `.env` de ejemplo se autogeneran, pero revísalos.

## Opción B: frontend en S3 + backend en EC2

1. Construir con la URL del backend:
   ```bash
   cd frontend
   VITE_API_URL=http://<IP-EC2>:8080 VITE_WS_URL=ws://<IP-EC2>:8080/ws npm run build
   aws s3 sync dist/ s3://tu-bucket-geologix --delete
   ```
   Habilitar *Static website hosting* en el bucket.
2. En la EC2, exponer el backend (`BACKEND_PORT=8080`, SG inbound 8080) y declarar el origen:
   `CORS_ALLOWED_ORIGINS=http://tu-bucket.s3-website.<region>.amazonaws.com` en el `.env`.
3. Opcional (sigue gratis 12 meses): CloudFront delante del bucket para HTTPS.

## Costos (us-east-1, free-tier)

| Recurso | Límite gratis | GeoLogix |
|---|---|---|
| EC2 t3.micro | 750 h/mes × 12 meses | 1 instancia 24/7 ≈ $0 |
| EBS gp3 | 30 GB | 20 GB ≈ $0 |
| S3 | 5 GB + 20k GET | frontend (~2 MB) ≈ $0 |
| Data transfer OUT | 100 GB/mes | demos ≈ $0 |
| SQS / Lambda | 1M req/mes (siempre gratis) | Fase 8 ≈ $0 |

**Cobros típicos a evitar:** Elastic IP sin asociar, NAT Gateway, Load Balancer,
RDS (usar PostGIS en Docker dentro de la EC2, no RDS), segunda instancia.

## Apagado / limpieza

```bash
cd /opt/geologix && docker-compose -f docker-compose.yml -f docker-compose.prod.yml down
```
Para costo $0 fuera de demos: **detener** la instancia (Stop). Terminarla (Terminate)
solo si ya no se necesita. Liberar la Elastic IP si se pidió una.
