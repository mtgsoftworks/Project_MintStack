import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { Skeleton } from '../skeleton'

describe('Skeleton', () => {
    it('renders without crashing', () => {
        render(<Skeleton data-testid="skeleton" />)
        expect(screen.getByTestId('skeleton')).toBeInTheDocument()
    })

    it('applies default animation and styling classes', () => {
        render(<Skeleton data-testid="skeleton" />)
        const skeleton = screen.getByTestId('skeleton')
        expect(skeleton).toHaveClass('animate-pulse', 'rounded-md', 'bg-muted')
    })

    it('accepts custom className', () => {
        render(<Skeleton className="h-4 w-32" data-testid="skeleton" />)
        const skeleton = screen.getByTestId('skeleton')
        expect(skeleton).toHaveClass('h-4', 'w-32')
    })

    it('passes additional props', () => {
        render(<Skeleton data-testid="skeleton" aria-label="loading content" role="status" />)
        const skeleton = screen.getByTestId('skeleton')
        expect(skeleton).toHaveAttribute('aria-label', 'loading content')
        expect(skeleton).toHaveAttribute('role', 'status')
    })

    it('can render with different dimensions', () => {
        const { rerender } = render(<Skeleton className="h-8 w-full" data-testid="skeleton" />)
        expect(screen.getByTestId('skeleton')).toHaveClass('h-8', 'w-full')

        rerender(<Skeleton className="h-12 w-12 rounded-full" data-testid="skeleton" />)
        expect(screen.getByTestId('skeleton')).toHaveClass('h-12', 'w-12', 'rounded-full')
    })

    it('can be used for text placeholder', () => {
        render(
            <div>
                <Skeleton className="h-4 w-48" data-testid="skeleton-text" />
                <Skeleton className="h-4 w-36" data-testid="skeleton-text-short" />
            </div>
        )
        expect(screen.getByTestId('skeleton-text')).toHaveClass('w-48')
        expect(screen.getByTestId('skeleton-text-short')).toHaveClass('w-36')
    })

    it('can be used for avatar placeholder', () => {
        render(<Skeleton className="h-10 w-10 rounded-full" data-testid="skeleton-avatar" />)
        const skeleton = screen.getByTestId('skeleton-avatar')
        expect(skeleton).toHaveClass('h-10', 'w-10', 'rounded-full')
    })

    it('can be used for card placeholder', () => {
        render(<Skeleton className="h-48 w-full rounded-lg" data-testid="skeleton-card" />)
        const skeleton = screen.getByTestId('skeleton-card')
        expect(skeleton).toHaveClass('h-48', 'w-full', 'rounded-lg')
    })
})
