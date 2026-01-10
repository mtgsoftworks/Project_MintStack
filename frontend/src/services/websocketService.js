import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

/**
 * WebSocket service for real-time price updates
 */
class WebSocketService {
    constructor() {
        this.client = null
        this.subscriptions = new Map()
        this.isConnected = false
        this.reconnectAttempts = 0
        this.maxReconnectAttempts = 5
        this.reconnectDelay = 3000
        this.listeners = new Map()
    }

    /**
     * Connect to WebSocket server
     */
    connect(options = {}) {
        const wsUrl = options.url || import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws'

        return new Promise((resolve, reject) => {
            try {
                this.client = new Client({
                    webSocketFactory: () => new SockJS(wsUrl),
                    debug: (str) => {
                        if (import.meta.env.DEV) {
                            console.log('[WebSocket]', str)
                        }
                    },
                    reconnectDelay: this.reconnectDelay,
                    heartbeatIncoming: 4000,
                    heartbeatOutgoing: 4000,
                    onConnect: () => {
                        console.log('[WebSocket] Connected')
                        this.isConnected = true
                        this.reconnectAttempts = 0
                        this.notifyListeners('connect', { connected: true })
                        resolve()
                    },
                    onDisconnect: () => {
                        console.log('[WebSocket] Disconnected')
                        this.isConnected = false
                        this.notifyListeners('disconnect', { connected: false })
                    },
                    onStompError: (frame) => {
                        console.error('[WebSocket] STOMP error:', frame.headers['message'])
                        this.notifyListeners('error', { error: frame.headers['message'] })
                        reject(new Error(frame.headers['message']))
                    },
                    onWebSocketError: (event) => {
                        console.error('[WebSocket] WebSocket error:', event)
                        this.handleReconnect()
                    },
                })

                this.client.activate()
            } catch (error) {
                console.error('[WebSocket] Connection error:', error)
                reject(error)
            }
        })
    }

    /**
     * Handle reconnection logic
     */
    handleReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++
            console.log(`[WebSocket] Reconnecting... Attempt ${this.reconnectAttempts}`)
            setTimeout(() => {
                this.connect()
            }, this.reconnectDelay * this.reconnectAttempts)
        } else {
            console.error('[WebSocket] Max reconnection attempts reached')
            this.notifyListeners('error', { error: 'Max reconnection attempts reached' })
        }
    }

    /**
     * Disconnect from WebSocket server
     */
    disconnect() {
        if (this.client) {
            this.subscriptions.forEach((subscription) => {
                subscription.unsubscribe()
            })
            this.subscriptions.clear()
            this.client.deactivate()
            this.isConnected = false
        }
    }

    /**
     * Subscribe to a topic
     */
    subscribe(topic, callback) {
        if (!this.client || !this.isConnected) {
            console.warn('[WebSocket] Not connected. Cannot subscribe.')
            return null
        }

        const subscription = this.client.subscribe(topic, (message) => {
            try {
                const data = JSON.parse(message.body)
                callback(data)
            } catch (error) {
                console.error('[WebSocket] Error parsing message:', error)
                callback(message.body)
            }
        })

        this.subscriptions.set(topic, subscription)
        console.log(`[WebSocket] Subscribed to ${topic}`)
        return subscription
    }

    /**
     * Unsubscribe from a topic
     */
    unsubscribe(topic) {
        const subscription = this.subscriptions.get(topic)
        if (subscription) {
            subscription.unsubscribe()
            this.subscriptions.delete(topic)
            console.log(`[WebSocket] Unsubscribed from ${topic}`)
        }
    }

    /**
     * Send a message to the server
     */
    send(destination, body) {
        if (!this.client || !this.isConnected) {
            console.warn('[WebSocket] Not connected. Cannot send message.')
            return
        }

        this.client.publish({
            destination,
            body: typeof body === 'string' ? body : JSON.stringify(body),
        })
    }

    /**
     * Add event listener
     */
    on(event, callback) {
        if (!this.listeners.has(event)) {
            this.listeners.set(event, [])
        }
        this.listeners.get(event).push(callback)
    }

    /**
     * Remove event listener
     */
    off(event, callback) {
        if (this.listeners.has(event)) {
            const callbacks = this.listeners.get(event)
            const index = callbacks.indexOf(callback)
            if (index > -1) {
                callbacks.splice(index, 1)
            }
        }
    }

    /**
     * Notify all listeners
     */
    notifyListeners(event, data) {
        if (this.listeners.has(event)) {
            this.listeners.get(event).forEach((callback) => callback(data))
        }
    }

    /**
     * Check if connected
     */
    getConnectionState() {
        return this.isConnected
    }
}

// Singleton instance
export const websocketService = new WebSocketService()
export default websocketService
