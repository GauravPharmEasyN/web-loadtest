import { fileURLToPath } from 'node:url';
import { dirname, join, resolve } from 'node:path';
import fs from 'node:fs';
import { createRequire } from 'node:module';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const diagramsDir = join(__dirname, 'diagrams');
const imagesDir = join(__dirname, 'images');

// Resolve Playwright from this repo even if this script lives outside the package
const resolvePlaywright = () => {
    const requireFromDocs = createRequire(import.meta.url);
    try {
        return requireFromDocs('playwright');
    } catch (_) {
        // Try from sibling 'playwright' workspace
        const siblingPkgJson = new URL('../playwright/package.json', import.meta.url);
        const requireFromSibling = createRequire(siblingPkgJson);
        return requireFromSibling('playwright');
    }
};
const playwright = resolvePlaywright();

if (!fs.existsSync(imagesDir)) fs.mkdirSync(imagesDir, { recursive: true });

const files = [
    { svg: 'high-level.svg', png: 'high-level.png', width: 900, height: 800 },
    { svg: 'combined.svg', png: 'combined.png', width: 1000, height: 600 },
    { svg: 'individual.svg', png: 'individual.png', width: 1100, height: 700 },
];

const renderSvgToPng = async (browser, inPath, outPath, width, height) => {
    const context = await browser.newContext({ viewport: { width, height }, deviceScaleFactor: 2 });
    const page = await context.newPage();
    const dataUrl = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(fs.readFileSync(inPath, 'utf-8'));
    await page.setContent(`<html><body style="margin:0;display:flex;align-items:center;justify-content:center;background:#ffffff;">
    <img id="img" src="${dataUrl}" style="max-width:100%;max-height:100%;" />
  </body></html>`, { waitUntil: 'load' });
    const img = await page.$('#img');
    await img.screenshot({ path: outPath });
    await context.close();
};

const run = async () => {
    const browser = await playwright.chromium.launch();
    try {
        for (const f of files) {
            const inPath = resolve(diagramsDir, f.svg);
            const outPath = resolve(imagesDir, f.png);
            await renderSvgToPng(browser, inPath, outPath, f.width, f.height);
            console.log(`Rendered ${f.svg} -> images/${f.png}`);
        }
    } finally {
        await browser.close();
    }
};

run().catch((e) => { console.error(e); process.exit(1); });


