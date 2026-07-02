import { baseApi } from './baseApi'

export const watchlistApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getWatchlists: builder.query({
      query: () => '/watchlist',
      transformResponse: (response) => response.data,
      providesTags: (result) =>
        result
          ? [
              ...result.map(({ id }: { id: string | number }) => ({ type: 'Watchlists' as const, id })),
              { type: 'Watchlists' as const, id: 'LIST' as const },
            ]
          : [{ type: 'Watchlists' as const, id: 'LIST' as const }],
    }),
    getWatchlist: builder.query({
      query: (id) => `/watchlist/${id}`,
      transformResponse: (response) => response.data,
      providesTags: (_result, _error, id: string | number) => [{ type: 'Watchlists' as const, id }],
    }),
    createWatchlist: builder.mutation({
      query: (data) => ({
        url: '/watchlist',
        method: 'POST',
        body: data,
      }),
      transformResponse: (response) => response.data,
      invalidatesTags: [{ type: 'Watchlists' as const, id: 'LIST' as const }],
    }),
    updateWatchlist: builder.mutation({
      query: ({ id, ...data }) => ({
        url: `/watchlist/${id}`,
        method: 'PUT',
        body: data,
      }),
      transformResponse: (response) => response.data,
      invalidatesTags: (_result, _error, { id }: { id: string | number }) => [
        { type: 'Watchlists' as const, id },
        { type: 'Watchlists' as const, id: 'LIST' as const },
      ],
    }),
    deleteWatchlist: builder.mutation({
      query: (id) => ({
        url: `/watchlist/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: (_result, _error, id: string | number) => [
        { type: 'Watchlists' as const, id },
        { type: 'Watchlists' as const, id: 'LIST' as const },
      ],
    }),
    addWatchlistInstrument: builder.mutation({
      query: ({ watchlistId, symbol }: { watchlistId: string | number; symbol: string }) => ({
        url: `/watchlist/${watchlistId}/items/${symbol}`,
        method: 'POST',
      }),
      transformResponse: (response) => response.data,
      invalidatesTags: (_result, _error, { watchlistId }: { watchlistId: string | number }) => [
        { type: 'Watchlists' as const, id: watchlistId },
        { type: 'Watchlists' as const, id: 'LIST' as const },
      ],
    }),
    addToDefaultWatchlist: builder.mutation({
      query: ({ symbol }) => ({
        url: `/watchlist/default/items/${symbol}`,
        method: 'POST',
      }),
      transformResponse: (response) => response.data,
      invalidatesTags: [{ type: 'Watchlists' as const, id: 'LIST' as const }],
    }),
    removeWatchlistInstrument: builder.mutation({
      query: ({ watchlistId, symbol }: { watchlistId: string | number; symbol: string }) => ({
        url: `/watchlist/${watchlistId}/items/${symbol}`,
        method: 'DELETE',
      }),
      transformResponse: (response) => response.data,
      invalidatesTags: (_result, _error, { watchlistId }: { watchlistId: string | number }) => [
        { type: 'Watchlists' as const, id: watchlistId },
        { type: 'Watchlists' as const, id: 'LIST' as const },
      ],
    }),
    reorderWatchlistItems: builder.mutation({
      query: ({ watchlistId, itemIds }: { watchlistId: string | number; itemIds: (string | number)[] }) => ({
        url: `/watchlist/${watchlistId}/items/order`,
        method: 'PUT',
        body: { itemIds },
      }),
      transformResponse: (response) => response.data,
      invalidatesTags: (_result, _error, { watchlistId }: { watchlistId: string | number }) => [
        { type: 'Watchlists' as const, id: watchlistId },
        { type: 'Watchlists' as const, id: 'LIST' as const },
      ],
    }),
    updateWatchlistItem: builder.mutation({
      query: ({ watchlistId, itemId, notes }: { watchlistId: string | number; itemId: string | number; notes?: string }) => ({
        url: `/watchlist/${watchlistId}/items/${itemId}`,
        method: 'PUT',
        body: { notes },
      }),
      transformResponse: (response) => response.data,
      invalidatesTags: (_result, _error, { watchlistId }: { watchlistId: string | number }) => [
        { type: 'Watchlists' as const, id: watchlistId },
        { type: 'Watchlists' as const, id: 'LIST' as const },
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
