import { vi, describe, it, expect, beforeEach } from 'vitest'
import { marketService } from '../marketService'
import api from '../api'

// Define the mock get function using vi.hoisted to ensure it's available for the factory
const mocks = vi.hoisted(() => ({
  get: vi.fn(),
}))

vi.mock('axios', () => ({
  default: {
    create: () => ({
      get: mocks.get,
      interceptors: {
        request: { use: vi.fn(), eject: vi.fn() },
        response: { use: vi.fn(), eject: vi.fn() },
      },
    }),
  },
}))

const mockGet = mocks.get

describe('marketService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getCurrencyRates', () => {
    it('should fetch currency rates', async () => {
      const mockRates = [
        { currencyCode: 'USD', buyingRate: 32.50 },
        { currencyCode: 'EUR', buyingRate: 35.20 },
      ]

      mockGet.mockResolvedValue({ data: { data: mockRates } })

      const result = await marketService.getCurrencies()

      expect(mockGet).toHaveBeenCalledWith('/market/currencies')
      expect(result).toEqual(mockRates)
    })

    it('should handle errors', async () => {
      mockGet.mockRejectedValue(new Error('Network error'))

      await expect(marketService.getCurrencies()).rejects.toThrow('Network error')
    })
  })

  describe('getCurrencyRate', () => {
    it('should fetch specific currency rate', async () => {
      const mockRate = { currencyCode: 'USD', buyingRate: 32.50 }

      mockGet.mockResolvedValue({ data: { data: mockRate } })

      const result = await marketService.getCurrency('USD')

      expect(mockGet).toHaveBeenCalledWith('/market/currencies/USD')
      expect(result).toEqual(mockRate)
    })
  })

  describe('getStocks', () => {
    it('should fetch stocks with pagination', async () => {
      const mockStocks = {
        content: [{ symbol: 'THYAO', name: 'THY' }],
        totalElements: 1,
      }

      mockGet.mockResolvedValue({ data: mockStocks })

      const result = await marketService.getStocks({ page: 0, size: 20 })

      expect(mockGet).toHaveBeenCalledWith('/market/stocks', {
        params: { page: 0, size: 20 },
      })
      expect(result).toEqual(mockStocks)
    })
  })

  describe('getStock', () => {
    it('should fetch specific stock', async () => {
      const mockStock = { symbol: 'THYAO', name: 'THY', currentPrice: 280.50 }

      mockGet.mockResolvedValue({ data: { data: mockStock } })

      const result = await marketService.getStock('THYAO')

      expect(mockGet).toHaveBeenCalledWith('/market/stocks/THYAO')
      expect(result).toEqual(mockStock)
    })
  })

  describe('getPriceHistory', () => {
    it('should fetch price history with date range', async () => {
      const mockHistory = [
        { date: '2024-01-01', close: 275.00 },
        { date: '2024-01-02', close: 280.50 },
      ]

      mockGet.mockResolvedValue({ data: { data: mockHistory } })

      const result = await marketService.getStockHistory('THYAO', {
        startDate: '2024-01-01',
        endDate: '2024-01-02',
      })

      expect(mockGet).toHaveBeenCalledWith('/market/stocks/THYAO/history', {
        params: { startDate: '2024-01-01', endDate: '2024-01-02' },
      })
      expect(result).toEqual(mockHistory)
    })
  })

  describe('searchStocks', () => {
    it('should search stocks by query', async () => {
      const mockResults = {
        content: [{ symbol: 'THYAO', name: 'Türk Hava Yolları' }],
      }

      mockGet.mockResolvedValue({ data: mockResults })

      const result = await marketService.search('THY')

      expect(mockGet).toHaveBeenCalledWith('/market/search', {
        params: { query: 'THY' },
      })
      expect(result).toEqual(mockResults)
    })
  })

  describe('getBonds', () => {
    it('should fetch bonds', async () => {
      const mockBonds = {
        content: [{ symbol: 'GOVT-2Y', name: '2 Yıllık DİBS' }],
      }

      mockGet.mockResolvedValue({ data: mockBonds })

      const result = await marketService.getBonds()

      expect(mockGet).toHaveBeenCalledWith('/market/bonds', expect.any(Object))
    })
  })

  describe('getFunds', () => {
    it('should fetch funds', async () => {
      const mockFunds = {
        content: [{ symbol: 'TRF001', name: 'Türk Fonu' }],
      }

      mockGet.mockResolvedValue({ data: mockFunds })

      const result = await marketService.getFunds()

      expect(mockGet).toHaveBeenCalledWith('/market/funds', expect.any(Object))
    })
  })

  describe('getViop', () => {
    it('should fetch VIOP contracts', async () => {
      const mockViop = {
        content: [{ symbol: 'F_XU030', name: 'BIST 30 Vadeli' }],
      }

      mockGet.mockResolvedValue({ data: mockViop })

      const result = await marketService.getViop()

      expect(mockGet).toHaveBeenCalledWith('/market/viop', expect.any(Object))
    })
  })
})
