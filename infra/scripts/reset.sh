#!/usr/bin/env bash
# Full "start from scratch": stops everything AND removes the Postgres volume (so Keycloak
# re-imports the realm cleanly on next startup), then rebuilds and starts.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

echo "Stopping and removing containers + volumes..."
docker compose --profile observability down -v

echo "Rebuilding and starting..."
./scripts/up.sh "${1:-}"
