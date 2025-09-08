import { test, expect } from '@playwright/test';

const urls = [
  'https://pharmeasy.in/',
  'https://pharmeasy.in/online-medicine-order?src=homecard',
  'https://pharmeasy.in/diagnostics',
  'https://pharmeasy.in/blog/',
  'https://pharmeasy.in/health-care/9066?src=homecard',
  'https://pharmeasy.in/cart?src=header',
  'https://pharmeasy.in/diag-pwa/cart'
];

test.describe('PharmEasy smoke navigation', () => {
  for (const url of urls) {
    test(`loads ${url}`, async ({ page }) => {
      const response = await page.goto(url, { waitUntil: 'domcontentloaded' });
      expect(response?.status()).toBeGreaterThanOrEqual(200);
      expect(response?.status()).toBeLessThan(400);
      await expect(page).toHaveTitle(/PharmEasy|Medicine|Healthcare|Lab Tests|Cart/i);
    });
  }
});
