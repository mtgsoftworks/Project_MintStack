// @ts-check
const { test, expect } = require('@playwright/test')

test.describe('Watchlist Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/watchlist')
  })

  test('watchlist page loads successfully', async ({ page }) => {
    await expect(page).toHaveURL(/.*watchlist/)
  })

  test('displays watchlist or empty state', async ({ page }) => {
    // Either shows watchlist items or an empty state message
    const content = page.locator(
      '[data-testid="watchlist-container"], .watchlist, text=Takip Listesi, text=Watchlist'
    ).first()
    await expect(content).toBeVisible({ timeout: 10000 })
  })

  test('can create a new watchlist', async ({ page }) => {
    // Look for "New Watchlist" / "Yeni Liste" button
    const createButton = page.locator(
      'button:has-text("Yeni"), button:has-text("Oluştur"), button:has-text("New"), [data-testid="create-watchlist"]'
    ).first()
    if (await createButton.isVisible({ timeout: 5000 })) {
      await createButton.click()
      // A dialog or form should appear
      const dialog = page.locator('[role="dialog"], dialog, .modal, form').first()
      await expect(dialog).toBeVisible({ timeout: 5000 })
    }
  })
})
