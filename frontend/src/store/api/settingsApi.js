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
        clearCache: builder.mutation({
            query: () => ({
                url: '/settings/cache',
                method: 'DELETE',
            }),
        }),
        deleteMarketData: builder.mutation({
            query: () => ({
                url: '/settings/market-data',
                method: 'DELETE',
            }),
            invalidatesTags: ['Market', 'Currency'],
        }),
        // Data Source endpoints
        getDataSourceCapabilities: builder.query({
            query: () => '/data-sources/capabilities',
            providesTags: ['DataSources'],
        }),
        getDataPreferences: builder.query({
            query: () => '/data-sources/preferences',
            providesTags: ['DataSources'],
        }),
        setDataPreference: builder.mutation({
            query: (data) => ({
                url: '/data-sources/preferences',
                method: 'POST',
                body: data,
            }),
            invalidatesTags: ['DataSources'],
        }),
        triggerDataFetch: builder.mutation({
            query: (apiConfigId) => ({
                url: `/data-sources/trigger/${apiConfigId}`,
                method: 'POST',
            }),
            invalidatesTags: ['DataSources', 'Settings'],
        }),
    }),
})

export const {
    useGetApiConfigsQuery,
    useTestApiKeyMutation,
    useAddApiConfigMutation,
    useDeleteApiConfigMutation,
    useClearCacheMutation,
    useDeleteMarketDataMutation,
    useGetDataSourceCapabilitiesQuery,
    useGetDataPreferencesQuery,
    useSetDataPreferenceMutation,
    useTriggerDataFetchMutation,
} = settingsApi

