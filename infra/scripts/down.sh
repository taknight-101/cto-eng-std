#!/usr/bin/env bash
# Stops the environment. Container state/data (Postgres volume) is preserved - use reset.sh for
# a clean-slate restart.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

docker compose --profile observability down
