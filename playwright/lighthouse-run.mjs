import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import lighthouse from 'lighthouse';
import chromeLauncher from 'chrome-launcher';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const urlsPath = path.join(__dirname, 'urls.json');
const reportsDir = path.join(__dirname, 'lighthouse-reports');

if (!fs.existsSync(reportsDir)) fs.mkdirSync(reportsDir);

const urls = JSON.parse(fs.readFileSync(urlsPath, 'utf-8'));

const run = async () => {
    const chrome = await chromeLauncher.launch({ chromeFlags: ['--headless=new'] });
    // Emit both HTML and JSON so we can aggregate later
    const opts = { port: chrome.port, output: ['html', 'json'] };
    // Force inclusion of PWA category to avoid missing scores in some configs
    const config = {
        extends: 'lighthouse:default',
        settings: {
            onlyCategories: ['performance', 'accessibility', 'best-practices', 'seo', 'pwa']
        }
    };

    try {
        for (const [name, url] of Object.entries(urls)) {
            const result = await lighthouse(url, opts, config);
            const [reportHtml, reportJson] = result.report;
            const htmlPath = path.join(reportsDir, `${name}.html`);
            const jsonPath = path.join(reportsDir, `${name}.json`);
            fs.writeFileSync(htmlPath, reportHtml);
            fs.writeFileSync(jsonPath, typeof reportJson === 'string' ? reportJson : JSON.stringify(reportJson, null, 2));
            console.log(`Saved Lighthouse reports: ${htmlPath} , ${jsonPath}`);
        }
    } finally {
        await chrome.kill();
    }
};

run().catch((e) => {
    console.error(e);
    process.exit(1);
});


