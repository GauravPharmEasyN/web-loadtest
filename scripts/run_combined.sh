#!/usr/bin/env bash
set -euo pipefail

# Usage: COMBINED_USERS=100 COMBINED_DURATION_SECS=120 ./scripts/run_combined.sh

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)

cd "${ROOT_DIR}"

sbt -no-colors -Dsbt.log.noformat=true "Gatling/testOnly pharmeasy.CombinedUrlsSimulation"
