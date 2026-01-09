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
      query: ({ symbol, period = '1M' }) => ({
        url: `/market/stocks/${symbol}/history`,
        params: { period },
      }),
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
  useSearchMarketQuery,
} = marketApi
