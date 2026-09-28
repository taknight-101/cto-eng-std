#!/usr/bin/env bash
# End-to-end smoke test against the running infra/docker-compose.yml environment. No manual
# browser interaction required - uses the ROPC/password grant (test users only) and
# client_credentials grants, both fully curl-scriptable. Requires: curl, jq.
set -uo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

KEYCLOAK_URL="http://localhost:${KEYCLOAK_PORT:-8080}"
REALM="${KEYCLOAK_REALM:-common-platform}"
API_URL="http://localhost:${SPRING_API_EXAMPLE_PORT:-8081}"
DOWNSTREAM_URL="http://localhost:${DOWNSTREAM_API_PORT:-8082}"

PASS=0
FAIL=0

step() { echo; echo "==> $*"; }
ok()   { echo "  [PASS] $*"; PASS=$((PASS + 1)); }
bad()  { echo "  [FAIL] $*"; FAIL=$((FAIL + 1)); }

require_status() {
  local description="$1" expected="$2" actual="$3"
  if [[ "$actual" == "$expected" ]]; then
    ok "$description (status $actual)"
  else
    bad "$description (expected $expected, got $actual)"
  fi
}

token_for_user() {
  local username="$1" password="$2"
  curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=interactive-client" \
    -d "username=$username" \
    -d "password=$password" | jq -r '.access_token // empty'
}

token_client_credentials() {
  curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=client_credentials" \
    -d "client_id=service-client" \
    -d "client_secret=service-client-secret-dev-only" | jq -r '.access_token // empty'
}

step "1. Keycloak is reachable and the realm exists"
DISCOVERY_STATUS=$(curl -s -o /dev/null -w '%{http_code}' "$KEYCLOAK_URL/realms/$REALM/.well-known/openid-configuration")
require_status "OIDC discovery document for realm '$REALM'" 200 "$DISCOVERY_STATUS"

step "2. Token can be obtained for each test user"
ALICE_TOKEN=$(token_for_user alice password)
BOB_TOKEN=$(token_for_user bob password)
ADMIN_TOKEN=$(token_for_user admin password)
[[ -n "$ALICE_TOKEN" ]] && ok "alice token acquired" || bad "alice token acquisition"
[[ -n "$BOB_TOKEN" ]] && ok "bob token acquired" || bad "bob token acquisition"
[[ -n "$ADMIN_TOKEN" ]] && ok "admin token acquired" || bad "admin token acquisition"

step "3. Spring API and downstream-api are reachable"
require_status "spring-api-example health" 200 "$(curl -s -o /dev/null -w '%{http_code}' "$API_URL/actuator/health")"
require_status "downstream-api health" 200 "$(curl -s -o /dev/null -w '%{http_code}' "$DOWNSTREAM_URL/actuator/health")"

step "4. Unauthenticated / unauthorized requests are rejected"
require_status "GET /api/users/1 with no token" 401 \
  "$(curl -s -o /dev/null -w '%{http_code}' "$API_URL/api/users/1")"
require_status "POST /api/orders as alice (USER only, needs ADMIN)" 403 \
  "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API_URL/api/orders" \
     -H "Authorization: Bearer $ALICE_TOKEN" -H "Content-Type: application/json" \
     -d '{"sku":"WIDGET","quantity":1}')"

step "5. Authenticated request succeeds"
require_status "GET /api/users/1 as alice" 200 \
  "$(curl -s -o /dev/null -w '%{http_code}' "$API_URL/api/users/1" -H "Authorization: Bearer $ALICE_TOKEN")"

step "6. Service-to-service OAuth2 (client_credentials) works directly against downstream-api"
SERVICE_TOKEN=$(token_client_credentials)
if [[ -n "$SERVICE_TOKEN" ]]; then
  ok "service-client token acquired"
  require_status "GET /internal/inventory/WIDGET with service-client token" 200 \
    "$(curl -s -o /dev/null -w '%{http_code}' "$DOWNSTREAM_URL/internal/inventory/WIDGET" \
       -H "Authorization: Bearer $SERVICE_TOKEN")"
else
  bad "service-client token acquisition"
fi

step "7. HTTP client dispatch works end-to-end (spring-api-example -> http-client-lib -> downstream-api)"
CORRELATION_ID="smoke-test-$(date +%s)-$$"
ORDER_STATUS=$(curl -s -o /tmp/smoke-order-response.json -w '%{http_code}' -X POST "$API_URL/api/orders" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -d '{"sku":"WIDGET","quantity":1}')
require_status "POST /api/orders as admin" 201 "$ORDER_STATUS"
if [[ "$ORDER_STATUS" == "201" ]]; then
  grep -q '"status":"CONFIRMED"' /tmp/smoke-order-response.json && ok "order response has status=CONFIRMED" \
    || bad "order response missing status=CONFIRMED"
fi

step "8. Downstream forwarding: OUT-OF-STOCK sku is rejected with 409"
require_status "POST /api/orders with insufficient inventory" 409 \
  "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API_URL/api/orders" \
     -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
     -d '{"sku":"OUT-OF-STOCK","quantity":1}')"

step "9. Conditional forwarding gateway: BLOCKED sku is rejected without calling downstream"
require_status "GET /api/gateway/inventory/BLOCKED-1" 403 \
  "$(curl -s -o /dev/null -w '%{http_code}' "$API_URL/api/gateway/inventory/BLOCKED-1" \
     -H "Authorization: Bearer $BOB_TOKEN")"

step "10. Correlation ID propagated across services"
sleep 2 # give the async log lines a moment to flush
if docker compose logs spring-api-example 2>/dev/null | grep -q "$CORRELATION_ID" \
    && docker compose logs downstream-api 2>/dev/null | grep -q "$CORRELATION_ID"; then
  ok "correlation ID '$CORRELATION_ID' found in both services' logs"
else
  bad "correlation ID '$CORRELATION_ID' not found in both services' logs"
fi

step "Summary"
echo "  PASS: $PASS   FAIL: $FAIL"
if (( FAIL > 0 )); then
  exit 1
fi
