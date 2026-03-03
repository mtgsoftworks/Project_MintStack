/**
 * Custom hooks for user settings
 * Provides easy access to currency/timezone formatting
 * Automatically syncs TCMB rates for dynamic currency conversion
 */

import { useEffect } from 'react'
import { useSelector } from 'react-redux'
import { selectCurrency, selectTimezone, selectAutoUpdate, selectRefreshRate } from '@/store/slices/uiSlice'
import { useGetCurrenciesQuery } from '@/store/api/marketApi'
import {
    formatCurrency,
    formatNumber,
    formatPercentage,
    getCurrencySymbol,
    convertCurrency,
    setLiveRates,
    calculateRatesFromTCMB,
    getLiveRates
} from '@/lib/currency'
import { formatDate, formatDateTime, formatTime, formatRelativeTime } from '@/lib/datetime'

/**
 * Hook for currency formatting with live TCMB rates
 * Automatically syncs rates from TCMB API (no API key needed)
 */
export function useCurrency() {
    const currency = useSelector(selectCurrency)

    // Fetch TCMB rates - this is public data, no API key required
    const { data: currencies } = useGetCurrenciesQuery(undefined)

    // Sync TCMB rates to currency.js whenever data changes
    useEffect(() => {
        if (currencies && currencies.length > 0) {
            const rates = calculateRatesFromTCMB(currencies)
            setLiveRates(rates)
        }
    }, [currencies])

    const currentRates = getLiveRates()

    return {
        currency,
        rates: currentRates,
        format: (amount: any, options: any = {}) => formatCurrency(amount, currency, { ...options, rates: currentRates }),
        formatAs: (amount: any, targetCurrency: any, options: any = {}) => formatCurrency(amount, targetCurrency, { ...options, rates: currentRates }),
        formatNumber: (num, decimals = 2) => formatNumber(num, decimals),
        formatPercentage: (value, decimals = 2) => formatPercentage(value, decimals),
        symbol: getCurrencySymbol(currency),
        convert: (amount, fromCurrency = 'TRY') => convertCurrency(amount, fromCurrency, currency, currentRates),
    }
}

/**
 * Hook for date/time formatting with user's preferred timezone
 */
export function useDateTime() {
    const timezone = useSelector(selectTimezone)

    return {
        timezone,
        formatDate: (date: any, options: any = {}) => formatDate(date, timezone, options),
        formatDateTime: (date: any, options: any = {}) => formatDateTime(date, timezone, options),
        formatTime: (date: any, options: any = {}) => formatTime(date, timezone, options),
        formatRelative: (date) => formatRelativeTime(date, 'tr'),
    }
}

/**
 * Hook for auto-update settings
 */
export function useAutoUpdate() {
    const autoUpdate = useSelector(selectAutoUpdate)
    const refreshRate = useSelector(selectRefreshRate)

    return {
        enabled: autoUpdate,
        refreshRate,
        refreshRateMs: refreshRate * 1000,
    }
}

/**
 * Combined hook for all user settings
 */
export function useUserSettings() {
    const currency = useCurrency()
    const dateTime = useDateTime()
    const autoUpdate = useAutoUpdate()

    return { currency, dateTime, autoUpdate }
}

export default { useCurrency, useDateTime, useAutoUpdate, useUserSettings }
