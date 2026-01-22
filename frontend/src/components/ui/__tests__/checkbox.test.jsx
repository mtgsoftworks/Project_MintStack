import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Checkbox } from '../checkbox'

describe('Checkbox', () => {
    it('renders without crashing', () => {
        render(<Checkbox aria-label="Accept terms" />)
        expect(screen.getByRole('checkbox')).toBeInTheDocument()
    })

    it('is unchecked by default', () => {
        render(<Checkbox aria-label="Accept terms" />)
        expect(screen.getByRole('checkbox')).not.toBeChecked()
    })

    it('can be checked by default', () => {
        render(<Checkbox aria-label="Accept terms" defaultChecked />)
        expect(screen.getByRole('checkbox')).toBeChecked()
    })

    it('can be controlled', () => {
        const { rerender } = render(<Checkbox aria-label="Toggle" checked={false} onCheckedChange={() => { }} />)
        expect(screen.getByRole('checkbox')).not.toBeChecked()

        rerender(<Checkbox aria-label="Toggle" checked={true} onCheckedChange={() => { }} />)
        expect(screen.getByRole('checkbox')).toBeChecked()
    })

    it('calls onCheckedChange when clicked', () => {
        const handleChange = vi.fn()
        render(<Checkbox aria-label="Toggle" onCheckedChange={handleChange} />)

        fireEvent.click(screen.getByRole('checkbox'))
        expect(handleChange).toHaveBeenCalledTimes(1)
        expect(handleChange).toHaveBeenCalledWith(true)
    })

    it('can be disabled', () => {
        render(<Checkbox aria-label="Disabled" disabled />)
        expect(screen.getByRole('checkbox')).toBeDisabled()
    })

    it('does not call onCheckedChange when disabled', () => {
        const handleChange = vi.fn()
        render(<Checkbox aria-label="Disabled" disabled onCheckedChange={handleChange} />)

        fireEvent.click(screen.getByRole('checkbox'))
        expect(handleChange).not.toHaveBeenCalled()
    })

    it('applies custom className', () => {
        render(<Checkbox aria-label="Custom" className="custom-checkbox" />)
        expect(screen.getByRole('checkbox')).toHaveClass('custom-checkbox')
    })

    it('supports aria-labelledby', () => {
        render(
            <>
                <label id="checkbox-label">Custom label</label>
                <Checkbox aria-labelledby="checkbox-label" />
            </>
        )
        expect(screen.getByRole('checkbox')).toHaveAttribute('aria-labelledby', 'checkbox-label')
    })
})
