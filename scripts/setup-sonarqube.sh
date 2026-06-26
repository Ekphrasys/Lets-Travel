#!/usr/bin/env bash
# Configure SonarQube for Lets-Travel: project, token, quality gate
set -euo pipefail

SONAR_URL="${SONAR_URL:-http://localhost:9002}"
SONAR_ADMIN="${SONAR_ADMIN:-admin}"
PROJECT_KEY="${SONAR_PROJECT_KEY:-lets-travel}"
GATE_NAME="${SONAR_QUALITY_GATE:-Lets-Travel Quality Gate}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_CI_FILE="${ENV_CI_FILE:-$ROOT/.env.ci}"

_CLI_SONAR_PASSWORD="${SONAR_ADMIN_PASSWORD:-}"
if [[ -f "$ENV_CI_FILE" ]]; then
   source "$ENV_CI_FILE" 2>/dev/null || true
fi
if [[ -n "$_CLI_SONAR_PASSWORD" ]]; then
   SONAR_ADMIN_PASSWORD="$_CLI_SONAR_PASSWORD"
else
   SONAR_ADMIN_PASSWORD="${SONAR_ADMIN_PASSWORD:-admin}"
fi

sonar_api() {
   local method=$1
   local path=$2
   shift 2
   curl -s -u "$SONAR_ADMIN:$SONAR_ADMIN_PASSWORD" -X "$method" \
     "$SONAR_URL/api/$path" "$@"
}

sonar_check_auth() {
   local resp
   resp=$(curl -s -u "$SONAR_ADMIN:$SONAR_ADMIN_PASSWORD" "$SONAR_URL/api/authentication/validate")
   if echo "$resp" | grep -q '"valid":true'; then
     return 0
   fi
   echo "ERREUR: authentification SonarQube échouée (login=$SONAR_ADMIN)."
   exit 1
}

echo "=== Attente SonarQube ($SONAR_URL) ==="
for _ in $(seq 1 60); do
   if curl -sf "$SONAR_URL/api/system/status" | grep -q '"status":"UP"'; then
     break
   fi
   sleep 5
done
curl -sf "$SONAR_URL/api/system/status" | grep -q '"status":"UP"' || {
   echo "SonarQube indisponible sur $SONAR_URL"
   exit 1
}

echo "=== Authentification SonarQube ==="
sonar_check_auth

echo "=== Création projet $PROJECT_KEY ==="
sonar_api POST "projects/create" -d "project=$PROJECT_KEY&name=Lets-Travel Management System" >/dev/null 2>&1 || true

echo "=== Quality Gate : $GATE_NAME ==="
QG_ID=$(sonar_api GET "qualitygates/list" | python3 -c "
import sys, json
data = json.load(sys.stdin)
for g in data.get('qualitygates', []):
     if g.get('name') == '$GATE_NAME':
         print(g['id'])
         break
" 2>/dev/null || true)

if [[ -z "$QG_ID" ]]; then
   sonar_api POST "qualitygates/create" -d "name=$GATE_NAME" >/dev/null
   QG_ID=$(sonar_api GET "qualitygates/show" --data-urlencode "name=$GATE_NAME" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
   echo "Quality Gate créée (id=$QG_ID)"
else
   echo "Quality Gate existante (id=$QG_ID)"
fi

echo "=== Configuration Quality Gate conditions ==="
mapfile -t _qg_cond_ids < <(sonar_api GET "qualitygates/show" --data-urlencode "name=$GATE_NAME" | python3 -c "
import sys, json
for c in json.load(sys.stdin).get('conditions', []):
     print(c['id'])
")
for cond_id in "${_qg_cond_ids[@]}"; do
   [[ -n "$cond_id" ]] || continue
   curl -s -u "$SONAR_ADMIN:$SONAR_ADMIN_PASSWORD" -X POST \
     "$SONAR_URL/api/qualitygates/delete_condition?id=${cond_id}" >/dev/null \
     || echo "WARN: suppression condition $cond_id"
done

sonar_api POST "qualitygates/create_condition" -d "gateId=$QG_ID&metric=bugs&op=GT&error=0" >/dev/null
sonar_api POST "qualitygates/create_condition" -d "gateId=$QG_ID&metric=vulnerabilities&op=GT&error=0" >/dev/null
sonar_api POST "qualitygates/create_condition" -d "gateId=$QG_ID&metric=security_hotspots&op=GT&error=0" >/dev/null
sonar_api POST "qualitygates/create_condition" -d "gateId=$QG_ID&metric=code_smells&op=GT&error=50" >/dev/null
echo "Conditions configurées: bugs=0, vulnerabilities=0, security_hotspots=0, code_smells<50"

sonar_api POST "qualitygates/select" -d "projectKey=$PROJECT_KEY&gateId=$QG_ID" >/dev/null
echo "Quality Gate associée au projet $PROJECT_KEY"

echo "=== Génération token SonarQube ==="
TOKEN=$(sonar_api POST "user_tokens/generate" -d "name=jenkins-lets-travel-$(date +%Y%m%d-%H%M%S)" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

PRESERVE_GIT_USERNAME=""
PRESERVE_GIT_TOKEN=""
PRESERVE_NEO4J_PASSWORD=""
if [[ -f "$ENV_CI_FILE" ]]; then
   PRESERVE_GIT_USERNAME=$(grep -E '^GIT_USERNAME=' "$ENV_CI_FILE" | cut -d= -f2- || true)
   PRESERVE_GIT_TOKEN=$(grep -E '^GIT_TOKEN=' "$ENV_CI_FILE" | cut -d= -f2- || true)
   PRESERVE_NEO4J_PASSWORD=$(grep -E '^NEO4J_PASSWORD=' "$ENV_CI_FILE" | cut -d= -f2- || true)
fi

cat > "$ENV_CI_FILE" <<EOF
# Généré par scripts/setup-sonarqube.sh — ne pas committer
SONAR_TOKEN=$TOKEN
SONAR_ADMIN_PASSWORD=$SONAR_ADMIN_PASSWORD
EOF
if [[ -n "$PRESERVE_GIT_USERNAME" && -n "$PRESERVE_GIT_TOKEN" ]]; then
   {
     echo "GIT_USERNAME=$PRESERVE_GIT_USERNAME"
     echo "GIT_TOKEN=$PRESERVE_GIT_TOKEN"
   } >> "$ENV_CI_FILE"
fi
if [[ -n "$PRESERVE_NEO4J_PASSWORD" ]]; then
   echo "NEO4J_PASSWORD=$PRESERVE_NEO4J_PASSWORD" >> "$ENV_CI_FILE"
fi
chmod 600 "$ENV_CI_FILE"

echo ""
echo "=== SonarQube configuré ==="
echo "  URL      : $SONAR_URL"
echo "  Projet   : $PROJECT_KEY"
echo "  Gate     : $GATE_NAME ($QG_ID)"
echo "  Token    : enregistré dans $ENV_CI_FILE"
