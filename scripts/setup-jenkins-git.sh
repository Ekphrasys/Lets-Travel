#!/usr/bin/env bash
# Configure les credentials Git Gitea pour Jenkins (JCasC git-travel-repo)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

GIT_REMOTE="${GIT_REMOTE:-https://zone01normandie.org/git/ajoly/travel-plan.git}"
ENV_FILE="$ROOT/.env.ci"

read_git_credentials_from_store() {
  local creds host user pass
  [[ -f "$HOME/.git-credentials" ]] || return 1
  while IFS= read -r creds; do
    [[ "$creds" =~ ^https?:// ]] || continue
    creds="${creds#https://}"
    creds="${creds#http://}"
    host="${creds%%/*}"
    [[ "$host" == *"@"* ]] || continue
    user="${host%%@*}"
    host="${host#*@}"
    [[ "$host" == zone01normandie.org* ]] || continue
    pass="${user#*:}"
    user="${user%%:*}"
    [[ -n "$user" && -n "$pass" ]] || continue
    GIT_USERNAME="$user"
    GIT_TOKEN="$pass"
    return 0
  done < "$HOME/.git-credentials"
  return 1
}

if [[ -z "${GIT_USERNAME:-}" || -z "${GIT_TOKEN:-}" ]]; then
  if read_git_credentials_from_store; then
    echo "Credentials Git lus depuis ~/.git-credentials (utilisateur: $GIT_USERNAME)"
  else
    echo "Credentials Git Gitea requis pour cloner $GIT_REMOTE"
    read -r -p "Utilisateur Gitea [ajoly]: " GIT_USERNAME
    GIT_USERNAME="${GIT_USERNAME:-ajoly}"
    read -r -s -p "Token ou mot de passe Gitea: " GIT_TOKEN
    echo
    [[ -n "$GIT_TOKEN" ]] || { echo "ERREUR: token vide"; exit 1; }
  fi
fi

touch "$ENV_FILE"
grep -v '^GIT_USERNAME=' "$ENV_FILE" 2>/dev/null | grep -v '^GIT_TOKEN=' > "${ENV_FILE}.tmp" || true
mv "${ENV_FILE}.tmp" "$ENV_FILE"
{
  echo "GIT_USERNAME=$GIT_USERNAME"
  echo "GIT_TOKEN=$GIT_TOKEN"
} >> "$ENV_FILE"
chmod 600 "$ENV_FILE"

echo "=== Test clone Git (master) ==="
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
GIT_TERMINAL_PROMPT=0 git clone --depth 1 -b master \
  "https://${GIT_USERNAME}:${GIT_TOKEN}@zone01normandie.org/git/ajoly/travel-plan.git" \
  "$TMP_DIR/repo" >/dev/null
echo "Clone OK"

echo "=== Redémarrage Jenkins (JCasC git-travel-repo) ==="
docker compose -f jenkins-compose.yml up -d --build jenkins

echo "=== Attente Jenkins ==="
for _ in $(seq 1 60); do
  curl -sf http://localhost:9092/login >/dev/null 2>&1 && break
  sleep 3
done

echo "=== Rechargement JCasC ==="
CRUMB=$(curl -sf -u "admin:${JENKINS_ADMIN_PASSWORD:-admin123}" \
  'http://localhost:9092/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,":",//crumb)' || true)
if [[ -n "$CRUMB" ]]; then
  curl -sf -u "admin:${JENKINS_ADMIN_PASSWORD:-admin123}" -H "$CRUMB" \
    -X POST http://localhost:9092/configuration-as-code/reload >/dev/null || true
fi

echo ""
echo "OK — Jenkins clone master depuis Gitea (poll SCM toutes les 5 min)"
echo "  Job : http://localhost:9092/job/travel/"
echo ""
echo "Webhook Gitea (optionnel, si Jenkins accessible depuis zone01normandie.org) :"
echo "  URL : http://<IP_PUBLIQUE>:9092/gitea-webhook/post"
echo "  Événement : Push"
