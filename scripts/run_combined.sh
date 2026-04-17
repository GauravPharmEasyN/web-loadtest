#!/usr/bin/env bash
set -euo pipefail

# Usage: COMBINED_USERS=100 COMBINED_DURATION_SECS=120 ./scripts/run_combined.sh
#
# Heavy load (e.g. 2500 users / 300s): raise Gatling fork heap (plugin defaults include -Xmx1G; env appends after).
#   export GATLING_JAVA_OPTS="-Xmx8g -Xms512m -XX:+UseG1GC"   # keep -Xms < -Xmx if you set both
#   ulimit -n 65535
# Prefer a Linux VM in the same region as the target; laptops often hit CPU, RAM, or ephemeral ports first.
#
# Log each outgoing GET (URL + optional Cookie): export GATLING_DEBUG=true  OR  DEBUG=true
#   OR append to GATLING_JAVA_OPTS: -Dgatling.request.debug=true
# (High volume: disable for very large runs to reduce I/O.)

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)

cd "${ROOT_DIR}"

sbt -warn -no-colors -Dsbt.log.noformat=true "Gatling/testOnly pharmeasy.CombinedUrlsSimulation"
