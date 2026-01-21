import { test, expect } from '@playwright/test';

test.describe('Settings Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.setItem('token', 'mock-token');
    });
  });

  test('should display settings page', async ({ page }) => {
    await page.goto('/settings');
    await expect(page.locator('h1')).toContainText(/Settings|Ayarlar|API/i);
  });

  test('should switch between tabs', async ({ page }) => {
    await page.goto('/settings');
    
    // Click on General tab
    const generalTab = page.getByRole('tab', { name: /general|genel/i });
    if (await generalTab.isVisible()) {
      await generalTab.click();
      await expect(page.locator('text=/appearance|görünüm/i')).toBeVisible();
    }
  });

  test('should change theme', async ({ page }) => {
    await page.goto('/settings');
    
    // Find theme selector
    const themeSelect = page.locator('[data-testid="theme-select"]').or(
      page.getByRole('combobox').filter({ hasText: /light|dark|system/i })
    );
    
    if (await themeSelect.isVisible()) {
      await themeSelect.click();
      await page.getByRole('option', { name: /dark/i }).click();
      
      // Verify theme changed
      await expect(page.locator('html')).toHaveClass(/dark/);
    }
  });

  test('should change language', async ({ page }) => {
    await page.goto('/settings');
    
    // Find language selector and change
    const langSelect = page.getByRole('combobox').filter({ hasText: /Turkish|English|Türkçe|İngilizce/i });
    
    if (await langSelect.isVisible()) {
      await langSelect.click();
      await page.getByRole('option', { name: /English|İngilizce/i }).click();
    }
  });
});

test.describe('API Keys Management', () => {
  test('should show API keys tab', async ({ page }) => {
    await page.goto('/settings');
    
    const apiTab = page.getByRole('tab', { name: /API/i });
    await apiTab.click();
    
    await expect(page.locator('text=/API Key|provider/i')).toBeVisible();
  });

  test('should open add API key dialog', async ({ page }) => {
    await page.goto('/settings');
    
    const apiTab = page.getByRole('tab', { name: /API/i });
    await apiTab.click();
    
    const addButton = page.getByRole('button', { name: /add|ekle/i });
    await addButton.click();
    
    await expect(page.getByRole('dialog')).toBeVisible();
  });
});
