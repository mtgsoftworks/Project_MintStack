import { vi, describe, it, expect, beforeEach } from 'vitest'

// Mock SockJS and STOMP
vi.mock('sockjs-client', () => ({
  default: vi.fn().mockImplementation(() => ({
    close: vi.fn(),
  })),
}))

vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn().mockImplementation(() => ({
    activate: vi.fn(),
    deactivate: vi.fn(),
    subscribe: vi.fn(() => ({ unsubscribe: vi.fn() })),
    publish: vi.fn(),
  })),
}))

describe('websocketService', () => {
  let websocketService

  beforeEach(async () => {
    vi.clearAllMocks()
    // Re-import to get fresh module
    const module = await import('@/services/websocketService')
    websocketService = module.default
  })

  it('should export a service object', () => {
    expect(websocketService).toBeDefined()
  })

  it('should have connect method', () => {
    expect(typeof websocketService.connect).toBe('function')
  })

  it('should have disconnect method', () => {
    expect(typeof websocketService.disconnect).toBe('function')
  })

  it('should have subscribe method', () => {
    expect(typeof websocketService.subscribe).toBe('function')
  })

  it('should have isConnected property', () => {
    expect(websocketService).toHaveProperty('isConnected')
  })
})
