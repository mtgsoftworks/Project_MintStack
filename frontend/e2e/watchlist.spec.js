import { test, expect } from '@playwright/test'
import { installApiMocks, expectPageHeading } from './helpers'

test.describe('Watchlist Page', () => {
  test.beforeEach(async ({ page }) => {
    await installApiMocks(page)
  })

  test('watchlist page renders heading', async ({ page }) => {
    await expectPageHeading(page, '/watchlist')
  })

  test('create watchlist modal opens', async ({ page }) => {
    await page.goto('/watchlist')

    const createButton = page.locator('button').filter({
      hasText: /create|new|yeni|olu[sş]tur|watchlist\.create/i,
    }).first()

    await expect(createButton).toBeVisible({ timeout: 15000 })
    await createButton.click()

    await expect(page.locator('div.fixed.inset-0').first()).toBeVisible()
  })
})
