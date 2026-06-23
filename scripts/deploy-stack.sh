#!/usr/bin/env bash
# Déploiement idempotent de la stack applicative (Jenkins + Ansible)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${DEPLOY_ENV_FILE:-$ROOT/.env}"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
else
  echo "ERREUR: fichier .env absent ($ENV_FILE)" >&2
  exit 1
fi

COMPOSE=(docker compose -f "$ROOT/infrastructure/docker-compose.yml" --project-name travel)

# Conteneurs fantômes laissés par un recreate interrompu (stack app uniquement, pas Jenkins/Sonar)
docker ps -aq --filter "label=com.docker.compose.project=travel" | xargs -r docker rm -f || true

"${COMPOSE[@]}" up -d --build --remove-orphans
"${COMPOSE[@]}" ps

container_health_status() {
  local container=$1
  docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || echo "missing"
}

wait_for_container() {
  local container=$1 label=$2 max=${3:-30}
  echo "Attente $label (conteneur $container)..."
  for _ in $(seq 1 "$max"); do
    local status
    status="$(container_health_status "$container")"
    if [[ "$status" == "healthy" ]]; then
      echo "OK — $label"
      return 0
    fi
    if [[ "$status" == "running" ]]; then
      local has_healthcheck
      has_healthcheck="$(docker inspect --format='{{if .State.Health}}yes{{end}}' "$container" 2>/dev/null || true)"
      if [[ -z "$has_healthcheck" ]]; then
        echo "OK — $label (running)"
        return 0
      fi
    fi
    sleep 10
  done
  echo "ERREUR: $label non prêt (conteneur $container, statut: $(container_health_status "$container"))" >&2
  return 1
}

# Depuis Jenkins (conteneur), localhost:8080 ne pointe pas vers la stack sur l'hôte.
# On s'appuie sur les healthchecks Docker, accessibles via le socket monté.
wait_for_container "travel-gateway" "API Gateway" 30
wait_for_container "travel-admin" "Frontend" 18
