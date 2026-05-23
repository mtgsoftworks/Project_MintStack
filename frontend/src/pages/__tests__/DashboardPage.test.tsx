import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '@/utils/test-utils'
import DashboardPage from '@/pages/DashboardPage'

vi.mock('@/store/api/marketApi', () => ({
  useGetCurrenciesQuery: vi.fn(() => ({
    data: [
      { currencyCode: 'USD', currencyName: 'US Dollar', sellingRate: 32.50, changePercent: 0.5 },
      { currencyCode: 'EUR', currencyName: 'Euro', sellingRate: 35.20, changePercent: 0.3 },
    ],
    isLoading: false,
    error: null,
  })),
  useGetStocksQuery: vi.fn(() => ({
    data: {
      data: [
        { symbol: 'THYAO', name: 'Türk Hava Yolları', currentPrice: 280.50, changePercent: 2.0 },
      ],
    },
    isLoading: false,
  })),
  useGetMarketIndexQuery: vi.fn(() => ({
    data: { data: { currentPrice: 8000, changePercent: 1.5 } },
    isLoading: false,
    error: null,
  })),
  useRefreshMarketDataMutation: vi.fn(() => [
    vi.fn(() => ({
      unwrap: vi.fn().mockResolvedValue({}),
    })),
    { isLoading: false },
  ]),
}))

vi.mock('@/store/api/newsApi', () => ({
  useGetNewsQuery: vi.fn(() => ({
    data: {
      data: [
        { id: 1, title: 'Test News', sourceName: 'Test', publishedAt: new Date().toISOString() },
      ],
    },
    isLoading: false,
  })),
}))

vi.mock('@/store/api/portfolioApi', () => ({
  useGetPortfoliosQuery: vi.fn(() => ({
    data: [
      { id: 1, name: 'Test Portfolio', totalValue: 10000, profitLoss: 500, totalCost: 9500 },
    ],
    isLoading: false,
  })),
}))

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders without crashing', async () => {
    const { container } = renderWithProviders(<DashboardPage />)
    await waitFor(() => expect(container).toBeInTheDocument())
  })

  it('renders page heading', async () => {
    renderWithProviders(<DashboardPage />)
    await waitFor(() => {
      const heading = screen.getByRole('heading', { level: 1 })
      expect(heading).toBeInTheDocument()
    })
  })

  it('shows USD currency', async () => {
    renderWithProviders(<DashboardPage />)
    await waitFor(() => {
      expect(screen.getAllByText(/USD/i).length).toBeGreaterThan(0)
    })
  })
})
