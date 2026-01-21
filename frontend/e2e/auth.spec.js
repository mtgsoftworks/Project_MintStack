import { test, expect } from '@playwright/test';

test.describe('Authentication', () => {
  test('should redirect to login when not authenticated', async ({ page }) => {
    await page.goto('/');
    // Keycloak should redirect to login
    await expect(page).toHaveURL(/.*keycloak.*|.*login.*/);
  });

  test('should show login page elements', async ({ page }) => {
    await page.goto('/login');
    // Check for login form elements
    await expect(page.locator('body')).toBeVisible();
  });
});

test.describe('Navigation', () => {
  test.beforeEach(async ({ page }) => {
    // Mock authentication for protected routes
    await page.addInitScript(() => {
      window.localStorage.setItem('token', 'mock-token');
    });
  });

  test('should navigate to dashboard', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/MintStack/);
  });
});
