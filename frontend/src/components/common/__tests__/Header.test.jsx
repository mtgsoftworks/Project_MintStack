import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '../../../utils/test-utils'
import Header from '../Header'

vi.mock('../../../context/AuthContext', () => ({
  useAuth: () => ({
    isAuthenticated: true,
    user: { firstName: 'Test', lastName: 'User', email: 'test@test.com' },
    logout: vi.fn(),
    hasRole: () => true,
  }),
}))

describe('Header', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders without crashing', () => {
    const { container } = renderWithProviders(<Header />)
    expect(container).toBeInTheDocument()
  })

  it('renders header element', () => {
    renderWithProviders(<Header />)
    expect(screen.getByRole('banner')).toBeInTheDocument()
  })

  it('has search input', () => {
    renderWithProviders(<Header />)
    const searchInput = screen.queryByPlaceholderText(/ara|search/i)
    expect(searchInput).toBeInTheDocument()
  })
})
