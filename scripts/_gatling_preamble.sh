# Sourced by run_individual.sh / run_combined.sh after SCRIPT_DIR is set.
# shellcheck shell=bash

ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)
cd "${ROOT_DIR}"

# Optional: raise open-file soft limit for this shell (and child sbt/Gatling).
# Example: GATLING_ULIMIT_NO=1048576 ./scripts/run_individual.sh
if [[ -n "${GATLING_ULIMIT_NO:-}" ]]; then
  if ! ulimit -n "${GATLING_ULIMIT_NO}" 2>/dev/null; then
    echo "warning: could not ulimit -n ${GATLING_ULIMIT_NO} (try lower value or raise hard limit)" >&2
  fi
fi

# -batch: non-interactive, exit when done (no sbt shell).
# -Dsbt.supershell=false: less terminal redraw / CPU during long runs.
SBT_GATLING_FLAGS=(-batch -warn -no-colors -Dsbt.log.noformat=true -Dsbt.supershell=false)
