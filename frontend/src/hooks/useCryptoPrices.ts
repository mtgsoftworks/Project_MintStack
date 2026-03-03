import { useState, useEffect, useCallback, useMemo } from 'react'
import websocketService from '@/services/websocketService'

export function useCryptoPrices(symbols = []) {
    const [cryptos, setCryptos] = useState({})
    const [connected, setConnected] = useState(false)
    
    const handleCryptoUpdate = useCallback((data) => {
        setCryptos(prev => ({
            ...prev,
            [data.symbol]: {
                symbol: data.symbol,
                price: data.price,
                previousClose: data.previousPrice,
                change24h: data.additionalData?.changePercent24h || 0,
                high24h: data.additionalData?.high24h || data.price,
                low24h: data.additionalData?.low24h || data.price,
                volume24h: data.additionalData?.volume24h || 0,
                timestamp: data.timestamp
            }
        }))
    }, [])
    
    const symbolsKey = useMemo(() => symbols.join(','), [symbols])
    
    useEffect(() => {
        const handleConnect = () => setConnected(true)
        const handleDisconnect = () => setConnected(false)
        
        websocketService.on('connect', handleConnect)
        websocketService.on('disconnect', handleDisconnect)
        
        websocketService.subscribe('/topic/prices/crypto', handleCryptoUpdate)
        
        symbols.forEach(symbol => {
            websocketService.subscribe(`/topic/prices/crypto/${symbol}`, handleCryptoUpdate)
        })
        
        if (websocketService.getConnectionState() === 'CONNECTED') {
            setConnected(true)
        }
        
        return () => {
            websocketService.off('connect', handleConnect)
            websocketService.off('disconnect', handleDisconnect)
        }
    }, [symbolsKey, symbols, handleCryptoUpdate])
    
    return { cryptos, connected }
}
