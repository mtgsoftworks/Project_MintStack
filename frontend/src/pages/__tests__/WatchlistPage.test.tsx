import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '@/utils/test-utils'
import WatchlistPage from '@/pages/WatchlistPage'

vi.mock('@/store/api/watchlistApi', () => ({
  useGetWatchlistsQuery: vi.fn(() => ({
    data: [
      {
        id: 1,
        name: 'Ana Takip Listem',
        description: 'Test takip listesi',
        isDefault: true,
        itemCount: 2,
      },
    ],
    isLoading: false,
  })),
  useGetWatchlistQuery: vi.fn(() => ({
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
    isLoading: false,
  })),
  useCreateWatchlistMutation: vi.fn(() => [vi.fn(), { isLoading: false }]),
  useDeleteWatchlistMutation: vi.fn(() => [vi.fn(), { isLoading: false }]),
  useRemoveWatchlistInstrumentMutation: vi.fn(() => [vi.fn(), { isLoading: false }]),
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
