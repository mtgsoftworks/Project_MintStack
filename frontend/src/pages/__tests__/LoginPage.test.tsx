import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '../../utils/test-utils'
import LoginPage from '../LoginPage'

vi.mock('../../App', () => ({
  keycloak: {
    init: vi.fn().mockResolvedValue(true),
    login: vi.fn(),
    logout: vi.fn(),
    token: 'mock-token',
    tokenParsed: {
      sub: 'mock-user-id',
      email: 'test@example.com',
      given_name: 'Test',
      family_name: 'User',
      name: 'Test User',
      realm_access: {
        roles: ['user'],
      },
    },
    subject: 'mock-user-id',
    updateToken: vi.fn().mockResolvedValue(true),
    authenticated: true,
  },
}))

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const preloadedState = {
    auth: {
      user: null,
      token: null,
      isAuthenticated: false,
      isInitialized: true,
      loading: false,
      error: null
    }
  }

  it('renders without crashing', async () => {
    const { container } = renderWithProviders(<LoginPage />, { preloadedState })
    await waitFor(() => expect(container).toBeInTheDocument())
  })

  it('renders MintStack branding', async () => {
    renderWithProviders(<LoginPage />, { preloadedState })
    await waitFor(() => expect(screen.getByText(/MintStack/i)).toBeInTheDocument())
  })

  it('renders login button', async () => {
    renderWithProviders(<LoginPage />, { preloadedState })
    await waitFor(() => {
      const buttons = screen.getAllByRole('button')
      expect(buttons.length).toBeGreaterThan(0)
    })
  })
})
