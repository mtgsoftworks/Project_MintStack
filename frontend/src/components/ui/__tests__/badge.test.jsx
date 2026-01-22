import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { Badge } from '../badge'

describe('Badge', () => {
    it('renders children correctly', () => {
        render(<Badge>New</Badge>)
        expect(screen.getByText('New')).toBeInTheDocument()
    })

    it('applies default variant classes', () => {
        render(<Badge data-testid="badge">Default</Badge>)
        expect(screen.getByTestId('badge')).toHaveClass('bg-primary', 'text-primary-foreground')
    })

    it('applies variant classes correctly', () => {
        const { rerender } = render(<Badge variant="secondary" data-testid="badge">Secondary</Badge>)
        expect(screen.getByTestId('badge')).toHaveClass('bg-secondary')

        rerender(<Badge variant="destructive" data-testid="badge">Destructive</Badge>)
        expect(screen.getByTestId('badge')).toHaveClass('bg-destructive')

        rerender(<Badge variant="outline" data-testid="badge">Outline</Badge>)
        expect(screen.getByTestId('badge')).toHaveClass('text-foreground')

        rerender(<Badge variant="success" data-testid="badge">Success</Badge>)
        expect(screen.getByTestId('badge')).toHaveClass('text-success')

        rerender(<Badge variant="warning" data-testid="badge">Warning</Badge>)
        expect(screen.getByTestId('badge')).toHaveClass('text-warning-dark')

        rerender(<Badge variant="danger" data-testid="badge">Danger</Badge>)
        expect(screen.getByTestId('badge')).toHaveClass('text-danger')

        rerender(<Badge variant="info" data-testid="badge">Info</Badge>)
        expect(screen.getByTestId('badge')).toHaveClass('text-info')
    })

    it('applies solid variant classes', () => {
        const { rerender } = render(<Badge variant="success-solid" data-testid="badge">Success</Badge>)
        expect(screen.getByTestId('badge')).toHaveClass('bg-success', 'text-white')

        rerender(<Badge variant="warning-solid" data-testid="badge">Warning</Badge>)
        expect(screen.getByTestId('badge')).toHaveClass('bg-warning')

        rerender(<Badge variant="danger-solid" data-testid="badge">Danger</Badge>)
        expect(screen.getByTestId('badge')).toHaveClass('bg-danger', 'text-white')

        rerender(<Badge variant="info-solid" data-testid="badge">Info</Badge>)
        expect(screen.getByTestId('badge')).toHaveClass('bg-info', 'text-white')
    })

    it('applies base styling classes', () => {
        render(<Badge data-testid="badge">Styled</Badge>)
        const badge = screen.getByTestId('badge')
        expect(badge).toHaveClass('inline-flex', 'items-center', 'rounded-full', 'px-2.5', 'py-0.5', 'text-xs', 'font-semibold')
    })

    it('accepts custom className', () => {
        render(<Badge className="custom-class" data-testid="badge">Custom</Badge>)
        expect(screen.getByTestId('badge')).toHaveClass('custom-class')
    })

    it('passes additional props', () => {
        render(<Badge data-testid="badge" aria-label="status badge">Status</Badge>)
        expect(screen.getByTestId('badge')).toHaveAttribute('aria-label', 'status badge')
    })
})
