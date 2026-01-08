import api from './api'

export const portfolioService = {
  // Get all portfolios with error handling
  getPortfolios: async () => {
    try {
      const response = await api.get('/portfolios')
      return response.data?.data || []
    } catch (error) {
      console.error('Failed to fetch portfolios:', error)
      // Return empty array instead of throwing to prevent crash
      if (error.response?.status === 401) {
        return [] // User not authenticated, return empty
      }
      throw error
    }
  },

  // Get single portfolio
  getPortfolio: async (id) => {
    try {
      const response = await api.get(`/portfolios/${id}`)
      return response.data?.data || null
    } catch (error) {
      console.error('Failed to fetch portfolio:', error)
      throw error
    }
  },

  // Create portfolio
  createPortfolio: async (data) => {
    const response = await api.post('/portfolios', data)
    return response.data?.data
  },

  // Update portfolio
  updatePortfolio: async (id, data) => {
    const response = await api.put(`/portfolios/${id}`, data)
    return response.data?.data
  },

  // Delete portfolio
  deletePortfolio: async (id) => {
    const response = await api.delete(`/portfolios/${id}`)
    return response.data
  },

  // Add item to portfolio
  addItem: async (portfolioId, data) => {
    const response = await api.post(`/portfolios/${portfolioId}/items`, data)
    return response.data?.data
  },

  // Remove item from portfolio
  removeItem: async (portfolioId, itemId) => {
    const response = await api.delete(`/portfolios/${portfolioId}/items/${itemId}`)
    return response.data?.data
  },

  // Get portfolio summary
  getSummary: async (id) => {
    const response = await api.get(`/portfolios/${id}/summary`)
    return response.data?.data
  },
}
