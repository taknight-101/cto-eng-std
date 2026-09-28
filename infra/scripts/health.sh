#!/usr/bin/env bash
# Polls every service's Docker healthcheck until all are healthy, or times out.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

SERVICES=(postgres keycloak downstream-api spring-api-example)
TIMEOUT_SECONDS=180
INTERVAL_SECONDS=3
elapsed=0

container_name() {
  docker compose ps -q "$1" 2>/dev/null
}

is_healthy() {
  local cid
  cid="$(container_name "$1")"
  [[ -z "$cid" ]] && return 1
  local status
  status="$(docker inspect --format '{{.State.Health.Status}}' "$cid" 2>/dev/null || echo "unknown")"
  [[ "$status" == "healthy" ]]
}

echo "Waiting for services to become healthy (timeout ${TIMEOUT_SECONDS}s)..."
while true; do
  all_healthy=true
  for svc in "${SERVICES[@]}"; do
    if is_healthy "$svc"; then
      status_str="healthy"
    else
      status_str="not-yet-healthy"
      all_healthy=false
    fi
    printf "  %-20s %s\n" "$svc" "$status_str"
  done

  if $all_healthy; then
    echo "All services healthy."
    exit 0
  fi

  if (( elapsed >= TIMEOUT_SECONDS )); then
    echo "Timed out waiting for services to become healthy. Run 'docker compose logs <service>' to investigate."
    exit 1
  fi

  sleep "$INTERVAL_SECONDS"
  elapsed=$((elapsed + INTERVAL_SECONDS))
  echo "---"
done
