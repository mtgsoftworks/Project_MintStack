import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { BrowserRouter } from 'react-router-dom'
import LoginPage from '../LoginPage'

// Mock useAuth hook
const mockLogin = vi.fn()

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    isAuthenticated: false,
    isLoading: false,
    login: mockLogin,
    user: null,
  }),
}))

const renderLoginPage = () => {
  return render(
    <BrowserRouter>
      <LoginPage />
    </BrowserRouter>
  )
}

describe('LoginPage Component', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders login page with branding', () => {
    renderLoginPage()
    
    // Check for MintStack branding
    expect(screen.getByText(/MintStack/i)).toBeInTheDocument()
  })

  it('renders login button', () => {
    renderLoginPage()
    
    const loginButton = screen.getByRole('button', { name: /giriş/i }) ||
                       screen.getByText(/keycloak/i)
    
    expect(loginButton).toBeInTheDocument()
  })

  it('calls login function when button clicked', () => {
    renderLoginPage()
    
    const loginButton = screen.getByRole('button', { name: /giriş/i }) ||
                       screen.getByText(/keycloak ile giriş/i)
    
    fireEvent.click(loginButton)
    
    expect(mockLogin).toHaveBeenCalled()
  })

  it('displays welcome message', () => {
    renderLoginPage()
    
    expect(screen.getByText(/hoş geldiniz/i)).toBeInTheDocument()
  })

  it('displays feature highlights', () => {
    renderLoginPage()
    
    // Check for feature descriptions
    const hasFeatures = screen.queryByText(/gerçek zamanlı/i) ||
                       screen.queryByText(/güvenli/i) ||
                       screen.queryByText(/analiz/i)
    
    expect(hasFeatures).toBeTruthy()
  })

  it('has registration link', () => {
    renderLoginPage()
    
    const registerLink = screen.queryByText(/kayıt/i) ||
                        screen.queryByText(/hesap/i)
    
    expect(registerLink).toBeTruthy()
  })

  it('displays terms and privacy links', () => {
    renderLoginPage()
    
    const termsLink = screen.queryByText(/kullanım şartları/i) ||
                     screen.queryByText(/gizlilik/i)
    
    expect(termsLink).toBeTruthy()
  })
})
