import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '@/utils/test-utils'
import AdminDashboard from '@/pages/AdminDashboard'

vi.mock('@/store/api/adminApi', () => ({
  useGetAdminDashboardQuery: vi.fn(() => ({
    data: {
      totalUsers: 150,
      totalPortfolios: 320,
      totalInstruments: 500,
      totalAlerts: 75,
      activeUsers: 45,
      recentUsers: [],
    },
    isLoading: false,
    error: null,
  })),
  useGetUsersQuery: vi.fn(() => ({
    data: { data: [], pagination: { totalElements: 0 } },
    isLoading: false,
  })),
}))

describe('AdminDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders without crashing', () => {
    const { container } = renderWithProviders(<AdminDashboard />)
    expect(container).toBeInTheDocument()
  })
})
