#!/usr/bin/env bash
set -euo pipefail

# Usage: HOME_USERS=20 HOME_DURATION_SECS=60 MEDICINE_USERS=10 ... ./scripts/run_individual.sh
#
# Large runs (heap + FDs; same shell as this script):
#   export GATLING_JAVA_OPTS="-Xmx8g -Xms512m -XX:+UseG1GC -XX:MaxDirectMemorySize=2g"
#   GATLING_ULIMIT_NO=1048576 ./scripts/run_individual.sh
# Or: ulimit -n 65535 manually before calling (macOS soft limit may cap lower).
#
# Log each GET (avoid at huge volume): GATLING_DEBUG=true  or  -Dgatling.request.debug=true in GATLING_JAVA_OPTS

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_gatling_preamble.sh"

exec sbt "${SBT_GATLING_FLAGS[@]}" "Gatling/testOnly pharmeasy.IndividualUrlsSimulation"
