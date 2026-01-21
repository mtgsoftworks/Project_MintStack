import { vi, describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '../../utils/test-utils'
import LoginPage from '../LoginPage'

describe('LoginPage', () => {
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

  it('renders without crashing', () => {
    const { container } = renderWithProviders(<LoginPage />, { preloadedState })
    expect(container).toBeInTheDocument()
  })

  it('renders MintStack branding', () => {
    renderWithProviders(<LoginPage />, { preloadedState })
    expect(screen.getByText(/MintStack/i)).toBeInTheDocument()
  })

  it('renders login button', () => {
    renderWithProviders(<LoginPage />, { preloadedState })
    const buttons = screen.getAllByRole('button')
    expect(buttons.length).toBeGreaterThan(0)
  })
})
