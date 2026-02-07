import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '@/utils/test-utils'
import WatchlistPage from '@/pages/WatchlistPage'

vi.mock('@/store/api/watchlistApi', () => ({
  useGetWatchlistsQuery: vi.fn(() => ({
    data: [
      {
        id: '1',
        name: 'Ana Takip Listem',
        description: 'Test takip listesi',
        isDefault: true,
        itemCount: 2,
        items: [
          { id: '1', symbol: 'AAPL', name: 'Apple Inc.', type: 'STOCK', currentPrice: 185.50 },
          { id: '2', symbol: 'GOOGL', name: 'Alphabet Inc.', type: 'STOCK', currentPrice: 140.25 },
        ],
      },
    ],
    isLoading: false,
    error: null,
  })),
  useCreateWatchlistMutation: vi.fn(() => [vi.fn(), { isLoading: false }]),
  useDeleteWatchlistMutation: vi.fn(() => [vi.fn(), { isLoading: false }]),
  useAddToWatchlistMutation: vi.fn(() => [vi.fn(), { isLoading: false }]),
  useRemoveFromWatchlistMutation: vi.fn(() => [vi.fn(), { isLoading: false }]),
}))

describe('WatchlistPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders without crashing', () => {
    const { container } = renderWithProviders(<WatchlistPage />)
    expect(container).toBeInTheDocument()
  })

  it('displays watchlist name', () => {
    renderWithProviders(<WatchlistPage />)
    expect(screen.getByText(/Ana Takip Listem/i)).toBeInTheDocument()
  })
})
