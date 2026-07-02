import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { toast } from 'sonner'
import { skipToken } from '@reduxjs/toolkit/query/react'
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
import { useDeletePortfolioMutation, useGetPortfoliosQuery } from '@/store/api/portfolioApi'
import { useDeleteWatchlistMutation, useGetWatchlistsQuery } from '@/store/api/watchlistApi'
import { useDeleteAlertMutation, useGetAlertsQuery } from '@/store/api/alertsApi'
import type { NotificationSettings } from '../types'

const DEFAULT_NOTIFICATION_SETTINGS: NotificationSettings = {
    priceAlerts: true,
    portfolioUpdates: true,
    emailNotifications: true,
    pushNotifications: false
}

interface UseGeneralSettingsOptions {
    t: (key: string, options?: Record<string, unknown>) => string
    i18n: { language?: string; changeLanguage: (lang: string) => void }
}

export function useGeneralSettings({ t, i18n }: UseGeneralSettingsOptions) {
    const dispatch = useDispatch()

    const theme = useSelector(selectTheme)
    const currency = useSelector(selectCurrency)
    const timezone = useSelector(selectTimezone)
    const autoUpdate = useSelector(selectAutoUpdate)
    const refreshRate = useSelector(selectRefreshRate)

    const { data: profile } = useGetProfileQuery(skipToken)
    const [updateProfile] = useUpdateProfileMutation()
    const [clearCache, { isLoading: isClearingCache }] = useClearCacheMutation(undefined)
    const [deleteMarketData] = useDeleteMarketDataMutation()
    const { data: portfolios = [] } = useGetPortfoliosQuery(skipToken)
    const { data: watchlists = [] } = useGetWatchlistsQuery(skipToken)
    const { data: alerts = [] } = useGetAlertsQuery(skipToken)
    const [deletePortfolio] = useDeletePortfolioMutation()
    const [deleteWatchlist] = useDeleteWatchlistMutation()
    const [deleteAlert] = useDeleteAlertMutation()

    const [notificationSettings, setNotificationSettings] = useState<NotificationSettings>(DEFAULT_NOTIFICATION_SETTINGS)
    const [isSavingSettings, setIsSavingSettings] = useState(false)

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

    const handleNotificationToggle = async (key: keyof NotificationSettings, value: boolean) => {
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

    const handleThemeChange = (value: string) => {
        dispatch(setTheme(value as 'light' | 'dark'))

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

    const handleLanguageChange = (value: string) => {
        i18n.changeLanguage(value)
        toast.success(t('settingsPage.appearance.languageChanged', {
            language: t(`settingsPage.appearance.languageOptions.${value}`)
        }))
    }

    const handleCurrencyChange = (value: string) => {
        dispatch(setCurrency(value as 'TRY' | 'USD' | 'EUR' | 'GBP'))
        toast.success(t('success.saved'))
    }

    const handleTimezoneChange = (value: string) => {
        dispatch(setTimezone(value))
        toast.success(t('success.saved'))
    }

    const handleAutoUpdateChange = (value: boolean) => {
        dispatch(setAutoUpdate(value))
        toast.success(t('success.saved'))
    }

    const handleRefreshRateChange = (value: number) => {
        dispatch(setRefreshRate(value))
        toast.success(t('success.saved'))
    }

    const handleClearCache = async () => {
        try {
            if ('caches' in window) {
                const cacheNames = await caches.keys()
                await Promise.all(cacheNames.map((name) => caches.delete(name)))
            }

            const result = await clearCache(undefined)
            if (result.data && 'clearedCaches' in result.data) {
                toast.success(t('settingsPage.cache.toastSuccessDetailed', {
                    count: (result.data as { clearedCaches: number }).clearedCaches
                }))
            } else {
                toast.success(t('settingsPage.cache.toastSuccess'))
            }
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
                    <p className="text-xs text-muted-foreground">{t('settingsPage.dangerZone.reset.toast.progressDescription')}</p>
                </div>
            </div>,
            { duration: Infinity }
        )

        try {
            for (const portfolio of portfolios) {
                await deletePortfolio(portfolio.id).unwrap()
            }
        } catch (error) {
            console.warn('Portfolio reset skipped:', (error as { message?: string })?.message)
        }

        try {
            for (const watchlist of watchlists) {
                await deleteWatchlist(watchlist.id).unwrap()
            }
        } catch (error) {
            console.warn('Watchlist reset skipped:', (error as { message?: string })?.message)
        }

        try {
            for (const alert of alerts) {
                await deleteAlert(alert.id).unwrap()
            }
        } catch (error) {
            console.warn('Alerts reset skipped:', (error as { message?: string })?.message)
        }

        try {
            await deleteMarketData(undefined)
        } catch (error) {
            console.warn('Market data reset skipped:', (error as { message?: string })?.message)
        }

        localStorage.clear()
        sessionStorage.clear()

        toast.dismiss(loadingToastId)
        toast.success(t('settingsPage.dangerZone.reset.toast.success'))
        setTimeout(() => window.location.reload(), 1500)
    }

    const handleSaveSettings = async () => {
        setIsSavingSettings(true)

        try {
            await updateProfile({
                priceAlerts: notificationSettings.priceAlerts,
                portfolioUpdates: notificationSettings.portfolioUpdates,
                emailNotifications: notificationSettings.emailNotifications,
                pushNotifications: notificationSettings.pushNotifications
            }).unwrap()

            localStorage.setItem('theme', theme)
            localStorage.setItem('currency', currency)
            localStorage.setItem('timezone', timezone)
            localStorage.setItem('autoUpdate', String(autoUpdate))
            localStorage.setItem('refreshRate', String(refreshRate))
            localStorage.setItem('language', i18n.language?.split('-')[0] || 'tr')

            toast.success(t('success.saved'))
        } catch {
            toast.error(t('common.error'))
        } finally {
            setIsSavingSettings(false)
        }
    }

    return {
        theme,
        currency,
        timezone,
        autoUpdate,
        refreshRate,
        notificationSettings,
        isClearingCache,
        isSavingSettings,
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
