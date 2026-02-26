import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '@/utils/test-utils'
import AdminDashboard from '@/pages/AdminDashboard'

vi.mock('@/store/api/adminApi', () => ({
  useGetAdminDashboardQuery: vi.fn(() => ({
    data: {
      totalUsers: 150,
      totalPortfolios: 320,
      totalInstruments: 500,
      activeAlerts: 75,
      totalWatchlists: 200,
      activeUsers: 45,
      recentUsers: [],
    },
    isLoading: false,
    refetch: vi.fn(),
  })),
  useGetAdminUsersQuery: vi.fn(() => ({
    data: { content: [], totalElements: 0 },
    isFetching: false,
    refetch: vi.fn(),
  })),
  useSearchAdminUsersQuery: vi.fn(() => ({
    data: { content: [], totalElements: 0 },
    isFetching: false,
    refetch: vi.fn(),
  })),
  useActivateAdminUserMutation: vi.fn(() => [vi.fn(), { isLoading: false }]),
  useDeactivateAdminUserMutation: vi.fn(() => [vi.fn(), { isLoading: false }]),
}))

describe('AdminDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders without crashing', async () => {
    const { container } = renderWithProviders(<AdminDashboard />)
    await waitFor(() => expect(container).toBeInTheDocument())
  })

  it('displays stats after loading', async () => {
    renderWithProviders(<AdminDashboard />)
    await waitFor(() => expect(screen.getByText('150')).toBeInTheDocument())
  })
})
