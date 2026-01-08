import axios from 'axios'

// Public API client (no auth required for market data)
const publicApi = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

export const marketService = {
  // Currencies (public)
  getCurrencies: async () => {
    const response = await publicApi.get('/market/currencies')
    return response.data.data
  },

  getCurrency: async (code) => {
    const response = await publicApi.get(`/market/currencies/${code}`)
    return response.data.data
  },

  getCurrencyHistory: async (code, startDate, endDate) => {
    const response = await publicApi.get(`/market/currencies/${code}/history`, {
      params: { startDate, endDate },
    })
    return response.data.data
  },

  // Stocks (public)
  getStocks: async (params = {}) => {
    const response = await publicApi.get('/market/stocks', { params })
    return response.data
  },

  getStock: async (symbol) => {
    const response = await publicApi.get(`/market/stocks/${symbol}`)
    return response.data.data
  },

  getStockHistory: async (symbol, params = {}) => {
    const response = await publicApi.get(`/market/stocks/${symbol}/history`, { params })
    return response.data.data
  },

  // Bonds (public)
  getBonds: async (params = {}) => {
    const response = await publicApi.get('/market/bonds', { params })
    return response.data
  },

  // Funds (public)
  getFunds: async (params = {}) => {
    const response = await publicApi.get('/market/funds', { params })
    return response.data
  },

  // VIOP (public)
  getViop: async (params = {}) => {
    const response = await publicApi.get('/market/viop', { params })
    return response.data
  },

  // Search (public)
  search: async (query) => {
    const response = await publicApi.get('/market/search', { params: { query } })
    return response.data
  },
}
