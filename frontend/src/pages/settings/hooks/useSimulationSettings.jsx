import { toast } from 'sonner'
import {
    useGetSimulationConfigQuery,
    useUpdateSimulationConfigMutation,
    useToggleSimulationMutation,
    useResetSimulationMutation,
    useGetSimulationStatusQuery
} from '@/store/api/simulationApi'
import { getApiErrorMessage } from '@/pages/settings/getApiErrorMessage'

export function useSimulationSettings({ t }) {
    const { data: simConfigData, refetch: refetchSimConfig } = useGetSimulationConfigQuery()
    const { data: simStatusData } = useGetSimulationStatusQuery(undefined, { pollingInterval: 5000 })
    const [updateSimConfig] = useUpdateSimulationConfigMutation()
    const [toggleSimulation] = useToggleSimulationMutation()
    const [resetSimulation] = useResetSimulationMutation()

    const simConfig = simConfigData?.data
    const simStatus = simStatusData?.data

    const handleToggleSimulation = async () => {
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
