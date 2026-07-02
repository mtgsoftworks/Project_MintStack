import { baseApi } from './baseApi'

interface ApiResponse<T> {
    data: T
}

export interface SimulationConfig {
    enabled: boolean
    volatilityLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'EXTREME'
    marketTrend: 'BULLISH' | 'NEUTRAL' | 'BEARISH'
    updateIntervalSeconds: number
}

export interface SimulationMetrics {
    tickCount: number
    stocks: number
    bonds: number
    funds: number
    viop: number
    currencies: number
    indices: number
    activeEvents: number
    uptime: { seconds: number }
}

export interface SimulationHealth {
    status: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY'
}

export interface SimulationEvent {
    id: string
    type: string
    severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'
    description: string
    remainingDurationTicks: number
}

export interface VolatilityData {
    regimeDistribution: Record<string, number>
}

export const simulationApi = baseApi.injectEndpoints({
    endpoints: (builder) => ({
        getSimulationConfig: builder.query<ApiResponse<SimulationConfig>, void>({
            query: () => '/simulation/config',
            providesTags: ['Simulation'],
        }),
        updateSimulationConfig: builder.mutation<ApiResponse<SimulationConfig>, Partial<SimulationConfig>>({
            query: (data) => ({
                url: '/simulation/config',
                method: 'POST',
                body: data,
            }),
            invalidatesTags: ['Simulation'],
        }),
        toggleSimulation: builder.mutation<ApiResponse<SimulationConfig>, void>({
            query: () => ({
                url: '/simulation/toggle',
                method: 'POST',
            }),
            invalidatesTags: ['Simulation'],
        }),
        resetSimulation: builder.mutation<ApiResponse<string>, void>({
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
        getSimulatedBonds: builder.query({
            query: () => '/simulation/bonds',
            providesTags: ['Simulation'],
        }),
        getSimulatedFunds: builder.query({
            query: () => '/simulation/funds',
            providesTags: ['Simulation'],
        }),
        getSimulatedViop: builder.query({
            query: () => '/simulation/viop',
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
        getSimulationMetrics: builder.query<ApiResponse<SimulationMetrics>, void>({
            query: () => '/simulation/metrics',
            providesTags: ['Simulation'],
        }),
        getSimulationHealth: builder.query<ApiResponse<SimulationHealth>, void>({
            query: () => '/simulation/health',
            providesTags: ['Simulation'],
        }),
        getSimulationEvents: builder.query<ApiResponse<SimulationEvent[]>, void>({
            query: () => '/simulation/events',
            providesTags: ['Simulation'],
        }),
        getSimulationVolatility: builder.query<ApiResponse<VolatilityData>, void>({
            query: () => '/simulation/volatility',
            providesTags: ['Simulation'],
        }),
        triggerSimulationEvent: builder.mutation<ApiResponse<SimulationEvent>, string>({
            query: (eventType) => ({
                url: '/simulation/events/trigger',
                method: 'POST',
                params: { eventType },
            }),
            invalidatesTags: ['Simulation'],
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
    useGetSimulatedBondsQuery,
    useGetSimulatedFundsQuery,
    useGetSimulatedViopQuery,
    useGetSimulatedCurrenciesQuery,
    useGetSimulatedIndicesQuery,
    useGetSimulationMetricsQuery,
    useGetSimulationHealthQuery,
    useGetSimulationEventsQuery,
    useGetSimulationVolatilityQuery,
    useTriggerSimulationEventMutation,
} = simulationApi
