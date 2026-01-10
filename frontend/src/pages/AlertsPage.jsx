import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus, Trash2, Bell, BellOff, TrendingUp, TrendingDown } from 'lucide-react'
import alertService from '@/services/alertService'

const ALERT_TYPES = [
    { value: 'PRICE_ABOVE', labelKey: 'alerts.priceAbove', icon: TrendingUp },
    { value: 'PRICE_BELOW', labelKey: 'alerts.priceBelow', icon: TrendingDown },
    { value: 'PERCENT_UP', labelKey: 'alerts.percentUp', icon: TrendingUp },
    { value: 'PERCENT_DOWN', labelKey: 'alerts.percentDown', icon: TrendingDown },
]

export default function AlertsPage() {
    const { t } = useTranslation()
    const [alerts, setAlerts] = useState([])
    const [loading, setLoading] = useState(true)
    const [showCreateModal, setShowCreateModal] = useState(false)
    const [newAlert, setNewAlert] = useState({
        symbol: '',
        alertType: 'PRICE_ABOVE',
        targetValue: '',
        notes: '',
    })

    useEffect(() => {
        loadAlerts()
    }, [])

    const loadAlerts = async () => {
        try {
            setLoading(true)
            const response = await alertService.getAll()
            setAlerts(response.data || [])
        } catch (error) {
            console.error('Error loading alerts:', error)
        } finally {
            setLoading(false)
        }
    }

    const handleCreateAlert = async () => {
        if (!newAlert.symbol || !newAlert.targetValue) return
        try {
            await alertService.create({
                symbol: newAlert.symbol.toUpperCase(),
                alertType: newAlert.alertType,
                targetValue: parseFloat(newAlert.targetValue),
                notes: newAlert.notes,
            })
            setNewAlert({ symbol: '', alertType: 'PRICE_ABOVE', targetValue: '', notes: '' })
            setShowCreateModal(false)
            loadAlerts()
        } catch (error) {
            console.error('Error creating alert:', error)
        }
    }

    const handleDeleteAlert = async (id) => {
        if (!confirm(t('common.confirm'))) return
        try {
            await alertService.delete(id)
            loadAlerts()
        } catch (error) {
            console.error('Error deleting alert:', error)
        }
    }

    const handleDeactivate = async (id) => {
        try {
            await alertService.deactivate(id)
            loadAlerts()
        } catch (error) {
            console.error('Error deactivating alert:', error)
        }
    }

    const getAlertTypeLabel = (type) => {
        const alertType = ALERT_TYPES.find(t => t.value === type)
        return alertType ? t(alertType.labelKey) : type
    }

    if (loading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-500" />
            </div>
        )
    }

    return (
        <div className="p-6">
            <div className="flex items-center justify-between mb-6">
                <h1 className="text-2xl font-bold text-gray-900">{t('alerts.title')}</h1>
                <button
                    onClick={() => setShowCreateModal(true)}
                    className="flex items-center gap-2 px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600 transition-colors"
                >
                    <Plus className="w-4 h-4" />
                    {t('alerts.create')}
                </button>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                <div className="bg-white rounded-xl p-4 shadow-sm">
                    <div className="text-sm text-gray-500">{t('alerts.active')}</div>
                    <div className="text-2xl font-bold text-emerald-600">
                        {alerts.filter(a => a.isActive).length}
                    </div>
                </div>
                <div className="bg-white rounded-xl p-4 shadow-sm">
                    <div className="text-sm text-gray-500">{t('alerts.triggered')}</div>
                    <div className="text-2xl font-bold text-orange-600">
                        {alerts.filter(a => a.isTriggered).length}
                    </div>
                </div>
                <div className="bg-white rounded-xl p-4 shadow-sm">
                    <div className="text-sm text-gray-500">{t('alerts.inactive')}</div>
                    <div className="text-2xl font-bold text-gray-400">
                        {alerts.filter(a => !a.isActive && !a.isTriggered).length}
                    </div>
                </div>
            </div>

            {/* Alerts List */}
            <div className="bg-white rounded-xl shadow-sm overflow-hidden">
                <table className="w-full">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">{t('portfolio.symbol')}</th>
                            <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">Tip</th>
                            <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">{t('alerts.targetPrice')}</th>
                            <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">Durum</th>
                            <th className="px-4 py-3 text-right text-sm font-medium text-gray-600"></th>
                        </tr>
                    </thead>
                    <tbody className="divide-y">
                        {alerts.map((alert) => (
                            <tr key={alert.id} className="hover:bg-gray-50">
                                <td className="px-4 py-3">
                                    <div>
                                        <div className="font-medium text-gray-900">{alert.symbol}</div>
                                        <div className="text-sm text-gray-500">{alert.instrumentName}</div>
                                    </div>
                                </td>
                                <td className="px-4 py-3">
                                    <span className="px-2 py-1 bg-gray-100 rounded text-sm">
                                        {getAlertTypeLabel(alert.alertType)}
                                    </span>
                                </td>
                                <td className="px-4 py-3 font-medium">
                                    {alert.alertType.includes('PERCENT')
                                        ? `%${alert.targetValue}`
                                        : `₺${alert.targetValue?.toLocaleString('tr-TR')}`}
                                </td>
                                <td className="px-4 py-3">
                                    {alert.isTriggered ? (
                                        <span className="flex items-center gap-1 text-orange-600">
                                            <Bell className="w-4 h-4" />
                                            {t('alerts.triggered')}
                                        </span>
                                    ) : alert.isActive ? (
                                        <span className="flex items-center gap-1 text-emerald-600">
                                            <Bell className="w-4 h-4" />
                                            {t('alerts.active')}
                                        </span>
                                    ) : (
                                        <span className="flex items-center gap-1 text-gray-400">
                                            <BellOff className="w-4 h-4" />
                                            {t('alerts.inactive')}
                                        </span>
                                    )}
                                </td>
                                <td className="px-4 py-3 text-right">
                                    <div className="flex items-center gap-2 justify-end">
                                        {alert.isActive && !alert.isTriggered && (
                                            <button
                                                onClick={() => handleDeactivate(alert.id)}
                                                className="text-gray-400 hover:text-orange-500"
                                                title="Devre dışı bırak"
                                            >
                                                <BellOff className="w-4 h-4" />
                                            </button>
                                        )}
                                        <button
                                            onClick={() => handleDeleteAlert(alert.id)}
                                            className="text-gray-400 hover:text-red-500"
                                        >
                                            <Trash2 className="w-4 h-4" />
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                        {alerts.length === 0 && (
                            <tr>
                                <td colSpan={5} className="px-4 py-8 text-center text-gray-500">
                                    {t('common.noData')}
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>

            {/* Create Modal */}
            {showCreateModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-xl p-6 w-full max-w-md">
                        <h3 className="text-lg font-semibold mb-4">{t('alerts.create')}</h3>

                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    {t('portfolio.symbol')}
                                </label>
                                <input
                                    type="text"
                                    value={newAlert.symbol}
                                    onChange={(e) => setNewAlert({ ...newAlert, symbol: e.target.value })}
                                    placeholder="THYAO"
                                    className="w-full px-4 py-2 border rounded-lg"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Alarm Tipi
                                </label>
                                <select
                                    value={newAlert.alertType}
                                    onChange={(e) => setNewAlert({ ...newAlert, alertType: e.target.value })}
                                    className="w-full px-4 py-2 border rounded-lg"
                                >
                                    {ALERT_TYPES.map((type) => (
                                        <option key={type.value} value={type.value}>
                                            {t(type.labelKey)}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    {t('alerts.targetPrice')}
                                </label>
                                <input
                                    type="number"
                                    value={newAlert.targetValue}
                                    onChange={(e) => setNewAlert({ ...newAlert, targetValue: e.target.value })}
                                    placeholder="300"
                                    className="w-full px-4 py-2 border rounded-lg"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Not (Opsiyonel)
                                </label>
                                <input
                                    type="text"
                                    value={newAlert.notes}
                                    onChange={(e) => setNewAlert({ ...newAlert, notes: e.target.value })}
                                    className="w-full px-4 py-2 border rounded-lg"
                                />
                            </div>
                        </div>

                        <div className="flex gap-3 justify-end mt-6">
                            <button
                                onClick={() => setShowCreateModal(false)}
                                className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg"
                            >
                                {t('common.cancel')}
                            </button>
                            <button
                                onClick={handleCreateAlert}
                                className="px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600"
                            >
                                {t('common.save')}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
