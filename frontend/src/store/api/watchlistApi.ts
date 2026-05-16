import { baseApi } from './baseApi'

export const watchlistApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getWatchlists: builder.query({
      query: () => '/watchlist',
      transformResponse: (response) => response.data,
      providesTags: (result) =>
        result
          ? [
              ...result.map(({ id }) => ({ type: 'Watchlists', id })),
              { type: 'Watchlists', id: 'LIST' },
            ]
          : [{ type: 'Watchlists', id: 'LIST' }],
    }),
    getWatchlist: builder.query({
      query: (id) => `/watchlist/${id}`,
      transformResponse: (response) => response.data,
      providesTags: (result, error, id) => [{ type: 'Watchlists', id }],
    }),
    createWatchlist: builder.mutation({
      query: (data) => ({
        url: '/watchlist',
        method: 'POST',
        body: data,
      }),
      transformResponse: (response) => response.data,
      invalidatesTags: [{ type: 'Watchlists', id: 'LIST' }],
    }),
    updateWatchlist: builder.mutation({
      query: ({ id, ...data }) => ({
        url: `/watchlist/${id}`,
        method: 'PUT',
        body: data,
      }),
      transformResponse: (response) => response.data,
      invalidatesTags: (result, error, { id }) => [
        { type: 'Watchlists', id },
        { type: 'Watchlists', id: 'LIST' },
      ],
    }),
    deleteWatchlist: builder.mutation({
      query: (id) => ({
        url: `/watchlist/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: (result, error, id) => [
        { type: 'Watchlists', id },
        { type: 'Watchlists', id: 'LIST' },
      ],
    }),
    addWatchlistInstrument: builder.mutation({
      query: ({ watchlistId, symbol }) => ({
        url: `/watchlist/${watchlistId}/items/${symbol}`,
        method: 'POST',
      }),
      transformResponse: (response) => response.data,
      invalidatesTags: (result, error, { watchlistId }) => [
        { type: 'Watchlists', id: watchlistId },
        { type: 'Watchlists', id: 'LIST' },
      ],
    }),
    addToDefaultWatchlist: builder.mutation({
      query: ({ symbol }) => ({
        url: `/watchlist/default/items/${symbol}`,
        method: 'POST',
      }),
      transformResponse: (response) => response.data,
      invalidatesTags: [{ type: 'Watchlists', id: 'LIST' }],
    }),
    removeWatchlistInstrument: builder.mutation({
      query: ({ watchlistId, symbol }) => ({
        url: `/watchlist/${watchlistId}/items/${symbol}`,
        method: 'DELETE',
      }),
      transformResponse: (response) => response.data,
      invalidatesTags: (result, error, { watchlistId }) => [
        { type: 'Watchlists', id: watchlistId },
        { type: 'Watchlists', id: 'LIST' },
      ],
    }),
    reorderWatchlistItems: builder.mutation({
      query: ({ watchlistId, itemIds }) => ({
        url: `/watchlist/${watchlistId}/items/order`,
        method: 'PUT',
        body: { itemIds },
      }),
      transformResponse: (response) => response.data,
      invalidatesTags: (result, error, { watchlistId }) => [
        { type: 'Watchlists', id: watchlistId },
        { type: 'Watchlists', id: 'LIST' },
      ],
    }),
    updateWatchlistItem: builder.mutation({
      query: ({ watchlistId, itemId, notes }) => ({
        url: `/watchlist/${watchlistId}/items/${itemId}`,
        method: 'PUT',
        body: { notes },
      }),
      transformResponse: (response) => response.data,
      invalidatesTags: (result, error, { watchlistId }) => [
        { type: 'Watchlists', id: watchlistId },
        { type: 'Watchlists', id: 'LIST' },
      ],
    }),
  }),
})

export const {
  useGetWatchlistsQuery,
  useGetWatchlistQuery,
  useCreateWatchlistMutation,
  useUpdateWatchlistMutation,
  useDeleteWatchlistMutation,
  useAddWatchlistInstrumentMutation,
  useAddToDefaultWatchlistMutation,
  useRemoveWatchlistInstrumentMutation,
  useReorderWatchlistItemsMutation,
  useUpdateWatchlistItemMutation,
} = watchlistApi
