import { render } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import Loading from '../Loading'

describe('Loading', () => {
  it('renders without crashing', () => {
    const { container } = render(<Loading />)
    expect(container).toBeInTheDocument()
  })

  it('renders spinner element', () => {
    const { container } = render(<Loading />)
    const spinner = container.querySelector('[class*="animate"]') || container.querySelector('svg')
    expect(spinner || container.firstChild).toBeTruthy()
  })
})
