import fs from 'node:fs';
import path from 'node:path';

import { defineConfig, devices } from '@playwright/test';

function truthyEnv(v: string | undefined): boolean {
  if (v == null) return false;
  const t = String(v).trim().toLowerCase();
  return t === '1' || t === 'true' || t === 'yes';
}

function resolveDefaultCookiePath(): string | undefined {
  const candidates = [
    path.join(process.cwd(), 'config', 'pharmeasy-default-cookie.txt'),
    path.join(process.cwd(), '..', 'config', 'pharmeasy-default-cookie.txt')
  ];
  return candidates.find((p) => fs.existsSync(p));
}

function loadDefaultCookieFile(): string | undefined {
  const p = resolveDefaultCookiePath();
  if (!p) return undefined;
  const line = fs
    .readFileSync(p, 'utf8')
    .split(/\r?\n/)
    .map((l) => l.trim())
    .find((l) => l.length > 0 && !l.startsWith('#'));
  return line || undefined;
}

/** Same cookie rules as Gatling `CommonConfig.cookieHeader` / `lighthouse-run.mjs`. */
function buildPharmeasyCookieHeader(): string | undefined {
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

export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  use: {
    baseURL: 'https://pharmeasy.in',
    /** No UI for perf/load-style runs; set HEADED=1 to debug locally. */
    headless: process.env.HEADED !== '1' && process.env.PWDEBUG !== '1',
    trace: 'off',
    actionTimeout: 20_000,
    navigationTimeout: 30_000,
    ignoreHTTPSErrors: true,
    video: 'off',
    ...(pharmeasyCookie ? { extraHTTPHeaders: { Cookie: pharmeasyCookie } } : {})
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  reporter: [['list']]
});
