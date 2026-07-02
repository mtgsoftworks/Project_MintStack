import { baseApi } from './baseApi'

export const marketApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    // Currencies
    getCurrencies: builder.query({
      query: (params = {}) => ({
        url: '/market/currencies',
        params,
      }),
      transformResponse: (response) => response.data,
      providesTags: ['Currencies'],
    }),
    getCurrency: builder.query({
      query: (code) => `/market/currencies/${code}`,
      transformResponse: (response) => response.data,
      providesTags: (_result, _error, code: string) => [{ type: 'Currencies' as const, id: code }],
    }),
    getCurrencyHistory: builder.query({
      query: ({ code, startDate, endDate }) => ({
        url: `/market/currencies/${code}/history`,
        params: { startDate, endDate },
      }),
      transformResponse: (response) => response.data,
    }),
    refreshMarketData: builder.mutation({
      query: (data = {}) => ({
        url: '/market/refresh',
        method: 'POST',
        body: data,
      }),
      invalidatesTags: ['Currencies', 'Stocks', 'Bonds', 'Funds', 'Viop', 'Indices'],
    }),

    // Stocks
    getStocks: builder.query({
      query: (params = {}) => ({
        url: '/market/stocks',
        params,
      }),
      transformResponse: (response) => response,
      providesTags: ['Stocks'],
    }),
    getStock: builder.query({
      query: (arg) => {
        if (typeof arg === 'string') {
          return `/market/stocks/${arg}`
        }
        const { symbol, ...params } = arg || {}
        return {
          url: `/market/stocks/${symbol}`,
          params,
        }
      },
      transformResponse: (response) => response.data,
      providesTags: (_result, _error, arg: string | { symbol: string }) => [{ type: 'Stocks' as const, id: typeof arg === 'string' ? arg : arg?.symbol }],
    }),
    getStockHistory: builder.query({
      query: ({ symbol, period = '1M', days, startDate, endDate }) => {
        if (startDate && endDate) {
          return {
            url: `/market/stocks/${symbol}/history`,
            params: { startDate, endDate },
          }
        }

        const daysMap: Record<string, number> = { '1D': 1, '1W': 7, '1M': 30, '3M': 90, '6M': 180, '1Y': 365 }
        const resolvedDays = days ?? daysMap[period] ?? 30

        return {
          url: `/market/stocks/${symbol}/history`,
          params: { days: resolvedDays },
        }
      },
      transformResponse: (response) => response.data,
    }),

    // Bonds
    getBonds: builder.query({
      query: (params = {}) => ({
        url: '/market/bonds',
        params,
      }),
      transformResponse: (response) => response,
      providesTags: ['Bonds'],
    }),
    getBond: builder.query({
      query: (symbol) => `/market/bonds/${symbol}`,
      transformResponse: (response) => response.data,
      providesTags: (_result, _error, symbol: string) => [{ type: 'Bonds' as const, id: symbol }],
    }),

    // Funds
    getFunds: builder.query({
      query: (params = {}) => ({
        url: '/market/funds',
        params,
      }),
      transformResponse: (response) => response,
      providesTags: ['Funds'],
    }),
    getFund: builder.query({
      query: (symbol) => `/market/funds/${symbol}`,
      transformResponse: (response) => response.data,
      providesTags: (_result, _error, symbol: string) => [{ type: 'Funds' as const, id: symbol }],
    }),

    // VIOP
    getViop: builder.query({
      query: (params = {}) => ({
        url: '/market/viop',
        params,
      }),
      transformResponse: (response) => response,
      providesTags: ['Viop'],
    }),

    // Index
    getMarketIndex: builder.query({
      query: (arg) => {
        if (typeof arg === 'string') {
          return `/market/indices/${arg}`
        }
        const { symbol, ...params } = arg || {}
        return {
          url: `/market/indices/${symbol}`,
          params,
        }
      },
      transformResponse: (response) => response.data,
      providesTags: (_result, _error, arg: string | { symbol: string }) => [{ type: 'Indices' as const, id: typeof arg === 'string' ? arg : arg?.symbol }],
    }),

    // Search
    searchMarket: builder.query({
      query: (query) => ({
        url: '/market/search',
        params: { query },
      }),
      transformResponse: (response) => response.data,
    }),
  }),
})

export const {
  useGetCurrenciesQuery,
  useGetCurrencyQuery,
  useGetCurrencyHistoryQuery,
  useRefreshMarketDataMutation,
  useGetStocksQuery,
  useGetStockQuery,
  useGetStockHistoryQuery,
  useGetBondsQuery,
  useGetBondQuery,
  useGetFundsQuery,
  useGetFundQuery,
  useGetViopQuery,
  useGetMarketIndexQuery,
  useSearchMarketQuery,
} = marketApi
