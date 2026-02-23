import { test } from '@playwright/test'
import { installApiMocks, expectPageHeading } from './helpers'

test.describe('Market Pages', () => {
  test.beforeEach(async ({ page }) => {
    await installApiMocks(page)
  })

  test('stocks page renders', async ({ page }) => {
    await expectPageHeading(page, '/market/stocks', /stock|hisse|stocksPage\.title/i)
  })

  test('currency page renders', async ({ page }) => {
    await expectPageHeading(page, '/market/currencies', /currency|d[oö]viz|kur|currencyPage\.title/i)
  })

  test('bonds page renders', async ({ page }) => {
    await expectPageHeading(page, '/market/bonds', /bond|tahvil|bondsPage\.title/i)
  })

  test('funds page renders', async ({ page }) => {
    await expectPageHeading(page, '/market/funds', /fund|fon|fundsPage\.title/i)
  })

  test('viop page renders', async ({ page }) => {
    await expectPageHeading(page, '/market/viop')
  })
})
