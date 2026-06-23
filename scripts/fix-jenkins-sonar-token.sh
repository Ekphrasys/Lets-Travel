#!/usr/bin/env bash
# Recharge le token SonarQube dans Jenkins (credential JCasC sonar-token)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -f .env.ci ]] || ! grep -q '^SONAR_TOKEN=squ_' .env.ci; then
  echo "=== Génération token SonarQube ==="
  SONAR_ADMIN_PASSWORD="${SONAR_ADMIN_PASSWORD:-admin123}" bash scripts/setup-sonarqube.sh
fi

echo "=== Redémarrage Jenkins avec .env.ci ==="
docker compose -f jenkins-compose.yml up -d --force-recreate jenkins

echo "=== Attente Jenkins ==="
for _ in $(seq 1 60); do
  if curl -sf http://localhost:9092/login >/dev/null 2>&1; then
    break
  fi
  sleep 3
done

echo "=== Rechargement JCasC (credential sonar-token) ==="
CRUMB=$(curl -sf -u admin:admin123 \
  'http://localhost:9092/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,":",//crumb)' || true)
if [[ -n "$CRUMB" ]]; then
  curl -sf -u admin:admin123 -H "$CRUMB" -X POST http://localhost:9092/configuration-as-code/reload >/dev/null
  echo "JCasC rechargé"
else
  echo "WARN: recharge JCasC manuelle — Manage Jenkins → Configuration as Code → Reload"
fi

echo "OK — relancez le build Jenkins (job travel)"
