# Frontend GeoLogix — build Vite + Nginx (proxy /api y /ws al backend: mismo origen).
ARG VITE_API_URL=""
ARG VITE_WS_URL=""
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . ./
RUN npm run build -- --base ./

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
