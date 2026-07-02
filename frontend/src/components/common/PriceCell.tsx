import { useState, useEffect, type ReactNode } from 'react'

interface PriceCellProps {
    price: number
    previousPrice?: number
    changePercent: number
    decimals?: number
    showArrow?: boolean
    className?: string
}

/**
 * Animated price cell that flashes on price change
 */
export function PriceCell({
    price,
    previousPrice,
    changePercent,
    decimals = 2,
    showArrow = true,
    className = ''
}: PriceCellProps): ReactNode {
    const [flash, setFlash] = useState<'up' | 'down' | null>(null)

    useEffect(() => {
        if (!previousPrice || price === previousPrice) return

        setFlash(price > previousPrice ? 'up' : 'down')
        const timer = setTimeout(() => setFlash(null), 1000)
        return () => clearTimeout(timer)
    }, [price, previousPrice])

    const formatPrice = (p: number): string => {
        return new Intl.NumberFormat('tr-TR', {
            minimumFractionDigits: decimals,
            maximumFractionDigits: decimals
        }).format(p)
    }
    
    const changeClass = changePercent > 0 ? 'text-green-500' : changePercent < 0 ? 'text-red-500' : 'text-gray-500'
    const flashClass = flash === 'up' ? 'bg-green-500/20' : flash === 'down' ? 'bg-red-500/20' : ''
    const arrow = changePercent > 0 ? '↑' : changePercent < 0 ? '↓' : ''
    
    return (
        <div className={`transition-all duration-300 px-2 py-1 rounded ${flashClass} ${className}`}>
            <span className={`font-mono font-semibold ${changeClass}`}>
                {showArrow && arrow && <span className="mr-1">{arrow}</span>}
                {formatPrice(price)}
            </span>
        </div>
    )
}
