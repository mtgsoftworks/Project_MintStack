import { format, formatDistanceToNow } from 'date-fns'
import { tr } from 'date-fns/locale'

export const formatCurrency = (value, currency = 'TRY') => {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value || 0)
}

export const formatNumber = (value, decimals = 2) => {
  return new Intl.NumberFormat('tr-TR', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value || 0)
}

export const formatPercent = (value, decimals = 2) => {
  const sign = value > 0 ? '+' : ''
  return `${sign}${formatNumber(value, decimals)}%`
}

export const formatDate = (date, formatStr = 'd MMM yyyy') => {
  if (!date) return ''
  return format(new Date(date), formatStr, { locale: tr })
}

export const formatDateTime = (date) => {
  if (!date) return ''
  return format(new Date(date), 'd MMM yyyy HH:mm', { locale: tr })
}

export const formatRelativeTime = (date) => {
  if (!date) return ''
  return formatDistanceToNow(new Date(date), { addSuffix: true, locale: tr })
}

export const formatShortNumber = (num) => {
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
