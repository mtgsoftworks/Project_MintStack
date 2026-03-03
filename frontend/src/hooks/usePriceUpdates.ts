import { useState, useEffect, useCallback, useRef } from 'react'
import websocketService from '@/services/websocketService'

/**
 * React hook for subscribing to real-time price updates via WebSocket
 * 
 * @param {string} topic - The topic to subscribe to (e.g., '/topic/prices/currency')
 * @param {Object} options - Hook options
 * @param {boolean} options.enabled - Whether to enable the subscription (default: true)
 * @param {function} options.onMessage - Custom message handler
 * @param {number} options.debounceMs - Debounce interval for batch updates (default: 100)
 * @returns {Object} - { data, isConnected, error, subscribe, unsubscribe }
 */
export function usePriceUpdates(topic: string, options: any = {}) {
    const { enabled = true, onMessage, debounceMs = 100 } = options

    const [data, setData] = useState(null)
    const [isConnected, setIsConnected] = useState(false)
    const [error, setError] = useState(null)
    const [pendingUpdates, setPendingUpdates] = useState({})
    const subscriptionRef = useRef(null)

    // Debounce batch updates
    useEffect(() => {
        if (Object.keys(pendingUpdates).length === 0) return

        const timer = setTimeout(() => {
            setData(prev => prev ? { ...prev, ...pendingUpdates } : pendingUpdates)
            setPendingUpdates({})
        }, debounceMs)

        return () => clearTimeout(timer)
    }, [pendingUpdates, debounceMs])

    // Handle incoming messages
    const handleMessage = useCallback((message) => {
        if (message.symbol) {
            setPendingUpdates(prev => ({
                ...prev,
                [message.symbol]: message
            }))
        } else {
            setData(message)
        }
        if (onMessage) {
            onMessage(message)
        }
    }, [onMessage])

    // Subscribe to topic
    const subscribe = useCallback(() => {
        if (!websocketService.getConnectionState()) {
            console.warn('[usePriceUpdates] WebSocket not connected')
            return
        }

        if (subscriptionRef.current) {
            return // Already subscribed
        }

        subscriptionRef.current = websocketService.subscribe(topic, handleMessage)
    }, [topic, handleMessage])

    // Unsubscribe from topic
    const unsubscribe = useCallback(() => {
        if (subscriptionRef.current) {
            websocketService.unsubscribe(topic)
            subscriptionRef.current = null
        }
    }, [topic])

    // Connection state handlers
    useEffect(() => {
        const handleConnect = () => {
            setIsConnected(true)
            setError(null)
            if (enabled) {
                subscribe()
            }
        }

        const handleDisconnect = () => {
            setIsConnected(false)
        }

        const handleError = ({ error: err }) => {
            setError(err)
        }

        websocketService.on('connect', handleConnect)
        websocketService.on('disconnect', handleDisconnect)
        websocketService.on('error', handleError)

        // Check if already connected
        if (websocketService.getConnectionState()) {
            setIsConnected(true)
            if (enabled) {
                subscribe()
            }
        }

        return () => {
            websocketService.off('connect', handleConnect)
            websocketService.off('disconnect', handleDisconnect)
            websocketService.off('error', handleError)
            unsubscribe()
        }
    }, [enabled, subscribe, unsubscribe])

    // Handle enabled changes
    useEffect(() => {
        if (enabled && isConnected && !subscriptionRef.current) {
            subscribe()
        } else if (!enabled && subscriptionRef.current) {
            unsubscribe()
        }
    }, [enabled, isConnected, subscribe, unsubscribe])

    return {
        data,
        isConnected,
        error,
        subscribe,
        unsubscribe,
    }
}

/**
 * Hook for subscribing to currency price updates
 */
export function useCurrencyPrices(currencyCode = null) {
    const topic = currencyCode
        ? `/topic/prices/currency/${currencyCode}`
        : '/topic/prices/currency'

    return usePriceUpdates(topic)
}

/**
 * Hook for subscribing to stock price updates
 */
export function useStockPrices(symbol = null) {
    const topic = symbol
        ? `/topic/prices/stocks/${symbol}`
        : '/topic/prices/stocks'

    return usePriceUpdates(topic)
}

/**
 * Hook for subscribing to all market price updates
 */
export function useMarketPrices() {
    return usePriceUpdates('/topic/prices')
}

export default usePriceUpdates
