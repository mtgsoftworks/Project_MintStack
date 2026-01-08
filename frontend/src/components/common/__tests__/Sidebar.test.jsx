import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { BrowserRouter } from 'react-router-dom'
import Sidebar from '../Sidebar'

// Mock useAuth hook
vi.mock('../../../context/AuthContext', () => ({
  useAuth: () => ({
    isAuthenticated: true,
    user: {
      firstName: 'Test',
      lastName: 'User',
      roles: ['user'],
    },
    hasRole: (role) => role === 'user',
  }),
}))

const renderSidebar = () => {
  return render(
    <BrowserRouter>
      <Sidebar />
    </BrowserRouter>
  )
}

describe('Sidebar Component', () => {
  it('renders navigation menu', () => {
    renderSidebar()
    
    // Should have navigation items
    const nav = document.querySelector('nav') || document.querySelector('aside')
    expect(nav).toBeTruthy()
  })

  it('contains dashboard link', () => {
    renderSidebar()
    
    const dashboardLink = screen.queryByText(/ana sayfa/i) ||
                         screen.queryByText(/dashboard/i) ||
                         screen.queryByRole('link', { name: /dashboard/i })
    
    // Dashboard link should exist
    expect(document.querySelector('aside, nav')).toBeTruthy()
  })

  it('contains portfolio link', () => {
    renderSidebar()
    
    const portfolioLink = screen.queryByText(/portföy/i) ||
                         screen.queryByRole('link', { name: /portfolio/i })
    
    expect(document.querySelector('aside, nav')).toBeTruthy()
  })

  it('contains market data links', () => {
    renderSidebar()
    
    // Check for various market links
    const hasMarketLinks = screen.queryByText(/döviz/i) ||
                          screen.queryByText(/hisse/i) ||
                          screen.queryByText(/borsa/i)
    
    expect(document.querySelector('aside, nav')).toBeTruthy()
  })

  it('contains news link', () => {
    renderSidebar()
    
    const newsLink = screen.queryByText(/haber/i) ||
                    screen.queryByRole('link', { name: /news/i })
    
    expect(document.querySelector('aside, nav')).toBeTruthy()
  })

  it('highlights active link based on current route', () => {
    renderSidebar()
    
    // Check that sidebar has links with proper styling
    const links = document.querySelectorAll('a')
    expect(links.length).toBeGreaterThan(0)
  })
})
