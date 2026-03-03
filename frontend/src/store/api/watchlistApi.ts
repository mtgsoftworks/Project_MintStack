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
      invalidatesTags: [{ type: 'Watchlists', id: 'LIST' }],
    }),
    updateWatchlist: builder.mutation({
      query: ({ id, ...data }) => ({
        url: `/watchlist/${id}`,
        method: 'PUT',
        body: data,
      }),
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
      invalidatesTags: (result, error, { watchlistId }) => [
        { type: 'Watchlists', id: watchlistId },
        { type: 'Watchlists', id: 'LIST' },
      ],
    }),
    removeWatchlistInstrument: builder.mutation({
      query: ({ watchlistId, symbol }) => ({
        url: `/watchlist/${watchlistId}/items/${symbol}`,
        method: 'DELETE',
      }),
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
  useRemoveWatchlistInstrumentMutation,
} = watchlistApi
