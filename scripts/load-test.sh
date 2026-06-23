#!/usr/bin/env bash
# Simulation de charge — load balancing via Eureka + Gateway
set -euo pipefail

API="${API_URL:-https://localhost:8080}"
REQUESTS="${REQUESTS:-200}"
CONCURRENCY="${CONCURRENCY:-20}"

echo "=== Load test: GET /actuator/health ($REQUESTS req, $CONCURRENCY concurrent) ==="

if command -v hey >/dev/null 2>&1; then
  hey -n "$REQUESTS" -c "$CONCURRENCY" -k "$API/actuator/health"
elif command -v ab >/dev/null 2>&1; then
  ab -n "$REQUESTS" -c "$CONCURRENCY" -k "$API/actuator/health"
else
  echo "hey/ab non installés — boucle curl simple"
  ok=0
  for i in $(seq 1 "$REQUESTS"); do
    code=$(curl -sk -o /dev/null -w "%{http_code}" "$API/actuator/health")
    [[ "$code" == "200" ]] && ok=$((ok+1))
  done
  echo "Succès: $ok / $REQUESTS"
fi

echo "=== Eureka instances ==="
curl -s http://localhost:8761/eureka/apps 2>/dev/null | grep -o '<name>[A-Z-]*</name>' | sort -u || true

echo "=== Load test terminé ==="
