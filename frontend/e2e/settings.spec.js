import { test, expect } from '@playwright/test'
import { installApiMocks, expectPageHeading } from './helpers'

test.describe('Settings Page', () => {
  test.beforeEach(async ({ page }) => {
    await installApiMocks(page)
  })

  test('settings page renders heading and tabs', async ({ page }) => {
    await expectPageHeading(page, '/settings', /setting|ayar|api|settings\.apiKeys\.title/i)
    await expect(page.getByRole('tab').first()).toBeVisible({ timeout: 15000 })
  })

  test('simulation tab can be selected', async ({ page }) => {
    await page.goto('/settings')

    const simulationTab = page.getByRole('tab').filter({
      hasText: /sim|simulation|settings\.simulation/i,
    }).first()

    await expect(simulationTab).toBeVisible({ timeout: 15000 })
  })
})

test.describe('API Keys Management', () => {
  test.beforeEach(async ({ page }) => {
    await installApiMocks(page)
  })

  test('add API key dialog opens', async ({ page }) => {
    await page.goto('/settings')

    const addButton = page.locator('button').filter({
      hasText: /add|ekle|settings\.apiKeys\.add/i,
    }).first()

    await expect(addButton).toBeVisible({ timeout: 15000 })
    await addButton.click()

    await expect(page.getByRole('dialog').first()).toBeVisible()
  })
})
