import { baseApi } from './baseApi'

export const simulationApi = baseApi.injectEndpoints({
    endpoints: (builder) => ({
        getSimulationConfig: builder.query({
            query: () => '/simulation/config',
            providesTags: ['Simulation'],
        }),
        updateSimulationConfig: builder.mutation({
            query: (data) => ({
                url: '/simulation/config',
                method: 'POST',
                body: data,
            }),
            invalidatesTags: ['Simulation'],
        }),
        toggleSimulation: builder.mutation({
            query: () => ({
                url: '/simulation/toggle',
                method: 'POST',
            }),
            invalidatesTags: ['Simulation'],
        }),
        resetSimulation: builder.mutation({
            query: () => ({
                url: '/simulation/reset',
                method: 'POST',
            }),
            invalidatesTags: ['Simulation'],
        }),
        getSimulationStatus: builder.query({
            query: () => '/simulation/status',
            providesTags: ['Simulation'],
        }),
        getSimulatedStocks: builder.query({
            query: () => '/simulation/stocks',
            providesTags: ['Simulation'],
        }),
        getSimulatedCurrencies: builder.query({
            query: () => '/simulation/currencies',
            providesTags: ['Simulation'],
        }),
        getSimulatedIndices: builder.query({
            query: () => '/simulation/indices',
            providesTags: ['Simulation'],
        }),
    }),
})

export const {
    useGetSimulationConfigQuery,
    useUpdateSimulationConfigMutation,
    useToggleSimulationMutation,
    useResetSimulationMutation,
    useGetSimulationStatusQuery,
    useGetSimulatedStocksQuery,
    useGetSimulatedCurrenciesQuery,
    useGetSimulatedIndicesQuery,
} = simulationApi
