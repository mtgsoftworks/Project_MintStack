import { test, expect } from '@playwright/test'
import { installApiMocks } from './helpers'

const authBypassEnabled = process.env.VITE_E2E_BYPASS_AUTH !== 'false'

test.describe('Authentication Flow', () => {
  test.beforeEach(async ({ page }) => {
    await installApiMocks(page)
  })

  test('login route is reachable', async ({ page }) => {
    await page.goto('/login')
    await expect(page).toHaveURL(/\/(login)?$/)
    await expect(page.locator('html')).toBeVisible()
  })

  test('protected route behavior matches auth mode', async ({ page }) => {
    await page.goto('/portfolio')

    if (authBypassEnabled) {
      await expect(page).toHaveURL(/\/portfolio$/)
      await expect(page.locator('main h1').first()).toBeVisible({ timeout: 15000 })
      return
    }

    await expect(page).toHaveURL(/\/login$/)
  })
})
