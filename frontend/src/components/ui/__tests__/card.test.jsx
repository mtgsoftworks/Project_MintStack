import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '../card'

describe('Card Components', () => {
    describe('Card', () => {
        it('renders children correctly', () => {
            render(<Card>Card Content</Card>)
            expect(screen.getByText('Card Content')).toBeInTheDocument()
        })

        it('applies default classes', () => {
            render(<Card data-testid="card">Content</Card>)
            const card = screen.getByTestId('card')
            expect(card).toHaveClass('rounded-xl', 'border', 'bg-card', 'shadow-sm')
        })

        it('accepts custom className', () => {
            render(<Card className="custom-class" data-testid="card">Content</Card>)
            expect(screen.getByTestId('card')).toHaveClass('custom-class')
        })

        it('forwards ref correctly', () => {
            const ref = vi.fn()
            render(<Card ref={ref}>Content</Card>)
            expect(ref).toHaveBeenCalled()
        })
    })

    describe('CardHeader', () => {
        it('renders children correctly', () => {
            render(<CardHeader>Header Content</CardHeader>)
            expect(screen.getByText('Header Content')).toBeInTheDocument()
        })

        it('applies default classes', () => {
            render(<CardHeader data-testid="header">Content</CardHeader>)
            expect(screen.getByTestId('header')).toHaveClass('flex', 'flex-col', 'p-6')
        })
    })

    describe('CardTitle', () => {
        it('renders as h3 element', () => {
            render(<CardTitle>Title</CardTitle>)
            const title = screen.getByRole('heading', { level: 3 })
            expect(title).toHaveTextContent('Title')
        })

        it('applies default classes', () => {
            render(<CardTitle data-testid="title">Title</CardTitle>)
            expect(screen.getByTestId('title')).toHaveClass('text-lg', 'font-semibold')
        })
    })

    describe('CardDescription', () => {
        it('renders children correctly', () => {
            render(<CardDescription>Description text</CardDescription>)
            expect(screen.getByText('Description text')).toBeInTheDocument()
        })

        it('applies muted text style', () => {
            render(<CardDescription data-testid="desc">Description</CardDescription>)
            expect(screen.getByTestId('desc')).toHaveClass('text-sm', 'text-muted-foreground')
        })
    })

    describe('CardContent', () => {
        it('renders children correctly', () => {
            render(<CardContent>Content area</CardContent>)
            expect(screen.getByText('Content area')).toBeInTheDocument()
        })

        it('applies padding classes', () => {
            render(<CardContent data-testid="content">Content</CardContent>)
            expect(screen.getByTestId('content')).toHaveClass('p-6', 'pt-0')
        })
    })

    describe('CardFooter', () => {
        it('renders children correctly', () => {
            render(<CardFooter>Footer content</CardFooter>)
            expect(screen.getByText('Footer content')).toBeInTheDocument()
        })

        it('applies flex layout', () => {
            render(<CardFooter data-testid="footer">Footer</CardFooter>)
            expect(screen.getByTestId('footer')).toHaveClass('flex', 'items-center', 'p-6')
        })
    })

    describe('Card Composition', () => {
        it('renders complete card with all subcomponents', () => {
            render(
                <Card data-testid="full-card">
                    <CardHeader>
                        <CardTitle>Card Title</CardTitle>
                        <CardDescription>Card description text</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <p>Main content goes here</p>
                    </CardContent>
                    <CardFooter>
                        <button>Action</button>
                    </CardFooter>
                </Card>
            )

            expect(screen.getByRole('heading', { name: 'Card Title' })).toBeInTheDocument()
            expect(screen.getByText('Card description text')).toBeInTheDocument()
            expect(screen.getByText('Main content goes here')).toBeInTheDocument()
            expect(screen.getByRole('button', { name: 'Action' })).toBeInTheDocument()
        })
    })
})
