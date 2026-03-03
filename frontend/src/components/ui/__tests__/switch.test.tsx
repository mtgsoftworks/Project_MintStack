import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Switch } from '../switch'

describe('Switch', () => {
    it('renders without crashing', () => {
        render(<Switch aria-label="Toggle" />)
        expect(screen.getByRole('switch')).toBeInTheDocument()
    })

    it('is unchecked by default', () => {
        render(<Switch aria-label="Toggle" />)
        expect(screen.getByRole('switch')).not.toBeChecked()
    })

    it('can be checked by default', () => {
        render(<Switch aria-label="Toggle" defaultChecked />)
        expect(screen.getByRole('switch')).toBeChecked()
    })

    it('can be controlled', () => {
        const { rerender } = render(<Switch aria-label="Toggle" checked={false} onCheckedChange={() => { }} />)
        expect(screen.getByRole('switch')).not.toBeChecked()

        rerender(<Switch aria-label="Toggle" checked={true} onCheckedChange={() => { }} />)
        expect(screen.getByRole('switch')).toBeChecked()
    })

    it('calls onCheckedChange when clicked', () => {
        const handleChange = vi.fn()
        render(<Switch aria-label="Toggle" onCheckedChange={handleChange} />)

        fireEvent.click(screen.getByRole('switch'))
        expect(handleChange).toHaveBeenCalledTimes(1)
        expect(handleChange).toHaveBeenCalledWith(true)
    })

    it('can be disabled', () => {
        render(<Switch aria-label="Toggle" disabled />)
        const switchEl = screen.getByRole('switch')
        expect(switchEl).toBeDisabled()
    })

    it('does not call onCheckedChange when disabled', () => {
        const handleChange = vi.fn()
        render(<Switch aria-label="Toggle" disabled onCheckedChange={handleChange} />)

        fireEvent.click(screen.getByRole('switch'))
        expect(handleChange).not.toHaveBeenCalled()
    })

    it('applies custom className', () => {
        render(<Switch aria-label="Toggle" className="custom-switch" />)
        expect(screen.getByRole('switch')).toHaveClass('custom-switch')
    })

    it('applies data-state attribute based on checked state', () => {
        render(<Switch aria-label="Toggle" />)
        expect(screen.getByRole('switch')).toHaveAttribute('data-state', 'unchecked')
    })

    it('has checked data-state when defaultChecked', () => {
        render(<Switch aria-label="Toggle Checked" defaultChecked />)
        expect(screen.getByRole('switch')).toHaveAttribute('data-state', 'checked')
    })

    it('renders with correct base styling', () => {
        render(<Switch aria-label="Toggle" />)
        const switchEl = screen.getByRole('switch')
        expect(switchEl).toHaveClass('peer', 'inline-flex', 'cursor-pointer', 'rounded-full')
    })

    it('has proper accessibility attributes', () => {
        render(<Switch aria-label="Toggle feature" />)
        const switchEl = screen.getByRole('switch')
        expect(switchEl).toHaveAttribute('aria-label', 'Toggle feature')
    })

    it('can use aria-labelledby', () => {
        render(
            <>
                <label id="switch-label">Enable notifications</label>
                <Switch aria-labelledby="switch-label" />
            </>
        )
        expect(screen.getByRole('switch')).toHaveAttribute('aria-labelledby', 'switch-label')
    })
})
