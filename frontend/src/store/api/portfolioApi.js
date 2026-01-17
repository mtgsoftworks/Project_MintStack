import { baseApi } from './baseApi'

export const portfolioApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    // Get all portfolios
    getPortfolios: builder.query({
      query: () => '/portfolios',
      transformResponse: (response) => response.data,
      providesTags: (result) =>
        result
          ? [
              ...result.map(({ id }) => ({ type: 'Portfolios', id })),
              { type: 'Portfolios', id: 'LIST' },
            ]
          : [{ type: 'Portfolios', id: 'LIST' }],
    }),

    // Get single portfolio
    getPortfolio: builder.query({
      query: (id) => `/portfolios/${id}`,
      transformResponse: (response) => response.data,
      providesTags: (result, error, id) => [{ type: 'Portfolios', id }],
    }),

    // Get portfolio summary
    getPortfolioSummary: builder.query({
      query: () => '/portfolios/summary',
      transformResponse: (response) => response.data,
      providesTags: [{ type: 'Portfolios', id: 'SUMMARY' }],
    }),

    // Create portfolio
    createPortfolio: builder.mutation({
      query: (data) => ({
        url: '/portfolios',
        method: 'POST',
        body: data,
      }),
      invalidatesTags: [{ type: 'Portfolios', id: 'LIST' }],
    }),

    // Update portfolio
    updatePortfolio: builder.mutation({
      query: ({ id, ...data }) => ({
        url: `/portfolios/${id}`,
        method: 'PUT',
        body: data,
      }),
      invalidatesTags: (result, error, { id }) => [
        { type: 'Portfolios', id },
        { type: 'Portfolios', id: 'LIST' },
      ],
    }),

    // Delete portfolio
    deletePortfolio: builder.mutation({
      query: (id) => ({
        url: `/portfolios/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: (result, error, id) => [
        { type: 'Portfolios', id },
        { type: 'Portfolios', id: 'LIST' },
        { type: 'Portfolios', id: 'SUMMARY' },
      ],
    }),

    // Add item to portfolio
    addPortfolioItem: builder.mutation({
      query: ({ portfolioId, ...data }) => ({
        url: `/portfolios/${portfolioId}/items`,
        method: 'POST',
        body: data,
      }),
      invalidatesTags: (result, error, { portfolioId }) => [
        { type: 'Portfolios', id: portfolioId },
        { type: 'PortfolioItems', id: 'LIST' },
        { type: 'Portfolios', id: 'SUMMARY' },
        { type: 'PortfolioTransactions', id: `LIST_${portfolioId}` },
      ],
    }),

    // Update portfolio item
    updatePortfolioItem: builder.mutation({
      query: ({ portfolioId, itemId, ...data }) => ({
        url: `/portfolios/${portfolioId}/items/${itemId}`,
        method: 'PUT',
        body: data,
      }),
      invalidatesTags: (result, error, { portfolioId, itemId }) => [
        { type: 'Portfolios', id: portfolioId },
        { type: 'PortfolioItems', id: itemId },
        { type: 'Portfolios', id: 'SUMMARY' },
      ],
    }),

    // Remove item from portfolio
    removePortfolioItem: builder.mutation({
      query: ({ portfolioId, itemId }) => ({
        url: `/portfolios/${portfolioId}/items/${itemId}`,
        method: 'DELETE',
      }),
      invalidatesTags: (result, error, { portfolioId, itemId }) => [
        { type: 'Portfolios', id: portfolioId },
        { type: 'PortfolioItems', id: itemId },
        { type: 'Portfolios', id: 'SUMMARY' },
        { type: 'PortfolioTransactions', id: `LIST_${portfolioId}` },
      ],
    }),

    // Get portfolio transactions
    getPortfolioTransactions: builder.query({
      query: ({ portfolioId, page = 0, size = 10 }) => ({
        url: `/portfolios/${portfolioId}/transactions`,
        params: { page, size },
      }),
      transformResponse: (response) => response,
      providesTags: (result, error, { portfolioId }) => [
        { type: 'PortfolioTransactions', id: `LIST_${portfolioId}` },
      ],
    }),
  }),
})

export const {
  useGetPortfoliosQuery,
  useGetPortfolioQuery,
  useGetPortfolioSummaryQuery,
  useCreatePortfolioMutation,
  useUpdatePortfolioMutation,
  useDeletePortfolioMutation,
  useAddPortfolioItemMutation,
  useUpdatePortfolioItemMutation,
  useRemovePortfolioItemMutation,
  useGetPortfolioTransactionsQuery,
} = portfolioApi
