import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '../../utils/test-utils'
import DashboardPage from '../DashboardPage'
import { useGetCurrenciesQuery, useGetStocksQuery } from '../../store/api/marketApi'
import { useGetNewsQuery } from '../../store/api/newsApi'
import { useGetPortfoliosQuery } from '../../store/api/portfolioApi'

// Mock API hooks
vi.mock('../../store/api/marketApi', () => ({
  useGetCurrenciesQuery: vi.fn(),
  useGetStocksQuery: vi.fn(),
}))

vi.mock('../../store/api/newsApi', () => ({
  useGetNewsQuery: vi.fn(),
}))

vi.mock('../../store/api/portfolioApi', () => ({
  useGetPortfoliosQuery: vi.fn(),
}))

// Mock Auth Context (although likely handled by renderWithProviders, explicitly mocking helps if used directly)
// But DashboardPage uses Redux for auth: useSelector(selectIsAuthenticated)
// mockAuthContext is not needed if we rely on Redux state.
// renderWithProviders handles Redux.
// So no need to mock AuthContext hook if it's not used (DashboardPage uses useSelector)

describe('DashboardPage Component', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    // Setup default mock values
    useGetCurrenciesQuery.mockReturnValue({
      data: [
        { currencyCode: 'USD', sellingRate: 32.50, changePercent: 0.5, currencyName: 'US Dollar' },
        { currencyCode: 'EUR', sellingRate: 35.20, changePercent: -0.2, currencyName: 'Euro' }
      ],
      isLoading: false
    })

    useGetStocksQuery.mockReturnValue({
      data: {
        data: [
          { symbol: 'THYAO', name: 'Türk Hava Yolları', currentPrice: 280.50, changePercent: 1.2 }
        ]
      },
      isLoading: false
    })

    useGetNewsQuery.mockReturnValue({
      data: {
        data: [
          { id: 1, title: 'Market News', sourceName: 'Bloomberg', publishedAt: new Date().toISOString() }
        ]
      },
      isLoading: false
    })

    useGetPortfoliosQuery.mockReturnValue({
      data: [
        { totalValue: 1000, profitLoss: 100, totalCost: 900 }
      ],
      isLoading: false
    })
  })

  it('renders dashboard title', () => {
    renderWithProviders(<DashboardPage />)
    expect(screen.getByRole('heading', { level: 1, name: /Dashboard/i })).toBeInTheDocument()
  })

  // Removed welcome message test as it's not present in the component

  it('shows currency rates section', () => {
    renderWithProviders(<DashboardPage />)
    // USD is in stat card and widget
    expect(screen.getAllByText(/USD/i).length).toBeGreaterThan(0)
    expect(screen.getByText(/D.viz Kurlar./i)).toBeInTheDocument()
  })

  it('shows portfolio summary when authenticated', () => {
    const preloadedState = {
      auth: {
        isAuthenticated: true,
        user: { name: 'Test User' },
        isInitialized: true
      }
    }
    renderWithProviders(<DashboardPage />, { preloadedState })
    // Check for Portfolio Value card
    expect(screen.getByText(/Portf.y De.eri/i)).toBeInTheDocument()
    // Check for Total P/L
    expect(screen.getByText(/Toplam K\/Z/i)).toBeInTheDocument()
  })

  it('shows recent news section', () => {
    renderWithProviders(<DashboardPage />)
    expect(screen.getByText(/Son Haberler/i)).toBeInTheDocument()
    // "Tümünü Gör" appears multiple times, check if at least one exists
    expect(screen.getAllByText(/T.m.n. G.r/i).length).toBeGreaterThan(0)
  })

  it('renders without crashing', () => {
    const { container } = renderWithProviders(<DashboardPage />)
    expect(container).toBeInTheDocument()
  })
})
