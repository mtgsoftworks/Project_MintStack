import { describe, it, expect, vi } from 'vitest'
import { renderWithProviders } from '../../../utils/test-utils'
import Sidebar from '../Sidebar'

describe('Sidebar', () => {
  it('renders without crashing', () => {
    const { container } = renderWithProviders(<Sidebar />)
    expect(container).toBeInTheDocument()
  })

  it('renders aside element', () => {
    const { container } = renderWithProviders(<Sidebar />)
    const aside = container.querySelector('aside')
    expect(aside).toBeTruthy()
  })

  it('contains navigation links', () => {
    const { container } = renderWithProviders(<Sidebar />)
    const links = container.querySelectorAll('a')
    expect(links.length).toBeGreaterThan(0)
  })
})
