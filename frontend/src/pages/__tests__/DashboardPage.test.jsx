import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '@/utils/test-utils'
import DashboardPage from '@/pages/DashboardPage'

vi.mock('@/store/api/marketApi', () => ({
  useGetCurrenciesQuery: vi.fn(() => ({
    data: [{ currencyCode: 'USD', sellingRate: 32.50, changePercent: 0.5 }],
    isLoading: false
  })),
  useGetStocksQuery: vi.fn(() => ({ data: { data: [] }, isLoading: false })),
  useGetMarketIndexQuery: vi.fn(() => ({ data: { data: { currentPrice: 8000 } }, isLoading: false })),
}))

vi.mock('@/store/api/newsApi', () => ({
  useGetNewsQuery: vi.fn(() => ({ data: { data: [] }, isLoading: false })),
}))

vi.mock('@/store/api/portfolioApi', () => ({
  useGetPortfoliosQuery: vi.fn(() => ({ data: [], isLoading: false })),
}))

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders without crashing', () => {
    const { container } = renderWithProviders(<DashboardPage />)
    expect(container).toBeInTheDocument()
  })

  it('renders page heading', () => {
    renderWithProviders(<DashboardPage />)
    const heading = screen.getByRole('heading', { level: 1 })
    expect(heading).toBeInTheDocument()
  })

  it('shows USD currency', () => {
    renderWithProviders(<DashboardPage />)
    expect(screen.getAllByText(/USD/i).length).toBeGreaterThan(0)
  })
})
