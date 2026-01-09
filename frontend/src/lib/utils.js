import { clsx } from "clsx"
import { twMerge } from "tailwind-merge"

/**
 * Utility function to merge Tailwind CSS classes
 * Combines clsx and tailwind-merge for optimal class merging
 */
export function cn(...inputs) {
  return twMerge(clsx(inputs))
}

/**
 * Format currency value
 */
export function formatCurrency(value, currency = 'TRY', locale = 'tr-TR') {
  if (value === null || value === undefined) return '-'
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

/**
 * Format number with locale
 */
export function formatNumber(value, decimals = 2, locale = 'tr-TR') {
  if (value === null || value === undefined) return '-'
  return new Intl.NumberFormat(locale, {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value)
}

/**
 * Format percentage
 */
export function formatPercent(value, decimals = 2) {
  if (value === null || value === undefined) return '-'
  const sign = value > 0 ? '+' : ''
  return `${sign}${formatNumber(value, decimals)}%`
}

/**
 * Format date
 */
export function formatDate(date, options = {}) {
  if (!date) return '-'
  const defaultOptions = {
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
export function formatDateTime(date) {
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
 * Format relative time (e.g., "2 hours ago")
 */
export function formatRelativeTime(date) {
  if (!date) return '-'
  const now = new Date()
  const past = new Date(date)
  const diffInSeconds = Math.floor((now - past) / 1000)

  if (diffInSeconds < 60) return 'Az önce'
  if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} dakika önce`
  if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)} saat önce`
  if (diffInSeconds < 604800) return `${Math.floor(diffInSeconds / 86400)} gün önce`
  
  return formatDate(date)
}

/**
 * Get price change class based on value
 */
export function getPriceChangeClass(value) {
  if (value > 0) return 'price-up'
  if (value < 0) return 'price-down'
  return 'price-neutral'
}

/**
 * Get badge variant based on status
 */
export function getStatusVariant(status) {
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
export function truncate(str, length = 50) {
  if (!str) return ''
  if (str.length <= length) return str
  return str.slice(0, length) + '...'
}

/**
 * Generate initials from name
 */
export function getInitials(name) {
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
export function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * Debounce function
 */
export function debounce(func, wait) {
  let timeout
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout)
      func(...args)
    }
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}
