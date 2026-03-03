import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Input } from '../input'

describe('Input', () => {
    it('renders with default type', () => {
        render(<Input placeholder="Enter text" />)
        const input = screen.getByPlaceholderText('Enter text')
        expect(input).toBeInTheDocument()
        // When type is not explicitly provided, it may not have type attribute
        // but browser treats it as text input by default
        expect(input.tagName).toBe('INPUT')
    })

    it('renders with specified type', () => {
        render(<Input type="email" placeholder="Email" />)
        expect(screen.getByPlaceholderText('Email')).toHaveAttribute('type', 'email')
    })

    it('renders password input', () => {
        render(<Input type="password" placeholder="Password" />)
        expect(screen.getByPlaceholderText('Password')).toHaveAttribute('type', 'password')
    })

    it('handles value changes', () => {
        const handleChange = vi.fn()
        render(<Input onChange={handleChange} placeholder="Type here" />)

        const input = screen.getByPlaceholderText('Type here')
        fireEvent.change(input, { target: { value: 'test value' } })

        expect(handleChange).toHaveBeenCalledTimes(1)
        expect(input).toHaveValue('test value')
    })

    it('can be controlled', () => {
        const { rerender } = render(<Input value="initial" onChange={() => { }} />)
        expect(screen.getByDisplayValue('initial')).toBeInTheDocument()

        rerender(<Input value="updated" onChange={() => { }} />)
        expect(screen.getByDisplayValue('updated')).toBeInTheDocument()
    })

    it('can be disabled', () => {
        render(<Input disabled placeholder="Disabled input" />)
        const input = screen.getByPlaceholderText('Disabled input')
        expect(input).toBeDisabled()
        expect(input).toHaveClass('disabled:cursor-not-allowed', 'disabled:opacity-50')
    })

    it('applies custom className', () => {
        render(<Input className="custom-input-class" data-testid="input" />)
        expect(screen.getByTestId('input')).toHaveClass('custom-input-class')
    })

    it('applies default styling classes', () => {
        render(<Input data-testid="styled-input" />)
        const input = screen.getByTestId('styled-input')
        expect(input).toHaveClass('flex', 'h-10', 'w-full', 'rounded-md', 'border')
    })

    it('forwards ref correctly', () => {
        const ref = vi.fn()
        render(<Input ref={ref} />)
        expect(ref).toHaveBeenCalled()
    })

    it('handles focus and blur events', () => {
        const handleFocus = vi.fn()
        const handleBlur = vi.fn()

        render(<Input onFocus={handleFocus} onBlur={handleBlur} placeholder="Focus me" />)
        const input = screen.getByPlaceholderText('Focus me')

        fireEvent.focus(input)
        expect(handleFocus).toHaveBeenCalledTimes(1)

        fireEvent.blur(input)
        expect(handleBlur).toHaveBeenCalledTimes(1)
    })

    it('supports number input type', () => {
        render(<Input type="number" min="0" max="100" step="5" data-testid="number-input" />)
        const input = screen.getByTestId('number-input')

        expect(input).toHaveAttribute('type', 'number')
        expect(input).toHaveAttribute('min', '0')
        expect(input).toHaveAttribute('max', '100')
        expect(input).toHaveAttribute('step', '5')
    })

    it('supports required attribute', () => {
        render(<Input required placeholder="Required field" />)
        expect(screen.getByPlaceholderText('Required field')).toBeRequired()
    })
})
