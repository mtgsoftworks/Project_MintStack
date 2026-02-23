import { test, expect } from '@playwright/test'
import { installApiMocks, expectPageHeading } from './helpers'

test.describe('Admin Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await installApiMocks(page)
  })

  test('admin page renders heading', async ({ page }) => {
    await expectPageHeading(page, '/admin', /admin|y[oö]netim/i)
  })

  test('admin page renders stats and users table shell', async ({ page }) => {
    await page.goto('/admin')
    await expect(page.locator('table').first()).toBeVisible({ timeout: 15000 })
    await expect(page.locator('button').filter({ hasText: /search|ara/i }).first()).toBeVisible()
  })
})
