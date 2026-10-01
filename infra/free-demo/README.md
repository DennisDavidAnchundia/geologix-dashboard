# Demo gratis — Supabase + Render + Vercel (sin tarjeta)

Arquitectura: `Vercel (front estático)` → `Render free (Spring Boot)` → `Supabase free (Postgres+PostGIS)`.

## 1. Base de datos (Supabase, 5 min)

1. Crea cuenta en https://supabase.com → New Project (plan Free, región `South America (São Paulo)`).
2. En SQL Editor ejecuta:
   ```sql
   CREATE EXTENSION IF NOT EXISTS postgis;
   ```
3. En Project Settings → Database copia la **Connection string (URI)** y el password.
   Formato JDBC para Render:
   `jdbc:postgresql://db.XXXX.supabase.co:5432/postgres?sslmode=require`
   Usuario: `postgres`, clave: la que pusiste.
4. El backend con `ddl-auto=update` crea las tablas solo al arrancar (incluye el CHECK de
   `alerts.tipo` con los 5 tipos, ya no hay que tocar SQL a mano).

> Límite: 500MB, se pausa a los 7 días sin uso y despierta solo. Suficiente para demo.

## 2. Backend (Render free, 5 min)

1. Cuenta en https://render.com (sin tarjeta) → New → Blueprint → conecta este repo
   (usa `render.yaml` de la raíz).
2. Completa las env vars que piden manual (`sync: false`):
   - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USER`, `SPRING_DATASOURCE_PASSWORD` (del paso 1)
   - `CORS_ALLOWED_ORIGINS` → déjala vacía por ahora, se llena con la URL de Vercel
3. Deploy. Healthcheck: `/actuator/health`. Primer arranque tarda 2-4 min (build Maven).
   URL resultante: `https://geologix-backend.onrender.com`
4. Verifica: `POST https://geologix-backend.onrender.com/api/auth/login`
   con `{"username":"operador","password":"geologix123"}` → debe devolver JWT
   (el `AdminSeeder` crea `admin/operador` al primer arranque).

> Límite: 512MB RAM (ya ajustado con `-Xmx300m`), se duerme a los 15 min sin tráfico
> y despierta en ~60s. En el portafolio avisa: "la demo tarda 50s en despertar".

## 3. Frontend (Vercel, 3 min)

1. Cuenta en https://vercel.com → Add New → Project → importa este repo,
   Root Directory = `frontend`, Framework = Vite.
2. Environment Variables:
   - `VITE_API_URL=https://geologix-backend.onrender.com`
   - `VITE_WS_URL=wss://geologix-backend.onrender.com/ws`
3. Deploy. URL resultante: `https://tu-app.vercel.app`
4. Vuelve a Render y pon `CORS_ALLOWED_ORIGINS=https://tu-app.vercel.app` → Redeploy.
5. Entra con `operador / geologix123`.

## 4. Actualizar el README

Cambia la línea de demo por la URL de Vercel:
`> Demo en vivo: https://tu-app.vercel.app — usuario demo: operador / geologix123 (tarda ~60s en despertar la primera vez).`

## Costo

$0 los tres (sin tarjeta). Si Render free se queda corto, alternativa $7/mes Starter.
EC2 queda como opción B de pago, no se borra (`infra/aws/`).
