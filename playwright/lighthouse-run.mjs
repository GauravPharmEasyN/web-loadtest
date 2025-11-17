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

const run = async () => {
    const chromePath = typeof chromium?.executablePath === 'function' ? chromium.executablePath() : undefined;
    const chrome = await chromeLauncher.launch({
        chromePath,
        chromeFlags: [
            '--headless',                 // avoid '=new' which can be unstable on some Chrome builds
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
            screenEmulation: { mobile: false, width: 1366, height: 768, deviceScaleFactor: 1, disabled: false }
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


