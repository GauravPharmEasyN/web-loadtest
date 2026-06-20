# PharmEasy Web Load Test

A toolkit combining **Gatling** (Scala) load generation with **Playwright + Lighthouse** (Node.js) for HTTP load tests, smoke checks, and web performance audits on `pharmeasy.in`.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Repository Structure](#repository-structure)
- [How It Works](#how-it-works)
- [Configuration](#configuration)
  - [URLs](#urls)
  - [Environment Variables](#environment-variables)
  - [Cookie & Auth Setup](#cookie--auth-setup)
  - [Debug Logging](#debug-logging)
- [Running Load Tests](#running-load-tests)
  - [Combined URLs](#combined-urls)
  - [Individual URLs](#individual-urls)
  - [JSON API Load Test](#json-api-load-test)
  - [Heavy Load Tips](#heavy-load-tips)
- [Optional: Playwright Smoke + Lighthouse](#optional-playwright-smoke--lighthouse)
- [HTTP Timeouts](#http-timeouts)
- [Understanding Reports](#understanding-reports)
  - [Gatling Metrics](#gatling-metrics)
  - [Lighthouse Metrics](#lighthouse-metrics)
  - [Combined Analysis](#combined-analysis)
- [Common Scenarios](#common-scenarios)
- [Troubleshooting](#troubleshooting)
- [Rendering Diagrams](#rendering-diagrams)

---

## Prerequisites

- Java 11+
- sbt 1.9+
- Node.js 18+ (only for Playwright / Lighthouse steps)

---

## Quick Start

```bash
# Dry run: 2 users, 5 seconds, all 4 API endpoints
API_USERS=2 API_DURATION_SECS=5 ./scripts/run_api_load.sh

# Combined page load: 100 users, 2 minutes
COMBINED_USERS=100 COMBINED_DURATION_SECS=120 ./scripts/run_combined.sh

# Full pipeline: load test + collect JSON + Lighthouse + open reports (macOS)
COMBINED_USERS=2500 COMBINED_DURATION_SECS=300 ./scripts/run_combined.sh && \
./scripts/collect_json.sh && \
(cd playwright && npm i --silent && npm run lh:run && npm run lh:aggregate) && \
open "$(ls -1dt target/gatling/combinedurlssimulation-*/ | head -1)/index.html" \
     "playwright/lighthouse-reports/index.html"
```

---

## Repository Structure

```
web-loadtest/
├── build.sbt
├── conf/
│   └── gatling.conf                   # Gatling defaults (override via -D or env)
├── config/
│   └── pharmeasy-default-cookie.txt   # Default cookie template (edit locally)
├── scripts/
│   ├── _gatling_preamble.sh           # Shared sbt flags + ulimit helper
│   ├── run_api_load.sh                # Run PharmeasyApiSimulation (JSON APIs)
│   ├── run_combined.sh                # Run CombinedUrlsSimulation
│   ├── run_individual.sh              # Run IndividualUrlsSimulation
│   └── collect_json.sh               # Copy latest Gatling JSONs to reports/
├── src/test/scala/pharmeasy/
│   ├── CommonConfig.scala             # URL map, env readers, shared HTTP protocol
│   ├── HeavyLoadHttpProtocol.scala    # Shared connection pool + async DNS tuning
│   ├── RequestDebug.scala             # Per-request debug logging
│   ├── CombinedUrlsSimulation.scala   # Random URL distribution
│   ├── IndividualUrlsSimulation.scala # Per-URL controlled load
│   └── PharmeasyApiSimulation.scala   # JSON API load test (200 RPS default)
├── playwright/
│   ├── tests/smoke.spec.ts
│   ├── lighthouse-run.mjs
│   ├── lighthouse-aggregate.mjs
│   ├── urls.json
│   └── playwright.config.ts
└── docs/
    └── diagrams/                      # Mermaid (.mmd) source files
```

---

## How It Works

```
Virtual Users (Gatling)
        │
        ▼
  pharmeasy.in  ──► Measure response time, track errors
        │
        ▼
  Gatling HTML report  +  JSON stats
        │
        ▼
  Lighthouse audit (optional)  ──► Per-page performance scores
        │
        ▼
  Aggregate dashboard (lighthouse-reports/index.html)
```

Two Gatling simulations cover page-level traffic; one covers JSON APIs:

| Simulation | Script | What it does |
|---|---|---|
| `CombinedUrlsSimulation` | `run_combined.sh` | One scenario; each virtual user hits a random page |
| `IndividualUrlsSimulation` | `run_individual.sh` | One scenario per page; load tunable per page via env |
| `PharmeasyApiSimulation` | `run_api_load.sh` | 4 JSON API endpoints at configurable aggregate RPS |

---

## Configuration

### URLs

Defined in `CommonConfig.scala` as a `ListMap` (insertion order preserved):

```
home, online_medicine, diagnostics, blog, healthcare_category, cart, diag_cart, pdp
```

### Environment Variables

#### Combined simulation
| Variable | Default | Description |
|---|---|---|
| `COMBINED_USERS` | `1` | Total virtual users to ramp |
| `COMBINED_DURATION_SECS` | `2` | Ramp duration in seconds |

#### Individual simulation — one pair per page
| Prefix | Page |
|---|---|
| `HOME` | pharmeasy.in/ |
| `MEDICINE` | /online-medicine-order |
| `DIAG` | /diagnostics |
| `BLOG` | /blog/ |
| `HCAT` | /health-care/… |
| `CART` | /cart |
| `DCART` | /diag-pwa/cart |
| `PDP` | product detail page |

```bash
HOME_USERS=50 HOME_DURATION_SECS=120 \
MEDICINE_USERS=0 DIAG_USERS=0 BLOG_USERS=0 HCAT_USERS=0 CART_USERS=0 DCART_USERS=0 \
./scripts/run_individual.sh
```

#### API simulation
| Variable | Default | Description |
|---|---|---|
| `API_RPS` | `200` | Aggregate RPS across all 4 endpoints |
| `API_DURATION_SECS` | `600` | Test duration in seconds |
| `API_USERS` | unset | If set (positive int): dry-run mode — ramps N users through all 4 APIs once |
| `API_PINCODE` | `400602` | Pincode for fetchPincodeDetails |
| `API_OTC_ID` | `3491142` | Product ID for fetchOtcEdd |

#### JVM & system
| Variable | Example | Description |
|---|---|---|
| `GATLING_JAVA_OPTS` | `-Xmx8g -Xms512m -XX:+UseG1GC` | Appended to Gatling fork JVM options (overrides default `-Xmx1G`) |
| `GATLING_ULIMIT_NO` | `65535` | Raises open-file limit for the shell (handled in `_gatling_preamble.sh`) |
| `GATLING_ASYNC_DNS` | `false` | Set to `false`/`0`/`no`/`off` to disable async DNS (uses JDK resolver instead) |

### Cookie & Auth Setup

Resolution order (highest priority first):

0. `DISABLE_PHARMEASY_COOKIE=1` — sends **no** Cookie header at all.
1. `PHARMEASY_COOKIE` — full cookie string, e.g. `X-Access-Token=…; XdI=…`
2. `X_ACCESS_TOKEN` + optional `XDI` — combined as `X-Access-Token=<token>; XdI=<xdi>`
3. `ACCESS_TOKEN` alone — legacy `accessToken=<value>`
4. First non-comment line of `config/pharmeasy-default-cookie.txt` (repo default; edit locally)

The API simulation also checks `CART_COOKIE` first (highest priority for the cart endpoint only), then falls through to the chain above.

Treat tokens as secrets. Prefer CI-injected env vars over committing live values in `config/pharmeasy-default-cookie.txt`.

```bash
# Example with token
export PHARMEASY_COOKIE='X-Access-Token=7qZ0ifsxDw…; XdI=ffy13v6U5p…'
COMBINED_USERS=100 COMBINED_DURATION_SECS=120 ./scripts/run_combined.sh

# Example with separate vars
export X_ACCESS_TOKEN='7qZ0ifsxDw…'
export XDI='ffy13v6U5p…'
COMBINED_USERS=100 COMBINED_DURATION_SECS=120 ./scripts/run_combined.sh
```

### Debug Logging

Enable per-request line logging (URL + optional Cookie) with any of:

```bash
export GATLING_DEBUG=true          # recommended
export DEBUG=true                  # alternative
# or via JVM flag:
export GATLING_JAVA_OPTS="-Dgatling.request.debug=true"
```

For noisier HTTP-level logs, use the dry-run logback config:

```bash
export GATLING_JAVA_OPTS="-Dlogback.configurationFile=classpath:logback-dryrun.xml"
```

Turn debug **off** at high user counts — log I/O slows the generator.

---

## Running Load Tests

### Combined URLs

One scenario; each virtual user picks a random page from `CommonConfig.urls`.

```bash
COMBINED_USERS=100 COMBINED_DURATION_SECS=120 ./scripts/run_combined.sh
```

Output: `target/gatling/combinedurlssimulation-<timestamp>/index.html`

### Individual URLs

One scenario per page, running in parallel. Control each page's load independently.

```bash
# All pages, different loads
HOME_USERS=20 HOME_DURATION_SECS=60 \
MEDICINE_USERS=10 MEDICINE_DURATION_SECS=60 \
DIAG_USERS=10 DIAG_DURATION_SECS=60 \
BLOG_USERS=5 BLOG_DURATION_SECS=60 \
HCAT_USERS=5 HCAT_DURATION_SECS=60 \
CART_USERS=5 CART_DURATION_SECS=60 \
DCART_USERS=5 DCART_DURATION_SECS=60 \
./scripts/run_individual.sh

# Single page only (set all others to 0)
HOME_USERS=50 HOME_DURATION_SECS=120 \
MEDICINE_USERS=0 DIAG_USERS=0 BLOG_USERS=0 HCAT_USERS=0 CART_USERS=0 DCART_USERS=0 \
./scripts/run_individual.sh
```

### JSON API Load Test

Hits 4 JSON endpoints at a configurable aggregate RPS (default 200 RPS, split evenly — 50 RPS each):

- `GET /api/home/fetchCategories`
- `GET /api/app/fetchPincodeDetails?pincode=<PINCODE>`
- `GET /api/otc/fetchOtcEdd/<OTC_ID>`
- `GET /api/cart/getCartCount` (sends Cookie when set)

```bash
# Dry run: 2 virtual users, 5 seconds
API_USERS=2 API_DURATION_SECS=5 ./scripts/run_api_load.sh

# Full run: 200 RPS for 10 minutes
API_RPS=200 API_DURATION_SECS=600 ./scripts/run_api_load.sh

# With cookie
export CART_COOKIE='X-Access-Token=…'
API_RPS=200 API_DURATION_SECS=600 ./scripts/run_api_load.sh
```

### Heavy Load Tips

The Gatling sbt plugin forks with `-Xmx1G` by default. For large runs:

```bash
# Override heap (this project appends GATLING_JAVA_OPTS to Gatling/javaOptions, overriding the cap)
export GATLING_JAVA_OPTS="-Xmx8g -Xms512m -XX:+UseG1GC -XX:MaxDirectMemorySize=2g"

# Raise open-file limit (or use GATLING_ULIMIT_NO env var instead)
ulimit -n 65535

# Then run
COMBINED_USERS=2500 COMBINED_DURATION_SECS=300 ./scripts/run_combined.sh
```

> **Heap note:** If you set `-Xms`, keep it strictly below `-Xmx`, or omit `-Xms`. Setting `-Xms2g` without a matching `-Xmx` larger than 2g causes *"Initial heap size set to a larger value than the maximum heap size"*.

**Platform guidance:**
- Prefer a dedicated Linux VM (8+ vCPU) in the same region as the target. Laptops hit thermal throttling, Wi-Fi latency, and low default `ulimit` quickly.
- Very high load against **production** can trigger WAF blocks or IP bans. Use staging, coordinate with the team, or split load across multiple VMs.
- Beyond one JVM: use multiple load generators (separate machines, each running a slice), or Gatling Enterprise.

**Peak-style individual run (600s ramp, blog off):**

```bash
export GATLING_JAVA_OPTS="-Xmx8g -Xms512m -XX:+UseG1GC -XX:MaxDirectMemorySize=2g"
GATLING_ULIMIT_NO=65535 \
HOME_USERS=45000 HOME_DURATION_SECS=600 \
MEDICINE_USERS=75000 MEDICINE_DURATION_SECS=600 \
DIAG_USERS=45000 DIAG_DURATION_SECS=600 \
BLOG_USERS=0 BLOG_DURATION_SECS=600 \
HCAT_USERS=75000 HCAT_DURATION_SECS=600 \
CART_USERS=45000 CART_DURATION_SECS=600 \
DCART_USERS=45000 DCART_DURATION_SECS=600 \
PDP_USERS=75000 PDP_DURATION_SECS=600 \
./scripts/run_individual.sh
```

---

## Optional: Playwright Smoke + Lighthouse

### Playwright smoke

```bash
cd playwright
npm i
npm run install-browsers
npm test
```

Config: `playwright/playwright.config.ts`. URLs: `playwright/tests/smoke.spec.ts`.

### Lighthouse reports + dashboard

```bash
cd playwright
npm i
npm run lh:run          # generates HTML + JSON per page into ./lighthouse-reports/
npm run lh:aggregate    # creates ./lighthouse-reports/index.html summary
npm run lh:serve        # optional static server on http://localhost:5600
```

Targets are read from `playwright/urls.json`.

**Chrome binary resolution** (`lighthouse-run.mjs`):
1. `CHROME_PATH` — if set and the file exists, that binary is used.
2. Playwright's bundled Chromium — used when available (`npx playwright install chromium`).
3. Auto-detect — `chrome-launcher` picks a system Chrome/Chromium install.

**Headless mode:**
- Lighthouse: headless by default (`--headless=new`). Use `HEADED=1` or `LIGHTHOUSE_HEADED=1` for a visible window. Use `LIGHTHOUSE_HEADLESS_MODE=old` for the legacy `--headless` flag.
- Playwright tests: headless by default. Use `HEADED=1` for visible browser, `PWDEBUG=1` for the Playwright inspector.

**Include Gatling stats in the Lighthouse dashboard:**

```bash
./scripts/collect_json.sh   # copies js/stats.json → reports/gatling-json/stats.json
cd playwright && npm run lh:aggregate
```

---

## HTTP Timeouts

Project defaults are in `conf/gatling.conf` (`connectTimeout`, `handshakeTimeout`, `http.requestTimeout`, `http.dns.queryTimeout`).

| Symptom | Cause | Fix |
|---|---|---|
| `ConnectTimeoutException after 1000ms` | Default timeout too low for CloudFront multi-IP under concurrency | Set connect + handshake timeout to 10–20s |
| `SslHandshakeTimeoutException` | TLS overload or too many parallel handshakes | Raise `gatling.ssl.handshakeTimeout`; consider `.enableHttp2()` to multiplex |
| `500` responses | Origin/edge errors under load | Reduce `COMBINED_USERS`, lengthen ramp, or use staging |
| `Request timeout after 60000ms` | Slow pages or cold caches | Raise `gatling.http.requestTimeout` in `conf/gatling.conf` |

Override for a single run without editing the config file:

```bash
GATLING_JAVA_OPTS="-Dgatling.socket.connectTimeout=20000 \
  -Dgatling.ssl.handshakeTimeout=20000 \
  -Dgatling.http.requestTimeout=120000" \
  COMBINED_USERS=100 COMBINED_DURATION_SECS=120 ./scripts/run_combined.sh
```

---

## Understanding Reports

### Gatling Metrics

```
Global Information
├── Request Count       Total requests made
├── Response Times
│   ├── Min             Fastest response
│   ├── Max             Slowest response
│   ├── Mean            Average
│   └── Percentiles
│       ├── p50         Median
│       ├── p95         95% of requests faster than this
│       └── p99         Outlier detection
└── Distribution
    ├── t < 800ms       Fast (target: > 90%)
    ├── 800–1200ms      Acceptable
    ├── > 1200ms        Slow
    └── Failed          Errors (target: 0%)
```

**Sample output (2500 users, 300s):**
```
t < 800ms       2269  (91%)
800–1200ms        93   (4%)
t >= 1200ms      138   (6%)
failed             0   (0%)
Mean: 444ms   p95: 1248ms   p99: ~2s
```

**Charts to read:**
- **Response Time Distribution** — is performance clustered or scattered?
- **Active Users over Time** — does the system handle ramp consistently?
- **Response Times Percentiles** — steeper curve = more variability

### Lighthouse Metrics

**Core Web Vitals:**

| Metric | Good | Needs Work | Poor |
|---|---|---|---|
| LCP (Largest Contentful Paint) | < 2.5s | 2.5–4s | > 4s |
| FID (First Input Delay) | < 100ms | 100–300ms | > 300ms |
| CLS (Cumulative Layout Shift) | < 0.1 | 0.1–0.25 | > 0.25 |

**Score categories (0–100):**

| Category | Meaning |
|---|---|
| Performance | Overall speed and responsiveness |
| Accessibility | How well all users can access content |
| Best Practices | Adherence to web standards |
| SEO | Search engine optimization |
| PWA | Progressive Web App capabilities |

### Combined Analysis

```
Gatling shows:          Lighthouse shows:
High response time   →  Poor LCP
Variable times       →  Poor CLS
Slow responses       →  Poor FID

If you see:             Consider:
High response times  →  Server/DB optimization
Poor Lighthouse     →  Frontend optimization
Both are poor       →  Full-stack review
High load + poor LCP → CDN implementation
Spikes + poor CLS   →  Static rendering
```

**Example:**
```
Gatling (2500 users, 300s): 91% < 800ms, 0 failures, mean 444ms  ✅
Lighthouse: Performance 68, LCP 3.1s, FID 320ms                  ⚠️

→ Server handles load well; frontend needs optimization.
  Priority: reduce JavaScript execution time (FID), optimize images (LCP).
```

---

## Common Scenarios

### Baseline → scale-up workflow

```bash
# 1. Baseline
COMBINED_USERS=100 COMBINED_DURATION_SECS=120 ./scripts/run_combined.sh

# 2. Increase
COMBINED_USERS=500 COMBINED_DURATION_SECS=120 ./scripts/run_combined.sh

# 3. Heavy
export GATLING_JAVA_OPTS="-Xmx8g -Xms512m -XX:+UseG1GC"
COMBINED_USERS=2500 COMBINED_DURATION_SECS=300 ./scripts/run_combined.sh
```

### Pre-release check

```bash
rm -rf target/gatling/* reports/run-* reports/gatling-json/*

COMBINED_USERS=2500 COMBINED_DURATION_SECS=300 ./scripts/run_combined.sh
./scripts/collect_json.sh
cd playwright && npm run lh:run && npm run lh:aggregate
```

### API smoke

```bash
API_USERS=2 API_DURATION_SECS=5 ./scripts/run_api_load.sh
```

### Individual page focus

```bash
# Only home page, 50 users
HOME_USERS=50 HOME_DURATION_SECS=120 \
MEDICINE_USERS=0 DIAG_USERS=0 BLOG_USERS=0 HCAT_USERS=0 CART_USERS=0 DCART_USERS=0 \
./scripts/run_individual.sh
```

---

## Troubleshooting

| Problem | Symptom | Check |
|---|---|---|
| High response times | Mean > 1s | DB queries, caching config, server resources |
| Error spikes | KO > 0 | Server logs, network issues, `ulimit -n` too low |
| Poor Lighthouse scores | Performance < 70 | Image sizes, JS bundles, render-blocking resources |
| `sbt` not found | Command not found | Install sbt, ensure it's on PATH |
| Java missing / wrong version | Error on launch | Install JDK 11+ |
| Initial heap > max heap | JVM crash on start | Set `-Xms` below `-Xmx`, or omit `-Xms` |
| Chrome launch error (`pid` TypeError) | Lighthouse fails | Set `CHROME_PATH` or run `npx playwright install chromium` |
| `NO_LCP` trace warning | Lighthouse log | Non-fatal; reports are still written |
| Proxy-restricted env | Connection failures | Set `JAVA_TOOL_OPTIONS` or `-Dhttp.proxyHost`/`-Dhttps.proxyHost` |

---

## Rendering Diagrams

Mermaid source files live in `docs/diagrams/`. To render to SVG:

**VS Code / Cursor:** install "Markdown Preview Mermaid Support" and "Mermaid Markdown Syntax Highlighting" extensions — diagrams render inline in preview.

**Command line:**
```bash
cd docs/diagrams
npm install @mermaid-js/mermaid-cli

npx mmdc -i detailed-flow.mmd -o detailed-flow.svg
npx mmdc -i data-flow.mmd -o data-flow.svg
npx mmdc -i folder-structure.mmd -o folder-structure.svg
```

**Online:** paste `.mmd` content into [mermaid.live](https://mermaid.live) and export.

**Batch script (`docs/diagrams/render.sh`):**
```bash
for file in *.mmd; do
  npx mmdc -i "$file" -o "${file%.mmd}.svg"
done
```

---

## Resources

- [Gatling Documentation](https://gatling.io/docs/current/)
- [Lighthouse Documentation](https://developers.google.com/web/tools/lighthouse)
- [Web Performance Scoring](https://web.dev/performance-scoring/)
