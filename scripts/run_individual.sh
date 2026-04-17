#!/usr/bin/env bash
set -euo pipefail
# Optional for large runs: export GATLING_JAVA_OPTS="-Xmx8g -Xms512m -XX:+UseG1GC" && ulimit -n 65535
# Log each GET: export GATLING_DEBUG=true  OR  GATLING_JAVA_OPTS="$GATLING_JAVA_OPTS -Dgatling.request.debug=true"

# Usage: HOME_USERS=20 HOME_DURATION_SECS=60 MEDICINE_USERS=10 ... ./scripts/run_individual.sh

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)

cd "${ROOT_DIR}"

sbt -warn -no-colors -Dsbt.log.noformat=true "Gatling/testOnly pharmeasy.IndividualUrlsSimulation"
