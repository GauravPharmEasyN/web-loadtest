PharmEasy Web Load Test - Quick Start

Prereqs:
- Java 11+
- sbt 1.9+
- Node.js 18+

Install Playwright browsers (optional, for smoke test):
  cd playwright && npm i && npm run install-browsers

Run individual URL load (env controls per URL):
  cd ..
  HOME_USERS=20 HOME_DURATION_SECS=60 \
  MEDICINE_USERS=10 MEDICINE_DURATION_SECS=60 \
  DIAG_USERS=10 DIAG_DURATION_SECS=60 \
  BLOG_USERS=5 BLOG_DURATION_SECS=60 \
  HCAT_USERS=5 HCAT_DURATION_SECS=60 \
  CART_USERS=5 CART_DURATION_SECS=60 \
  DCART_USERS=5 DCART_DURATION_SECS=60 \
  ./scripts/run_individual.sh

Run combined load across all URLs:
  COMBINED_USERS=100 COMBINED_DURATION_SECS=120 ./scripts/run_combined.sh

Collect JSON from latest Gatling run:
  ./scripts/collect_json.sh
  # Outputs to reports/gatling-json

Notes:
- Scenarios target public, unauthenticated pages only.
- Use CDN migration windows carefully; coordinate with owners.
