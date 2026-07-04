import { clsx } from "clsx"
import { twMerge } from "tailwind-merge"
import i18n from '@/i18n'

/**
 * Utility function to merge Tailwind CSS classes
 * Combines clsx and tailwind-merge for optimal class merging
 */
export function cn(...inputs: any[]) {
  return twMerge(clsx(inputs))
}

import { formatCurrency as formatCurrencyWithRates } from '@/lib/currency'

/**
 * Format market quote price in its native currency (e.g. ₺34.50 for TRY quotes)
 */
export function formatCurrency(value: any, currency = 'TRY', locale = 'tr-TR') {
  if (value === null || value === undefined || isNaN(value)) return '-'
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

/**
 * Format user portfolio/balance values converted dynamically into user's preferred settings currency (USD/EUR/GBP/TRY)
 */
export function formatUserCurrency(value: any, sourceCurrency = 'TRY') {
  if (value === null || value === undefined || isNaN(value)) return '-'
  const userPreferredCurrency = (typeof window !== 'undefined' && localStorage.getItem('currency')) || 'TRY'
  return formatCurrencyWithRates(value, userPreferredCurrency, { sourceCurrency })
}

/**
 * Format number with locale
 */
export function formatNumber(value: any, decimals = 2, locale = 'tr-TR') {
  if (value === null || value === undefined) return '-'
  return new Intl.NumberFormat(locale, {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value)
}

/**
 * Format percentage
 */
export function formatPercent(value: any, decimals = 2) {
  if (value === null || value === undefined) return '-'
  const sign = value > 0 ? '+' : ''
  return `${sign}${formatNumber(value, decimals)}%`
}

/**
 * Format date
 */
export function formatDate(date: any, options: any = {}) {
  if (!date) return '-'
  const defaultOptions: Intl.DateTimeFormatOptions = {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    ...options
  }
  return new Date(date).toLocaleDateString('tr-TR', defaultOptions)
}

/**
 * Format datetime
 */
export function formatDateTime(date: any) {
  if (!date) return '-'
  return new Date(date).toLocaleString('tr-TR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/**
 * Format datetime with second precision
 */
export function formatDateTimeWithSeconds(date: any) {
  if (!date) return '-'
  return new Date(date).toLocaleString('tr-TR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

/**
 * Format relative time.
 */
export function formatRelativeTime(date: any) {
  if (!date) return '-'
  const now = new Date()
  const past = new Date(date)
  const diffInSeconds = Math.floor((now.getTime() - past.getTime()) / 1000)

  if (diffInSeconds < 60) return i18n.t('time.justNow')
  if (diffInSeconds < 3600) return i18n.t('time.minutesAgo', { count: Math.floor(diffInSeconds / 60) })
  if (diffInSeconds < 86400) return i18n.t('time.hoursAgo', { count: Math.floor(diffInSeconds / 3600) })
  if (diffInSeconds < 604800) return i18n.t('time.daysAgo', { count: Math.floor(diffInSeconds / 86400) })

  return formatDate(date)
}

/**
 * Get price change class based on value
 */
export function getPriceChangeClass(value: any) {
  if (value > 0) return 'price-up'
  if (value < 0) return 'price-down'
  return 'price-neutral'
}

/**
 * Get badge variant based on status
 */
export function getStatusVariant(status: any) {
  const variants = {
    success: 'success',
    active: 'success',
    completed: 'success',
    pending: 'warning',
    processing: 'warning',
    error: 'destructive',
    failed: 'destructive',
    cancelled: 'destructive',
    info: 'info',
    default: 'secondary',
  }
  return variants[status?.toLowerCase()] || variants.default
}

/**
 * Truncate text with ellipsis
 */
export function truncate(str: any, length = 50) {
  if (!str) return ''
  if (str.length <= length) return str
  return str.slice(0, length) + '...'
}

/**
 * Generate initials from name
 */
export function getInitials(name: any) {
  if (!name) return '?'
  return name
    .split(' ')
    .map(word => word[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

/**
 * Sleep utility for async operations
 */
export function sleep(ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * Format large numbers with K, M, B suffixes
 */
export function formatShortNumber(num: number) {
  if (num >= 1e9) {
    return (num / 1e9).toFixed(1) + 'B'
  }
  if (num >= 1e6) {
    return (num / 1e6).toFixed(1) + 'M'
  }
  if (num >= 1e3) {
    return (num / 1e3).toFixed(1) + 'K'
  }
  return num.toString()
}

/**
 * Debounce function
 */
export function debounce(func: (...args: any[]) => void, wait: number) {
  let timeout: any
  return function executedFunction(...args: any[]) {
    const later = () => {
      clearTimeout(timeout)
      func(...args)
    }
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}
