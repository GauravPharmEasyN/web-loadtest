import { defineConfig, devices } from '@playwright/test';

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
    video: 'off'
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  reporter: [['list']]
});
