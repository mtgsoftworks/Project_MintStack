import { test, expect } from '@playwright/test';

test.describe('Portfolio Page', () => {
  test.beforeEach(async ({ page }) => {
    // Setup: Mock authentication
    await page.addInitScript(() => {
      window.localStorage.setItem('token', 'mock-token');
    });
  });

  test('should display portfolio page', async ({ page }) => {
    await page.goto('/portfolio');
    await expect(page.locator('h1')).toContainText(/Portfolio|Portföy/i);
  });

  test('should show create portfolio button', async ({ page }) => {
    await page.goto('/portfolio');
    const createButton = page.getByRole('button', { name: /new|yeni|create|oluştur/i });
    await expect(createButton).toBeVisible();
  });

  test('should open create portfolio dialog', async ({ page }) => {
    await page.goto('/portfolio');
    await page.getByRole('button', { name: /new|yeni|create|oluştur/i }).click();
    await expect(page.getByRole('dialog')).toBeVisible();
  });
});

test.describe('Portfolio CRUD Operations', () => {
  test('should create a new portfolio', async ({ page }) => {
    await page.goto('/portfolio');
    
    // Click create button
    await page.getByRole('button', { name: /new|yeni/i }).click();
    
    // Fill form
    await page.getByLabel(/name|ad/i).fill('Test Portfolio');
    await page.getByLabel(/description|açıklama/i).fill('Test description');
    
    // Submit
    await page.getByRole('button', { name: /create|oluştur/i }).click();
    
    // Verify creation (toast or portfolio appears)
    await expect(page.locator('text=Test Portfolio')).toBeVisible({ timeout: 5000 });
  });
});
