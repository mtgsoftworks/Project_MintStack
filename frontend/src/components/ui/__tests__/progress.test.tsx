import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { Progress } from '../progress'

describe('Progress', () => {
    it('renders without crashing', () => {
        render(<Progress value={50} />)
        expect(screen.getByRole('progressbar')).toBeInTheDocument()
    })

    it('renders as a progressbar element', () => {
        render(<Progress value={75} />)
        const progressbar = screen.getByRole('progressbar')
        expect(progressbar).toBeInTheDocument()
    })

    it('applies default styling classes', () => {
        render(<Progress value={50} data-testid="progress" />)
        const progress = screen.getByTestId('progress')
        expect(progress).toHaveClass('relative', 'h-4', 'w-full', 'overflow-hidden', 'rounded-full')
    })

    it('applies custom className', () => {
        render(<Progress value={50} className="custom-progress" data-testid="progress" />)
        expect(screen.getByTestId('progress')).toHaveClass('custom-progress')
    })

    it('renders with 0% progress', () => {
        render(<Progress value={0} />)
        expect(screen.getByRole('progressbar')).toBeInTheDocument()
    })

    it('renders with 100% progress', () => {
        render(<Progress value={100} />)
        expect(screen.getByRole('progressbar')).toBeInTheDocument()
    })

    it('handles undefined value gracefully', () => {
        render(<Progress />)
        expect(screen.getByRole('progressbar')).toBeInTheDocument()
    })
})
