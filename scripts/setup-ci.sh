#!/usr/bin/env bash
# Lance Jenkins + SonarQube et applique toute la config audit
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "=== Prérequis système SonarQube ==="
sudo sysctl -w vm.max_map_count=262144 2>/dev/null || sysctl -w vm.max_map_count=262144 2>/dev/null || true

echo "=== Démarrage SonarQube + Jenkins ==="
docker compose -f jenkins-compose.yml up -d --build

echo "=== Configuration SonarQube ==="
bash scripts/setup-sonarqube.sh

echo "=== Configuration Git SCM Jenkins ==="
bash scripts/setup-jenkins-git.sh

echo ""
echo "=== Attente démarrage Jenkins (plugins + JCasC) ==="
echo "    Le 1er démarrage peut prendre 1-3 min..."
for i in $(seq 1 60); do
  if curl -sf http://localhost:9092/login >/dev/null 2>&1; then
    if docker exec travel-jenkins test -d /var/jenkins_home/jobs/travel 2>/dev/null; then
      echo "    Job travel créé (tentative $i)"
      break
    fi
    echo "    Jenkins répond (tentative $i)"
  fi
  sleep 5
done

echo ""
echo "=== CI prêt (audit) ==="
echo "  Jenkins   : http://localhost:9092"
echo "  SonarQube : http://localhost:9002"
echo "  Job       : travel (pipeline auto-créé)"
echo ""
echo "Lancer un build : Jenkins → travel → Build Now"
