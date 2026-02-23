import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '@/utils/test-utils'
import AdminDashboard from '@/pages/AdminDashboard'

vi.mock('@/services/adminService', () => ({
  default: {
    getDashboard: vi.fn(() => Promise.resolve({
      data: {
        totalUsers: 150,
        totalPortfolios: 320,
        totalInstruments: 500,
        activeAlerts: 75,
        totalWatchlists: 200,
        activeUsers: 45,
        recentUsers: [],
      },
    })),
    getUsers: vi.fn(() => Promise.resolve({
      data: { content: [], totalElements: 0 },
    })),
    searchUsers: vi.fn(() => Promise.resolve({
      data: { content: [], totalElements: 0 },
    })),
    activateUser: vi.fn(() => Promise.resolve({})),
    deactivateUser: vi.fn(() => Promise.resolve({})),
  },
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
