import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { toast } from 'sonner'
import {
    selectTheme,
    setTheme,
    selectCurrency,
    setCurrency,
    selectTimezone,
    setTimezone,
    selectAutoUpdate,
    setAutoUpdate,
    selectRefreshRate,
    setRefreshRate
} from '@/store/slices/uiSlice'
import { useGetProfileQuery, useUpdateProfileMutation } from '@/store/api/userApi'
import { useClearCacheMutation, useDeleteMarketDataMutation } from '@/store/api/settingsApi'
import { portfolioService } from '@/services/portfolioService'
import watchlistService from '@/services/watchlistService'
import alertService from '@/services/alertService'

const DEFAULT_NOTIFICATION_SETTINGS = {
    priceAlerts: true,
    portfolioUpdates: true,
    emailNotifications: true,
    pushNotifications: false
}

export function useGeneralSettings({ t, i18n }) {
    const dispatch = useDispatch()

    const theme = useSelector(selectTheme)
    const currency = useSelector(selectCurrency)
    const timezone = useSelector(selectTimezone)
    const autoUpdate = useSelector(selectAutoUpdate)
    const refreshRate = useSelector(selectRefreshRate)

    const { data: profile } = useGetProfileQuery()
    const [updateProfile] = useUpdateProfileMutation()
    const [clearCache, { isLoading: isClearingCache }] = useClearCacheMutation()
    const [deleteMarketData] = useDeleteMarketDataMutation()

    const [notificationSettings, setNotificationSettings] = useState(DEFAULT_NOTIFICATION_SETTINGS)

    useEffect(() => {
        if (!profile) {
            return
        }

        setNotificationSettings({
            priceAlerts: profile.priceAlerts ?? true,
            portfolioUpdates: profile.portfolioUpdates ?? true,
            emailNotifications: profile.emailNotifications ?? true,
            pushNotifications: profile.pushNotifications ?? false
        })
    }, [profile])

    const handleNotificationToggle = async (key, value) => {
        const previousSettings = notificationSettings
        const nextSettings = { ...previousSettings, [key]: value }

        setNotificationSettings(nextSettings)

        try {
            await updateProfile({ [key]: value }).unwrap()
            toast.success(t('success.saved'))
        } catch {
            setNotificationSettings(previousSettings)
            toast.error(t('common.error'))
        }
    }

    const handleThemeChange = (value) => {
        dispatch(setTheme(value))

        if (value === 'dark') {
            document.documentElement.classList.add('dark')
        } else if (value === 'light') {
            document.documentElement.classList.remove('dark')
        } else {
            const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
            document.documentElement.classList.toggle('dark', prefersDark)
        }

        toast.success(t('settingsPage.appearance.themeChanged'))
    }

    const handleLanguageChange = (value) => {
        i18n.changeLanguage(value)
        toast.success(t('settingsPage.appearance.languageChanged', {
            language: t(`settingsPage.appearance.languageOptions.${value}`)
        }))
    }

    const handleCurrencyChange = (value) => {
        dispatch(setCurrency(value))
        toast.success(t('success.saved'))
    }

    const handleTimezoneChange = (value) => {
        dispatch(setTimezone(value))
        toast.success(t('success.saved'))
    }

    const handleAutoUpdateChange = (value) => {
        dispatch(setAutoUpdate(value))
        toast.success(t('success.saved'))
    }

    const handleRefreshRateChange = (value) => {
        dispatch(setRefreshRate(value))
        toast.success(t('success.saved'))
    }

    const handleClearCache = async () => {
        try {
            const result = await clearCache().unwrap()

            if ('caches' in window) {
                const cacheNames = await caches.keys()
                await Promise.all(cacheNames.map((name) => caches.delete(name)))
            }

            const backendCaches = result.data?.clearedCaches || 0
            toast.success(t('settingsPage.cache.toastSuccessDetailed', {
                count: backendCaches,
                defaultValue: `${backendCaches} onbellek temizlendi`
            }))
        } catch {
            if ('caches' in window) {
                const cacheNames = await caches.keys()
                await Promise.all(cacheNames.map((name) => caches.delete(name)))
            }
            toast.success(t('settingsPage.cache.toastSuccess'))
        }
    }

    const handleFullReset = async () => {
        const loadingToastId = toast(
            <div className="flex items-center gap-3">
                <div className="animate-spin rounded-full h-5 w-5 border-2 border-red-500 border-t-transparent" />
                <div>
                    <p className="font-medium">{t('settingsPage.dangerZone.reset.toast.loading')}</p>
                    <p className="text-xs text-muted-foreground">Tum veriler siliniyor...</p>
                </div>
            </div>,
            { duration: Infinity }
        )

        try {
            const portfolios = await portfolioService.getPortfolios()
            for (const portfolio of portfolios) {
                await portfolioService.deletePortfolio(portfolio.id)
            }
        } catch (error) {
            console.warn('Portfolio reset skipped:', error?.message)
        }

        try {
            const watchlistsResponse = await watchlistService.getAll()
            const watchlists = watchlistsResponse?.data || []
            for (const watchlist of watchlists) {
                await watchlistService.delete(watchlist.id)
            }
        } catch (error) {
            console.warn('Watchlist reset skipped:', error?.message)
        }

        try {
            const alertsResponse = await alertService.getAll()
            const alerts = alertsResponse?.data || []
            for (const alert of alerts) {
                await alertService.delete(alert.id)
            }
        } catch (error) {
            console.warn('Alerts reset skipped:', error?.message)
        }

        try {
            await deleteMarketData().unwrap()
        } catch (error) {
            console.warn('Market data reset skipped:', error?.message)
        }

        localStorage.clear()
        sessionStorage.clear()

        toast.dismiss(loadingToastId)
        toast.success(t('settingsPage.dangerZone.reset.toast.success'))
        setTimeout(() => window.location.reload(), 1500)
    }

    const handleSaveSettings = () => toast.success(t('success.saved'))

    return {
        theme,
        currency,
        timezone,
        autoUpdate,
        refreshRate,
        notificationSettings,
        isClearingCache,
        handleThemeChange,
        handleLanguageChange,
        handleCurrencyChange,
        handleTimezoneChange,
        handleAutoUpdateChange,
        handleRefreshRateChange,
        handleNotificationToggle,
        handleFullReset,
        handleClearCache,
        handleSaveSettings
    }
}
