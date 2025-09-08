import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const reportsDir = path.join(__dirname, 'lighthouse-reports');
const outPath = path.join(reportsDir, 'index.html');

const files = fs.readdirSync(reportsDir).filter(f => f.endsWith('.json'));

// Try to read latest Gatling stats collected into reports/gatling-json/stats.json
function loadGatlingMeans() {
  try {
    // Primary location populated by ./scripts/collect_json.sh
    const fixedStats = path.resolve(__dirname, '../../reports/gatling-json/stats.json');
    let candidate = fs.existsSync(fixedStats) ? fixedStats : null;

    // Fallback: try latest reports/run-*/gatling/js/stats.json
    if (!candidate) {
      const reportsRoot = path.resolve(__dirname, '../../reports');
      if (fs.existsSync(reportsRoot)) {
        const runDirs = fs.readdirSync(reportsRoot).filter((d) => d.startsWith('run-'));
        runDirs.sort((a, b) => {
          try {
            const aTime = fs.statSync(path.join(reportsRoot, a)).mtimeMs;
            const bTime = fs.statSync(path.join(reportsRoot, b)).mtimeMs;
            return bTime - aTime;
          } catch (_) {
            return 0;
          }
        });
        for (const dir of runDirs) {
          const p = path.join(reportsRoot, dir, 'gatling', 'js', 'stats.json');
          if (fs.existsSync(p)) { candidate = p; break; }
        }
      }
    }

    if (!candidate || !fs.existsSync(candidate)) return {};

    const json = JSON.parse(fs.readFileSync(candidate, 'utf-8'));
    const means = {};
    const contents = json.contents || {};
    for (const key of Object.keys(contents)) {
      const item = contents[key];
      const name = item && item.stats && item.stats.name; // e.g., "GET - home"
      const meanOk = item && item.stats && item.stats.meanResponseTime && item.stats.meanResponseTime.ok;
      if (name && typeof meanOk === 'number') {
        const m = name.match(/^GET\s+-\s+(.*)$/);
        const pageKey = m ? m[1] : name;
        means[pageKey] = meanOk; // milliseconds
      }
    }
    return means;
  } catch (_) {
    return {};
  }
}

const gatlingMeansByPage = loadGatlingMeans();

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf-8'));
}

function percent(score) {
  if (score == null) return '—';
  const s = typeof score === 'number' && score <= 1 ? score * 100 : score;
  return `${Math.round(s)}`;
}

const rows = [];
for (const f of files) {
  const json = readJson(path.join(reportsDir, f));
  const url = json.requestedUrl || json.finalUrl;
  const name = path.basename(f, '.json');
  const cat = json.categories || {};
  const audits = json.audits || {};

  // Derive a PWA score if the category exists but score is null by checking key audits
  let pwaScore = cat.pwa && typeof cat.pwa.score === 'number' ? cat.pwa.score : null;
  if (pwaScore == null) {
    const installable = audits['installable-manifest'] && audits['installable-manifest'].score === 1;
    const serviceWorker = audits['service-worker'] && audits['service-worker'].score === 1;
    if (installable || serviceWorker) {
      // Simple heuristic: both pass => 100, one passes => 50
      pwaScore = (installable && serviceWorker) ? 1 : 0.5;
    }
  }

  rows.push({
    name,
    url,
    performance: percent(cat.performance && cat.performance.score),
    accessibility: percent(cat.accessibility && cat.accessibility.score),
    bestPractices: percent(cat['best-practices'] && cat['best-practices'].score),
    seo: percent(cat.seo && cat.seo.score),
    pwa: percent(pwaScore),
    lcp: audits['largest-contentful-paint'] && audits['largest-contentful-paint'].displayValue || '—',
    fid: (audits['max-potential-fid'] && audits['max-potential-fid'].displayValue) || '—',
    inp: (audits['interaction-to-next-paint'] && audits['interaction-to-next-paint'].displayValue) ||
      (audits['experimental-interaction-to-next-paint'] && audits['experimental-interaction-to-next-paint'].displayValue) || '—',
    cls: audits['cumulative-layout-shift'] && audits['cumulative-layout-shift'].displayValue || '—',
    tti: audits['interactive'] && audits['interactive'].displayValue || '—',
    tbt: audits['total-blocking-time'] && audits['total-blocking-time'].displayValue || '—',
    gmean: (gatlingMeansByPage[name] != null ? String(gatlingMeansByPage[name]) : '—')
  });
}

// Optional: enrich INP from CrUX (PageSpeed Insights) if PSI_API_KEY is set
async function enrichInpFromFieldData() {
  const apiKey = process.env.PSI_API_KEY;
  if (!apiKey) return;
  for (const r of rows) {
    try {
      if (!r.url || (r.inp && r.inp !== '—')) continue;
      const endpoint = `https://www.googleapis.com/pagespeedonline/v5/runPagespeed?url=${encodeURIComponent(r.url)}&category=performance&strategy=mobile&key=${apiKey}`;
      const res = await fetch(endpoint);
      if (!res.ok) continue;
      const data = await res.json();
      const metrics = (data && data.loadingExperience && data.loadingExperience.metrics) || {};
      const inp = (metrics.INTERACTION_TO_NEXT_PAINT && metrics.INTERACTION_TO_NEXT_PAINT.percentile)
        || (metrics.EXPERIMENTAL_INTERACTION_TO_NEXT_PAINT && metrics.EXPERIMENTAL_INTERACTION_TO_NEXT_PAINT.percentile);
      if (typeof inp === 'number') r.inp = `${inp} ms`;
    } catch (_) {
      // ignore
    }
  }
}

// Wrap generation to allow async enrichment
(async () => {
  await enrichInpFromFieldData();

  const html = `<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Lighthouse Summary</title>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css" />
  <style>
    body { padding: 1rem; }
    table { font-size: 14px; }
    th, td { white-space: nowrap; }
    td.url { max-width: 420px; white-space: normal; }
    .table-wrap { overflow-x: auto; }
  </style>
</head>
<body>
  <main class="container">
    <h2>Lighthouse Summary</h2>
    <p>Generated from ${files.length} report(s). Click a page name to open the full HTML report.</p>
    <div class="table-wrap">
    <table>
      <thead>
        <tr>
          <th>Page</th>
          <th class="url">URL</th>
          <th>Perf</th>
          <th>Acc</th>
          <th>Best</th>
          <th>SEO</th>
          <th>PWA</th>
          <th>LCP</th>
          <th>FID</th>
          <th>INP</th>
          <th>CLS</th>
          <th>TTI</th>
          <th>TBT</th>
          <th>Gatling mean (ms)</th>
        </tr>
      </thead>
      <tbody>
        ${rows.map(r => `
          <tr>
            <td><a href="./${r.name}.html" target="_blank" rel="noreferrer">${r.name}</a></td>
            <td class="url">${r.url || ''}</td>
            <td>${r.performance}</td>
            <td>${r.accessibility}</td>
            <td>${r.bestPractices}</td>
            <td>${r.seo}</td>
            <td>${r.pwa}</td>
            <td>${r.lcp}</td>
            <td>${r.fid}</td>
            <td>${r.inp}</td>
            <td>${r.cls}</td>
            <td>${r.tti}</td>
            <td>${r.tbt}</td>
            <td>${r.gmean}</td>
          </tr>
        `).join('')}
      </tbody>
    </table>
    </div>
  </main>
</body>
</html>`;

  fs.writeFileSync(outPath, html);
  console.log(`Wrote dashboard: ${outPath}`);
})();
