#!/usr/bin/env bash
set -euo pipefail

# Requires mmdc (mermaid-cli) to be installed:
# npm install -g @mermaid-js/mermaid-cli

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

for f in "${SCRIPT_DIR}"/*.mmd; do
  base=$(basename "$f" .mmd)
  mmdc -i "$f" -o "${SCRIPT_DIR}/${base}.svg" -t dark -b transparent
  mmdc -i "$f" -o "${SCRIPT_DIR}/${base}.png" -t dark -b transparent
done
