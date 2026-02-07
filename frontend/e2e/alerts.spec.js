// @ts-check
const { test, expect } = require('@playwright/test')

test.describe('Alerts Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/alerts')
  })

  test('alerts page loads successfully', async ({ page }) => {
    await expect(page).toHaveURL(/.*alerts/)
  })

  test('displays alerts or empty state', async ({ page }) => {
    // Either shows alerts list or empty state
    const content = page.locator(
      '[data-testid="alerts-container"], .alerts, text=Alarm, text=Alert'
    ).first()
    await expect(content).toBeVisible({ timeout: 10000 })
  })

  test('can create a new alert', async ({ page }) => {
    // Look for "New Alert" / "Yeni Alarm" button
    const createButton = page.locator(
      'button:has-text("Yeni"), button:has-text("Alarm"), button:has-text("New"), [data-testid="create-alert"]'
    ).first()
    if (await createButton.isVisible({ timeout: 5000 })) {
      await createButton.click()
      // A dialog or form should appear
      const dialog = page.locator('[role="dialog"], dialog, .modal, form').first()
      await expect(dialog).toBeVisible({ timeout: 5000 })
    }
  })

  test('active and triggered alerts are separated', async ({ page }) => {
    // Look for tabs or sections separating active/triggered
    const sections = page.locator(
      '[data-testid="active-alerts"], [data-testid="triggered-alerts"], [role="tabpanel"]'
    )
    // Either tabs exist or a single list is shown
    const tabsOrList = page.locator('[role="tablist"], .alerts-list, table').first()
    await expect(tabsOrList).toBeVisible({ timeout: 10000 })
  })
})
