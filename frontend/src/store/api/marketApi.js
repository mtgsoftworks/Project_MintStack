import { baseApi } from './baseApi'

export const marketApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    // Currencies
    getCurrencies: builder.query({
      query: () => '/market/currencies',
      transformResponse: (response) => response.data,
      providesTags: ['Currencies'],
    }),
    getCurrency: builder.query({
      query: (code) => `/market/currencies/${code}`,
      transformResponse: (response) => response.data,
      providesTags: (result, error, code) => [{ type: 'Currencies', id: code }],
    }),
    getCurrencyHistory: builder.query({
      query: ({ code, startDate, endDate }) => ({
        url: `/market/currencies/${code}/history`,
        params: { startDate, endDate },
      }),
      transformResponse: (response) => response.data,
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
      query: (symbol) => `/market/stocks/${symbol}`,
      transformResponse: (response) => response.data,
      providesTags: (result, error, symbol) => [{ type: 'Stocks', id: symbol }],
    }),
    getStockHistory: builder.query({
      query: ({ symbol, period = '1M', days, startDate, endDate }) => {
        if (startDate && endDate) {
          return {
            url: `/market/stocks/${symbol}/history`,
            params: { startDate, endDate },
          }
        }

        const daysMap = { '1W': 7, '1M': 30, '3M': 90, '6M': 180, '1Y': 365 }
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
      providesTags: (result, error, symbol) => [{ type: 'Bonds', id: symbol }],
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
      providesTags: (result, error, symbol) => [{ type: 'Funds', id: symbol }],
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
      query: (symbol) => `/market/indices/${symbol}`,
      transformResponse: (response) => response.data,
      providesTags: (result, error, symbol) => [{ type: 'Indices', id: symbol }],
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
