import { test, expect } from '@playwright/test'
import { installApiMocks, expectPageHeading } from './helpers'

test.describe('Portfolio Page', () => {
  test.beforeEach(async ({ page }) => {
    await installApiMocks(page)
  })

  test('portfolio page renders heading', async ({ page }) => {
    await expectPageHeading(page, '/portfolio', /portfolio|portf|portfolioPage\.title/i)
  })

  test('create portfolio dialog opens', async ({ page }) => {
    await page.goto('/portfolio')

    const createButton = page.locator('button').filter({
      hasText: /new|yeni|create|olu[sş]tur|portfolioPage\.new/i,
    }).first()

    await expect(createButton).toBeVisible({ timeout: 15000 })
    await createButton.click()

    await expect(page.getByRole('dialog').first()).toBeVisible()
  })
})
