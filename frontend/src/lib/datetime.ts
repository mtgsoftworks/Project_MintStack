/**
 * Date & Time Formatting Utilities
 * Uses user's preferred timezone from Redux store
 */
import i18n from '@/i18n'

const TIMEZONE_NAMES = {
    'Europe/Istanbul': 'Istanbul (GMT+3)',
    'Europe/London': 'London (GMT+0/+1)',
    'America/New_York': 'New York (GMT-5/-4)',
}

/**
 * Format a date according to user's timezone preference
 * @param {Date|string|number} date - Date to format
 * @param {string} timezone - Target timezone
 * @param {object} options - Intl.DateTimeFormat options
 */
export function formatDate(date: any, timezone = 'Europe/Istanbul', options: any = {}) {
    if (!date) return '-'

    const dateObj = date instanceof Date ? date : new Date(date)

    if (isNaN(dateObj.getTime())) {
        return '-'
    }

    const defaultOptions: Intl.DateTimeFormatOptions = {
        timeZone: timezone,
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        ...options
    }

    try {
        return new Intl.DateTimeFormat('tr-TR', defaultOptions).format(dateObj)
    } catch (error) {
        console.warn('Date formatting error:', error)
        return dateObj.toLocaleDateString('tr-TR')
    }
}

/**
 * Format a date and time according to user's timezone preference
 * @param {Date|string|number} date - Date to format
 * @param {string} timezone - Target timezone
 * @param {object} options - Additional options
 */
export function formatDateTime(date: any, timezone = 'Europe/Istanbul', options: any = {}) {
    if (!date) return '-'

    const dateObj = date instanceof Date ? date : new Date(date)

    if (isNaN(dateObj.getTime())) {
        return '-'
    }

    const { showSeconds = false, use24Hour = true } = options

    const formatOptions: Intl.DateTimeFormatOptions = {
        timeZone: timezone,
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        ...(showSeconds && { second: '2-digit' }),
        hour12: !use24Hour,
    }

    try {
        return new Intl.DateTimeFormat('tr-TR', formatOptions).format(dateObj)
    } catch (error) {
        console.warn('DateTime formatting error:', error)
        return dateObj.toLocaleString('tr-TR')
    }
}

/**
 * Format time only according to user's timezone preference
 * @param {Date|string|number} date - Date to format
 * @param {string} timezone - Target timezone
 */
export function formatTime(date: any, timezone = 'Europe/Istanbul', options: any = {}) {
    if (!date) return '-'

    const dateObj = date instanceof Date ? date : new Date(date)

    if (isNaN(dateObj.getTime())) {
        return '-'
    }

    const { showSeconds = false, use24Hour = true } = options

    const formatOptions: Intl.DateTimeFormatOptions = {
        timeZone: timezone,
        hour: '2-digit',
        minute: '2-digit',
        ...(showSeconds && { second: '2-digit' }),
        hour12: !use24Hour,
    }

    try {
        return new Intl.DateTimeFormat('tr-TR', formatOptions).format(dateObj)
    } catch (error) {
        console.warn('Time formatting error:', error)
        return dateObj.toLocaleTimeString('tr-TR')
    }
}

/**
 * Get relative time string.
 * @param {Date|string|number} date - Date to compare
 * @param {string} locale - Locale for formatting
 */
export function formatRelativeTime(date: any, locale = 'tr') {
    if (!date) return '-'

    const dateObj = date instanceof Date ? date : new Date(date)

    if (isNaN(dateObj.getTime())) {
        return '-'
    }

    const now = new Date()
    const diffInSeconds = Math.floor((now.getTime() - dateObj.getTime()) / 1000)

    if (locale === 'tr') {
        if (diffInSeconds < 60) {
            return i18n.t('time.justNow')
        } else if (diffInSeconds < 3600) {
            const minutes = Math.floor(diffInSeconds / 60)
            return i18n.t('time.minutesAgo', { count: minutes })
        } else if (diffInSeconds < 86400) {
            const hours = Math.floor(diffInSeconds / 3600)
            return i18n.t('time.hoursAgo', { count: hours })
        } else if (diffInSeconds < 604800) {
            const days = Math.floor(diffInSeconds / 86400)
            return i18n.t('time.daysAgo', { count: days })
        } else {
            return formatDate(dateObj)
        }
    }

    try {
        const rtf = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' })

        if (diffInSeconds < 60) {
            return rtf.format(-diffInSeconds, 'second')
        } else if (diffInSeconds < 3600) {
            return rtf.format(-Math.floor(diffInSeconds / 60), 'minute')
        } else if (diffInSeconds < 86400) {
            return rtf.format(-Math.floor(diffInSeconds / 3600), 'hour')
        } else {
            return rtf.format(-Math.floor(diffInSeconds / 86400), 'day')
        }
    } catch (_error) {
        return formatDate(dateObj)
    }
}

/**
 * Get timezone display name
 * @param {string} timezone - Timezone identifier
 */
export function getTimezoneDisplayName(timezone: string) {
    return TIMEZONE_NAMES[timezone] || timezone
}

/**
 * Get current time in a specific timezone
 * @param {string} timezone - Timezone identifier
 */
export function getCurrentTimeInTimezone(timezone = 'Europe/Istanbul') {
    return formatTime(new Date(), timezone)
}

export default {
    formatDate,
    formatDateTime,
    formatTime,
    formatRelativeTime,
    getTimezoneDisplayName,
    getCurrentTimeInTimezone,
    TIMEZONE_NAMES,
}
