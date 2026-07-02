import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Play, Pause, RefreshCw, Zap, Activity, Database, Wifi, Clock } from 'lucide-react'
import RefreshButton from '@/components/common/RefreshButton'
import {
    useGetSimulationConfigQuery,
    useGetSimulationEventsQuery,
    useGetSimulationHealthQuery,
    useGetSimulationMetricsQuery,
    useGetSimulationVolatilityQuery,
    useResetSimulationMutation,
    useToggleSimulationMutation,
    useTriggerSimulationEventMutation,
    useUpdateSimulationConfigMutation,
    type SimulationConfig,
} from '@/store/api/simulationApi'

export function SimulationDebugPanel() {
    const { t } = useTranslation()
    const [refreshing, setRefreshing] = useState(false)
    const polling = { pollingInterval: 5000 }
    const configQuery = useGetSimulationConfigQuery(undefined, polling)
    const metricsQuery = useGetSimulationMetricsQuery(undefined, polling)
    const eventsQuery = useGetSimulationEventsQuery(undefined, polling)
    const volatilityQuery = useGetSimulationVolatilityQuery(undefined, polling)
    const healthQuery = useGetSimulationHealthQuery(undefined, polling)
    const [toggleSimulationMutation, toggleState] = useToggleSimulationMutation()
    const [resetSimulationMutation, resetState] = useResetSimulationMutation()
    const [updateSimulationConfig, updateState] = useUpdateSimulationConfigMutation()
    const [triggerSimulationEvent, triggerState] = useTriggerSimulationEventMutation()

    const config = configQuery.data?.data
    const metrics = metricsQuery.data?.data
    const events = eventsQuery.data?.data ?? []
    const volatility = volatilityQuery.data?.data
    const health = healthQuery.data?.data
    const loading = [
        configQuery,
        metricsQuery,
        eventsQuery,
        volatilityQuery,
        healthQuery,
    ].some((query) => query.isLoading)
    const updating = [
        toggleState,
        resetState,
        updateState,
        triggerState,
    ].some((state) => state.isLoading)

    const refreshAllData = () => Promise.all([
        configQuery.refetch(),
        metricsQuery.refetch(),
        eventsQuery.refetch(),
        volatilityQuery.refetch(),
        healthQuery.refetch(),
    ])

    const handleRefresh = async () => {
        try {
            setRefreshing(true)
            await refreshAllData()
        } finally {
            setRefreshing(false)
        }
    }
    
    const toggleSimulation = async () => {
        try {
            await toggleSimulationMutation().unwrap()
            await refreshAllData()
        } catch (error) {
            console.error('Failed to toggle simulation:', error)
        }
    }
    
    const triggerEvent = async (eventType: string) => {
        try {
            await triggerSimulationEvent(eventType).unwrap()
            await refreshAllData()
        } catch (error) {
            console.error('Failed to trigger event:', error)
        }
    }
    
    const resetSimulation = async () => {
        if (!confirm(t('common.confirm'))) return
        try {
            await resetSimulationMutation().unwrap()
            await refreshAllData()
        } catch (error) {
            console.error('Failed to reset simulation:', error)
        }
    }
    
    const updateConfig = async (updates: Partial<SimulationConfig>) => {
        try {
            await updateSimulationConfig({ ...config, ...updates }).unwrap()
            await refreshAllData()
        } catch (error) {
            console.error('Failed to update config:', error)
        }
    }
    
    const formatDuration = (duration: { seconds: number } | null | undefined) => {
        if (!duration) return '0s'
        const hours = Math.floor(duration.seconds / 3600)
        const minutes = Math.floor((duration.seconds % 3600) / 60)
        const seconds = duration.seconds % 60
        if (hours > 0) return `${hours}h ${minutes}m`
        if (minutes > 0) return `${minutes}m ${seconds}s`
        return `${seconds}s`
    }
    
    if (loading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-500" />
            </div>
        )
    }

    const statusColor = health?.status === 'HEALTHY' ? 'text-green-500' : 
                        health?.status === 'DEGRADED' ? 'text-yellow-500' : 'text-red-500'
    
    return (
        <div className="space-y-6 p-6">
            <div className="flex justify-between items-center">
                <div>
                    <h2 className="text-2xl font-bold text-foreground">{t('simulation.debugPanel')}</h2>
                    <p className="text-sm text-muted-foreground">{t('simulation.debugPanelDesc')}</p>
                </div>
                <div className="flex gap-2">
                    <RefreshButton
                        variant="ghost"
                        className="px-4 py-2 text-muted-foreground hover:bg-muted"
                        onRefresh={handleRefresh}
                        isLoading={refreshing}
                        disabled={updating}
                    >
                        {t('common.refresh')}
                    </RefreshButton>
                    <button
                        onClick={toggleSimulation}
                        disabled={updating}
                        className={`flex items-center gap-2 px-4 py-2 rounded-lg text-white disabled:opacity-50 ${
                            config?.enabled ? 'bg-red-500 hover:bg-red-600' : 'bg-green-500 hover:bg-green-600'
                        }`}
                    >
                        {config?.enabled ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
                        {config?.enabled ? t('simulation.stop') : t('simulation.start')}
                    </button>
                </div>
            </div>
            
            <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-8 gap-4">
                <StatusCard 
                    title={t('simulation.metrics.tickCount')} 
                    value={metrics?.tickCount || 0} 
                    icon={Clock}
                />
                <StatusCard 
                    title={t('simulation.metrics.stocks')} 
                    value={metrics?.stocks || 0} 
                    icon={Activity}
                />
                <StatusCard
                    title={t('simulation.metrics.bonds')}
                    value={metrics?.bonds || 0}
                    icon={Activity}
                />
                <StatusCard
                    title={t('simulation.metrics.funds')}
                    value={metrics?.funds || 0}
                    icon={Activity}
                />
                <StatusCard
                    title={t('simulation.metrics.viop')}
                    value={metrics?.viop || 0}
                    icon={Activity}
                />
                <StatusCard
                    title={t('simulation.metrics.currencies')}
                    value={metrics?.currencies || 0}
                    icon={Activity}
                />
                <StatusCard
                    title={t('simulation.metrics.indices')}
                    value={metrics?.indices || 0}
                    icon={Activity}
                />
                <StatusCard 
                    title={t('simulation.metrics.activeEvents')} 
                    value={metrics?.activeEvents || 0} 
                    icon={Zap}
                    highlight={(metrics?.activeEvents ?? 0) > 0}
                />
                <StatusCard 
                    title={t('simulation.metrics.uptime')} 
                    value={formatDuration(metrics?.uptime)} 
                    icon={Clock}
                />
                <StatusCard 
                    title={t('simulation.health.status')} 
                    value={health?.status || 'UNKNOWN'} 
                    icon={health?.status === 'HEALTHY' ? Database : Wifi}
                    valueClassName={statusColor}
                />
            </div>
            
            <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
                <h3 className="mb-4 font-semibold text-foreground">{t('simulation.config.title')}</h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div>
                        <label className="mb-1 block text-sm font-medium text-foreground">
                            {t('simulation.config.volatility')}
                        </label>
                        <select 
                            value={config?.volatilityLevel || 'MEDIUM'}
                            onChange={(e) => updateConfig({ volatilityLevel: e.target.value as 'LOW' | 'MEDIUM' | 'HIGH' | 'EXTREME' })}
                            disabled={updating}
                            className="w-full rounded-lg border border-input bg-background p-2 text-foreground focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500"
                        >
                            <option value="LOW">{t('simulation.config.volatilityLow')}</option>
                            <option value="MEDIUM">{t('simulation.config.volatilityMedium')}</option>
                            <option value="HIGH">{t('simulation.config.volatilityHigh')}</option>
                            <option value="EXTREME">{t('simulation.config.volatilityExtreme')}</option>
                        </select>
                    </div>
                    <div>
                        <label className="mb-1 block text-sm font-medium text-foreground">
                            {t('simulation.config.trend')}
                        </label>
                        <select 
                            value={config?.marketTrend || 'NEUTRAL'}
                            onChange={(e) => updateConfig({ marketTrend: e.target.value as 'BULLISH' | 'NEUTRAL' | 'BEARISH' })}
                            disabled={updating}
                            className="w-full rounded-lg border border-input bg-background p-2 text-foreground focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500"
                        >
                            <option value="BULLISH">{t('simulation.config.trendBullish')}</option>
                            <option value="NEUTRAL">{t('simulation.config.trendNeutral')}</option>
                            <option value="BEARISH">{t('simulation.config.trendBearish')}</option>
                        </select>
                    </div>
                    <div>
                        <label className="mb-1 block text-sm font-medium text-foreground">
                            {t('simulation.config.updateInterval')}
                        </label>
                        <input 
                            type="number"
                            value={config?.updateIntervalSeconds || 1}
                            onChange={(e) => updateConfig({ updateIntervalSeconds: parseInt(e.target.value) })}
                            disabled={updating}
                            className="w-full rounded-lg border border-input bg-background p-2 text-foreground focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500"
                            min="1"
                            max="60"
                        />
                    </div>
                </div>
            </div>
            
            <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
                <h3 className="mb-4 font-semibold text-foreground">{t('simulation.events.trigger')}</h3>
                <div className="flex flex-wrap gap-2">
                    {['CIRCUIT_BREAKER', 'SHORT_SQUEEZE', 'WHALE_ACTIVITY', 'RALLY', 'FLASH_CRASH', 'VOLATILITY_SPIKE'].map(type => (
                        <button
                            key={type}
                            onClick={() => triggerEvent(type)}
                            disabled={updating}
                            className="px-3 py-1.5 bg-blue-100 text-blue-700 rounded-lg hover:bg-blue-200 disabled:opacity-50 text-sm font-medium"
                        >
                            {type.replace(/_/g, ' ')}
                        </button>
                    ))}
                </div>
            </div>
            
            {events.length > 0 && (
                <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
                    <h3 className="mb-4 font-semibold text-foreground">{t('simulation.events.active')}</h3>
                    <div className="space-y-2">
                        {events.map(event => (
                            <div key={event.id} className="p-3 bg-yellow-50 rounded-lg border border-yellow-200">
                                <div className="flex justify-between items-start">
                                    <div className="font-medium text-foreground">{event.type}</div>
                                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                                        event.severity === 'CRITICAL' ? 'bg-red-100 text-red-700' :
                                        event.severity === 'HIGH' ? 'bg-orange-100 text-orange-700' :
                                        event.severity === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700' :
                                        'bg-green-100 text-green-700'
                                    }`}>
                                        {event.severity}
                                    </span>
                                </div>
                                <div className="mt-1 text-sm text-muted-foreground">{event.description}</div>
                                <div className="mt-2 text-xs text-muted-foreground">
                                    {t('simulation.events.remaining')}: {event.remainingDurationTicks} tick
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
            
            {volatility && (
                <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
                    <h3 className="mb-4 font-semibold text-foreground">{t('simulation.volatility.distribution')}</h3>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                        {Object.entries(volatility.regimeDistribution || {}).map(([regime, count]) => (
                            <div 
                                key={regime} 
                                className={`p-4 rounded-lg ${
                                    regime === 'CRISIS' ? 'bg-red-100' :
                                    regime === 'HIGH' ? 'bg-orange-100' :
                                    regime === 'NORMAL' ? 'bg-green-100' : 'bg-blue-100'
                                }`}
                            >
                                <div className="font-medium text-foreground">{regime}</div>
                                <div className="text-2xl font-bold text-foreground">{String(count)}</div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
            
            <div className="flex gap-4">
                <button
                    onClick={resetSimulation}
                    disabled={updating}
                    className="flex items-center gap-2 px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 disabled:opacity-50"
                >
                    <RefreshCw className="w-4 h-4" />
                    {t('simulation.reset')}
                </button>
            </div>
        </div>
    )
}

interface StatusCardProps {
    title: string
    value: string | number
    icon: React.ComponentType<{ className?: string }>
    highlight?: boolean
    valueClassName?: string
}

function StatusCard({ title, value, icon: Icon, highlight = false, valueClassName = '' }: StatusCardProps) {
    return (
        <div className={`rounded-xl border border-border bg-card p-4 shadow-sm ${highlight ? 'ring-2 ring-yellow-400' : ''}`}>
            <div className="flex items-center gap-2 mb-2">
                {Icon && <Icon className="h-4 w-4 text-muted-foreground" />}
                <div className="text-sm text-muted-foreground">{title}</div>
            </div>
            <div className={`text-2xl font-bold ${valueClassName || 'text-foreground'}`}>{value}</div>
        </div>
    )
}

export default SimulationDebugPanel
