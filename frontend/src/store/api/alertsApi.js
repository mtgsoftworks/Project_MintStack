import { baseApi } from './baseApi'

export const alertsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAlerts: builder.query({
      query: () => '/alerts',
      transformResponse: (response) => response.data,
      providesTags: (result) =>
        result
          ? [
              ...result.map(({ id }) => ({ type: 'Alerts', id })),
              { type: 'Alerts', id: 'LIST' },
            ]
          : [{ type: 'Alerts', id: 'LIST' }],
    }),
    getActiveAlerts: builder.query({
      query: () => '/alerts/active',
      transformResponse: (response) => response.data,
      providesTags: [{ type: 'Alerts', id: 'ACTIVE' }],
    }),
    createAlert: builder.mutation({
      query: (data) => ({
        url: '/alerts',
        method: 'POST',
        body: data,
      }),
      invalidatesTags: [{ type: 'Alerts', id: 'LIST' }, { type: 'Alerts', id: 'ACTIVE' }],
    }),
    deleteAlert: builder.mutation({
      query: (id) => ({
        url: `/alerts/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: (result, error, id) => [
        { type: 'Alerts', id },
        { type: 'Alerts', id: 'LIST' },
        { type: 'Alerts', id: 'ACTIVE' },
      ],
    }),
    deactivateAlert: builder.mutation({
      query: (id) => ({
        url: `/alerts/${id}/deactivate`,
        method: 'PUT',
      }),
      invalidatesTags: (result, error, id) => [
        { type: 'Alerts', id },
        { type: 'Alerts', id: 'LIST' },
        { type: 'Alerts', id: 'ACTIVE' },
      ],
    }),
  }),
})

export const {
  useGetAlertsQuery,
  useGetActiveAlertsQuery,
  useCreateAlertMutation,
  useDeleteAlertMutation,
  useDeactivateAlertMutation,
} = alertsApi
