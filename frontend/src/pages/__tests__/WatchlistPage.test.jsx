import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '@/utils/test-utils'
import WatchlistPage from '@/pages/WatchlistPage'

vi.mock('@/services/watchlistService', () => ({
  default: {
    getAll: vi.fn(() => Promise.resolve({
      data: [
        {
          id: 1,
          name: 'Ana Takip Listem',
          description: 'Test takip listesi',
          isDefault: true,
          itemCount: 2,
        },
      ],
    })),
    getById: vi.fn(() => Promise.resolve({
      data: {
        id: 1,
        name: 'Ana Takip Listem',
        description: 'Test takip listesi',
        isDefault: true,
        itemCount: 2,
        items: [
          { id: 1, symbol: 'AAPL', name: 'Apple Inc.', type: 'STOCK', currentPrice: 185.50, changePercent: 1.5 },
          { id: 2, symbol: 'GOOGL', name: 'Alphabet Inc.', type: 'STOCK', currentPrice: 140.25, changePercent: -0.5 },
        ],
      },
    })),
    create: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({})),
    addInstrument: vi.fn(() => Promise.resolve({})),
    removeInstrument: vi.fn(() => Promise.resolve({})),
  },
}))

describe('WatchlistPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders without crashing', async () => {
    const { container } = renderWithProviders(<WatchlistPage />)
    await waitFor(() => expect(container).toBeInTheDocument())
  })

  it('displays watchlist name', async () => {
    renderWithProviders(<WatchlistPage />)
    await waitFor(() => expect(screen.getAllByText(/Ana Takip Listem/i).length).toBeGreaterThan(0))
  })
})
