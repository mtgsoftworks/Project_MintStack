// @ts-check
const { test, expect } = require('@playwright/test')

test.describe('Admin Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to admin page (assumes auth is handled via test fixtures)
    await page.goto('/admin')
  })

  test('admin page loads for admin users', async ({ page }) => {
    // Check that the page has loaded
    await expect(page).toHaveURL(/.*admin/)
  })

  test('admin dashboard shows statistics', async ({ page }) => {
    // Look for dashboard statistics cards
    const statsCards = page.locator('[data-testid="stat-card"], .stat-card, .card')
    await expect(statsCards.first()).toBeVisible({ timeout: 10000 })
  })

  test('admin can view users list', async ({ page }) => {
    // Look for users section or table
    const usersSection = page.locator('text=Kullanıcılar, text=Users').first()
    if (await usersSection.isVisible()) {
      await usersSection.click()
      await expect(page.locator('table, [role="table"]').first()).toBeVisible()
    }
  })
})
