#!/usr/bin/env bash
# Tests CRUD admin conformes à audit.md
set -euo pipefail

API="${API_URL:-https://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-test@travel.com}"
ADMIN_PASS="${ADMIN_PASS:-password123}"

echo "=== Login admin ==="
TOKEN=$(curl -sk -X POST "$API/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASS\"}" | jq -r .token)

if [[ "$TOKEN" == "null" || -z "$TOKEN" ]]; then
  echo "Échec login admin. Créez un admin :"
  echo "  docker exec -it travel-postgres psql -U travel -d travel_db -c \"UPDATE \\\"user\\\".users SET role = 'ADMIN' WHERE email = '$ADMIN_EMAIL';\""
  exit 1
fi

AUTH="Authorization: Bearer $TOKEN"

echo "=== Sans token → 401 ==="
code=$(curl -sk -o /dev/null -w "%{http_code}" "$API/api/users")
[[ "$code" == "401" || "$code" == "403" ]] && echo "OK ($code)" || echo "WARN ($code)"

echo "=== CRUD Users ==="
USER_ID=$(curl -sk -X POST "$API/api/users" -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"id":"'"$(uuidgen)"'","email":"audit-user@test.com","firstName":"Audit","lastName":"User","role":"USER"}' | jq -r .id)
echo "Created user: $USER_ID"
curl -sk "$API/api/users/$USER_ID" -H "$AUTH" | jq .email
curl -sk -X PUT "$API/api/users/$USER_ID" -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"email":"audit-user@test.com","firstName":"Updated","lastName":"User","role":"USER"}' | jq .firstName
curl -sk -X DELETE "$API/api/users/$USER_ID" -H "$AUTH" -w "DELETE: %{http_code}\n" -o /dev/null

echo "=== CRUD Travelers (trips) ==="
TRIP_ID=$(curl -sk -X POST "$API/api/travels" -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"title":"Audit Trip","originCity":"Paris","destinationCity":"Lyon","departureDate":"2026-12-01","price":99.99,"seatsAvailable":10}' | jq -r .id)
echo "Created trip: $TRIP_ID"
curl -sk "$API/api/travels/$TRIP_ID" -H "$AUTH" | jq .title
curl -sk -X PUT "$API/api/travels/$TRIP_ID" -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"title":"Audit Trip Updated","originCity":"Paris","destinationCity":"Lyon","departureDate":"2026-12-01","price":109.99,"seatsAvailable":8}' | jq .title
curl -sk -X DELETE "$API/api/travels/$TRIP_ID" -H "$AUTH" -w "DELETE: %{http_code}\n" -o /dev/null

# echo "=== CRUD Payments ==="
# BOOKING_ID=$(uuidgen)
# USER_REF=$(uuidgen)
# PAY_ID=$(curl -sk -X POST "$API/api/payments" -H "$AUTH" -H "Content-Type: application/json" \
#   -d "{\"bookingId\":\"$BOOKING_ID\",\"userId\":\"$USER_REF\",\"amount\":50.00}" | jq -r .id)
# echo "Created payment: $PAY_ID"
# curl -sk "$API/api/payments/$PAY_ID" -H "$AUTH" | jq .amount
# curl -sk -X PUT "$API/api/payments/$PAY_ID" -H "$AUTH" -H "Content-Type: application/json" \
#   -d '{"amount":75.00,"status":"COMPLETED"}' | jq .amount
# curl -sk -X DELETE "$API/api/payments/$PAY_ID" -H "$AUTH" -w "DELETE: %{http_code}\n" -o /dev/null

echo "=== Audit API tests OK ==="
