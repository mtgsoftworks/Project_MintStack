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
    // Component uses 'text' prop
    render(<Loading text="Veriler yükleniyor..." />)

    expect(screen.getByText(/Veriler yükleniyor/i)).toBeInTheDocument()
  })

  it('renders fullscreen variant', () => {
    // The component uses 'fullScreen' prop
    // Use 'render' instead of 'renderWithProviders' as it's a simple component
    const { getByRole } = render(<Loading fullScreen={true} />)
    const element = getByRole('status')
    // Check strict class match or substring match
    expect(element.className).toContain('fixed')
    expect(element.className).toContain('inset-0')
  })

  it('renders inline variant', () => {
    const { container } = render(<Loading inline />)

    // Should not have fullscreen styling
    expect(container.firstChild).not.toHaveClass('fixed')
  })
})
