import { useState, useEffect } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import {
    selectTheme, setTheme,
    selectCurrency, setCurrency,
    selectTimezone, setTimezone,
    selectAutoUpdate, setAutoUpdate,
    selectRefreshRate, setRefreshRate
} from '@/store/slices/uiSlice'
import { useGetProfileQuery, useUpdateProfileMutation } from '@/store/api/userApi'
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle
} from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue
} from '@/components/ui/select'
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow
} from '@/components/ui/table'
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger
} from '@/components/ui/dialog'
import { Switch } from '@/components/ui/switch'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Trash2, Plus, Key, RefreshCw, AlertCircle, Pencil, CheckCircle, Database, Zap, FlaskConical, TrendingUp, TrendingDown, Minus, RotateCcw } from 'lucide-react'
import {
    useGetApiConfigsQuery,
    useAddApiConfigMutation,
    useDeleteApiConfigMutation,
    useTestApiKeyMutation,
    useClearCacheMutation,
    useDeleteMarketDataMutation,
    useGetDataSourceCapabilitiesQuery,
    useGetDataPreferencesQuery,
    useSetDataPreferenceMutation,
    useTriggerDataFetchMutation
} from '@/store/api/settingsApi'
import {
    useGetSimulationConfigQuery,
    useUpdateSimulationConfigMutation,
    useToggleSimulationMutation,
    useResetSimulationMutation,
    useGetSimulationStatusQuery
} from '@/store/api/simulationApi'
import { toast } from 'sonner'
import { portfolioService } from '@/services/portfolioService'
import watchlistService from '@/services/watchlistService'
import alertService from '@/services/alertService'

// Provider Capabilities Info
import { useTranslation } from 'react-i18next'

export default function SettingsPage() {
    const { t, i18n } = useTranslation()
    const dispatch = useDispatch()
    const theme = useSelector(selectTheme)
    const currency = useSelector(selectCurrency)
    const timezone = useSelector(selectTimezone)
    const autoUpdate = useSelector(selectAutoUpdate)
    const refreshRate = useSelector(selectRefreshRate)

    // Profile & Notification Settings
    const { data: profile } = useGetProfileQuery()
    const [updateProfile] = useUpdateProfileMutation()

    const [notificationSettings, setNotificationSettings] = useState({
        priceAlerts: true,
        portfolioUpdates: true,
        emailNotifications: true,
        pushNotifications: false
    })

    // Sync notification settings from profile
    useEffect(() => {
        if (profile) {
            setNotificationSettings({
                priceAlerts: profile.priceAlerts ?? true,
                portfolioUpdates: profile.portfolioUpdates ?? true,
                emailNotifications: profile.emailNotifications ?? true,
                pushNotifications: profile.pushNotifications ?? false
            })
        }
    }, [profile])

    // Handle notification toggle
    const handleNotificationToggle = async (key, value) => {
        const newSettings = { ...notificationSettings, [key]: value }
        setNotificationSettings(newSettings)
        try {
            await updateProfile({ [key]: value }).unwrap()
            toast.success(t('success.saved'))
        } catch (error) {
            // Revert on error
            setNotificationSettings(notificationSettings)
            toast.error(t('common.error'))
        }
    }

    const { data: configsData, isLoading, refetch } = useGetApiConfigsQuery()
    const [addConfig, { isLoading: isAdding }] = useAddApiConfigMutation()
    const [deleteConfig, { isLoading: isDeleting }] = useDeleteApiConfigMutation()
    const [testApiKey, { isLoading: isTesting }] = useTestApiKeyMutation()
    const [clearCache, { isLoading: isClearingCache }] = useClearCacheMutation()
    const [deleteMarketData] = useDeleteMarketDataMutation()
    const [triggerDataFetch] = useTriggerDataFetchMutation()

    // Data Source preferences
    const { data: capabilitiesData } = useGetDataSourceCapabilitiesQuery()
    const { data: preferencesData, refetch: refetchPreferences } = useGetDataPreferencesQuery()
    const [setDataPreference] = useSetDataPreferenceMutation()

    // Simulation
    const { data: simConfigData, refetch: refetchSimConfig } = useGetSimulationConfigQuery()
    const { data: simStatusData } = useGetSimulationStatusQuery(undefined, { pollingInterval: 5000 })
    const [updateSimConfig] = useUpdateSimulationConfigMutation()
    const [toggleSimulation] = useToggleSimulationMutation()
    const [resetSimulation] = useResetSimulationMutation()
    const simConfig = simConfigData?.data
    const simStatus = simStatusData?.data

    const [isDialogOpen, setIsDialogOpen] = useState(false)
    const [editingConfig, setEditingConfig] = useState(null) // null = new, object = editing
    const [isValidated, setIsValidated] = useState(false) // API key validated?
    const [formData, setFormData] = useState({
        provider: 'ALPHA_VANTAGE',
        apiKey: '',
        secretKey: '',
        baseUrl: '',
        isActive: true
    })

    // Provider Capabilities Info
    const PROVIDER_INFO = {
        YAHOO_FINANCE: {
            title: t('settings.providers.info.YAHOO_FINANCE.title'),
            description: t('settings.providers.info.YAHOO_FINANCE.desc'),
            supported: t('settings.providers.info.YAHOO_FINANCE.supported', { returnObjects: true }),
            missing: t('settings.providers.info.YAHOO_FINANCE.missing', { returnObjects: true }),
            color: "bg-blue-50 text-blue-900 border-blue-200"
        },
        ALPHA_VANTAGE: {
            title: t('settings.providers.info.ALPHA_VANTAGE.title'),
            description: t('settings.providers.info.ALPHA_VANTAGE.desc'),
            supported: t('settings.providers.info.ALPHA_VANTAGE.supported', { returnObjects: true }),
            missing: t('settings.providers.info.ALPHA_VANTAGE.missing', { returnObjects: true }),
            color: "bg-yellow-50 text-yellow-900 border-yellow-200"
        },
        FINNHUB: {
            title: t('settings.providers.info.FINNHUB.title'),
            description: t('settings.providers.info.FINNHUB.desc'),
            supported: t('settings.providers.info.FINNHUB.supported', { returnObjects: true }),
            missing: t('settings.providers.info.FINNHUB.missing', { returnObjects: true }),
            color: "bg-orange-50 text-orange-900 border-orange-200"
        },
        TCMB: {
            title: t('settings.providers.info.TCMB.title'),
            description: t('settings.providers.info.TCMB.desc'),
            supported: t('settings.providers.info.TCMB.supported', { returnObjects: true }),
            missing: t('settings.providers.info.TCMB.missing', { returnObjects: true }),
            color: "bg-slate-50 text-slate-900 border-slate-200"
        }
    }

    // Safe access to the list from the response wrapper
    const apiConfigs = configsData?.data || []

    const resetForm = () => {
        setFormData({
            provider: 'ALPHA_VANTAGE',
            apiKey: '',
            secretKey: '',
            baseUrl: '',
            isActive: true
        })
        setEditingConfig(null)
        setIsValidated(false)
    }

    const handleOpenDialog = (config = null) => {
        if (config) {
            // Edit mode
            setEditingConfig(config)
            setFormData({
                provider: config.provider,
                apiKey: '', // Don't show masked key, user must enter new one
                secretKey: '',
                baseUrl: config.baseUrl || '',
                isActive: config.isActive
            })
        } else {
            // New mode
            resetForm()
        }
        setIsDialogOpen(true)
    }

    const handleTestKey = async () => {
        // TCMB validation doesn't need a key
        if (formData.provider !== 'TCMB' && !formData.apiKey.trim()) {
            toast.error(t('settings.apiKeys.validation.required'))
            return
        }

        // Ensure TCMB uses a placeholder if empty
        const dataToTest = {
            ...formData,
            apiKey: (formData.provider === 'TCMB' && !formData.apiKey.trim()) ? 'TCMB_PUBLIC' : formData.apiKey
        }

        try {
            const result = await testApiKey(dataToTest).unwrap()
            if (result.success && result.data?.valid) {
                toast.success(result.data.message || t('settings.apiKeys.valid'))
                setIsValidated(true)
            } else {
                toast.error(result.message || result.data?.message || t('settings.apiKeys.invalid'))
                setIsValidated(false)
            }
        } catch (error) {
            console.error('Test failed:', error)
            toast.error(error.data?.message || t('settings.apiKeys.invalid'))
            setIsValidated(false)
        }
    }

    const handleAddSubmit = async (e) => {
        e.preventDefault()

        // TCMB doesn't require validation - auto validate it
        if (formData.provider === 'TCMB') {
            setIsValidated(true)
        }

        if (!isValidated && formData.provider !== 'TCMB') {
            toast.error(t('settings.apiKeys.validation.testRequired'))
            return
        }

        // Prepare data with TCMB placeholder if needed
        const dataToSubmit = {
            ...formData,
            apiKey: (formData.provider === 'TCMB' && !formData.apiKey.trim()) ? 'TCMB_PUBLIC' : formData.apiKey
        }

        try {
            const result = await addConfig(dataToSubmit).unwrap()
            toast.success(editingConfig ? t('settings.apiKeys.update') : t('settings.apiKeys.save'))
            setIsDialogOpen(false)
            resetForm()

            // Trigger immediate data fetch for new API key
            if (result.data?.id && dataToSubmit.isActive) {
                const loadingToastId = toast(
                    <div className="flex items-center gap-3">
                        <div className="animate-spin rounded-full h-5 w-5 border-2 border-primary border-t-transparent" />
                        <div>
                            <p className="font-medium">{t('settings.apiKeys.fetchingData', { defaultValue: 'Veriler çekiliyor...' })}</p>
                            <p className="text-xs text-muted-foreground">API bağlantısı test ediliyor</p>
                        </div>
                    </div>,
                    { duration: Infinity }
                )
                try {
                    const triggerResult = await triggerDataFetch(result.data.id).unwrap()
                    toast.dismiss(loadingToastId)
                    toast.success(
                        <div className="flex items-center gap-2">
                            <CheckCircle className="h-5 w-5 text-green-500" />
                            <span>{triggerResult.message || t('settings.apiKeys.dataFetched', { defaultValue: 'Veriler başarıyla çekildi!' })}</span>
                        </div>
                    )
                    refetchPreferences()
                } catch (triggerError) {
                    toast.dismiss(loadingToastId)
                    console.warn('Trigger fetch failed:', triggerError)
                }
            }
        } catch (error) {
            console.error('Failed to add config:', error)
            toast.error(error.data?.message || t('common.error'))
        }
    }

    const handleDelete = async (id) => {
        if (confirm(t('settings.apiKeys.deleteConfirm'))) {
            try {
                await deleteConfig(id).unwrap()
                toast.success(t('settings.apiKeys.delete'))
            } catch (error) {
                toast.error(t('common.error'))
            }
        }
    }

    const getProviderLabel = (provider) => {
        // We can just return the title from PROVIDER_INFO if it matches
        if (PROVIDER_INFO[provider]) {
            return PROVIDER_INFO[provider].title
        }
        return provider
    }

    return (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div>
                <h1 className="text-2xl font-bold">{t('settings.apiKeys.title')}</h1>
                <p className="text-muted-foreground">
                    {t('settings.apiKeys.manage')}
                </p>
            </div>

            <Tabs defaultValue="api-keys" className="space-y-4">
                <TabsList>
                    <TabsTrigger value="general">{t('settingsPage.tabs.general')}</TabsTrigger>
                    <TabsTrigger value="api-keys">{t('settings.apiKeys.title')}</TabsTrigger>
                    <TabsTrigger value="data-sources">
                        <Database className="h-4 w-4 mr-2" />
                        {t('settings.dataSources.title', { defaultValue: 'Veri Kaynakları' })}
                    </TabsTrigger>
                    <TabsTrigger value="simulation">
                        <FlaskConical className="h-4 w-4 mr-2" />
                        {t('settings.simulation.title', { defaultValue: 'Simülasyon' })}
                    </TabsTrigger>
                </TabsList>

                <TabsContent value="general">
                    <Card>
                        <CardHeader>
                            <CardTitle>{t('settingsPage.general.title')}</CardTitle>
                            <CardDescription>{t('settingsPage.general.description')}</CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-6">
                            {/* Theme Settings */}
                            <div className="space-y-4">
                                <h3 className="text-sm font-medium">{t('settingsPage.sections.appearance')}</h3>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>{t('settings.theme')}</Label>
                                        <p className="text-sm text-muted-foreground">{t('settingsPage.appearance.themeDescription')}</p>
                                    </div>
                                    <Select
                                        value={theme}
                                        onValueChange={(val) => {
                                            dispatch(setTheme(val))
                                            // Apply theme to document
                                            if (val === 'dark') {
                                                document.documentElement.classList.add('dark')
                                            } else if (val === 'light') {
                                                document.documentElement.classList.remove('dark')
                                            } else {
                                                // System preference
                                                const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
                                                document.documentElement.classList.toggle('dark', prefersDark)
                                            }
                                            toast.success(t('settingsPage.appearance.themeChanged'))
                                        }}
                                    >
                                        <SelectTrigger className="w-40">
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="light">{t('settingsPage.appearance.themeOptions.light')}</SelectItem>
                                            <SelectItem value="dark">{t('settingsPage.appearance.themeOptions.dark')}</SelectItem>
                                            <SelectItem value="system">{t('settingsPage.appearance.themeOptions.system')}</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>{t('settings.language')}</Label>
                                        <p className="text-sm text-muted-foreground">{t('settingsPage.appearance.languageDescription')}</p>
                                    </div>
                                    <Select
                                        value={i18n.language?.split('-')[0] || 'tr'}
                                        onValueChange={(val) => {
                                            i18n.changeLanguage(val)
                                            toast.success(t('settingsPage.appearance.languageChanged', {
                                                language: t(`settingsPage.appearance.languageOptions.${val}`)
                                            }))
                                        }}
                                    >
                                        <SelectTrigger className="w-40">
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="tr">{t('settingsPage.appearance.languageOptions.tr')}</SelectItem>
                                            <SelectItem value="en">{t('settingsPage.appearance.languageOptions.en')}</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                            </div>

                            {/* Data Settings */}
                            <div className="space-y-4">
                                <h3 className="text-sm font-medium">{t('settingsPage.sections.data')}</h3>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>{t('settingsPage.data.currency.label')}</Label>
                                        <p className="text-sm text-muted-foreground">{t('settingsPage.data.currency.description')}</p>
                                    </div>
                                    <Select
                                        value={currency}
                                        onValueChange={(val) => {
                                            dispatch(setCurrency(val))
                                            toast.success(t('success.saved'))
                                        }}
                                    >
                                        <SelectTrigger className="w-40">
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="TRY">{t('settingsPage.data.currency.options.try')}</SelectItem>
                                            <SelectItem value="USD">{t('settingsPage.data.currency.options.usd')}</SelectItem>
                                            <SelectItem value="EUR">{t('settingsPage.data.currency.options.eur')}</SelectItem>
                                            <SelectItem value="GBP">{t('settingsPage.data.currency.options.gbp')}</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>{t('settingsPage.data.timezone.label')}</Label>
                                        <p className="text-sm text-muted-foreground">{t('settingsPage.data.timezone.description')}</p>
                                    </div>
                                    <Select
                                        value={timezone}
                                        onValueChange={(val) => {
                                            dispatch(setTimezone(val))
                                            toast.success(t('success.saved'))
                                        }}
                                    >
                                        <SelectTrigger className="w-40">
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="Europe/Istanbul">{t('settingsPage.data.timezone.options.istanbul')}</SelectItem>
                                            <SelectItem value="Europe/London">{t('settingsPage.data.timezone.options.london')}</SelectItem>
                                            <SelectItem value="America/New_York">{t('settingsPage.data.timezone.options.newYork')}</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>

                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>{t('settingsPage.data.autoUpdate.label')}</Label>
                                        <p className="text-sm text-muted-foreground">{t('settingsPage.data.autoUpdate.description')}</p>
                                    </div>
                                    <Switch
                                        checked={autoUpdate}
                                        onCheckedChange={(val) => {
                                            dispatch(setAutoUpdate(val))
                                            toast.success(t('success.saved'))
                                        }}
                                    />
                                </div>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>{t('settingsPage.data.refreshRate.label')}</Label>
                                        <p className="text-sm text-muted-foreground">{t('settingsPage.data.refreshRate.description')}</p>
                                    </div>
                                    <Select
                                        value={refreshRate.toString()}
                                        onValueChange={(val) => {
                                            dispatch(setRefreshRate(parseInt(val)))
                                            toast.success(t('success.saved'))
                                        }}
                                    >
                                        <SelectTrigger className="w-40">
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="30">{t('settingsPage.data.refreshRate.options.30')}</SelectItem>
                                            <SelectItem value="60">{t('settingsPage.data.refreshRate.options.60')}</SelectItem>
                                            <SelectItem value="300">{t('settingsPage.data.refreshRate.options.300')}</SelectItem>
                                            <SelectItem value="900">{t('settingsPage.data.refreshRate.options.900')}</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                            </div>

                            {/* Notification Settings */}
                            <div className="space-y-4">
                                <h3 className="text-sm font-medium">{t('settingsPage.sections.notifications')}</h3>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>{t('settingsPage.notifications.priceAlerts.label')}</Label>
                                        <p className="text-sm text-muted-foreground">{t('settingsPage.notifications.priceAlerts.description')}</p>
                                    </div>
                                    <Switch
                                        checked={notificationSettings.priceAlerts}
                                        onCheckedChange={(val) => handleNotificationToggle('priceAlerts', val)}
                                    />
                                </div>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>{t('settingsPage.notifications.portfolioUpdates.label')}</Label>
                                        <p className="text-sm text-muted-foreground">{t('settingsPage.notifications.portfolioUpdates.description')}</p>
                                    </div>
                                    <Switch
                                        checked={notificationSettings.portfolioUpdates}
                                        onCheckedChange={(val) => handleNotificationToggle('portfolioUpdates', val)}
                                    />
                                </div>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>{t('settingsPage.notifications.news.label')}</Label>
                                        <p className="text-sm text-muted-foreground">{t('settingsPage.notifications.news.description')}</p>
                                    </div>
                                    <Switch
                                        checked={notificationSettings.emailNotifications}
                                        onCheckedChange={(val) => handleNotificationToggle('emailNotifications', val)}
                                    />
                                </div>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>{t('settingsPage.notifications.sound.label')}</Label>
                                        <p className="text-sm text-muted-foreground">{t('settingsPage.notifications.sound.description')}</p>
                                    </div>
                                    <Switch
                                        checked={notificationSettings.pushNotifications}
                                        onCheckedChange={(val) => handleNotificationToggle('pushNotifications', val)}
                                    />
                                </div>
                            </div>

                            <Button className="w-full mt-4" onClick={() => toast.success(t('success.saved'))}>
                                {t('common.save')}
                            </Button>
                        </CardContent>
                    </Card>

                    {/* Danger Zone */}
                    <Card className="border-destructive/50 mt-6">
                        <CardHeader>
                            <CardTitle className="text-destructive">{t('settingsPage.dangerZone.title')}</CardTitle>
                            <CardDescription>{t('settingsPage.dangerZone.description')}</CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            <div className="flex items-center justify-between py-2">
                                <div className="space-y-0.5">
                                    <Label className="text-destructive font-medium">{t('settingsPage.dangerZone.reset.title')}</Label>
                                    <p className="text-sm text-muted-foreground">
                                        {t('settingsPage.dangerZone.reset.description')}
                                    </p>
                                </div>
                                <Dialog>
                                    <DialogTrigger asChild>
                                        <Button variant="destructive" size="sm">
                                            <Trash2 className="h-4 w-4 mr-2" />
                                            {t('settingsPage.dangerZone.reset.button')}
                                        </Button>
                                    </DialogTrigger>
                                    <DialogContent>
                                        <DialogHeader>
                                            <DialogTitle className="text-destructive flex items-center gap-2">
                                                <AlertCircle className="h-5 w-5" />
                                                {t('settingsPage.dangerZone.reset.dialog.title')}
                                            </DialogTitle>
                                            <DialogDescription>
                                                {t('settingsPage.dangerZone.reset.dialog.description')}
                                            </DialogDescription>
                                        </DialogHeader>
                                        <div className="py-4">
                                            <p className="text-sm text-muted-foreground mb-4">
                                                {t('settingsPage.dangerZone.reset.dialog.continue')}
                                            </p>
                                            <div className="p-3 bg-destructive/10 border border-destructive/20 rounded-md">
                                                <p className="text-sm font-medium text-destructive">
                                                    {t('settingsPage.dangerZone.reset.dialog.warningTitle')}
                                                </p>
                                                <ul className="text-sm text-muted-foreground mt-2 space-y-1 list-disc list-inside">
                                                    {t('settingsPage.dangerZone.reset.dialog.items', { returnObjects: true }).map((item) => (
                                                        <li key={item}>{item}</li>
                                                    ))}
                                                </ul>
                                            </div>
                                        </div>
                                        <DialogFooter className="gap-2">
                                            <Button variant="outline" onClick={() => { }}>
                                                {t('common.cancel')}
                                            </Button>
                                            <Button
                                                variant="destructive"
                                                onClick={async () => {
                                                    const loadingToastId = toast(
                                                        <div className="flex items-center gap-3">
                                                            <div className="animate-spin rounded-full h-5 w-5 border-2 border-red-500 border-t-transparent" />
                                                            <div>
                                                                <p className="font-medium">{t('settingsPage.dangerZone.reset.toast.loading')}</p>
                                                                <p className="text-xs text-muted-foreground">Tüm veriler siliniyor...</p>
                                                            </div>
                                                        </div>,
                                                        { duration: Infinity }
                                                    )

                                                    let hasError = false

                                                    // 1. Tüm portföyleri sil
                                                    try {
                                                        const portfolios = await portfolioService.getPortfolios()
                                                        for (const portfolio of portfolios) {
                                                            await portfolioService.deletePortfolio(portfolio.id)
                                                        }
                                                    } catch (e) {
                                                        console.warn('Portfolio reset skipped:', e.message)
                                                    }

                                                    // 2. Tüm izleme listelerini sil
                                                    try {
                                                        const watchlistsRes = await watchlistService.getAll()
                                                        const watchlists = watchlistsRes?.data || []
                                                        for (const wl of watchlists) {
                                                            await watchlistService.delete(wl.id)
                                                        }
                                                    } catch (e) {
                                                        console.warn('Watchlist reset skipped:', e.message)
                                                    }

                                                    // 3. Tüm alarmları sil
                                                    try {
                                                        const alertsRes = await alertService.getAll()
                                                        const alerts = alertsRes?.data || []
                                                        for (const alert of alerts) {
                                                            await alertService.delete(alert.id)
                                                        }
                                                    } catch (e) {
                                                        console.warn('Alerts reset skipped:', e.message)
                                                    }

                                                    // 4. Piyasa verilerini sil (döviz kurları, fiyat geçmişi)
                                                    try {
                                                        await deleteMarketData().unwrap()
                                                    } catch (e) {
                                                        console.warn('Market data reset skipped:', e.message)
                                                    }

                                                    // 5. Local storage temizle
                                                    localStorage.clear()
                                                    sessionStorage.clear()

                                                    toast.dismiss(loadingToastId)
                                                    toast.success(t('settingsPage.dangerZone.reset.toast.success'))
                                                    setTimeout(() => window.location.reload(), 1500)
                                                }}
                                            >
                                                <Trash2 className="h-4 w-4 mr-2" />
                                                {t('settingsPage.dangerZone.reset.confirmButton')}
                                            </Button>
                                        </DialogFooter>
                                    </DialogContent>
                                </Dialog>
                            </div>

                            <div className="flex items-center justify-between py-2 border-t">
                                <div className="space-y-0.5">
                                    <Label className="font-medium">{t('settingsPage.cache.title')}</Label>
                                    <p className="text-sm text-muted-foreground">
                                        {t('settingsPage.cache.description')}
                                    </p>
                                </div>
                                <Button
                                    variant="outline"
                                    size="sm"
                                    disabled={isClearingCache}
                                    onClick={async () => {
                                        try {
                                            // 1. Clear backend Redis cache
                                            const result = await clearCache().unwrap()

                                            // 2. Clear browser cache (Service Worker, PWA)
                                            if ('caches' in window) {
                                                const cacheNames = await caches.keys()
                                                await Promise.all(cacheNames.map(name => caches.delete(name)))
                                            }

                                            const backendCaches = result.data?.clearedCaches || 0
                                            toast.success(t('settingsPage.cache.toastSuccessDetailed', {
                                                count: backendCaches,
                                                defaultValue: `${backendCaches} önbellek temizlendi ✓`
                                            }))
                                        } catch (error) {
                                            console.error('Cache clear failed:', error)
                                            // Fallback: at least clear browser cache
                                            if ('caches' in window) {
                                                caches.keys().then(names => {
                                                    names.forEach(name => caches.delete(name))
                                                })
                                            }
                                            toast.success(t('settingsPage.cache.toastSuccess'))
                                        }
                                    }}
                                >
                                    {isClearingCache ? (
                                        <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                                    ) : (
                                        <RefreshCw className="h-4 w-4 mr-2" />
                                    )}
                                    {t('settingsPage.cache.button')}
                                </Button>
                            </div>
                        </CardContent>
                    </Card>
                </TabsContent>

                <TabsContent value="api-keys">
                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between">
                            <div>
                                <CardTitle>{t('settings.apiKeys.title')}</CardTitle>
                                <CardDescription>
                                    {t('settings.apiKeys.description')}
                                </CardDescription>
                            </div>
                            <Dialog open={isDialogOpen} onOpenChange={(open) => {
                                if (!open) resetForm()
                                setIsDialogOpen(open)
                            }}>
                                <DialogTrigger asChild>
                                    <Button onClick={() => handleOpenDialog(null)}>
                                        <Plus className="h-4 w-4 mr-2" />
                                        {t('settings.apiKeys.add')}
                                    </Button>
                                </DialogTrigger>
                                <DialogContent>
                                    <DialogHeader>
                                        <DialogTitle>
                                            {editingConfig ? t('settings.apiKeys.edit') : t('settings.apiKeys.add')}
                                        </DialogTitle>
                                        <DialogDescription>
                                            {editingConfig
                                                ? t('settings.apiKeys.editDescription')
                                                : t('settings.apiKeys.addDescription')}
                                        </DialogDescription>
                                    </DialogHeader>
                                    <form onSubmit={handleAddSubmit} className="space-y-4 py-4">
                                        <div className="space-y-2">
                                            <Label>{t('settings.apiKeys.provider')}</Label>
                                            <Select
                                                value={formData.provider}
                                                onValueChange={(val) => {
                                                    setFormData({ ...formData, provider: val })
                                                    setIsValidated(false)
                                                }}
                                                disabled={!!editingConfig}
                                            >
                                                <SelectTrigger>
                                                    <SelectValue />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    <SelectItem value="ALPHA_VANTAGE">{t('settings.apiKeys.providers.alphaVantage')}</SelectItem>
                                                    <SelectItem value="YAHOO_FINANCE">{t('settings.apiKeys.providers.yahooFinance')}</SelectItem>
                                                    <SelectItem value="FINNHUB">{t('settings.apiKeys.providers.finnhub')}</SelectItem>
                                                    <SelectItem value="TCMB">{t('settings.providers.info.TCMB.title')}</SelectItem>
                                                    <SelectItem value="OTHER">{t('settings.apiKeys.providerOther')}</SelectItem>
                                                </SelectContent>
                                            </Select>

                                            {/* Provider Info Alert */}
                                            {formData.provider && PROVIDER_INFO[formData.provider] && (
                                                <div className={`text-sm p-3 rounded-md border ${PROVIDER_INFO[formData.provider].color}`}>
                                                    <p className="font-semibold mb-1">{PROVIDER_INFO[formData.provider].title}</p>
                                                    <p className="mb-2 text-xs opacity-90">{PROVIDER_INFO[formData.provider].description}</p>

                                                    <div className="grid grid-cols-2 gap-2 text-xs">
                                                        <div>
                                                            <span className="font-semibold block mb-0.5">{t('settings.providers.supported')}</span>
                                                            <ul className="list-disc list-inside opacity-80 space-y-0.5">
                                                                {PROVIDER_INFO[formData.provider].supported.map((item, i) => (
                                                                    <li key={i}>{item}</li>
                                                                ))}
                                                            </ul>
                                                        </div>
                                                        {PROVIDER_INFO[formData.provider].missing.length > 0 && (
                                                            <div>
                                                                <span className="font-semibold block mb-0.5">{t('settings.providers.missing')}</span>
                                                                <ul className="list-disc list-inside opacity-80 space-y-0.5">
                                                                    {PROVIDER_INFO[formData.provider].missing.map((item, i) => (
                                                                        <li key={i}>{item}</li>
                                                                    ))}
                                                                </ul>
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>
                                            )}
                                        </div>

                                        <div className="space-y-2">
                                            <Label>{t('settings.apiKeys.apiKey')}</Label>
                                            <div className="flex gap-2">
                                                <Input
                                                    value={formData.apiKey}
                                                    onChange={(e) => {
                                                        setFormData({ ...formData, apiKey: e.target.value })
                                                        setIsValidated(false)
                                                    }}
                                                    placeholder={editingConfig ? t('settings.apiKeys.placeholder.unchanged') : (formData.provider === 'TCMB' ? t('settings.apiKeys.placeholder.tcmb') : t('settings.apiKeys.placeholder.key'))}
                                                    required={!editingConfig && formData.provider !== 'TCMB'}
                                                    className={isValidated ? 'border-green-500' : ''}
                                                />
                                                <Button
                                                    type="button"
                                                    variant={isValidated ? 'success' : 'outline'}
                                                    onClick={handleTestKey}
                                                    disabled={isTesting || (formData.provider !== 'TCMB' && !formData.apiKey.trim())}
                                                >
                                                    {isTesting ? (
                                                        <RefreshCw className="h-4 w-4 animate-spin" />
                                                    ) : isValidated ? (
                                                        <><CheckCircle className="h-4 w-4 mr-1" /> {t('settings.apiKeys.valid')}</>
                                                    ) : (
                                                        t('settings.apiKeys.test')
                                                    )}
                                                </Button>
                                            </div>
                                            {!isValidated && !editingConfig && (
                                                <p className="text-xs text-muted-foreground">
                                                    {formData.provider === 'TCMB'
                                                        ? t('settings.apiKeys.validation.tcmbInfo')
                                                        : t('settings.apiKeys.validation.testRequired')}
                                                </p>
                                            )}
                                        </div>

                                        <div className="space-y-2">
                                            <Label>{t('settings.apiKeys.secretKey')}</Label>
                                            <Input
                                                type="password"
                                                value={formData.secretKey}
                                                onChange={(e) => setFormData({ ...formData, secretKey: e.target.value })}
                                                placeholder={t('settings.apiKeys.placeholder.secretKey')}
                                            />
                                        </div>

                                        <div className="space-y-2">
                                            <Label>{t('settings.apiKeys.baseUrl')}</Label>
                                            <Input
                                                value={formData.baseUrl}
                                                onChange={(e) => setFormData({ ...formData, baseUrl: e.target.value })}
                                                placeholder={t('settings.apiKeys.placeholder.baseUrl')}
                                            />
                                        </div>

                                        <div className="flex items-center space-x-2 pt-2">
                                            <Switch
                                                checked={formData.isActive}
                                                onCheckedChange={(val) => setFormData({ ...formData, isActive: val })}
                                            />
                                            <Label>{t('settings.apiKeys.active')}</Label>
                                        </div>

                                        <DialogFooter>
                                            <Button
                                                type="submit"
                                                disabled={isAdding || (!isValidated && !editingConfig && formData.provider !== 'TCMB')}
                                                className={(!isValidated && !editingConfig && formData.provider !== 'TCMB') ? 'opacity-50' : ''}
                                            >
                                                {isAdding ? t('common.loading') : (editingConfig ? t('settings.apiKeys.update') : t('settings.apiKeys.save'))}
                                            </Button>
                                        </DialogFooter>
                                    </form>
                                </DialogContent>
                            </Dialog>
                        </CardHeader>
                        <CardContent>
                            {isLoading ? (
                                <div className="flex items-center justify-center p-8 text-muted-foreground">
                                    <RefreshCw className="h-6 w-6 animate-spin mr-2" />
                                    {t('common.loading')}
                                </div>
                            ) : apiConfigs.length === 0 ? (
                                <div className="text-center p-8 border-2 border-dashed rounded-lg text-muted-foreground">
                                    <Key className="h-10 w-10 mx-auto mb-3 opacity-50" />
                                    <p>{t('settings.apiKeys.noKeys')}</p>
                                    <Button variant="link" onClick={() => setIsDialogOpen(true)}>
                                        {t('settings.apiKeys.addFirstKey')}
                                    </Button>
                                </div>
                            ) : (
                                <div className="border rounded-md">
                                    <Table>
                                        <TableHeader>
                                            <TableRow>
                                                <TableHead>{t('settings.apiKeys.table.provider')}</TableHead>
                                                <TableHead>{t('settings.apiKeys.table.key')}</TableHead>
                                                <TableHead>{t('settings.apiKeys.table.status')}</TableHead>
                                                <TableHead>{t('settings.apiKeys.table.createdAt')}</TableHead>
                                                <TableHead className="text-right">{t('settings.apiKeys.table.actions')}</TableHead>
                                            </TableRow>
                                        </TableHeader>
                                        <TableBody>
                                            {apiConfigs.map((config) => (
                                                <TableRow key={config.id}>
                                                    <TableCell className="font-medium">
                                                        {getProviderLabel(config.provider)}
                                                    </TableCell>
                                                    <TableCell className="font-mono text-xs">
                                                        {config.apiKey}
                                                    </TableCell>
                                                    <TableCell>
                                                        <Badge variant={config.isActive ? "success" : "secondary"}>
                                                            {config.isActive ? t('settings.apiKeys.active') : t('settings.apiKeys.inactive')}
                                                        </Badge>
                                                    </TableCell>
                                                    <TableCell className="text-muted-foreground text-sm">
                                                        {new Date(config.createdAt).toLocaleDateString()}
                                                    </TableCell>
                                                    <TableCell className="text-right space-x-1">
                                                        <Button
                                                            variant="ghost"
                                                            size="icon"
                                                            className="text-primary hover:text-primary hover:bg-primary/10"
                                                            onClick={() => handleOpenDialog(config)}
                                                            title={t('common.edit')}
                                                        >
                                                            <Pencil className="h-4 w-4" />
                                                        </Button>
                                                        <Button
                                                            variant="ghost"
                                                            size="icon"
                                                            className="text-destructive hover:text-destructive hover:bg-destructive/10"
                                                            onClick={() => handleDelete(config.id)}
                                                            title={t('common.delete')}
                                                        >
                                                            <Trash2 className="h-4 w-4" />
                                                        </Button>
                                                    </TableCell>
                                                </TableRow>
                                            ))}
                                        </TableBody>
                                    </Table>
                                </div>
                            )}
                        </CardContent>
                    </Card>
                </TabsContent>

                {/* Data Sources Tab */}
                <TabsContent value="data-sources">
                    <Card>
                        <CardHeader>
                            <CardTitle className="flex items-center gap-2">
                                <Database className="h-5 w-5" />
                                {t('settings.dataSources.title', { defaultValue: 'Veri Kaynakları' })}
                            </CardTitle>
                            <CardDescription>
                                {t('settings.dataSources.description', { defaultValue: 'Her veri türü için hangi kaynağı kullanmak istediğinizi seçin' })}
                            </CardDescription>
                        </CardHeader>
                        <CardContent>
                            {!apiConfigs || apiConfigs.length === 0 ? (
                                <div className="text-center p-8 border-2 border-dashed rounded-lg text-muted-foreground">
                                    <Key className="h-10 w-10 mx-auto mb-3 opacity-50" />
                                    <p className="mb-2">{t('settings.dataSources.noApiKeys', { defaultValue: 'Önce API anahtarı eklemeniz gerekiyor' })}</p>
                                    <Button variant="link" onClick={() => document.querySelector('[value="api-keys"]')?.click()}>
                                        {t('settings.apiKeys.add')}
                                    </Button>
                                </div>
                            ) : (
                                <div className="space-y-6">
                                    {/* Data Type Selection - Updated based on official API docs */}
                                    {/* TCMB: https://evds2.tcmb.gov.tr/ */}
                                    {/* Yahoo Finance: https://ranaroussi.github.io/yfinance/ */}
                                    {/* Alpha Vantage: https://www.alphavantage.co/documentation/ */}
                                    {/* Finnhub: https://finnhub.io/docs/api */}
                                    {[
                                        { type: 'CURRENCY_RATES', label: 'Döviz Kurları', providers: ['TCMB', 'YAHOO_FINANCE', 'ALPHA_VANTAGE', 'FINNHUB'] },
                                        { type: 'BIST_STOCKS', label: 'BIST Hisseleri', providers: ['YAHOO_FINANCE'] },
                                        { type: 'US_STOCKS', label: 'ABD Hisseleri', providers: ['YAHOO_FINANCE', 'ALPHA_VANTAGE', 'FINNHUB'] },
                                        { type: 'CRYPTO', label: 'Kripto Paralar', providers: ['YAHOO_FINANCE', 'ALPHA_VANTAGE', 'FINNHUB'] },
                                        { type: 'NEWS', label: 'Haberler', providers: ['YAHOO_FINANCE', 'ALPHA_VANTAGE', 'FINNHUB'] },
                                    ].map((dataType) => {
                                        const currentPref = preferencesData?.data?.find(p => p.dataType === dataType.type)
                                        const availableProviders = apiConfigs.filter(c =>
                                            c.isActive && dataType.providers.includes(c.provider)
                                        )

                                        return (
                                            <div key={dataType.type} className="flex items-center justify-between py-4 border-b last:border-0">
                                                <div className="flex items-center gap-3">
                                                    <div>
                                                        <Label className="text-base font-medium">{dataType.label}</Label>
                                                        <p className="text-sm text-muted-foreground">
                                                            {availableProviders.length > 0
                                                                ? `${availableProviders.length} kaynak mevcut`
                                                                : 'Kaynak yok - API anahtarı ekleyin'}
                                                        </p>
                                                    </div>
                                                </div>
                                                <div className="flex items-center gap-2">
                                                    {availableProviders.length > 0 ? (
                                                        <Select
                                                            value={currentPref?.provider || ''}
                                                            onValueChange={async (provider) => {
                                                                try {
                                                                    await setDataPreference({
                                                                        dataType: dataType.type,
                                                                        provider: provider,
                                                                        isEnabled: true
                                                                    }).unwrap()
                                                                    toast.success(`${dataType.label} kaynağı güncellendi`)
                                                                    refetchPreferences()
                                                                } catch (err) {
                                                                    toast.error(err.data?.message || 'Hata oluştu')
                                                                }
                                                            }}
                                                        >
                                                            <SelectTrigger className="w-48">
                                                                <SelectValue placeholder="Kaynak seçin" />
                                                            </SelectTrigger>
                                                            <SelectContent>
                                                                {availableProviders.map((config) => (
                                                                    <SelectItem key={config.provider} value={config.provider}>
                                                                        {getProviderLabel(config.provider)}
                                                                    </SelectItem>
                                                                ))}
                                                            </SelectContent>
                                                        </Select>
                                                    ) : (
                                                        <Badge variant="secondary">
                                                            {t('settings.dataSources.noProvider', { defaultValue: 'Kaynak yok' })}
                                                        </Badge>
                                                    )}
                                                    {currentPref && (
                                                        <Badge variant="success" className="ml-2">
                                                            <Zap className="h-3 w-3 mr-1" />
                                                            Aktif
                                                        </Badge>
                                                    )}
                                                </div>
                                            </div>
                                        )
                                    })}

                                    {/* Manual Refresh Button */}
                                    <div className="pt-4 border-t">
                                        <Button
                                            variant="outline"
                                            onClick={async () => {
                                                const activeConfig = apiConfigs.find(c => c.isActive)
                                                if (activeConfig) {
                                                    // Show loading toast with custom styling
                                                    const loadingToastId = toast(
                                                        <div className="flex items-center gap-3">
                                                            <div className="animate-spin rounded-full h-5 w-5 border-2 border-primary border-t-transparent" />
                                                            <div>
                                                                <p className="font-medium">Veriler güncelleniyor...</p>
                                                                <p className="text-xs text-muted-foreground">Lütfen bekleyin, bu birkaç saniye sürebilir</p>
                                                            </div>
                                                        </div>,
                                                        { duration: Infinity }
                                                    )
                                                    try {
                                                        await triggerDataFetch(activeConfig.id).unwrap()
                                                        toast.dismiss(loadingToastId)
                                                        toast.success(
                                                            <div className="flex items-center gap-2">
                                                                <CheckCircle className="h-5 w-5 text-green-500" />
                                                                <span>Veriler başarıyla güncellendi!</span>
                                                            </div>
                                                        )
                                                    } catch (err) {
                                                        toast.dismiss(loadingToastId)
                                                        toast.error('Veri çekme başarısız oldu. Lütfen tekrar deneyin.')
                                                    }
                                                }
                                            }}
                                            disabled={!apiConfigs.some(c => c.isActive)}
                                            className="group"
                                        >
                                            <RefreshCw className="h-4 w-4 mr-2 group-hover:animate-spin transition-transform" />
                                            {t('settings.dataSources.refreshNow', { defaultValue: 'Verileri Şimdi Güncelle' })}
                                        </Button>
                                    </div>
                                </div>
                            )}
                        </CardContent>
                    </Card>
                </TabsContent>

                {/* Simulation Tab */}
                <TabsContent value="simulation">
                    <Card>
                        <CardHeader>
                            <CardTitle className="flex items-center gap-2">
                                <FlaskConical className="h-5 w-5" />
                                {t('settings.simulation.title', { defaultValue: 'Simülasyon Modu' })}
                            </CardTitle>
                            <CardDescription>
                                {t('settings.simulation.description', { defaultValue: 'Gerçek API olmadan gerçekçi piyasa verileri ile test edin' })}
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-6">
                            {/* Main Toggle */}
                            <div className="flex items-center justify-between p-4 border rounded-lg bg-gradient-to-r from-purple-50 to-blue-50 dark:from-purple-950/20 dark:to-blue-950/20">
                                <div className="space-y-1">
                                    <Label className="text-base font-semibold flex items-center gap-2">
                                        <FlaskConical className="h-4 w-4" />
                                        {t('settings.simulation.enable', { defaultValue: 'Simülasyon Modunu Aktif Et' })}
                                    </Label>
                                    <p className="text-sm text-muted-foreground">
                                        {t('settings.simulation.enableDesc', { defaultValue: 'Aktif edildiğinde gerçek API\'ler yerine simüle edilmiş veriler kullanılır' })}
                                    </p>
                                </div>
                                <div className="flex items-center gap-3">
                                    {simConfig?.enabled && (
                                        <Badge variant="default" className="bg-green-500">
                                            {t('settings.simulation.active', { defaultValue: 'Aktif' })}
                                        </Badge>
                                    )}
                                    <Switch
                                        checked={simConfig?.enabled || false}
                                        onCheckedChange={async () => {
                                            try {
                                                await toggleSimulation().unwrap()
                                                refetchSimConfig()
                                                toast.success(simConfig?.enabled
                                                    ? t('settings.simulation.disabled', { defaultValue: 'Simülasyon kapatıldı' })
                                                    : t('settings.simulation.enabled', { defaultValue: '🎮 Simülasyon aktif!' }))
                                            } catch (e) {
                                                toast.error(t('common.error'))
                                            }
                                        }}
                                    />
                                </div>
                            </div>

                            {/* Status Display */}
                            {simConfig?.enabled && simStatus && (
                                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                                    <div className="p-3 border rounded-lg text-center">
                                        <p className="text-2xl font-bold text-blue-600">{simStatus.stockCount || 0}</p>
                                        <p className="text-xs text-muted-foreground">{t('settings.simulation.stocks', { defaultValue: 'Hisse Senedi' })}</p>
                                    </div>
                                    <div className="p-3 border rounded-lg text-center">
                                        <p className="text-2xl font-bold text-green-600">{simStatus.currencyCount || 0}</p>
                                        <p className="text-xs text-muted-foreground">{t('settings.simulation.currencies', { defaultValue: 'Döviz Kuru' })}</p>
                                    </div>
                                    <div className="p-3 border rounded-lg text-center">
                                        <p className="text-2xl font-bold text-purple-600">{simStatus.indexCount || 0}</p>
                                        <p className="text-xs text-muted-foreground">{t('settings.simulation.indices', { defaultValue: 'Endeks' })}</p>
                                    </div>
                                    <div className="p-3 border rounded-lg text-center">
                                        <p className="text-2xl font-bold text-orange-600">{simStatus.tickCount || 0}</p>
                                        <p className="text-xs text-muted-foreground">{t('settings.simulation.ticks', { defaultValue: 'Güncelleme' })}</p>
                                    </div>
                                </div>
                            )}

                            {/* Configuration Options */}
                            {simConfig?.enabled && (
                                <div className="space-y-4 p-4 border rounded-lg">
                                    <h4 className="font-medium">{t('settings.simulation.config', { defaultValue: 'Simülasyon Ayarları' })}</h4>

                                    {/* Volatility Level */}
                                    <div className="space-y-2">
                                        <Label>{t('settings.simulation.volatility', { defaultValue: 'Volatilite Seviyesi' })}</Label>
                                        <Select
                                            value={simConfig?.volatilityLevel || 'MEDIUM'}
                                            onValueChange={async (val) => {
                                                try {
                                                    await updateSimConfig({ volatilityLevel: val }).unwrap()
                                                    refetchSimConfig()
                                                    toast.success(t('settings.simulation.volatilityUpdated', { defaultValue: 'Volatilite güncellendi' }))
                                                } catch (e) {
                                                    toast.error(t('common.error'))
                                                }
                                            }}
                                        >
                                            <SelectTrigger>
                                                <SelectValue />
                                            </SelectTrigger>
                                            <SelectContent>
                                                <SelectItem value="LOW">
                                                    <span className="flex items-center gap-2">
                                                        <Minus className="h-4 w-4 text-green-500" />
                                                        {t('settings.simulation.volatility.low', { defaultValue: 'Düşük (Sakin Piyasa)' })}
                                                    </span>
                                                </SelectItem>
                                                <SelectItem value="MEDIUM">
                                                    <span className="flex items-center gap-2">
                                                        <TrendingUp className="h-4 w-4 text-blue-500" />
                                                        {t('settings.simulation.volatility.medium', { defaultValue: 'Orta (Normal)' })}
                                                    </span>
                                                </SelectItem>
                                                <SelectItem value="HIGH">
                                                    <span className="flex items-center gap-2">
                                                        <TrendingUp className="h-4 w-4 text-orange-500" />
                                                        {t('settings.simulation.volatility.high', { defaultValue: 'Yüksek (Hareketli)' })}
                                                    </span>
                                                </SelectItem>
                                                <SelectItem value="EXTREME">
                                                    <span className="flex items-center gap-2">
                                                        <AlertCircle className="h-4 w-4 text-red-500" />
                                                        {t('settings.simulation.volatility.extreme', { defaultValue: 'Ekstrem (Kriz Modu)' })}
                                                    </span>
                                                </SelectItem>
                                            </SelectContent>
                                        </Select>
                                    </div>

                                    {/* Market Trend */}
                                    <div className="space-y-2">
                                        <Label>{t('settings.simulation.trend', { defaultValue: 'Piyasa Trendi' })}</Label>
                                        <Select
                                            value={simConfig?.marketTrend || 'NEUTRAL'}
                                            onValueChange={async (val) => {
                                                try {
                                                    await updateSimConfig({ marketTrend: val }).unwrap()
                                                    refetchSimConfig()
                                                    toast.success(t('settings.simulation.trendUpdated', { defaultValue: 'Trend güncellendi' }))
                                                } catch (e) {
                                                    toast.error(t('common.error'))
                                                }
                                            }}
                                        >
                                            <SelectTrigger>
                                                <SelectValue />
                                            </SelectTrigger>
                                            <SelectContent>
                                                <SelectItem value="BULLISH">
                                                    <span className="flex items-center gap-2">
                                                        <TrendingUp className="h-4 w-4 text-green-500" />
                                                        {t('settings.simulation.trend.bullish', { defaultValue: 'Boğa (Yükseliş)' })}
                                                    </span>
                                                </SelectItem>
                                                <SelectItem value="NEUTRAL">
                                                    <span className="flex items-center gap-2">
                                                        <Minus className="h-4 w-4 text-gray-500" />
                                                        {t('settings.simulation.trend.neutral', { defaultValue: 'Nötr' })}
                                                    </span>
                                                </SelectItem>
                                                <SelectItem value="BEARISH">
                                                    <span className="flex items-center gap-2">
                                                        <TrendingDown className="h-4 w-4 text-red-500" />
                                                        {t('settings.simulation.trend.bearish', { defaultValue: 'Ayı (Düşüş)' })}
                                                    </span>
                                                </SelectItem>
                                            </SelectContent>
                                        </Select>
                                    </div>

                                    {/* Update Interval */}
                                    <div className="space-y-2">
                                        <Label>{t('settings.simulation.interval', { defaultValue: 'Güncelleme Aralığı' })}</Label>
                                        <Select
                                            value={String(simConfig?.updateIntervalSeconds || 5)}
                                            onValueChange={async (val) => {
                                                try {
                                                    await updateSimConfig({ updateIntervalSeconds: parseInt(val) }).unwrap()
                                                    refetchSimConfig()
                                                    toast.success(t('settings.simulation.intervalUpdated', { defaultValue: 'Güncelleme aralığı değiştirildi' }))
                                                } catch (e) {
                                                    toast.error(t('common.error'))
                                                }
                                            }}
                                        >
                                            <SelectTrigger>
                                                <SelectValue />
                                            </SelectTrigger>
                                            <SelectContent>
                                                <SelectItem value="1">1 {t('settings.simulation.seconds', { defaultValue: 'saniye' })}</SelectItem>
                                                <SelectItem value="3">3 {t('settings.simulation.seconds', { defaultValue: 'saniye' })}</SelectItem>
                                                <SelectItem value="5">5 {t('settings.simulation.seconds', { defaultValue: 'saniye' })}</SelectItem>
                                                <SelectItem value="10">10 {t('settings.simulation.seconds', { defaultValue: 'saniye' })}</SelectItem>
                                                <SelectItem value="30">30 {t('settings.simulation.seconds', { defaultValue: 'saniye' })}</SelectItem>
                                            </SelectContent>
                                        </Select>
                                    </div>

                                    {/* Random Events Toggle */}
                                    <div className="flex items-center justify-between">
                                        <div className="space-y-0.5">
                                            <Label>{t('settings.simulation.randomEvents', { defaultValue: 'Rastgele Piyasa Olayları' })}</Label>
                                            <p className="text-sm text-muted-foreground">
                                                {t('settings.simulation.randomEventsDesc', { defaultValue: 'Beklenmedik fiyat hareketleri simüle et' })}
                                            </p>
                                        </div>
                                        <Switch
                                            checked={simConfig?.enableRandomEvents || false}
                                            onCheckedChange={async (val) => {
                                                try {
                                                    await updateSimConfig({ enableRandomEvents: val }).unwrap()
                                                    refetchSimConfig()
                                                } catch (e) {
                                                    toast.error(t('common.error'))
                                                }
                                            }}
                                        />
                                    </div>

                                    {/* Market Hours Toggle */}
                                    <div className="flex items-center justify-between">
                                        <div className="space-y-0.5">
                                            <Label>{t('settings.simulation.marketHours', { defaultValue: 'Piyasa Saatlerini Kullan (10:00-18:00)' })}</Label>
                                            <p className="text-sm text-muted-foreground">
                                                {t('settings.simulation.marketHoursDesc', { defaultValue: 'Kapalıyken 7/24 çalışır' })}
                                            </p>
                                        </div>
                                        <Switch
                                            checked={simConfig?.enableMarketHours || false}
                                            onCheckedChange={async (val) => {
                                                try {
                                                    await updateSimConfig({ enableMarketHours: val }).unwrap()
                                                    refetchSimConfig()
                                                } catch (e) {
                                                    toast.error(t('common.error'))
                                                }
                                            }}
                                        />
                                    </div>
                                </div>
                            )}

                            {/* Reset Button */}
                            {simConfig?.enabled && (
                                <div className="flex justify-end">
                                    <Button
                                        variant="outline"
                                        onClick={async () => {
                                            try {
                                                await resetSimulation().unwrap()
                                                refetchSimConfig()
                                                toast.success(t('settings.simulation.reset', { defaultValue: '🔄 Simülasyon sıfırlandı' }))
                                            } catch (e) {
                                                toast.error(t('common.error'))
                                            }
                                        }}
                                    >
                                        <RotateCcw className="h-4 w-4 mr-2" />
                                        {t('settings.simulation.resetButton', { defaultValue: 'Simülasyonu Sıfırla' })}
                                    </Button>
                                </div>
                            )}

                            {/* Info Box */}
                            <div className="p-4 bg-blue-50 dark:bg-blue-950/20 border border-blue-200 dark:border-blue-800 rounded-lg">
                                <h4 className="font-medium text-blue-900 dark:text-blue-100 mb-2">
                                    {t('settings.simulation.infoTitle', { defaultValue: 'ℹ️ Simülasyon Hakkında' })}
                                </h4>
                                <ul className="text-sm text-blue-800 dark:text-blue-200 space-y-1 list-disc list-inside">
                                    <li>{t('settings.simulation.info1', { defaultValue: 'BIST 30 hisseleri için gerçekçi fiyat hareketleri' })}</li>
                                    <li>{t('settings.simulation.info2', { defaultValue: 'TCMB döviz kurları simülasyonu' })}</li>
                                    <li>{t('settings.simulation.info3', { defaultValue: 'Geometric Brownian Motion ve Mean Reversion algoritmaları' })}</li>
                                    <li>{t('settings.simulation.info4', { defaultValue: 'WebSocket üzerinden gerçek zamanlı güncellemeler' })}</li>
                                </ul>
                            </div>
                        </CardContent>
                    </Card>
                </TabsContent>
            </Tabs >
        </div >
    )
}
