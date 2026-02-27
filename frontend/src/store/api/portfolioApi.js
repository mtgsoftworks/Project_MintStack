import { baseApi, API_BASE_URL } from './baseApi'

/**
 * Export portfolio to Excel format.
 * This function makes a direct fetch call to handle binary file download.
 */
export async function exportPortfolioToExcel(portfolioId, token) {
  const response = await fetch(`${API_BASE_URL}/portfolios/${portfolioId}/export/excel`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  })
  
  if (!response.ok) {
    throw new Error('Export failed')
  }
  
  const blob = await response.blob()
  const contentDisposition = response.headers.get('Content-Disposition')
  let filename = `portfolio_${portfolioId}.xlsx`
  
  if (contentDisposition) {
    const filenameMatch = contentDisposition.match(/filename\*?=['"]?(?:UTF-8'')?([^;\r\n"']*)['"]?/i)
    if (filenameMatch && filenameMatch[1]) {
      filename = decodeURIComponent(filenameMatch[1])
    }
  }
  
  // Trigger download
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

/**
 * Export portfolio to PDF format.
 * This function makes a direct fetch call to handle binary file download.
 */
export async function exportPortfolioToPdf(portfolioId, token) {
  const response = await fetch(`${API_BASE_URL}/portfolios/${portfolioId}/export/pdf`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  })
  
  if (!response.ok) {
    throw new Error('Export failed')
  }
  
  const blob = await response.blob()
  const contentDisposition = response.headers.get('Content-Disposition')
  let filename = `portfolio_${portfolioId}.pdf`
  
  if (contentDisposition) {
    const filenameMatch = contentDisposition.match(/filename\*?=['"]?(?:UTF-8'')?([^;\r\n"']*)['"]?/i)
    if (filenameMatch && filenameMatch[1]) {
      filename = decodeURIComponent(filenameMatch[1])
    }
  }
  
  // Trigger download
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

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

    // Execute buy/sell trade
    executePortfolioTrade: builder.mutation({
      query: ({ portfolioId, ...data }) => ({
        url: `/portfolios/${portfolioId}/trades`,
        method: 'POST',
        body: data,
      }),
      invalidatesTags: (result, error, { portfolioId }) => [
        { type: 'Portfolios', id: portfolioId },
        { type: 'Portfolios', id: 'LIST' },
        { type: 'Portfolios', id: 'SUMMARY' },
        { type: 'PortfolioTransactions', id: `LIST_${portfolioId}` },
      ],
    }),

    // Process pending/partial orders
    processPortfolioOrders: builder.mutation({
      query: ({ portfolioId }) => ({
        url: `/portfolios/${portfolioId}/orders/process`,
        method: 'POST',
      }),
      invalidatesTags: (result, error, { portfolioId }) => [
        { type: 'Portfolios', id: portfolioId },
        { type: 'Portfolios', id: 'LIST' },
        { type: 'Portfolios', id: 'SUMMARY' },
        { type: 'PortfolioTransactions', id: `LIST_${portfolioId}` },
      ],
    }),

    // Cancel a pending order
    cancelPortfolioOrder: builder.mutation({
      query: ({ portfolioId, orderId, reason }) => ({
        url: `/portfolios/${portfolioId}/orders/${orderId}/cancel`,
        method: 'POST',
        params: reason ? { reason } : undefined,
      }),
      invalidatesTags: (result, error, { portfolioId }) => [
        { type: 'Portfolios', id: portfolioId },
        { type: 'Portfolios', id: 'LIST' },
        { type: 'Portfolios', id: 'SUMMARY' },
        { type: 'PortfolioTransactions', id: `LIST_${portfolioId}` },
      ],
    }),

    // Deposit/withdraw cash
    adjustPortfolioCash: builder.mutation({
      query: ({ portfolioId, ...data }) => ({
        url: `/portfolios/${portfolioId}/cash`,
        method: 'POST',
        body: data,
      }),
      invalidatesTags: (result, error, { portfolioId }) => [
        { type: 'Portfolios', id: portfolioId },
        { type: 'Portfolios', id: 'LIST' },
        { type: 'Portfolios', id: 'SUMMARY' },
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
      query: ({ portfolioId, page = 0, size = 10, orderStatus }) => ({
        url: `/portfolios/${portfolioId}/transactions`,
        params: { page, size, orderStatus },
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
  useExecutePortfolioTradeMutation,
  useProcessPortfolioOrdersMutation,
  useCancelPortfolioOrderMutation,
  useAdjustPortfolioCashMutation,
  useUpdatePortfolioItemMutation,
  useRemovePortfolioItemMutation,
  useGetPortfolioTransactionsQuery,
} = portfolioApi
