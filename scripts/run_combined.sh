#!/usr/bin/env bash
set -euo pipefail

# Usage: COMBINED_USERS=100 COMBINED_DURATION_SECS=120 ./scripts/run_combined.sh
#
# Heavy load (e.g. 2500 users / 300s): give the forked Gatling JVM more heap and raise open files.
#   export GATLING_JAVA_OPTS="-Xmx8g -Xms2g -XX:+UseG1GC"
#   ulimit -n 65535
# Prefer a Linux VM in the same region as the target; laptops often hit CPU, RAM, or ephemeral ports first.

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)

cd "${ROOT_DIR}"

sbt -no-colors -Dsbt.log.noformat=true "Gatling/testOnly pharmeasy.CombinedUrlsSimulation"
