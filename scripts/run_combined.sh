#!/usr/bin/env bash
set -euo pipefail

# Usage: COMBINED_USERS=100 COMBINED_DURATION_SECS=120 ./scripts/run_combined.sh
#
# Heavy load (plugin fork defaults include -Xmx1G; env appends after in build.sbt):
#   export GATLING_JAVA_OPTS="-Xmx8g -Xms512m -XX:+UseG1GC -XX:MaxDirectMemorySize=2g"
#   GATLING_ULIMIT_NO=1048576 ./scripts/run_combined.sh
# Prefer a Linux VM in the same region as the target; laptops often hit CPU, RAM, or FD limits first.
#
# Log each outgoing GET: GATLING_DEBUG=true  OR  DEBUG=true
#   OR add to GATLING_JAVA_OPTS: -Dgatling.request.debug=true
# (Disable for very large runs to reduce I/O.)

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_gatling_preamble.sh"

exec sbt "${SBT_GATLING_FLAGS[@]}" "Gatling/testOnly pharmeasy.CombinedUrlsSimulation"
