#!/usr/bin/env bash
set -euo pipefail

# Usage: HOME_USERS=20 HOME_DURATION_SECS=60 MEDICINE_USERS=10 ... ./scripts/run_individual.sh

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)

cd "${ROOT_DIR}"

sbt -no-colors -Dsbt.log.noformat=true "Gatling/testOnly pharmeasy.IndividualUrlsSimulation"
