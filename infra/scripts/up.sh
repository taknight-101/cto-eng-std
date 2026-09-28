#!/usr/bin/env bash
# Starts the full local infrastructure environment (Postgres + Keycloak + downstream-api +
# spring-api-example). Add --observability to also start the Jaeger/OTel-collector profile.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

if [[ ! -f .env ]]; then
  echo "No infra/.env found - copying infra/.env.example -> infra/.env (local-dev defaults)."
  cp .env.example .env
fi

PROFILE_ARGS=()
if [[ "${1:-}" == "--observability" ]]; then
  PROFILE_ARGS=(--profile observability)
  echo "Starting WITH the optional observability profile (Jaeger UI on http://localhost:16686)."
fi

docker compose "${PROFILE_ARGS[@]}" up -d --build

echo
echo "Started. Run ./scripts/health.sh to wait for everything to become healthy,"
echo "then ./scripts/smoke-test.sh to exercise the full stack end-to-end."
