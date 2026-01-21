import { test, expect } from '@playwright/test';

test.describe('Market Pages', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.setItem('token', 'mock-token');
    });
  });

  test('should display stocks page', async ({ page }) => {
    await page.goto('/market/stocks');
    await expect(page.locator('h1')).toContainText(/Stocks|Hisse/i);
  });

  test('should display currency page', async ({ page }) => {
    await page.goto('/market/currencies');
    await expect(page.locator('h1')).toContainText(/Currency|Döviz|Kur/i);
  });

  test('should search stocks', async ({ page }) => {
    await page.goto('/market/stocks');
    
    const searchInput = page.getByPlaceholder(/search|ara/i);
    if (await searchInput.isVisible()) {
      await searchInput.fill('THYAO');
      await page.waitForTimeout(500); // Debounce
      
      // Results should filter
      const table = page.locator('table');
      await expect(table).toBeVisible();
    }
  });

  test('should paginate stocks', async ({ page }) => {
    await page.goto('/market/stocks');
    
    const nextButton = page.getByRole('button', { name: /next|ileri|→/i });
    if (await nextButton.isVisible() && await nextButton.isEnabled()) {
      await nextButton.click();
      // URL or page state should change
      await page.waitForTimeout(500);
    }
  });
});

test.describe('Bonds Page', () => {
  test('should display bonds page', async ({ page }) => {
    await page.goto('/market/bonds');
    await expect(page.locator('h1')).toContainText(/Bonds|Tahvil/i);
  });
});

test.describe('Funds Page', () => {
  test('should display funds page', async ({ page }) => {
    await page.goto('/market/funds');
    await expect(page.locator('h1')).toContainText(/Funds|Fon/i);
  });
});

test.describe('VIOP Page', () => {
  test('should display VIOP page', async ({ page }) => {
    await page.goto('/market/viop');
    await expect(page.locator('h1')).toContainText(/VIOP|VİOP/i);
  });
});
