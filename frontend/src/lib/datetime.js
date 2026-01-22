/**
 * Date & Time Formatting Utilities
 * Uses user's preferred timezone from Redux store
 */

// Timezone display names
const TIMEZONE_NAMES = {
    'Europe/Istanbul': 'İstanbul (GMT+3)',
    'Europe/London': 'Londra (GMT+0/+1)',
    'America/New_York': 'New York (GMT-5/-4)',
}

/**
 * Format a date according to user's timezone preference
 * @param {Date|string|number} date - Date to format
 * @param {string} timezone - Target timezone
 * @param {object} options - Intl.DateTimeFormat options
 */
export function formatDate(date, timezone = 'Europe/Istanbul', options = {}) {
    if (!date) return '-'

    const dateObj = date instanceof Date ? date : new Date(date)

    if (isNaN(dateObj.getTime())) {
        return '-'
    }

    const defaultOptions = {
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
export function formatDateTime(date, timezone = 'Europe/Istanbul', options = {}) {
    if (!date) return '-'

    const dateObj = date instanceof Date ? date : new Date(date)

    if (isNaN(dateObj.getTime())) {
        return '-'
    }

    const { showSeconds = false, use24Hour = true } = options

    const formatOptions = {
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
export function formatTime(date, timezone = 'Europe/Istanbul', options = {}) {
    if (!date) return '-'

    const dateObj = date instanceof Date ? date : new Date(date)

    if (isNaN(dateObj.getTime())) {
        return '-'
    }

    const { showSeconds = false, use24Hour = true } = options

    const formatOptions = {
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
 * Get relative time string (e.g., "5 dakika önce")
 * @param {Date|string|number} date - Date to compare
 * @param {string} locale - Locale for formatting
 */
export function formatRelativeTime(date, locale = 'tr') {
    if (!date) return '-'

    const dateObj = date instanceof Date ? date : new Date(date)

    if (isNaN(dateObj.getTime())) {
        return '-'
    }

    const now = new Date()
    const diffInSeconds = Math.floor((now - dateObj) / 1000)

    // Turkish relative time strings
    if (locale === 'tr') {
        if (diffInSeconds < 60) {
            return 'Az önce'
        } else if (diffInSeconds < 3600) {
            const minutes = Math.floor(diffInSeconds / 60)
            return `${minutes} dakika önce`
        } else if (diffInSeconds < 86400) {
            const hours = Math.floor(diffInSeconds / 3600)
            return `${hours} saat önce`
        } else if (diffInSeconds < 604800) {
            const days = Math.floor(diffInSeconds / 86400)
            return `${days} gün önce`
        } else {
            return formatDate(dateObj)
        }
    }

    // English fallback
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
    } catch (error) {
        return formatDate(dateObj)
    }
}

/**
 * Get timezone display name
 * @param {string} timezone - Timezone identifier
 */
export function getTimezoneDisplayName(timezone) {
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
