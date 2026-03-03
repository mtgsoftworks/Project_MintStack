/**
 * Currency & Number Formatting Utilities
 * Uses TCMB live rates from Redux store (no API key required - public data)
 */

// Fallback rates (only used if TCMB data unavailable)
const FALLBACK_RATES = {
    TRY: 1,
    USD: 0.029,
    EUR: 0.027,
    GBP: 0.023,
}

const CURRENCY_SYMBOLS = {
    TRY: '₺',
    USD: '$',
    EUR: '€',
    GBP: '£',
}

const CURRENCY_LOCALES = {
    TRY: 'tr-TR',
    USD: 'en-US',
    EUR: 'de-DE',
    GBP: 'en-GB',
}

// Live rates storage - updated by useCurrencyRates hook from Redux
let liveRatesStore: any = null

/**
 * Set live currency rates (called by useCurrencyRates hook)
 * @param {object} rates - { TRY: 1, USD: 0.029, EUR: 0.027, GBP: 0.023 }
 */
export function setLiveRates(rates: any) {
    liveRatesStore = rates
}

/**
 * Get current exchange rates
 * Returns live TCMB rates if available, otherwise fallback
 */
export function getLiveRates() {
    if (liveRatesStore && Object.keys(liveRatesStore).length > 0) {
        return liveRatesStore
    }
    return FALLBACK_RATES
}

/**
 * Calculate rates from TCMB currency data
 * TCMB provides "1 USD = X TRY", we convert to "1 TRY = X USD"
 * @param {Array} currencies - Currency array from useGetCurrenciesQuery
 */
export function calculateRatesFromTCMB(currencies: any[] = []) {
    if (!currencies || !Array.isArray(currencies) || currencies.length === 0) {
        return FALLBACK_RATES
    }

    const rates = { TRY: 1 }

    currencies.forEach(currency => {
        // TCMB gives: sellingRate = how many TRY for 1 foreign currency
        // We need: 1 TRY = X foreign currency
        if (currency.sellingRate && currency.sellingRate > 0) {
            rates[currency.currencyCode] = 1 / currency.sellingRate
        }
    })

    return rates
}

/**
 * Format a number as currency
 * @param {number} amount - Amount in source currency
 * @param {string} targetCurrency - Target currency code
 * @param {object} options - Formatting options
 */
export function formatCurrency(amount: any, targetCurrency = 'TRY', options: any = {}) {
    if (amount === null || amount === undefined || isNaN(amount)) {
        return '-'
    }

    const {
        showSymbol = true,
        decimals = 2,
        sourceCurrency = 'TRY',
        rates = null  // Pass explicit rates or use stored
    } = options

    const effectiveRates = rates || getLiveRates()

    // Convert amount if different currencies
    let convertedAmount = amount
    if (sourceCurrency !== targetCurrency) {
        const amountInTRY = sourceCurrency === 'TRY'
            ? amount
            : amount / (effectiveRates[sourceCurrency] || 1)
        convertedAmount = amountInTRY * (effectiveRates[targetCurrency] || 1)
    }

    const locale = CURRENCY_LOCALES[targetCurrency] || 'tr-TR'

    try {
        if (showSymbol) {
            return new Intl.NumberFormat(locale, {
                style: 'currency',
                currency: targetCurrency,
                minimumFractionDigits: decimals,
                maximumFractionDigits: decimals,
            }).format(convertedAmount)
        } else {
            return new Intl.NumberFormat(locale, {
                minimumFractionDigits: decimals,
                maximumFractionDigits: decimals,
            }).format(convertedAmount)
        }
    } catch (error) {
        console.warn('Currency formatting error:', error)
        return `${CURRENCY_SYMBOLS[targetCurrency] || ''}${convertedAmount.toFixed(decimals)}`
    }
}

/**
 * Format a number with thousand separators
 */
export function formatNumber(num: any, decimals = 2, locale = 'tr-TR') {
    if (num === null || num === undefined || isNaN(num)) {
        return '-'
    }

    return new Intl.NumberFormat(locale, {
        minimumFractionDigits: decimals,
        maximumFractionDigits: decimals,
    }).format(num)
}

/**
 * Format percentage
 */
export function formatPercentage(value: any, decimals = 2) {
    if (value === null || value === undefined || isNaN(value)) {
        return '-'
    }

    const sign = value > 0 ? '+' : ''
    return `${sign}${value.toFixed(decimals)}%`
}

/**
 * Get currency symbol
 */
export function getCurrencySymbol(currencyCode = 'TRY') {
    return CURRENCY_SYMBOLS[currencyCode] || currencyCode
}

/**
 * Convert amount between currencies
 */
export function convertCurrency(amount: any, from = 'TRY', to = 'TRY', rates: any = null) {
    if (from === to) return amount

    const effectiveRates = rates || getLiveRates()

    const amountInTRY = from === 'TRY'
        ? amount
        : amount / (effectiveRates[from] || 1)
    return amountInTRY * (effectiveRates[to] || 1)
}

export default {
    formatCurrency,
    formatNumber,
    formatPercentage,
    getCurrencySymbol,
    convertCurrency,
    getLiveRates,
    setLiveRates,
    calculateRatesFromTCMB,
    CURRENCY_SYMBOLS,
    FALLBACK_RATES,
}
