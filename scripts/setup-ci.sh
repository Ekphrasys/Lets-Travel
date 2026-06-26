#!/usr/bin/env bash
# Launch Jenkins + SonarQube + Elasticsearch + Neo4j for Lets-Travel
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "=== Prérequis système SonarQube + Elasticsearch ==="
sudo sysctl -w vm.max_map_count=262144 2>/dev/null || sysctl -w vm.max_map_count=262144 2>/dev/null || true

echo "=== Démarrage SonarQube + Elasticsearch + Neo4j + Jenkins ==="
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
     if docker exec travel-jenkins test -d /var/jenkins_home/jobs/lets-travel 2>/dev/null; then
       echo "    Job lets-travel créé (tentative $i)"
       break
     fi
     echo "    Jenkins répond (tentative $i)"
   fi
   sleep 5
done

echo ""
echo "=== CI prêt pour Lets-Travel ==="
echo "  Jenkins      : http://localhost:9092"
echo "  SonarQube    : http://localhost:9002"
echo "  Elasticsearch: http://localhost:9200"
echo "  Neo4j        : http://localhost:7474"
echo "  Job          : lets-travel (pipeline auto-créé)"
echo ""
echo "Lancer un build : Jenkins → lets-travel → Build Now"
