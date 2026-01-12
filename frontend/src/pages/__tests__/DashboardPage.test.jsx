import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '@/utils/test-utils'
import DashboardPage from '@/pages/DashboardPage'
import { useGetCurrenciesQuery, useGetStocksQuery, useGetMarketIndexQuery } from '@/store/api/marketApi'
import { useGetNewsQuery } from '@/store/api/newsApi'
import { useGetPortfoliosQuery } from '@/store/api/portfolioApi'

// Mock API hooks using alias paths to match component imports
vi.mock('@/store/api/marketApi', () => ({
  useGetCurrenciesQuery: vi.fn(),
  useGetStocksQuery: vi.fn(),
  useGetMarketIndexQuery: vi.fn(),
}))

vi.mock('@/store/api/newsApi', () => ({
  useGetNewsQuery: vi.fn(),
}))

vi.mock('@/store/api/portfolioApi', () => ({
  useGetPortfoliosQuery: vi.fn(),
}))

// Mock react-i18next
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key) => {
      if (key === 'nav.home') return 'Dashboard'
      if (key === 'dashboard.subtitle') return 'Dashboard Subtitle'
      if (key === 'dashboard.widgets.bist100.title') return 'BIST 100'
      if (key === 'dashboard.widgets.bist100.noProvider') return '-'
      if (key === 'dashboard.widgets.bist100.error') return 'Error'
      return key
    },
    i18n: {
      changeLanguage: () => new Promise(() => { }),
    },
  }),
  initReactI18next: {
    type: '3rdParty',
    init: () => { },
  },
}))

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

    useGetMarketIndexQuery.mockReturnValue({
      data: {
        data: { currentPrice: 8000.00, changePercent: 1.5 }
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

  it('shows currency rates section', () => {
    renderWithProviders(<DashboardPage />)
    // USD is in stat card and widget
    expect(screen.getAllByText(/USD/i).length).toBeGreaterThan(0)
    expect(screen.getByText(/D.viz Kurlar./i)).toBeInTheDocument()
  })

  it('shows portfolio summary when authenticated', async () => {
    const preloadedState = {
      auth: {
        isAuthenticated: true,
        user: { name: 'Test User' },
        isInitialized: true,
        token: 'fake-token',
        roles: ['user']
      }
    }
    renderWithProviders(<DashboardPage />, { preloadedState })

    // Use findByText to wait for potential async rendering or state updates
    expect(await screen.findByText(/Portf.y De.eri/i)).toBeInTheDocument()
    // Check for Total P/L
    expect(await screen.findByText(/Toplam K\/Z/i)).toBeInTheDocument()
  })

  it('shows recent news section', () => {
    renderWithProviders(<DashboardPage />)
    expect(screen.getByText(/Son Haberler/i)).toBeInTheDocument()
    expect(screen.getAllByText(/T.m.n. G.r/i).length).toBeGreaterThan(0)
  })

  it('renders without crashing', () => {
    const { container } = renderWithProviders(<DashboardPage />)
    expect(container).toBeInTheDocument()
  })
})
