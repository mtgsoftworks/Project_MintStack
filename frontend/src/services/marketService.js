import api from './api'

export const marketService = {
  // Currencies
  getCurrencies: async () => {
    const response = await api.get('/market/currencies')
    return response.data.data
  },

  getCurrency: async (code) => {
    const response = await api.get(`/market/currencies/${code}`)
    return response.data.data
  },

  getCurrencyHistory: async (code, startDate, endDate) => {
    const response = await api.get(`/market/currencies/${code}/history`, {
      params: { startDate, endDate },
    })
    return response.data.data
  },

  // Stocks
  getStocks: async (params = {}) => {
    const response = await api.get('/market/stocks', { params })
    return response.data
  },

  getStock: async (symbol) => {
    const response = await api.get(`/market/stocks/${symbol}`)
    return response.data.data
  },

  getStockHistory: async (symbol, params = {}) => {
    const response = await api.get(`/market/stocks/${symbol}/history`, { params })
    return response.data.data
  },

  // Bonds
  getBonds: async (params = {}) => {
    const response = await api.get('/market/bonds', { params })
    return response.data
  },

  // Funds
  getFunds: async (params = {}) => {
    const response = await api.get('/market/funds', { params })
    return response.data
  },

  // VIOP
  getViop: async (params = {}) => {
    const response = await api.get('/market/viop', { params })
    return response.data
  },

  // Search
  search: async (query) => {
    const response = await api.get('/market/search', { params: { query } })
    return response.data
  },
}
