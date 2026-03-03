import api from './api'

export const analysisService = {
  // Get moving average
  getMovingAverage: async (symbol: any, period = 20, endDate: any) => {
    const params: any = { period }
    if (endDate) params.endDate = endDate
    
    const response = await api.get(`/analysis/ma/${symbol}`, { params })
    return response.data.data
  },

  // Get multiple moving averages (MA7, MA25, MA99)
  getMultipleMA: async (symbol: any, endDate: any) => {
    const params: any = {}
    if (endDate) params.endDate = endDate
    
    const response = await api.get(`/analysis/ma/multiple/${symbol}`, { params })
    return response.data.data
  },

  // Get trend analysis
  getTrend: async (symbol: any, days = 30) => {
    const response = await api.get(`/analysis/trend/${symbol}`, { params: { days } })
    return response.data.data
  },

  // Compare instruments
  compare: async (symbols: any, startDate: any, endDate: any) => {
    const response = await api.post('/analysis/compare', {
      symbols,
      startDate,
      endDate,
    })
    return response.data.data
  },
}
