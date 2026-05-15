import { useState } from 'react'
import { toast } from 'sonner'
import {
    useGetApiConfigsQuery,
    useAddApiConfigMutation,
    useDeleteApiConfigMutation,
    useTestApiKeyMutation,
    useGetDataSourceCapabilitiesQuery,
    useGetDataPreferencesQuery,
    useSetDataPreferenceMutation,
    useTriggerDataFetchMutation
} from '@/store/api/settingsApi'
import { getApiErrorMessage } from '@/pages/settings/getApiErrorMessage'

const DEFAULT_FORM_DATA = {
    provider: 'ALPHA_VANTAGE',
    apiKey: '',
    secretKey: '',
    modelName: '',
    baseUrl: '',
    isActive: true
}

const KEYLESS_PROVIDERS = new Set(['TCMB', 'TEFAS', 'RSS', 'YAHOO_FINANCE'])
const PROVIDER_ORDER_FOR_ADD = [
    'ALPHA_VANTAGE',
    'YAHOO_FINANCE',
    'FINNHUB',
    'TEFAS',
    'FINTABLES',
    'TCMB',
    'LLM_ENRICHMENT',
    'OTHER'
]

const requiresValidationForProvider = (provider) => !KEYLESS_PROVIDERS.has(provider)

const withProviderFallbackKey = (data) => {
    if ((data.provider === 'TCMB' || data.provider === 'TEFAS' || data.provider === 'YAHOO_FINANCE') && !data.apiKey.trim()) {
        return {
            ...data,
            apiKey: data.provider === 'TCMB'
                ? 'TCMB_PUBLIC'
                : data.provider === 'TEFAS'
                    ? 'TEFAS_PUBLIC'
                    : 'YAHOO_DIRECT'
        }
    }

    return data
}

const normalizePayload = (data) => {
    const payload = withProviderFallbackKey(data)

    if (KEYLESS_PROVIDERS.has(payload.provider)) {
        return {
            ...payload,
            baseUrl: '',
            secretKey: '',
            modelName: undefined
        }
    }

    if (payload.provider === 'LLM_ENRICHMENT') {
        return {
            ...payload,
            secretKey: '',
            modelName: payload.modelName?.trim() || ''
        }
    }

    return {
        ...payload,
        modelName: undefined
    }
}

export function useApiDataSourceSettings({ t }) {
    const { data: configsData, isLoading } = useGetApiConfigsQuery()
    const [addConfig, { isLoading: isAdding }] = useAddApiConfigMutation()
    const [deleteConfig] = useDeleteApiConfigMutation()
    const [testApiKey, { isLoading: isTesting }] = useTestApiKeyMutation()
    const [triggerDataFetch] = useTriggerDataFetchMutation()
    const [setDataPreference] = useSetDataPreferenceMutation()

    const { data: capabilitiesData } = useGetDataSourceCapabilitiesQuery()
    const { data: preferencesData, refetch: refetchPreferences } = useGetDataPreferencesQuery()

    const [isDialogOpen, setIsDialogOpen] = useState(false)
    const [editingConfig, setEditingConfig] = useState(null)
    const [isValidated, setIsValidated] = useState(false)
    const [isRefreshingDataSources, setIsRefreshingDataSources] = useState(false)
    const [formData, setFormData] = useState(DEFAULT_FORM_DATA)

    const apiConfigs = configsData?.data || []

    const getInitialProvider = () => {
        const usedProviders = new Set(apiConfigs.map((config) => config.provider))
        return PROVIDER_ORDER_FOR_ADD.find((provider) => !usedProviders.has(provider)) || PROVIDER_ORDER_FOR_ADD[0]
    }

    const resetForm = () => {
        setFormData({
            ...DEFAULT_FORM_DATA,
            provider: getInitialProvider()
        })
        setEditingConfig(null)
        setIsValidated(false)
    }

    const handleOpenDialog = (config = null) => {
        if (config) {
            setEditingConfig(config)
            setFormData({
                provider: config.provider,
                apiKey: '',
                secretKey: '',
                modelName: config.modelName || '',
                baseUrl: config.baseUrl || '',
                isActive: config.isActive
            })
        } else {
            resetForm()
        }

        setIsDialogOpen(true)
    }

    const handleDialogOpenChange = (open) => {
        if (!open) {
            resetForm()
        }

        setIsDialogOpen(open)
    }

    const handleFormFieldChange = (field, value, resetValidation = false) => {
        setFormData((previous) => ({
            ...previous,
            [field]: value
        }))

        if (resetValidation) {
            setIsValidated(false)
        }
    }

    const handleTestKey = async () => {
        const requiresValidation = requiresValidationForProvider(formData.provider)
        if (requiresValidation && !formData.apiKey.trim()) {
            toast.error(t('settings.apiKeys.validation.required'))
            return
        }

        const payload = normalizePayload(formData)

        try {
            const result = await testApiKey(payload).unwrap()
            if (result.success && result.data?.valid) {
                toast.success(result.data.message || t('settings.apiKeys.valid'))
                setIsValidated(true)
                return
            }

            toast.error(result.message || result.data?.message || t('settings.apiKeys.invalid'))
            setIsValidated(false)
        } catch (error) {
            toast.error(getApiErrorMessage(error, t('settings.apiKeys.invalid')))
            setIsValidated(false)
        }
    }

    const handleAddSubmit = async (event) => {
        event.preventDefault()

        const payload = normalizePayload(formData)
        const requiresValidation = requiresValidationForProvider(payload.provider)

        if (!isValidated && !editingConfig && requiresValidation) {
            toast.error(t('settings.apiKeys.validation.testRequired'))
            return
        }

        try {
            const result = await addConfig(payload).unwrap()
            toast.success(editingConfig ? t('settings.apiKeys.update') : t('settings.apiKeys.save'))

            handleDialogOpenChange(false)

            if (result.data?.id && payload.isActive) {
                const loadingToastId = toast(
                    <div className="flex items-center gap-3">
                        <div className="animate-spin rounded-full h-5 w-5 border-2 border-primary border-t-transparent" />
                        <div>
                            <p className="font-medium">{t('settings.apiKeys.fetchingData', { defaultValue: 'Veriler cekiliyor...' })}</p>
                            <p className="text-xs text-muted-foreground">{t('settings.dataSources.pleaseWait')}</p>
                        </div>
                    </div>,
                    { duration: Infinity }
                )

                try {
                    const triggerResult = await triggerDataFetch(result.data.id).unwrap()
                    toast.dismiss(loadingToastId)
                    toast.success(triggerResult.message || t('settings.apiKeys.dataFetched', { defaultValue: 'Veriler basariyla cekildi!' }))
                    refetchPreferences()
                } catch {
                    toast.dismiss(loadingToastId)
                }
            }
        } catch (error) {
            toast.error(getApiErrorMessage(error, t('common.error')))
        }
    }

    const handleDelete = async (id) => {
        if (!confirm(t('settings.apiKeys.deleteConfirm'))) {
            return
        }

        try {
            await deleteConfig(id).unwrap()
            toast.success(t('settings.apiKeys.delete'))
        } catch {
            toast.error(t('common.error'))
        }
    }

    const handleSelectDataPreference = async (dataType, provider) => {
        try {
            await setDataPreference({
                dataType: dataType.type,
                provider,
                isEnabled: true
            }).unwrap()

            toast.success(`${dataType.label} ${t('settings.dataSources.sourceUpdated')}`)
            refetchPreferences()
        } catch (error) {
            toast.error(getApiErrorMessage(error, t('settings.dataSources.errorOccurred')))
        }
    }

    const handleRefreshDataSources = async () => {
        const activeConfigs = apiConfigs.filter((config) => config.isActive)
        if (activeConfigs.length === 0) {
            return
        }

        setIsRefreshingDataSources(true)

        const loadingToastId = toast(
            <div className="flex items-center gap-3">
                <div className="animate-spin rounded-full h-5 w-5 border-2 border-primary border-t-transparent" />
                <div>
                    <p className="font-medium">{t('settings.dataSources.updating')}</p>
                    <p className="text-xs text-muted-foreground">{t('settings.dataSources.pleaseWait')}</p>
                </div>
            </div>,
            { duration: Infinity }
        )

        try {
            let successCount = 0
            for (const config of activeConfigs) {
                try {
                    await triggerDataFetch(config.id).unwrap()
                    successCount += 1
                } catch (error) {
                    console.error('Provider refresh failed:', config.provider, error)
                }
            }

            toast.dismiss(loadingToastId)
            if (successCount > 0) {
                toast.success(`${t('settings.dataSources.updateSuccess')} (${successCount}/${activeConfigs.length})`)
            } else {
                toast.error(t('settings.dataSources.updateError'))
            }
        } catch (error) {
            toast.dismiss(loadingToastId)
            toast.error(getApiErrorMessage(error, t('settings.dataSources.updateError')))
        } finally {
            setIsRefreshingDataSources(false)
        }
    }

    return {
        apiConfigs,
        providerCapabilities: capabilitiesData?.data || {},
        preferencesData,
        isLoading,
        isAdding,
        isTesting,
        isDialogOpen,
        editingConfig,
        formData,
        isValidated,
        isRefreshingDataSources,
        handleOpenDialog,
        handleDialogOpenChange,
        handleFormFieldChange,
        handleTestKey,
        handleAddSubmit,
        handleDelete,
        handleSelectDataPreference,
        handleRefreshDataSources
    }
}
