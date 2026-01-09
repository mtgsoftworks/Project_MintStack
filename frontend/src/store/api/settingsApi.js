import { baseApi } from './baseApi'

export const settingsApi = baseApi.injectEndpoints({
    endpoints: (builder) => ({
        getApiConfigs: builder.query({
            query: () => '/settings/api-keys',
            providesTags: ['Settings'],
        }),
        addApiConfig: builder.mutation({
            query: (data) => ({
                url: '/settings/api-keys',
                method: 'POST',
                body: data,
            }),
            invalidatesTags: ['Settings'],
        }),
        deleteApiConfig: builder.mutation({
            query: (id) => ({
                url: `/settings/api-keys/${id}`,
                method: 'DELETE',
            }),
            invalidatesTags: ['Settings'],
        }),
    }),
})

export const {
    useGetApiConfigsQuery,
    useAddApiConfigMutation,
    useDeleteApiConfigMutation,
} = settingsApi
