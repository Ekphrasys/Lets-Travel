#!/usr/bin/env bash
# Configure les credentials Git pour Jenkins (JCasC git-travel-repo)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# Dynamically read current git remote URL, fallback to GitHub repository
DETECTED_REMOTE="$(git config --get remote.origin.url || echo "https://github.com/Ekphrasys/Lets-Travel.git")"

# Transform SSH format (git@host:user/repo.git) to HTTPS format (https://host/user/repo.git)
if [[ "$DETECTED_REMOTE" =~ ^git@ ]]; then
  DETECTED_REMOTE="${DETECTED_REMOTE#git@}"
  DETECTED_REMOTE="https://${DETECTED_REMOTE/:/\/}"
fi

GIT_REMOTE="${GIT_REMOTE:-$DETECTED_REMOTE}"
ENV_FILE="$ROOT/.env.ci"

read_git_credentials_from_store() {
  local creds host user pass
  [[ -f "$HOME/.git-credentials" ]] || return 1
  local target_host
  target_host=$(echo "$GIT_REMOTE" | grep -oP 'https?://\K[^/]+' || echo "github.com")
  
  while IFS= read -r creds; do
    [[ "$creds" =~ ^https?:// ]] || continue
    creds="${creds#https://}"
    creds="${creds#http://}"
    host="${creds%%/*}"
    [[ "$host" == *"@"* ]] || continue
    user="${host%%@*}"
    host="${host#*@}"
    [[ "$host" == *"$target_host"* ]] || continue
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
    local default_user="Ekphrasys"
    if [[ "$GIT_REMOTE" =~ github\.com/([^/]+)/ ]]; then
      default_user="${BASH_REMATCH[1]}"
    fi
    echo "Credentials Git requis pour cloner $GIT_REMOTE"
    read -r -p "Utilisateur Git [$default_user]: " GIT_USERNAME
    GIT_USERNAME="${GIT_USERNAME:-$default_user}"
    read -r -s -p "Token ou mot de passe Git: " GIT_TOKEN
    echo
    [[ -n "$GIT_TOKEN" ]] || { echo "ERREUR: token vide"; exit 1; }
  fi
fi

# Write variables to .env.ci
touch "$ENV_FILE"
grep -v '^GIT_USERNAME=' "$ENV_FILE" 2>/dev/null | grep -v '^GIT_TOKEN=' | grep -v '^GIT_REMOTE=' > "${ENV_FILE}.tmp" || true
mv "${ENV_FILE}.tmp" "$ENV_FILE"
{
  echo "GIT_USERNAME=$GIT_USERNAME"
  echo "GIT_TOKEN=$GIT_TOKEN"
  echo "GIT_REMOTE=$GIT_REMOTE"
} >> "$ENV_FILE"
chmod 600 "$ENV_FILE"

echo "=== Test clone Git ==="
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

# Extract domain and path without scheme
url_without_scheme="${GIT_REMOTE#https://}"
url_without_scheme="${url_without_scheme#http://}"

# Construct authenticated URL for checking SCM clone access
auth_url="https://${GIT_USERNAME}:${GIT_TOKEN}@${url_without_scheme}"

GIT_TERMINAL_PROMPT=0 git clone --depth 1 -b main "$auth_url" "$TMP_DIR/repo" >/dev/null
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
echo "OK — Jenkins clone main depuis Git (poll SCM toutes les 5 min)"
echo "  Job : http://localhost:9092/job/lets-travel/"
echo ""
