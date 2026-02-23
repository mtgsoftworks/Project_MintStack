import { useState, useEffect } from 'react'
import websocketService from '@/services/websocketService'

export function ConnectionStatus() {
    const [state, setState] = useState('DISCONNECTED')
    const [reconnecting, setReconnecting] = useState(null)
    
    useEffect(() => {
        const handleConnect = () => {
            setState('CONNECTED')
            setReconnecting(null)
        }
        
        const handleDisconnect = () => setState('DISCONNECTED')
        
        const handleReconnecting = ({ attempt }) => {
            setState('RECONNECTING')
            setReconnecting(attempt)
        }
        
        websocketService.on('connect', handleConnect)
        websocketService.on('disconnect', handleDisconnect)
        websocketService.on('reconnecting', handleReconnecting)
        
        setState(websocketService.getConnectionState())
        
        return () => {
            websocketService.off('connect', handleConnect)
            websocketService.off('disconnect', handleDisconnect)
            websocketService.off('reconnecting', handleReconnecting)
        }
    }, [])
    
    const statusConfig = {
        CONNECTED: { color: 'bg-green-500', text: 'Bağlı', animate: false },
        CONNECTING: { color: 'bg-yellow-500', text: 'Bağlanıyor...', animate: true },
        RECONNECTING: { color: 'bg-orange-500', text: `Yeniden bağlanıyor (${reconnecting || 1}/5)`, animate: true },
        DISCONNECTED: { color: 'bg-red-500', text: 'Bağlantı kesildi', animate: false }
    }
    
    const config = statusConfig[state] || statusConfig.DISCONNECTED
    
    return (
        <div className="flex items-center gap-2 px-3 py-1.5 bg-gray-100 rounded-full">
            <div className={`w-2 h-2 rounded-full ${config.color} ${config.animate ? 'animate-pulse' : ''}`} />
            <span className="text-sm text-gray-700">{config.text}</span>
        </div>
    )
}
