import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import websocketService from '@/services/websocketService'

type ConnectionState = 'CONNECTED' | 'CONNECTING' | 'RECONNECTING' | 'DISCONNECTED'

interface StatusConfig {
    color: string
    text: string
    animate: boolean
}

const statusConfig: Record<ConnectionState, StatusConfig> = {
    CONNECTED: { color: 'bg-green-500', text: '', animate: false },
    CONNECTING: { color: 'bg-yellow-500', text: '', animate: true },
    RECONNECTING: { color: 'bg-orange-500', text: '', animate: true },
    DISCONNECTED: { color: 'bg-red-500', text: '', animate: false },
}

export function ConnectionStatus() {
    const { t } = useTranslation()
    const [state, setState] = useState('DISCONNECTED')
    const [reconnecting, setReconnecting] = useState<number | null>(null)

    useEffect(() => {
        const handleConnect = () => {
            setState('CONNECTED')
            setReconnecting(null)
        }

        const handleDisconnect = () => setState('DISCONNECTED')

        const handleReconnecting = (payload: { connected: boolean; attempt?: number }) => {
            setState('RECONNECTING')
            setReconnecting(payload.attempt ?? null)
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

    const config = statusConfig[state as ConnectionState] || statusConfig.DISCONNECTED

    const text = state === 'RECONNECTING'
        ? t('connection.reconnecting', { attempt: reconnecting || 1 })
        : t(`connection.${state.toLowerCase()}`)

    return (
        <div className="flex items-center gap-2 px-3 py-1.5 bg-gray-100 rounded-full">
            <div className={`w-2 h-2 rounded-full ${config.color} ${config.animate ? 'animate-pulse' : ''}`} />
            <span className="text-sm text-gray-700">{text}</span>
        </div>
    )
}
