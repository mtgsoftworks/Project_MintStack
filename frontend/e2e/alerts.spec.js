import { test, expect } from '@playwright/test'
import { installApiMocks, expectPageHeading } from './helpers'

test.describe('Alerts Page', () => {
  test.beforeEach(async ({ page }) => {
    await installApiMocks(page)
  })

  test('alerts page renders heading and table', async ({ page }) => {
    await expectPageHeading(page, '/alerts', /alert|alarm|uyar|alerts\.title/i)
    await expect(page.locator('table').first()).toBeVisible({ timeout: 15000 })
  })

  test('create alert modal opens', async ({ page }) => {
    await page.goto('/alerts')

    const createButton = page.locator('button').filter({
      hasText: /create|olu[sş]tur|ekle|alerts\.create/i,
    }).first()

    await expect(createButton).toBeVisible({ timeout: 15000 })
    await createButton.click()

    await expect(page.locator('div.fixed.inset-0').first()).toBeVisible()
  })
})
