import { expect } from '@playwright/test'

const jsonHeaders = {
  'content-type': 'application/json',
}

const buildMockGetResponse = (url) => {
  if (url.includes('/admin/dashboard')) {
    return {
      success: true,
      data: {
        totalUsers: 0,
        activeUsers: 0,
        totalPortfolios: 0,
        activeAlerts: 0,
        totalInstruments: 0,
        totalWatchlists: 0,
      },
    }
  }

  if (url.includes('/admin/users')) {
    return {
      success: true,
      data: {
        content: [],
        totalPages: 0,
      },
    }
  }

  if (url.includes('/portfolios/summary')) {
    return {
      success: true,
      data: {
        totalValue: 0,
        totalProfitLoss: 0,
        totalCost: 0,
      },
    }
  }

  if (url.includes('/simulation/config')) {
    return {
      success: true,
      data: {
        enabled: false,
        volatilityLevel: 'MEDIUM',
        marketTrend: 'NEUTRAL',
        updateIntervalSeconds: 5,
        enableRandomEvents: false,
        enableMarketHours: false,
      },
    }
  }

  if (url.includes('/simulation/status')) {
    return {
      success: true,
      data: {
        enabled: false,
        stockCount: 0,
        currencyCount: 0,
        indexCount: 0,
        tickCount: 0,
      },
    }
  }

  if (url.includes('/news')) {
    return {
      success: true,
      data: [],
      totalPages: 0,
    }
  }

  return {
    success: true,
    data: [],
  }
}

export async function installApiMocks(page) {
  await page.route('**/api/v1/**', async (route) => {
    const method = route.request().method()
    const url = route.request().url()

    if (method === 'GET') {
      await route.fulfill({
        status: 200,
        headers: jsonHeaders,
        body: JSON.stringify(buildMockGetResponse(url)),
      })
      return
    }

    await route.fulfill({
      status: 200,
      headers: jsonHeaders,
      body: JSON.stringify({ success: true, data: {} }),
    })
  })
}

export async function expectPageHeading(page, path, headingPattern) {
  await page.goto(path)
  const pageHeading = page.locator('main h1').first()
  await expect(pageHeading).toBeVisible({ timeout: 15000 })
  if (headingPattern) {
    await expect(pageHeading).toContainText(headingPattern)
  }
}
