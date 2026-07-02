import api from './api'
import type { AxiosError } from 'axios'

export interface Portfolio {
  id: string
  name: string
  description?: string
  userId: string
  createdAt: string
  updatedAt: string
}

export interface PortfolioItem {
  id: string
  portfolioId: string
  symbol: string
  quantity: number
  averagePrice: number
  currentPrice?: number
}

export interface PortfolioSummary {
  totalValue: number
  totalCost: number
  profitLoss: number
  profitLossPercentage: number
}

export interface Transaction {
  id: string
  portfolioId: string
  type: 'BUY' | 'SELL'
  symbol: string
  quantity: number
  price: number
  total: number
  timestamp: string
}

export interface PaginationParams {
  page?: number
  size?: number
}

export const portfolioService = {
  // Get all portfolios with error handling
  getPortfolios: async (): Promise<Portfolio[]> => {
    try {
      const response = await api.get('/portfolios')
      return response.data?.data || []
    } catch (error) {
      console.error('Failed to fetch portfolios:', error)
      // Return empty array instead of throwing to prevent crash
      const axiosError = error as AxiosError<{ status?: number }>
      if (axiosError.response?.status === 401) {
        return [] // User not authenticated, return empty
      }
      throw error
    }
  },

  // Get single portfolio
  getPortfolio: async (id: string): Promise<Portfolio | null> => {
    try {
      const response = await api.get(`/portfolios/${id}`)
      return response.data?.data || null
    } catch (error) {
      console.error('Failed to fetch portfolio:', error)
      throw error
    }
  },

  // Create portfolio
  createPortfolio: async (data: Partial<Portfolio>): Promise<Portfolio> => {
    const response = await api.post('/portfolios', data)
    return response.data?.data
  },

  // Update portfolio
  updatePortfolio: async (id: string, data: Partial<Portfolio>): Promise<Portfolio> => {
    const response = await api.put(`/portfolios/${id}`, data)
    return response.data?.data
  },

  // Delete portfolio
  deletePortfolio: async (id: string): Promise<void> => {
    const response = await api.delete(`/portfolios/${id}`)
    return response.data
  },

  // Add item to portfolio
  addItem: async (portfolioId: string, data: Partial<PortfolioItem>): Promise<PortfolioItem> => {
    const response = await api.post(`/portfolios/${portfolioId}/items`, data)
    return response.data?.data
  },

  // Remove item from portfolio
  removeItem: async (portfolioId: string, itemId: string): Promise<PortfolioItem> => {
    const response = await api.delete(`/portfolios/${portfolioId}/items/${itemId}`)
    return response.data?.data
  },

  // Get portfolio summary
  getSummary: async (id: string): Promise<PortfolioSummary> => {
    const response = await api.get(`/portfolios/${id}/summary`)
    return response.data?.data
  },

  // Get portfolio transaction history
  getTransactions: async (portfolioId: string, { page = 0, size = 10 }: PaginationParams = {}): Promise<{ data: Transaction[]; total: number }> => {
    const response = await api.get(`/portfolios/${portfolioId}/transactions`, {
      params: { page, size },
    })
    return response.data
  },
}
