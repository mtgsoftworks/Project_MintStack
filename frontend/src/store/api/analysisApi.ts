import { baseApi } from './baseApi'

const daysMap = { '1W': 7, '1M': 30, '3M': 90, '6M': 180, '1Y': 365 }

const transformIndicatorResponse = (response) => ({
  success: response?.success ?? false,
  message: response?.message || '',
  data: response?.data ?? null,
})

export const analysisApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    // Get moving average analysis
    getMovingAverage: builder.query({
      query: ({ symbol, maType = 'SMA', maPeriod = 20 }) => ({
        url: `/analysis/ma/${symbol}`,
        params: { period: maPeriod, type: maType },
      }),
      transformResponse: (response) => response.data,
    }),

    // Get trend analysis
    getTrendAnalysis: builder.query({
      query: ({ symbol, period = '1M' }) => {
        const days = daysMap[period] || 30
        return {
          url: `/analysis/trend/${symbol}`,
          params: { days },
        }
      },
      transformResponse: (response) => response.data,
    }),

    // Get comparison data for multiple symbols
    getComparison: builder.query({
      query: ({ symbols, period = '1M' }) => {
        const days = daysMap[period] || 30
        const endDate = new Date()
        const startDate = new Date(endDate)
        startDate.setDate(startDate.getDate() - days)

        const formatDate = (date) => date.toISOString().split('T')[0]

        return {
          url: '/analysis/compare',
          method: 'POST',
          body: {
            symbols,
            startDate: formatDate(startDate),
            endDate: formatDate(endDate),
          },
        }
      },
      transformResponse: (response) => response.data?.instruments || [],
    }),

    getRsi: builder.query({
      query: ({ symbol, period = 14 }) => ({
        url: `/indicators/rsi/${symbol}`,
        params: { period },
      }),
      transformResponse: transformIndicatorResponse,
    }),

    getMacd: builder.query({
      query: ({ symbol, fastPeriod = 12, slowPeriod = 26, signalPeriod = 9 }) => ({
        url: `/indicators/macd/${symbol}`,
        params: { fastPeriod, slowPeriod, signalPeriod },
      }),
      transformResponse: transformIndicatorResponse,
    }),

    getBollingerBands: builder.query({
      query: ({ symbol, period = 20, stdDev = 2.0 }) => ({
        url: `/indicators/bollinger/${symbol}`,
        params: { period, stdDev },
      }),
      transformResponse: transformIndicatorResponse,
    }),

    getStochastic: builder.query({
      query: ({ symbol, kPeriod = 14, dPeriod = 3 }) => ({
        url: `/indicators/stochastic/${symbol}`,
        params: { kPeriod, dPeriod },
      }),
      transformResponse: transformIndicatorResponse,
    }),

    getAllTechnicalIndicators: builder.query({
      query: ({ symbol }) => ({
        url: `/indicators/all/${symbol}`,
      }),
      transformResponse: transformIndicatorResponse,
    }),
  }),
})

export const {
  useGetMovingAverageQuery,
  useGetTrendAnalysisQuery,
  useGetComparisonQuery,
  useGetRsiQuery,
  useGetMacdQuery,
  useGetBollingerBandsQuery,
  useGetStochasticQuery,
  useGetAllTechnicalIndicatorsQuery,
} = analysisApi
