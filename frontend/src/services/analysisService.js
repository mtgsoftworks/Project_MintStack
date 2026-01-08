import api from './api'

export const analysisService = {
  // Get moving average
  getMovingAverage: async (symbol, period = 20, endDate) => {
    const params = { period }
    if (endDate) params.endDate = endDate
    
    const response = await api.get(`/analysis/ma/${symbol}`, { params })
    return response.data.data
  },

  // Get multiple moving averages (MA7, MA25, MA99)
  getMultipleMA: async (symbol, endDate) => {
    const params = {}
    if (endDate) params.endDate = endDate
    
    const response = await api.get(`/analysis/ma/multiple/${symbol}`, { params })
    return response.data.data
  },

  // Get trend analysis
  getTrend: async (symbol, days = 30) => {
    const response = await api.get(`/analysis/trend/${symbol}`, { params: { days } })
    return response.data.data
  },

  // Compare instruments
  compare: async (symbols, startDate, endDate) => {
    const response = await api.post('/analysis/compare', {
      symbols,
      startDate,
      endDate,
    })
    return response.data.data
  },
}
