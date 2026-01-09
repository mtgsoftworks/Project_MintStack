import { baseApi } from './baseApi'

export const analysisApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    // Get moving average analysis
    getMovingAverage: builder.query({
      query: ({ symbol, period = '1M', maType = 'SMA', maPeriod = 20 }) => ({
        url: `/analysis/moving-average/${symbol}`,
        params: { period, maType, maPeriod },
      }),
      transformResponse: (response) => response.data,
    }),

    // Get trend analysis
    getTrendAnalysis: builder.query({
      query: ({ symbol, period = '1M' }) => ({
        url: `/analysis/trend/${symbol}`,
        params: { period },
      }),
      transformResponse: (response) => response.data,
    }),

    // Get comparison data for multiple symbols
    getComparison: builder.query({
      query: ({ symbols, period = '1M' }) => ({
        url: '/analysis/compare',
        params: { symbols: symbols.join(','), period },
      }),
      transformResponse: (response) => response.data,
    }),

    // Get technical indicators
    getTechnicalIndicators: builder.query({
      query: ({ symbol, indicators = ['RSI', 'MACD'] }) => ({
        url: `/analysis/indicators/${symbol}`,
        params: { indicators: indicators.join(',') },
      }),
      transformResponse: (response) => response.data,
    }),

    // Get volatility analysis
    getVolatility: builder.query({
      query: ({ symbol, period = '1M' }) => ({
        url: `/analysis/volatility/${symbol}`,
        params: { period },
      }),
      transformResponse: (response) => response.data,
    }),

    // Get correlation matrix
    getCorrelation: builder.query({
      query: ({ symbols, period = '1M' }) => ({
        url: '/analysis/correlation',
        params: { symbols: symbols.join(','), period },
      }),
      transformResponse: (response) => response.data,
    }),
  }),
})

export const {
  useGetMovingAverageQuery,
  useGetTrendAnalysisQuery,
  useGetComparisonQuery,
  useGetTechnicalIndicatorsQuery,
  useGetVolatilityQuery,
  useGetCorrelationQuery,
} = analysisApi
