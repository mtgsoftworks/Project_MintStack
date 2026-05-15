import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus, Trash2, Bell, BellOff, TrendingUp, TrendingDown, AlertTriangle, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import {
    useCreateAlertMutation,
    useDeactivateAlertMutation,
    useDeleteAlertMutation,
    useGetAlertsQuery
} from '@/store/api/alertsApi'
import { getApiErrorMessage } from '@/pages/settings/getApiErrorMessage'
import { useInstrumentOptions } from '@/hooks/useInstrumentOptions'

const ALERT_TYPES = [
    { value: 'PRICE_ABOVE', labelKey: 'alerts.priceAbove', icon: TrendingUp },
    { value: 'PRICE_BELOW', labelKey: 'alerts.priceBelow', icon: TrendingDown },
    { value: 'PERCENT_UP', labelKey: 'alerts.percentUp', icon: TrendingUp },
    { value: 'PERCENT_DOWN', labelKey: 'alerts.percentDown', icon: TrendingDown },
]

export default function AlertsPage() {
    const { t } = useTranslation()
    const { data: alerts = [], isLoading, isFetching, error: alertsError, refetch } = useGetAlertsQuery()
    const [createAlert, { isLoading: creating }] = useCreateAlertMutation()
    const [deleteAlert, { isLoading: deleting }] = useDeleteAlertMutation()
    const [deactivateAlert, { isLoading: deactivating }] = useDeactivateAlertMutation()
    const { instrumentOptions, isFetching: instrumentsFetching } = useInstrumentOptions()

    const [showCreateModal, setShowCreateModal] = useState(false)
    const [newAlert, setNewAlert] = useState({
        symbol: '',
        alertType: 'PRICE_ABOVE',
        targetValue: '',
        notes: '',
    })

    const mutating = creating || deleting || deactivating
    const alertsErrorMessage = alertsError ? getApiErrorMessage(alertsError, t('common.error')) : null

    const handleCreateAlert = async () => {
        const symbol = newAlert.symbol.trim().toUpperCase()
        const targetValue = Number(newAlert.targetValue)

        if (!symbol || Number.isNaN(targetValue) || targetValue <= 0) {
            toast.error(t('common.error'))
            return
        }

        try {
            await createAlert({
                symbol,
                alertType: newAlert.alertType,
                targetValue,
                notes: newAlert.notes,
            }).unwrap()
            setNewAlert({ symbol: '', alertType: 'PRICE_ABOVE', targetValue: '', notes: '' })
            setShowCreateModal(false)
            toast.success(t('success.saved'))
        } catch (error) {
            toast.error(getApiErrorMessage(error, t('common.error')))
        }
    }

    const handleDeleteAlert = async (id) => {
        if (!window.confirm(t('common.confirm'))) return
        try {
            await deleteAlert(id).unwrap()
            toast.success(t('success.deleted'))
        } catch (error) {
            toast.error(getApiErrorMessage(error, t('common.error')))
        }
    }

    const handleDeactivate = async (id) => {
        try {
            await deactivateAlert(id).unwrap()
            toast.success(t('success.updated'))
        } catch (error) {
            toast.error(getApiErrorMessage(error, t('common.error')))
        }
    }

    const getAlertTypeLabel = (type) => {
        const alertType = ALERT_TYPES.find((item) => item.value === type)
        return alertType ? t(alertType.labelKey) : type
    }

    if (isLoading || isFetching) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-500" />
            </div>
        )
    }

    return (
        <div className="p-6">
            <div className="flex items-center justify-between mb-6">
                <h1 className="text-2xl font-bold text-foreground">{t('alerts.title')}</h1>
                <button
                    onClick={() => setShowCreateModal(true)}
                    disabled={mutating}
                    className="flex items-center gap-2 px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600 transition-colors"
                >
                    <Plus className="w-4 h-4" />
                    {t('alerts.create')}
                </button>
            </div>

            {alertsError && (
                <div className="mb-6 rounded-xl border border-danger/40 bg-danger/5 p-4 text-danger shadow-sm">
                    <div className="mb-3 flex items-center gap-2 font-medium">
                        <AlertTriangle className="h-4 w-4" />
                        {alertsErrorMessage || t('common.error')}
                    </div>
                    <button
                        onClick={() => refetch()}
                        className="inline-flex items-center gap-2 rounded-lg border border-danger/40 px-3 py-2 text-sm font-medium transition-colors hover:bg-danger/10"
                    >
                        <RefreshCw className="h-4 w-4" />
                        {t('common.refresh')}
                    </button>
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
                    <div className="text-sm text-muted-foreground">{t('alerts.active')}</div>
                    <div className="text-2xl font-bold text-emerald-600">
                        {alerts.filter((alert) => alert.isActive).length}
                    </div>
                </div>
                <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
                    <div className="text-sm text-muted-foreground">{t('alerts.triggered')}</div>
                    <div className="text-2xl font-bold text-orange-600">
                        {alerts.filter((alert) => alert.isTriggered).length}
                    </div>
                </div>
                <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
                    <div className="text-sm text-muted-foreground">{t('alerts.inactive')}</div>
                    <div className="text-2xl font-bold text-muted-foreground">
                        {alerts.filter((alert) => !alert.isActive && !alert.isTriggered).length}
                    </div>
                </div>
            </div>

            <div className="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
                <table className="w-full">
                    <thead className="bg-muted/50">
                        <tr>
                            <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('portfolio.symbol')}</th>
                            <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('alertsPage.table.type')}</th>
                            <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('alerts.targetPrice')}</th>
                            <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('alertsPage.table.status')}</th>
                            <th className="px-4 py-3 text-right text-sm font-medium text-muted-foreground"></th>
                        </tr>
                    </thead>
                    <tbody className="divide-y">
                        {alerts.map((alert) => (
                            <tr key={alert.id} className="hover:bg-muted/40">
                                <td className="px-4 py-3">
                                    <div>
                                        <div className="font-medium text-foreground">{alert.symbol}</div>
                                        <div className="text-sm text-muted-foreground">{alert.instrumentName}</div>
                                    </div>
                                </td>
                                <td className="px-4 py-3">
                                    <span className="rounded bg-muted px-2 py-1 text-sm text-foreground">
                                        {getAlertTypeLabel(alert.alertType)}
                                    </span>
                                </td>
                                <td className="px-4 py-3 font-medium">
                                    {alert.alertType.includes('PERCENT')
                                        ? `%${alert.targetValue}`
                                        : `TRY ${alert.targetValue?.toLocaleString('tr-TR')}`}
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
                                        <span className="flex items-center gap-1 text-muted-foreground">
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
                                                disabled={mutating}
                                                className="text-muted-foreground hover:text-orange-500 disabled:opacity-50"
                                                title={t('alertsPage.actions.deactivate')}
                                            >
                                                <BellOff className="w-4 h-4" />
                                            </button>
                                        )}
                                        <button
                                            onClick={() => handleDeleteAlert(alert.id)}
                                            disabled={mutating}
                                            className="text-muted-foreground hover:text-red-500 disabled:opacity-50"
                                        >
                                            <Trash2 className="w-4 h-4" />
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                        {alerts.length === 0 && (
                            <tr>
                                <td colSpan={5} className="px-4 py-8 text-center text-muted-foreground">
                                    {t('common.noData')}
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>

            {showCreateModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                    <div className="w-full max-w-md rounded-xl border border-border bg-card p-6 text-foreground">
                        <h3 className="text-lg font-semibold mb-4">{t('alerts.create')}</h3>

                        <div className="space-y-4">
                            <div>
                                <label className="mb-1 block text-sm font-medium text-foreground">
                                    {t('portfolio.symbol')}
                                </label>
                                <input
                                    type="text"
                                    list="alert-symbol-options"
                                    value={newAlert.symbol}
                                    onChange={(event) => setNewAlert({ ...newAlert, symbol: event.target.value.toUpperCase() })}
                                    placeholder={instrumentsFetching ? 'Semboller yukleniyor...' : t('alertsPage.placeholders.symbol')}
                                    className="w-full rounded-lg border border-input bg-background px-4 py-2 text-foreground placeholder:text-muted-foreground"
                                />
                                <datalist id="alert-symbol-options">
                                    {instrumentOptions.map((instrument) => (
                                        <option key={instrument.symbol} value={instrument.symbol}>
                                            {`${instrument.name} (${instrument.type})`}
                                        </option>
                                    ))}
                                </datalist>
                                <p className="mt-1 text-xs text-muted-foreground">
                                    Hisse, tahvil/bono, fon, VIOP ve doviz sembolleri sirali listeden secilebilir.
                                </p>
                            </div>

                            <div>
                                <label className="mb-1 block text-sm font-medium text-foreground">
                                    {t('alertsPage.form.alertType')}
                                </label>
                                <select
                                    value={newAlert.alertType}
                                    onChange={(event) => setNewAlert({ ...newAlert, alertType: event.target.value })}
                                    className="w-full rounded-lg border border-input bg-background px-4 py-2 text-foreground"
                                >
                                    {ALERT_TYPES.map((type) => (
                                        <option key={type.value} value={type.value}>
                                            {t(type.labelKey)}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div>
                                <label className="mb-1 block text-sm font-medium text-foreground">
                                    {t('alerts.targetPrice')}
                                </label>
                                <input
                                    type="number"
                                    value={newAlert.targetValue}
                                    onChange={(event) => setNewAlert({ ...newAlert, targetValue: event.target.value })}
                                    placeholder={t('alertsPage.placeholders.targetValue')}
                                    className="w-full rounded-lg border border-input bg-background px-4 py-2 text-foreground placeholder:text-muted-foreground"
                                />
                            </div>

                            <div>
                                <label className="mb-1 block text-sm font-medium text-foreground">
                                    {t('alertsPage.form.notes')}
                                </label>
                                <input
                                    type="text"
                                    value={newAlert.notes}
                                    onChange={(event) => setNewAlert({ ...newAlert, notes: event.target.value })}
                                    className="w-full rounded-lg border border-input bg-background px-4 py-2 text-foreground"
                                />
                            </div>
                        </div>

                        <div className="flex gap-3 justify-end mt-6">
                            <button
                                onClick={() => setShowCreateModal(false)}
                                className="rounded-lg px-4 py-2 text-muted-foreground hover:bg-muted"
                            >
                                {t('common.cancel')}
                            </button>
                            <button
                                onClick={handleCreateAlert}
                                disabled={creating}
                                className="px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600 disabled:opacity-50"
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
