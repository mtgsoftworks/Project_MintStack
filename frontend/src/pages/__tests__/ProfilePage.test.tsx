import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '@/utils/test-utils'
import ProfilePage from '@/pages/ProfilePage'

const mockedHooks = vi.hoisted(() => ({
  useGetProfileQuery: vi.fn(),
  updateProfileMutation: vi.fn(),
}))

vi.mock('@/store/api/userApi', () => ({
  useGetProfileQuery: () => mockedHooks.useGetProfileQuery(),
  useUpdateProfileMutation: () => [mockedHooks.updateProfileMutation, { isLoading: false }],
}))

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    mockedHooks.useGetProfileQuery.mockReturnValue({
      data: {
        firstName: 'Admin',
        lastName: 'User',
        fullName: 'Admin User',
        phoneNumber: '',
        bio: '',
        location: '',
        emailNotifications: true,
        pushNotifications: true,
        priceAlerts: true,
        portfolioUpdates: true,
        compactView: false,
      },
    })

    mockedHooks.updateProfileMutation.mockReturnValue({
      unwrap: () => Promise.resolve({}),
    })
  })

  it('renders profile page', () => {
    renderWithProviders(<ProfilePage />, {
      preloadedState: {
        auth: {
          isAuthenticated: true,
          isInitialized: true,
          token: 'mock-token',
          user: {
            username: 'admin',
            name: 'Admin User',
            email: 'admin@mintstack.local',
          },
          roles: ['admin'],
        },
      },
    })

    expect(screen.getByRole('heading', { name: 'Profil' })).toBeInTheDocument()
  })

  it('reverts toggle state when profile preference update fails', async () => {
    const user = userEvent.setup()
    mockedHooks.updateProfileMutation.mockReturnValueOnce({
      unwrap: () => Promise.reject(new Error('request failed')),
    })

    renderWithProviders(<ProfilePage />, {
      preloadedState: {
        auth: {
          isAuthenticated: true,
          isInitialized: true,
          token: 'mock-token',
          user: {
            username: 'admin',
            name: 'Admin User',
            email: 'admin@mintstack.local',
          },
          roles: ['admin'],
        },
      },
    })

    await user.click(screen.getByRole('tab', { name: 'Bildirimler' }))
    const switches = screen.getAllByRole('switch')
    const firstSwitch = switches[0]

    expect(firstSwitch).toHaveAttribute('aria-checked', 'true')
    await user.click(firstSwitch)

    await waitFor(() => {
      expect(firstSwitch).toHaveAttribute('aria-checked', 'true')
    })
  })
})
