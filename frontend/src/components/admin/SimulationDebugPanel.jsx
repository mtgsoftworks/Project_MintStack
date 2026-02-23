import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { Play, Pause, RefreshCw, Zap, Activity, Database, Wifi, Clock } from 'lucide-react'
import api from '../../services/api'

export function SimulationDebugPanel() {
    const { t } = useTranslation()
    const [config, setConfig] = useState(null)
    const [metrics, setMetrics] = useState(null)
    const [health, setHealth] = useState(null)
    const [events, setEvents] = useState([])
    const [volatility, setVolatility] = useState(null)
    const [loading, setLoading] = useState(true)
    const [updating, setUpdating] = useState(false)
    
    useEffect(() => {
        fetchAllData()
        const interval = setInterval(fetchAllData, 5000)
        return () => clearInterval(interval)
    }, [])
    
    const fetchAllData = async () => {
        try {
            const [configRes, metricsRes, eventsRes, volatilityRes, healthRes] = await Promise.all([
                api.get('/simulation/config'),
                api.get('/simulation/metrics'),
                api.get('/simulation/events'),
                api.get('/simulation/volatility'),
                api.get('/simulation/health')
            ])
            setConfig(configRes.data.data)
            setMetrics(metricsRes.data.data)
            setEvents(eventsRes.data.data || [])
            setVolatility(volatilityRes.data.data)
            setHealth(healthRes.data.data)
        } catch (error) {
            console.error('Failed to fetch debug data:', error)
        } finally {
            setLoading(false)
        }
    }
    
    const toggleSimulation = async () => {
        try {
            setUpdating(true)
            await api.post('/simulation/toggle')
            await fetchAllData()
        } catch (error) {
            console.error('Failed to toggle simulation:', error)
        } finally {
            setUpdating(false)
        }
    }
    
    const triggerEvent = async (eventType) => {
        try {
            setUpdating(true)
            await api.post(`/simulation/events/trigger?eventType=${eventType}`)
            await fetchAllData()
        } catch (error) {
            console.error('Failed to trigger event:', error)
        } finally {
            setUpdating(false)
        }
    }
    
    const resetSimulation = async () => {
        if (!confirm(t('common.confirm'))) return
        try {
            setUpdating(true)
            await api.post('/simulation/reset')
            await fetchAllData()
        } catch (error) {
            console.error('Failed to reset simulation:', error)
        } finally {
            setUpdating(false)
        }
    }
    
    const updateConfig = async (updates) => {
        try {
            setUpdating(true)
            await api.post('/simulation/config', { ...config, ...updates })
            await fetchAllData()
        } catch (error) {
            console.error('Failed to update config:', error)
        } finally {
            setUpdating(false)
        }
    }
    
    const formatDuration = (duration) => {
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
                    <h2 className="text-2xl font-bold text-gray-900">{t('simulation.debugPanel')}</h2>
                    <p className="text-sm text-gray-500">{t('simulation.debugPanelDesc')}</p>
                </div>
                <div className="flex gap-2">
                    <button
                        onClick={fetchAllData}
                        disabled={updating}
                        className="flex items-center gap-2 px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg disabled:opacity-50"
                    >
                        <RefreshCw className={`w-4 h-4 ${updating ? 'animate-spin' : ''}`} />
                        {t('common.refresh')}
                    </button>
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
            
            <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
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
                    title={t('simulation.metrics.cryptos')} 
                    value={metrics?.cryptos || 0} 
                    icon={Zap}
                />
                <StatusCard 
                    title={t('simulation.metrics.activeEvents')} 
                    value={metrics?.activeEvents || 0} 
                    icon={Zap}
                    highlight={metrics?.activeEvents > 0}
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
            
            <div className="bg-white rounded-xl shadow-sm p-4">
                <h3 className="font-semibold mb-4 text-gray-900">{t('simulation.config.title')}</h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            {t('simulation.config.volatility')}
                        </label>
                        <select 
                            value={config?.volatilityLevel || 'MEDIUM'}
                            onChange={(e) => updateConfig({ volatilityLevel: e.target.value })}
                            disabled={updating}
                            className="w-full p-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
                        >
                            <option value="LOW">{t('simulation.config.volatilityLow')}</option>
                            <option value="MEDIUM">{t('simulation.config.volatilityMedium')}</option>
                            <option value="HIGH">{t('simulation.config.volatilityHigh')}</option>
                            <option value="EXTREME">{t('simulation.config.volatilityExtreme')}</option>
                        </select>
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            {t('simulation.config.trend')}
                        </label>
                        <select 
                            value={config?.marketTrend || 'NEUTRAL'}
                            onChange={(e) => updateConfig({ marketTrend: e.target.value })}
                            disabled={updating}
                            className="w-full p-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
                        >
                            <option value="BULLISH">{t('simulation.config.trendBullish')}</option>
                            <option value="NEUTRAL">{t('simulation.config.trendNeutral')}</option>
                            <option value="BEARISH">{t('simulation.config.trendBearish')}</option>
                        </select>
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            {t('simulation.config.updateInterval')}
                        </label>
                        <input 
                            type="number"
                            value={config?.updateIntervalSeconds || 1}
                            onChange={(e) => updateConfig({ updateIntervalSeconds: parseInt(e.target.value) })}
                            disabled={updating}
                            className="w-full p-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
                            min="1"
                            max="60"
                        />
                    </div>
                </div>
            </div>
            
            <div className="bg-white rounded-xl shadow-sm p-4">
                <h3 className="font-semibold mb-4 text-gray-900">{t('simulation.events.trigger')}</h3>
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
                <div className="bg-white rounded-xl shadow-sm p-4">
                    <h3 className="font-semibold mb-4 text-gray-900">{t('simulation.events.active')}</h3>
                    <div className="space-y-2">
                        {events.map(event => (
                            <div key={event.id} className="p-3 bg-yellow-50 rounded-lg border border-yellow-200">
                                <div className="flex justify-between items-start">
                                    <div className="font-medium text-gray-900">{event.type}</div>
                                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                                        event.severity === 'CRITICAL' ? 'bg-red-100 text-red-700' :
                                        event.severity === 'HIGH' ? 'bg-orange-100 text-orange-700' :
                                        event.severity === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700' :
                                        'bg-green-100 text-green-700'
                                    }`}>
                                        {event.severity}
                                    </span>
                                </div>
                                <div className="text-sm text-gray-600 mt-1">{event.description}</div>
                                <div className="text-xs text-gray-400 mt-2">
                                    {t('simulation.events.remaining')}: {event.remainingDurationTicks} tick
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
            
            {volatility && (
                <div className="bg-white rounded-xl shadow-sm p-4">
                    <h3 className="font-semibold mb-4 text-gray-900">{t('simulation.volatility.distribution')}</h3>
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
                                <div className="font-medium text-gray-700">{regime}</div>
                                <div className="text-2xl font-bold text-gray-900">{count}</div>
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

function StatusCard({ title, value, icon: Icon, highlight, valueClassName }) {
    return (
        <div className={`bg-white rounded-xl p-4 shadow-sm ${highlight ? 'ring-2 ring-yellow-400' : ''}`}>
            <div className="flex items-center gap-2 mb-2">
                {Icon && <Icon className="w-4 h-4 text-gray-400" />}
                <div className="text-sm text-gray-500">{title}</div>
            </div>
            <div className={`text-2xl font-bold ${valueClassName || 'text-gray-900'}`}>{value}</div>
        </div>
    )
}

export default SimulationDebugPanel
