import { describe, it, expect, beforeEach, vi } from 'vitest'
import { marketService } from '../marketService'
import api from '../api'

// Mock the api module
vi.mock('../api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

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
      
      api.get.mockResolvedValue({ data: { data: mockRates } })

      const result = await marketService.getCurrencyRates()

      expect(api.get).toHaveBeenCalledWith('/market/currencies')
      expect(result).toEqual(mockRates)
    })

    it('should handle errors', async () => {
      api.get.mockRejectedValue(new Error('Network error'))

      await expect(marketService.getCurrencyRates()).rejects.toThrow('Network error')
    })
  })

  describe('getCurrencyRate', () => {
    it('should fetch specific currency rate', async () => {
      const mockRate = { currencyCode: 'USD', buyingRate: 32.50 }
      
      api.get.mockResolvedValue({ data: { data: mockRate } })

      const result = await marketService.getCurrencyRate('USD')

      expect(api.get).toHaveBeenCalledWith('/market/currencies/USD')
      expect(result).toEqual(mockRate)
    })
  })

  describe('getStocks', () => {
    it('should fetch stocks with pagination', async () => {
      const mockStocks = {
        content: [{ symbol: 'THYAO', name: 'THY' }],
        totalElements: 1,
      }
      
      api.get.mockResolvedValue({ data: { data: mockStocks } })

      const result = await marketService.getStocks({ page: 0, size: 20 })

      expect(api.get).toHaveBeenCalledWith('/market/stocks', {
        params: { page: 0, size: 20 },
      })
      expect(result).toEqual(mockStocks)
    })
  })

  describe('getStock', () => {
    it('should fetch specific stock', async () => {
      const mockStock = { symbol: 'THYAO', name: 'THY', currentPrice: 280.50 }
      
      api.get.mockResolvedValue({ data: { data: mockStock } })

      const result = await marketService.getStock('THYAO')

      expect(api.get).toHaveBeenCalledWith('/market/stocks/THYAO')
      expect(result).toEqual(mockStock)
    })
  })

  describe('getPriceHistory', () => {
    it('should fetch price history with date range', async () => {
      const mockHistory = [
        { date: '2024-01-01', close: 275.00 },
        { date: '2024-01-02', close: 280.50 },
      ]
      
      api.get.mockResolvedValue({ data: { data: mockHistory } })

      const result = await marketService.getPriceHistory('THYAO', {
        startDate: '2024-01-01',
        endDate: '2024-01-02',
      })

      expect(api.get).toHaveBeenCalledWith('/market/stocks/THYAO/history', {
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
      
      api.get.mockResolvedValue({ data: { data: mockResults } })

      const result = await marketService.searchStocks('THY')

      expect(api.get).toHaveBeenCalledWith('/market/stocks/search', {
        params: { q: 'THY' },
      })
      expect(result).toEqual(mockResults)
    })
  })

  describe('getBonds', () => {
    it('should fetch bonds', async () => {
      const mockBonds = {
        content: [{ symbol: 'GOVT-2Y', name: '2 Yıllık DİBS' }],
      }
      
      api.get.mockResolvedValue({ data: { data: mockBonds } })

      const result = await marketService.getBonds()

      expect(api.get).toHaveBeenCalledWith('/market/bonds', expect.any(Object))
    })
  })

  describe('getFunds', () => {
    it('should fetch funds', async () => {
      const mockFunds = {
        content: [{ symbol: 'TRF001', name: 'Türk Fonu' }],
      }
      
      api.get.mockResolvedValue({ data: { data: mockFunds } })

      const result = await marketService.getFunds()

      expect(api.get).toHaveBeenCalledWith('/market/funds', expect.any(Object))
    })
  })

  describe('getViop', () => {
    it('should fetch VIOP contracts', async () => {
      const mockViop = {
        content: [{ symbol: 'F_XU030', name: 'BIST 30 Vadeli' }],
      }
      
      api.get.mockResolvedValue({ data: { data: mockViop } })

      const result = await marketService.getViop()

      expect(api.get).toHaveBeenCalledWith('/market/viop', expect.any(Object))
    })
  })
})
