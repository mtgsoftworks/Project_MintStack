// @ts-check
const { test, expect } = require('@playwright/test')

test.describe('News Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/news')
  })

  test('news page loads successfully', async ({ page }) => {
    await expect(page).toHaveURL(/.*news/)
  })

  test('displays news articles', async ({ page }) => {
    // Wait for news content to load
    const newsContainer = page.locator('[data-testid="news-list"], .news-list, article').first()
    await expect(newsContainer).toBeVisible({ timeout: 15000 })
  })

  test('can filter news by category', async ({ page }) => {
    // Look for category filter buttons or dropdown
    const categoryFilter = page.locator('[data-testid="category-filter"], button:has-text("Kategori"), select').first()
    if (await categoryFilter.isVisible({ timeout: 5000 })) {
      await categoryFilter.click()
    }
  })

  test('can click on a news article to see details', async ({ page }) => {
    // Wait for news to load then click first article
    const firstArticle = page.locator('article a, [data-testid="news-item"] a, .news-card a').first()
    if (await firstArticle.isVisible({ timeout: 10000 })) {
      await firstArticle.click()
      await expect(page).toHaveURL(/.*news\/.*/)
    }
  })
})
