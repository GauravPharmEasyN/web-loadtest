#!/usr/bin/env bash
set -euo pipefail
#
# Pharmeasy JSON API load (PharmeasyApiSimulation): default aggregate 200 RPS for 600s across 4 endpoints.
#
# Only getCartCount sends Cookie when you set one of: X_ACCESS_TOKEN, PHARMEASY_COOKIE, or CART_COOKIE.
#
#   export X_ACCESS_TOKEN='…'   # becomes Cookie: X-Access-Token=…
#   export GATLING_JAVA_OPTS="-Xmx4g -Xms512m -XX:+UseG1GC"
#   GATLING_ULIMIT_NO=65535 API_RPS=200 API_DURATION_SECS=600 ./scripts/run_api_load.sh

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_gatling_preamble.sh"

exec sbt "${SBT_GATLING_FLAGS[@]}" "Gatling/testOnly pharmeasy.PharmeasyApiSimulation"
