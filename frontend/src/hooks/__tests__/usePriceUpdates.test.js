import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { usePriceUpdates, useCurrencyPrices, useStockPrices, useMarketPrices } from '../usePriceUpdates'

const mockSubscribe = vi.fn()
const mockUnsubscribe = vi.fn()
const mockOn = vi.fn()
const mockOff = vi.fn()
const mockGetConnectionState = vi.fn(() => false)

vi.mock('@/services/websocketService', () => ({
  default: {
    subscribe: (...args) => mockSubscribe(...args),
    unsubscribe: (...args) => mockUnsubscribe(...args),
    on: (...args) => mockOn(...args),
    off: (...args) => mockOff(...args),
    getConnectionState: () => mockGetConnectionState(),
  },
}))

describe('usePriceUpdates', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetConnectionState.mockReturnValue(false)
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('initialization', () => {
    it('should initialize with default values', () => {
      const { result } = renderHook(() => usePriceUpdates('/topic/test'))

      expect(result.current.data).toBe(null)
      expect(result.current.isConnected).toBe(false)
      expect(result.current.error).toBe(null)
    })

    it('should return subscribe and unsubscribe functions', () => {
      const { result } = renderHook(() => usePriceUpdates('/topic/test'))

      expect(typeof result.current.subscribe).toBe('function')
      expect(typeof result.current.unsubscribe).toBe('function')
    })
  })

  describe('connection handling', () => {
    it('should register event listeners on mount', () => {
      renderHook(() => usePriceUpdates('/topic/test'))

      expect(mockOn).toHaveBeenCalledWith('connect', expect.any(Function))
      expect(mockOn).toHaveBeenCalledWith('disconnect', expect.any(Function))
      expect(mockOn).toHaveBeenCalledWith('error', expect.any(Function))
    })

    it('should set isConnected to true when connect event fires', () => {
      let connectHandler
      mockOn.mockImplementation((event, handler) => {
        if (event === 'connect') connectHandler = handler
      })

      const { result } = renderHook(() => usePriceUpdates('/topic/test'))

      act(() => {
        connectHandler({ connected: true })
      })

      expect(result.current.isConnected).toBe(true)
    })

    it('should set isConnected to false when disconnect event fires', () => {
      let disconnectHandler
      mockOn.mockImplementation((event, handler) => {
        if (event === 'disconnect') disconnectHandler = handler
      })
      mockGetConnectionState.mockReturnValue(true)

      const { result } = renderHook(() => usePriceUpdates('/topic/test'))

      act(() => {
        disconnectHandler({ connected: false })
      })

      expect(result.current.isConnected).toBe(false)
    })

    it('should set error when error event fires', () => {
      let errorHandler
      mockOn.mockImplementation((event, handler) => {
        if (event === 'error') errorHandler = handler
      })

      const { result } = renderHook(() => usePriceUpdates('/topic/test'))

      act(() => {
        errorHandler({ error: 'Test error' })
      })

      expect(result.current.error).toBe('Test error')
    })
  })

  describe('subscribe/unsubscribe functionality', () => {
    it('should not subscribe when not connected', () => {
      mockGetConnectionState.mockReturnValue(false)

      const { result } = renderHook(() => usePriceUpdates('/topic/test'))

      act(() => {
        result.current.subscribe()
      })

      expect(mockSubscribe).not.toHaveBeenCalled()
    })

    it('should subscribe when connected', () => {
      mockGetConnectionState.mockReturnValue(true)
      mockSubscribe.mockReturnValue({ id: 'sub-1' })

      const { result } = renderHook(() => usePriceUpdates('/topic/test'))

      act(() => {
        result.current.subscribe()
      })

      expect(mockSubscribe).toHaveBeenCalledWith('/topic/test', expect.any(Function))
    })

    it('should not subscribe twice', () => {
      mockGetConnectionState.mockReturnValue(true)
      mockSubscribe.mockReturnValue({ id: 'sub-1' })

      const { result } = renderHook(() => usePriceUpdates('/topic/test'))

      act(() => {
        result.current.subscribe()
        result.current.subscribe()
      })

      expect(mockSubscribe).toHaveBeenCalledTimes(1)
    })

    it('should unsubscribe correctly', () => {
      mockGetConnectionState.mockReturnValue(true)
      mockSubscribe.mockReturnValue({ id: 'sub-1' })

      const { result } = renderHook(() => usePriceUpdates('/topic/test'))

      act(() => {
        result.current.subscribe()
        result.current.unsubscribe()
      })

      expect(mockUnsubscribe).toHaveBeenCalledWith('/topic/test')
    })

    it('should not unsubscribe if not subscribed', () => {
      const { result } = renderHook(() => usePriceUpdates('/topic/test'))

      act(() => {
        result.current.unsubscribe()
      })

      expect(mockUnsubscribe).not.toHaveBeenCalled()
    })
  })

  describe('cleanup on unmount', () => {
    it('should remove event listeners on unmount', () => {
      const { unmount } = renderHook(() => usePriceUpdates('/topic/test'))

      unmount()

      expect(mockOff).toHaveBeenCalledWith('connect', expect.any(Function))
      expect(mockOff).toHaveBeenCalledWith('disconnect', expect.any(Function))
      expect(mockOff).toHaveBeenCalledWith('error', expect.any(Function))
    })

    it('should unsubscribe on unmount', () => {
      mockGetConnectionState.mockReturnValue(true)
      mockSubscribe.mockReturnValue({ id: 'sub-1' })

      const { result, unmount } = renderHook(() => usePriceUpdates('/topic/test'))

      act(() => {
        result.current.subscribe()
      })

      unmount()

      expect(mockUnsubscribe).toHaveBeenCalledWith('/topic/test')
    })
  })

  describe('enabled option', () => {
    it('should not subscribe when disabled', () => {
      mockGetConnectionState.mockReturnValue(true)

      renderHook(() => usePriceUpdates('/topic/test', { enabled: false }))

      expect(mockSubscribe).not.toHaveBeenCalled()
    })

    it('should subscribe when enabled and connected', () => {
      mockGetConnectionState.mockReturnValue(true)
      mockSubscribe.mockReturnValue({ id: 'sub-1' })

      renderHook(() => usePriceUpdates('/topic/test', { enabled: true }))

      expect(mockSubscribe).toHaveBeenCalledWith('/topic/test', expect.any(Function))
    })
  })

  describe('onMessage callback', () => {
    it('should call onMessage when message received', () => {
      const onMessage = vi.fn()
      let messageHandler

      mockGetConnectionState.mockReturnValue(true)
      mockSubscribe.mockImplementation((topic, handler) => {
        messageHandler = handler
        return { id: 'sub-1' }
      })

      renderHook(() => usePriceUpdates('/topic/test', { onMessage }))

      act(() => {
        messageHandler({ price: 100 })
      })

      expect(onMessage).toHaveBeenCalledWith({ price: 100 })
    })
  })

  describe('data state', () => {
    it('should update data when message received', async () => {
      let messageHandler

      mockGetConnectionState.mockReturnValue(true)
      mockSubscribe.mockImplementation((topic, handler) => {
        messageHandler = handler
        return { id: 'sub-1' }
      })

      const { result } = renderHook(() => usePriceUpdates('/topic/test'))

      act(() => {
        messageHandler({ price: 100, symbol: 'TEST' })
      })

      await waitFor(() => {
        expect(result.current.data).toEqual({
          TEST: { price: 100, symbol: 'TEST' },
        })
      })
    })
  })
})

describe('useCurrencyPrices', () => {
  it('should use correct topic for all currencies', () => {
    mockGetConnectionState.mockReturnValue(true)
    mockSubscribe.mockReturnValue({ id: 'sub-1' })

    renderHook(() => useCurrencyPrices())

    expect(mockSubscribe).toHaveBeenCalledWith('/topic/prices/currency', expect.any(Function))
  })

  it('should use correct topic for specific currency', () => {
    mockGetConnectionState.mockReturnValue(true)
    mockSubscribe.mockReturnValue({ id: 'sub-1' })

    renderHook(() => useCurrencyPrices('USD'))

    expect(mockSubscribe).toHaveBeenCalledWith('/topic/prices/currency/USD', expect.any(Function))
  })
})

describe('useStockPrices', () => {
  it('should use correct topic for all stocks', () => {
    mockGetConnectionState.mockReturnValue(true)
    mockSubscribe.mockReturnValue({ id: 'sub-1' })

    renderHook(() => useStockPrices())

    expect(mockSubscribe).toHaveBeenCalledWith('/topic/prices/stocks', expect.any(Function))
  })

  it('should use correct topic for specific stock', () => {
    mockGetConnectionState.mockReturnValue(true)
    mockSubscribe.mockReturnValue({ id: 'sub-1' })

    renderHook(() => useStockPrices('THYAO'))

    expect(mockSubscribe).toHaveBeenCalledWith('/topic/prices/stocks/THYAO', expect.any(Function))
  })
})

describe('useMarketPrices', () => {
  it('should use correct topic for all market prices', () => {
    mockGetConnectionState.mockReturnValue(true)
    mockSubscribe.mockReturnValue({ id: 'sub-1' })

    renderHook(() => useMarketPrices())

    expect(mockSubscribe).toHaveBeenCalledWith('/topic/prices', expect.any(Function))
  })
})
