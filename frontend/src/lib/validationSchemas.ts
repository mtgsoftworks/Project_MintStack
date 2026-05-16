import * as Yup from 'yup'

// Portfolio schemas
export const createPortfolioSchema = Yup.object({
  name: Yup.string()
    .min(2, 'portfolioPage.validation.nameMin')
    .max(50, 'portfolioPage.validation.nameMax')
    .required('portfolioPage.validation.nameRequired'),
  description: Yup.string()
    .max(200, 'portfolioPage.validation.descriptionMax')
})

export const addPortfolioItemSchema = Yup.object({
  symbol: Yup.string()
    .min(1, 'portfolioDetailPage.validation.symbolRequired')
    .max(10, 'portfolioDetailPage.validation.symbolMax')
    .matches(/^[A-Z0-9.]+$/, 'portfolioDetailPage.validation.symbolFormat')
    .required('portfolioDetailPage.validation.symbolRequired'),
  quantity: Yup.number()
    .min(0.0001, 'portfolioDetailPage.validation.quantityMin')
    .max(999999999, 'portfolioDetailPage.validation.quantityMax')
    .required('portfolioDetailPage.validation.quantityRequired'),
  purchasePrice: Yup.number()
    .min(0.01, 'portfolioDetailPage.validation.priceMin')
    .max(999999999, 'portfolioDetailPage.validation.priceMax')
    .required('portfolioDetailPage.validation.priceRequired'),
  purchaseDate: Yup.date()
    .max(new Date(), 'portfolioDetailPage.validation.dateMax')
})

// Alert schemas
export const createAlertSchema = Yup.object({
  symbol: Yup.string()
    .min(1, 'alerts.validation.symbolRequired')
    .max(10, 'alerts.validation.symbolMax')
    .required('alerts.validation.symbolRequired'),
  type: Yup.string()
    .oneOf(['PRICE_ABOVE', 'PRICE_BELOW', 'PERCENT_UP', 'PERCENT_DOWN'])
    .required('alerts.validation.typeRequired'),
  targetValue: Yup.number()
    .min(0.0001, 'alerts.validation.targetMin')
    .required('alerts.validation.targetRequired'),
  notes: Yup.string()
    .max(200, 'alerts.validation.notesMax')
})

// Profile schemas
export const updateProfileSchema = Yup.object({
  firstName: Yup.string()
    .min(2, 'profile.validation.firstNameMin')
    .max(50, 'profile.validation.firstNameMax'),
  lastName: Yup.string()
    .min(2, 'profile.validation.lastNameMin')
    .max(50, 'profile.validation.lastNameMax'),
  phoneNumber: Yup.string()
    .matches(/^[+]?[0-9\s-()]+$/, 'profile.validation.phoneFormat')
    .nullable(),
  bio: Yup.string()
    .max(500, 'profile.validation.bioMax'),
  location: Yup.string()
    .max(100, 'profile.validation.locationMax')
})

// API Key schemas
export const apiKeySchema = Yup.object({
  provider: Yup.string()
    .oneOf(['ALPHA_VANTAGE', 'YAHOO_FINANCE', 'FINNHUB', 'FINTABLES', 'LLM_ENRICHMENT', 'TCMB', 'TEFAS', 'BIST_DATASTORE', 'RSS', 'OTHER'])
    .required('settings.apiKeys.validation.providerRequired'),
  apiKey: Yup.string()
    .when('provider', {
      is: (provider) => !['TCMB', 'TEFAS', 'BIST_DATASTORE', 'RSS', 'YAHOO_FINANCE'].includes(provider),
      then: (schema) => schema.required('settings.apiKeys.validation.required'),
      otherwise: (schema) => schema
    }),
  baseUrl: Yup.string()
    .url('settings.apiKeys.validation.urlFormat')
    .nullable()
})

// Watchlist schemas
export const createWatchlistSchema = Yup.object({
  name: Yup.string()
    .min(2, 'watchlist.validation.nameMin')
    .max(50, 'watchlist.validation.nameMax')
    .required('watchlist.validation.nameRequired')
})

export const addWatchlistItemSchema = Yup.object({
  symbol: Yup.string()
    .min(1, 'watchlist.validation.symbolRequired')
    .max(10, 'watchlist.validation.symbolMax')
    .required('watchlist.validation.symbolRequired')
})

// Login schema (for custom login if not using Keycloak UI)
export const loginSchema = Yup.object({
  username: Yup.string()
    .min(3, 'auth.validation.usernameMin')
    .required('auth.validation.usernameRequired'),
  password: Yup.string()
    .min(6, 'auth.validation.passwordMin')
    .required('auth.validation.passwordRequired')
})

// Helper to get translated error message
export function getValidationError(t, errorKey) {
  if (!errorKey) return ''
  return t(errorKey)
}
