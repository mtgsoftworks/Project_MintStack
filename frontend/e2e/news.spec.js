import { test, expect } from '@playwright/test'
import { installApiMocks, expectPageHeading } from './helpers'

test.describe('News Page', () => {
  test.beforeEach(async ({ page }) => {
    await installApiMocks(page)
  })

  test('news page renders heading', async ({ page }) => {
    await expectPageHeading(page, '/news', /haber|news/i)
  })

  test('search input and category tabs are visible', async ({ page }) => {
    await page.goto('/news')
    await expect(page.getByRole('textbox').first()).toBeVisible({ timeout: 15000 })
    await expect(page.getByRole('tab').first()).toBeVisible()
  })
})
