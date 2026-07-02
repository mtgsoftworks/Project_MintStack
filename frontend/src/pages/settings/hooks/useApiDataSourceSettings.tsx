import { useState } from 'react'
import type { FormEvent } from 'react'
import { toast } from 'sonner'
import {
    useGetApiConfigsQuery,
    useAddApiConfigMutation,
    useDeleteApiConfigMutation,
    useTestApiKeyMutation,
    useGetDataSourceCapabilitiesQuery,
    useGetDataPreferencesQuery,
    useSetDataPreferenceMutation,
    useTriggerDataFetchMutation,
    useBackfillMarketDataMutation
} from '@/store/api/settingsApi'
import { getApiErrorMessage } from '@/pages/settings/getApiErrorMessage'
import type {
    ApiKeyConfig,
    ApiConfigFormData,
    BackfillFormData,
    DataPreference,
    DataSourceType,
    ProviderCapabilities
} from '../types'

// API payload type that allows modelName to be undefined
interface ApiConfigPayload {
    provider: string
    apiKey: string
    secretKey: string
    modelName: string | undefined
    baseUrl: string
    isActive: boolean
}

const DEFAULT_FORM_DATA: ApiConfigFormData = {
    provider: 'ALPHA_VANTAGE',
    apiKey: '',
    secretKey: '',
    modelName: '',
    baseUrl: '',
    isActive: true
}

const DEFAULT_BACKFILL_FORM: BackfillFormData = {
    days: '30',
    maxInstruments: '500',
    instrumentTypes: ['STOCK', 'FUND', 'CURRENCY'],
    symbols: '',
    includeSyntheticFallback: false
}

const KEYLESS_PROVIDERS = new Set(['TCMB', 'TEFAS', 'BIST_DATASTORE', 'RSS', 'YAHOO_FINANCE'])
const PROVIDER_ORDER_FOR_ADD = [
    'ALPHA_VANTAGE',
    'YAHOO_FINANCE',
    'FINNHUB',
    'TEFAS',
    'BIST_DATASTORE',
    'FINTABLES',
    'TCMB',
    'LLM_ENRICHMENT',
    'OTHER'
]

const requiresValidationForProvider = (provider: string): boolean => !KEYLESS_PROVIDERS.has(provider)

const withProviderFallbackKey = (data: ApiConfigFormData): ApiConfigPayload => {
    if ((data.provider === 'TCMB' || data.provider === 'TEFAS' || data.provider === 'BIST_DATASTORE' || data.provider === 'YAHOO_FINANCE') && !data.apiKey.trim()) {
        return {
            provider: data.provider,
            apiKey: data.provider === 'TCMB'
                ? 'TCMB_PUBLIC'
                : data.provider === 'TEFAS'
                    ? 'TEFAS_PUBLIC'
                    : data.provider === 'BIST_DATASTORE'
                        ? 'BIST_DATASTORE_PUBLIC'
                        : 'YAHOO_DIRECT',
            secretKey: data.secretKey,
            modelName: data.modelName,
            baseUrl: data.baseUrl,
            isActive: data.isActive
        }
    }

    return {
        provider: data.provider,
        apiKey: data.apiKey,
        secretKey: data.secretKey,
        modelName: data.modelName,
        baseUrl: data.baseUrl,
        isActive: data.isActive
    }
}

const normalizePayload = (data: ApiConfigFormData): ApiConfigPayload => {
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

interface UseApiDataSourceSettingsOptions {
    t: (key: string, options?: Record<string, unknown>) => string
    isAdmin?: boolean
}

export function useApiDataSourceSettings({ t, isAdmin = false }: UseApiDataSourceSettingsOptions) {
    const { data: configsData, isLoading: isLoadingConfigs } = useGetApiConfigsQuery(undefined, {
        skip: !isAdmin
    })
    const [addConfig, { isLoading: isAdding }] = useAddApiConfigMutation()
    const [deleteConfig] = useDeleteApiConfigMutation()
    const [testApiKey, { isLoading: isTesting }] = useTestApiKeyMutation()
    const [triggerDataFetch] = useTriggerDataFetchMutation()
    const [setDataPreference] = useSetDataPreferenceMutation()
    const [backfillMarketData, { isLoading: isBackfillingMarketData }] = useBackfillMarketDataMutation()

    const { data: capabilitiesData } = useGetDataSourceCapabilitiesQuery(undefined, {
        skip: !isAdmin
    })
    const { data: preferencesData, refetch: refetchPreferences } = useGetDataPreferencesQuery(undefined, {
        skip: !isAdmin
    })

    const [isDialogOpen, setIsDialogOpen] = useState(false)
    const [editingConfig, setEditingConfig] = useState<ApiKeyConfig | null>(null)
    const [isValidated, setIsValidated] = useState(false)
    const [isRefreshingDataSources, setIsRefreshingDataSources] = useState(false)
    const [formData, setFormData] = useState<ApiConfigFormData>(DEFAULT_FORM_DATA)
    const [backfillForm, setBackfillForm] = useState<BackfillFormData>(DEFAULT_BACKFILL_FORM)

    const apiConfigs: ApiKeyConfig[] = isAdmin ? configsData?.data || [] : []
    const isLoading = isAdmin && isLoadingConfigs

    const getInitialProvider = (): string => {
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

    const handleOpenDialog = (config: ApiKeyConfig | null = null) => {
        if (!isAdmin) {
            toast.error(t('common.forbidden'))
            return
        }

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

    const handleDialogOpenChange = (open: boolean) => {
        if (!open) {
            resetForm()
        }

        setIsDialogOpen(open)
    }

    const handleFormFieldChange = (field: string, value: string | boolean, resetValidation = false) => {
        setFormData((previous) => ({
            ...previous,
            [field]: value
        }))

        if (resetValidation) {
            setIsValidated(false)
        }
    }

    const handleTestKey = async () => {
        if (!isAdmin) {
            toast.error(t('common.forbidden'))
            return
        }

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

    const handleAddSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()

        if (!isAdmin) {
            toast.error(t('common.forbidden'))
            return
        }

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
                            <p className="font-medium">{t('settings.apiKeys.fetchingData')}</p>
                            <p className="text-xs text-muted-foreground">{t('settings.dataSources.pleaseWait')}</p>
                        </div>
                    </div>,
                    { duration: Infinity }
                )

                try {
                    const triggerResult = await triggerDataFetch(result.data.id).unwrap()
                    toast.dismiss(loadingToastId)
                    toast.success(triggerResult.message || t('settings.apiKeys.dataFetched'))
                    refetchPreferences()
                } catch {
                    toast.dismiss(loadingToastId)
                }
            }
        } catch (error) {
            toast.error(getApiErrorMessage(error, t('common.error')))
        }
    }

    const handleDelete = async (id: number | string) => {
        if (!isAdmin) {
            toast.error(t('common.forbidden'))
            return
        }

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

    const handleSelectDataPreference = async (dataType: DataSourceType, provider: string) => {
        if (!isAdmin) {
            toast.error(t('common.forbidden'))
            return
        }

        try {
            await setDataPreference({
                dataType: dataType.type,
                provider,
                isEnabled: true
            }).unwrap()

            toast.success(`${t(dataType.labelKey)} ${t('settings.dataSources.sourceUpdated')}`)
            refetchPreferences()
        } catch (error) {
            toast.error(getApiErrorMessage(error, t('settings.dataSources.errorOccurred')))
        }
    }

    const handleRefreshDataSources = async () => {
        if (!isAdmin) {
            toast.error(t('common.forbidden'))
            return
        }

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

    const handleBackfillFormChange = (field: string, value: string) => {
        setBackfillForm((previous) => ({
            ...previous,
            [field]: value
        }))
    }

    const handleToggleBackfillType = (type: string, checked: boolean) => {
        setBackfillForm((previous) => {
            const current = new Set(previous.instrumentTypes)
            if (checked) {
                current.add(type)
            } else {
                current.delete(type)
            }
            return {
                ...previous,
                instrumentTypes: [...current]
            }
        })
    }

    const handleBackfillMarketData = async () => {
        if (!isAdmin) {
            toast.error(t('common.forbidden'))
            return
        }

        if (backfillForm.instrumentTypes.length === 0) {
            toast.error(t('settings.dataSources.backfill.validation.selectType'))
            return
        }

        const payload = {
            days: Number(backfillForm.days),
            maxInstruments: Number(backfillForm.maxInstruments),
            instrumentTypes: backfillForm.instrumentTypes,
            includeSyntheticFallback: false,
            symbols: backfillForm.symbols
                .split(',')
                .map((symbol) => symbol.trim().toUpperCase())
                .filter(Boolean)
        }

        try {
            const result = await backfillMarketData(payload).unwrap()
            const savedRows = (result.data?.savedPriceRows || 0) + (result.data?.savedCurrencyRows || 0)
            toast.success(t('settings.dataSources.backfill.success', { count: savedRows }))
        } catch (error) {
            toast.error(getApiErrorMessage(error, t('settings.dataSources.backfill.error')))
        }
    }

    return {
        apiConfigs,
        providerCapabilities: (isAdmin ? capabilitiesData?.data || {} : {}) as ProviderCapabilities,
        preferencesData: isAdmin ? preferencesData : { data: [] as DataPreference[] },
        isLoading,
        isAdding,
        isTesting,
        isDialogOpen,
        editingConfig,
        formData,
        backfillForm,
        isValidated,
        isRefreshingDataSources,
        isBackfillingMarketData,
        handleOpenDialog,
        handleDialogOpenChange,
        handleFormFieldChange,
        handleTestKey,
        handleAddSubmit,
        handleDelete,
        handleSelectDataPreference,
        handleRefreshDataSources,
        handleBackfillFormChange,
        handleToggleBackfillType,
        handleBackfillMarketData
    }
}
