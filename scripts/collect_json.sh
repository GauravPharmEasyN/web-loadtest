#!/usr/bin/env bash
set -euo pipefail

# Collect Gatling run JSONs (global_stats.json, requests.json if present)
# Usage: ./scripts/collect_json.sh [GATLING_RESULTS_DIR]

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)
DEST_DIR="${ROOT_DIR}/reports/gatling-json"
RESULTS_DIR="${1:-${ROOT_DIR}/target/gatling}"

mkdir -p "${DEST_DIR}"

latest=$(ls -1t "${RESULTS_DIR}" | head -n1 || true)
if [[ -z "${latest}" ]]; then
  echo "No Gatling results found in ${RESULTS_DIR}" >&2
  exit 1
fi

run_dir="${RESULTS_DIR}/${latest}"

# Copy json files if present
shopt -s nullglob
for f in "${run_dir}"/*.json; do
  cp -f "$f" "${DEST_DIR}/"
  echo "Copied $(basename "$f")"
done

# Also copy stats.json if exists
if [[ -f "${run_dir}/js/stats.json" ]]; then
  cp -f "${run_dir}/js/stats.json" "${DEST_DIR}/stats.json"
  echo "Copied stats.json"
fi

echo "Collected JSONs to ${DEST_DIR}"
