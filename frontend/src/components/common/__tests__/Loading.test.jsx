import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import Loading from '../Loading'

describe('Loading Component', () => {
  it('renders loading spinner', () => {
    render(<Loading />)
    
    // Check for loading indicator
    const loadingElement = screen.getByRole('status') || document.querySelector('.animate-spin')
    expect(loadingElement || document.querySelector('[class*="animate"]')).toBeTruthy()
  })

  it('renders with custom message', () => {
    render(<Loading message="Veriler yükleniyor..." />)
    
    expect(screen.getByText(/yükleniyor/i)).toBeInTheDocument()
  })

  it('renders fullscreen variant', () => {
    const { container } = render(<Loading fullscreen />)
    
    // Check for fullscreen styling
    expect(container.firstChild).toHaveClass(/fixed|inset|min-h-screen/i)
  })

  it('renders inline variant', () => {
    const { container } = render(<Loading inline />)
    
    // Should not have fullscreen styling
    expect(container.firstChild).not.toHaveClass('fixed')
  })
})
