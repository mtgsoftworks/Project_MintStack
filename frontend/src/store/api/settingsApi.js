import { baseApi } from './baseApi'

export const settingsApi = baseApi.injectEndpoints({
    endpoints: (builder) => ({
        getApiConfigs: builder.query({
            query: () => '/settings/api-keys',
            providesTags: ['Settings'],
        }),
        testApiKey: builder.mutation({
            query: (data) => ({
                url: '/settings/api-keys/test',
                method: 'POST',
                body: data,
            }),
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
    useTestApiKeyMutation,
    useAddApiConfigMutation,
    useDeleteApiConfigMutation,
} = settingsApi

