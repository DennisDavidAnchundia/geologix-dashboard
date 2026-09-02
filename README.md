# GeoLogix Dashboard

Sistema de trazabilidad y monitorización de flota en tiempo real: un panel que rastrea
vehículos en movimiento, visualiza su ubicación en un mapa interactivo y genera métricas
operativas. Construido con **Java (Spring Boot)** en el backend y **React** en el frontend,
con soporte de **geoinformación (GIS / PostGIS)**.

> **Nota:** este README se actualiza al final de cada fase del desarrollo.
> Fase actual: **Fase 0 (inicialización)** · en desarrollo la **Fase 1 (dominio y simulador GPS)**.

---

## ✨ Características

- Monitoreo de vehículos en tiempo real (WebSockets/STOMP).
- Simulador GPS que genera coordenadas de flotas en movimiento.
- API REST para vehículos, posiciones y alertas.
- Base de datos geográfica con **PostGIS** (posiciones como geometrías, geofences, análisis espacial).
- Panel con mapa (Leaflet) y KPIs (Recharts). *(frontend, en fase posterior)*
- Interfaz multilingüe **ES / EN**. *(fase posterior)*

---

## 🛠️ Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Java 21 · Spring Boot · Spring Data JPA · Spring WebSocket/STOMP · Lombok |
| Frontend | React · Vite · Leaflet · Recharts · i18next |
| Base de datos | PostgreSQL + PostGIS (y H2 en memoria para desarrollo) |
| Infraestructura | Docker / docker-compose · (AWS/LocalStack en fases posteriores) |

---

## 🗂️ Estructura del proyecto

```
geologix-dashboard/
├── backend/          # API Spring Boot (Java)
├── frontend/         # Dashboard React (Vite)
├── docker-compose.yml # Orquestación de servicios (PostGIS)
└── README.md
```

---

## 🚀 Cómo levantar el proyecto

### Requisitos
- JDK 21+ y Maven (o el Maven Wrapper incluido en `backend/`)
- Node.js 18+
- Docker (opcional, para PostGIS en producción)

### Backend (puerto 8080)
```bash
cd backend
# Con H2 en memoria (sin Docker, arranca al instante):
./mvnw spring-boot:run
```
> Para usar PostgreSQL/PostGIS: `./mvnw spring-boot:run -Dspring-boot.run.profiles=docker`
> (tras levantar la BD con `docker compose up -d`).

### Frontend (puerto 5173)
```bash
cd frontend
npm install
npm run dev
```

---

## ✅ Estado por fases

| Fase | Descripción | Estado |
|---|---|---|
| 0 | Inicialización: estructura, esqueleto backend + frontend, Docker/PostGIS | ✅ En curso |
| 1 | Dominio (Vehicle, Position, Alert) + simulador GPS + API REST | ⏳ Siguiente |
| 2 | WebSockets (tiempo real) | ⏳ Pendiente |
| 3 | PostGIS: persistencia y consultas espaciales | ⏳ Pendiente |
| 4 | Frontend: mapa, KPIs y i18n ES/EN | ⏳ Pendiente |
| 5 | GIS aplicado: geofences y análisis | ⏳ Pendiente |
| 6 | Docker completo | ⏳ Pendiente |
| 7 | Presentación y documentación | ⏳ Pendiente |

---

## 🧠 Decisiones de diseño

*(Se documentará en detalle tras cada fase; ideas generales:)*

- **WebSockets (no polling)** para tiempo real de baja latencia en monitoreo.
- **PostGIS** para tratar los datos como geográficos reales (geometrías, geofences, distancias).
- **H2 en memoria en desarrollo** para arranque instantáneo sin dependencias externas.

---

## 📄 Licencia

Proyecto de portafolio personal. Uso educativo/demostrativo.
