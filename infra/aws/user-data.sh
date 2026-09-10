#!/bin/bash
# GeoLogix en EC2 (Amazon Linux 2023, free-tier t2/t3.micro).
# Pegar en "User data" al crear la instancia. Instala Docker, clona el repo
# y levanta PostGIS + backend + frontend con docker compose.
set -eux

dnf update -y
dnf install -y docker git
systemctl enable --now docker
usermod -aG docker ec2-user

DOCKER_COMPOSE_VERSION="v2.35.1"
curl -SL "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-linux-x86_64" \
  -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose

REPO_URL="https://github.com/DennisDavidAnchundia/geologix-dashboard.git"
APP_DIR="/opt/geologix"
if [ ! -d "$APP_DIR/.git" ]; then
  git clone "$REPO_URL" "$APP_DIR"
fi
cd "$APP_DIR"
git pull --ff-only || true

# .env con secretos (el usuario DEBE editarlos tras el primer arranque).
if [ ! -f .env ]; then
  cp .env.example .env
  sed -i 's/cambia-esta-clave/geologix-prod-01/' .env
  # Secreto JWT aleatorio
  SECRET=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | head -c 48)
  sed -i "s/cambia-este-secreto-por-uno-largo-y-aleatorio-1234567890/$SECRET/" .env
fi

docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
docker-compose -f docker-compose.yml -f docker-compose.prod.yml ps
