import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import lighthouse from 'lighthouse';
import chromeLauncher from 'chrome-launcher';
import { chromium } from 'playwright';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const urlsPath = path.join(__dirname, 'urls.json');
const reportsDir = path.join(__dirname, 'lighthouse-reports');

if (!fs.existsSync(reportsDir)) fs.mkdirSync(reportsDir);

const urls = JSON.parse(fs.readFileSync(urlsPath, 'utf-8'));

function truthyEnv(v) {
    if (v == null) return false;
    const t = String(v).trim().toLowerCase();
    return t === '1' || t === 'true' || t === 'yes';
}

/** First non-# line from repo `config/pharmeasy-default-cookie.txt` (same as Gatling). */
function loadDefaultCookieFile() {
    const p = path.join(__dirname, '..', 'config', 'pharmeasy-default-cookie.txt');
    if (!fs.existsSync(p)) return undefined;
    const line = fs
        .readFileSync(p, 'utf8')
        .split(/\r?\n/)
        .map((l) => l.trim())
        .find((l) => l.length > 0 && !l.startsWith('#'));
    return line || undefined;
}

/** Same rules as `CommonConfig.cookieHeader` (Gatling). */
function buildPharmeasyCookieHeader() {
    if (truthyEnv(process.env.DISABLE_PHARMEASY_COOKIE)) return undefined;
    const raw = process.env.PHARMEASY_COOKIE?.trim();
    if (raw) return raw;
    const xt = process.env.X_ACCESS_TOKEN?.trim();
    const xdi = process.env.XDI?.trim();
    if (xt && xdi) return `X-Access-Token=${xt}; XdI=${xdi}`;
    if (xt) return `X-Access-Token=${xt}`;
    const legacy = process.env.ACCESS_TOKEN?.trim();
    if (legacy) return `accessToken=${legacy}`;
    return loadDefaultCookieFile();
}

const pharmeasyCookie = buildPharmeasyCookieHeader();
const lighthouseExtraHeaders = pharmeasyCookie ? { Cookie: pharmeasyCookie } : undefined;

/** Prefer CHROME_PATH, then Playwright's Chromium if present; else let chrome-launcher find Chrome/Chromium. */
function resolveChromePath() {
    const fromEnv = process.env.CHROME_PATH?.trim();
    if (fromEnv && fs.existsSync(fromEnv)) return fromEnv;

    try {
        if (typeof chromium?.executablePath === 'function') {
            const p = chromium.executablePath();
            if (p && fs.existsSync(p)) return p;
        }
    } catch {
        // Playwright browsers missing or executablePath not usable in this environment
    }
    return undefined;
}

const run = async () => {
    const chromePath = resolveChromePath();
    const headed = process.env.HEADED === '1' || process.env.LIGHTHOUSE_HEADED === '1';
    /** Default headless Chromium for Lighthouse; set HEADED=1 to open a real window for debugging. */
    const headlessFlag =
        process.env.LIGHTHOUSE_HEADLESS_MODE === 'old'
            ? '--headless'
            : '--headless=new';
    const chrome = await chromeLauncher.launch({
        ...(chromePath ? { chromePath } : {}),
        chromeFlags: [
            ...(headed ? [] : [headlessFlag]),
            '--disable-gpu',
            '--no-sandbox',
            '--disable-dev-shm-usage',
            '--disable-extensions',
            '--no-first-run',
            '--remote-allow-origins=*'
        ]
    });
    // Emit both HTML and JSON so we can aggregate later
    const opts = { port: chrome.port, output: ['html', 'json'] };
    // Force inclusion of PWA category to avoid missing scores in some configs
    const config = {
        extends: 'lighthouse:default',
        settings: {
            onlyCategories: ['performance', 'accessibility', 'best-practices', 'seo', 'pwa'],
            // Be a bit more forgiving on slower pages
            maxWaitForFcp: 45000,
            maxWaitForLoad: 90000,
            formFactor: 'desktop',
            screenEmulation: { mobile: false, width: 1366, height: 768, deviceScaleFactor: 1, disabled: false },
            ...(lighthouseExtraHeaders ? { extraHeaders: lighthouseExtraHeaders } : {})
        }
    };

    try {
        for (const [name, url] of Object.entries(urls)) {
            console.log(`Running Lighthouse for "${name}" -> ${url}`);
            let attempt = 0;
            let lastError = null;
            while (attempt < 2) {
                try {
                    const result = await lighthouse(url, opts, config);
                    const [reportHtml, reportJson] = result.report;
                    const htmlPath = path.join(reportsDir, `${name}.html`);
                    const jsonPath = path.join(reportsDir, `${name}.json`);
                    fs.writeFileSync(htmlPath, reportHtml);
                    fs.writeFileSync(jsonPath, typeof reportJson === 'string' ? reportJson : JSON.stringify(reportJson, null, 2));
                    console.log(`Saved Lighthouse reports: ${htmlPath} , ${jsonPath}`);
                    lastError = null;
                    break;
                } catch (e) {
                    lastError = e;
                    const msg = (e && e.message) ? e.message : String(e);
                    const isTargetClosed = msg.includes('Target closed') || msg.includes('Protocol error');
                    attempt += 1;
                    if (attempt < 2 && isTargetClosed) {
                        console.warn(`Lighthouse failed for "${name}" (attempt ${attempt}) with "${msg}". Retrying once...`);
                        await new Promise(r => setTimeout(r, 1500));
                        continue;
                    }
                    console.error(`Lighthouse failed for "${name}" after ${attempt} attempt(s): ${msg}`);
                    break;
                }
            }
            if (lastError) {
                // continue with next URL instead of aborting whole batch
                continue;
            }
        }
    } finally {
        await chrome.kill();
    }
};

run().catch((e) => {
    console.error(e);
    process.exit(1);
});


