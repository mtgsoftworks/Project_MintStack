import { toast } from 'sonner'
import { useSelector } from 'react-redux'
import {
    useGetSimulationConfigQuery,
    useUpdateSimulationConfigMutation,
    useToggleSimulationMutation,
    useResetSimulationMutation,
    useGetSimulationStatusQuery
} from '@/store/api/simulationApi'
import { getApiErrorMessage } from '@/pages/settings/getApiErrorMessage'
import { selectIsAdmin } from '@/store/slices/authSlice'

export function useSimulationSettings({ t }) {
    const isAdmin = useSelector(selectIsAdmin)
    const { data: simConfigData, refetch: refetchSimConfig } = useGetSimulationConfigQuery(undefined, { skip: !isAdmin })
    const { data: simStatusData } = useGetSimulationStatusQuery(undefined, { pollingInterval: 5000, skip: !isAdmin })
    const [updateSimConfig] = useUpdateSimulationConfigMutation()
    const [toggleSimulation] = useToggleSimulationMutation()
    const [resetSimulation] = useResetSimulationMutation()

    const simConfig = simConfigData?.data
    const simStatus = simStatusData?.data

    const handleToggleSimulation = async () => {
        if (!isAdmin) {
            toast.error(t('common.unauthorized'))
            return
        }
        try {
            await toggleSimulation().unwrap()
            refetchSimConfig()
            toast.success(
                simConfig?.enabled
                    ? t('settings.simulation.disabled', { defaultValue: 'Simulasyon kapatildi' })
                    : t('settings.simulation.enabled', { defaultValue: 'Simulasyon aktif' })
            )
        } catch (error) {
            toast.error(getApiErrorMessage(error, t('common.error')))
        }
    }

    const handleUpdateSimulationConfig = async (payload) => {
        if (!isAdmin) {
            toast.error(t('common.unauthorized'))
            return
        }
        try {
            await updateSimConfig(payload).unwrap()
            refetchSimConfig()

            if (Object.prototype.hasOwnProperty.call(payload, 'volatilityLevel')) {
                toast.success(t('settings.simulation.volatilityUpdated', { defaultValue: 'Volatilite guncellendi' }))
            }

            if (Object.prototype.hasOwnProperty.call(payload, 'marketTrend')) {
                toast.success(t('settings.simulation.trendUpdated', { defaultValue: 'Trend guncellendi' }))
            }

            if (Object.prototype.hasOwnProperty.call(payload, 'updateIntervalSeconds')) {
                toast.success(t('settings.simulation.intervalUpdated', { defaultValue: 'Guncelleme araligi degistirildi' }))
            }
        } catch (error) {
            toast.error(getApiErrorMessage(error, t('common.error')))
        }
    }

    const handleResetSimulation = async () => {
        if (!isAdmin) {
            toast.error(t('common.unauthorized'))
            return
        }
        try {
            await resetSimulation().unwrap()
            refetchSimConfig()
            toast.success(t('settings.simulation.reset', { defaultValue: 'Simulasyon sifirlandi' }))
        } catch (error) {
            toast.error(getApiErrorMessage(error, t('common.error')))
        }
    }

    return {
        simConfig,
        simStatus,
        handleToggleSimulation,
        handleUpdateSimulationConfig,
        handleResetSimulation
    }
}
