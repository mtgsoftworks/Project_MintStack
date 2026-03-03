export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1'

export const KEYCLOAK_CONFIG = {
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'mintstack-finance',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'finance-frontend',
}

export const INSTRUMENT_TYPES = {
  CURRENCY: { label: 'Döviz', color: '#f59e0b' },
  STOCK: { label: 'Hisse', color: '#22c55e' },
  BOND: { label: 'Tahvil', color: '#3b82f6' },
  FUND: { label: 'Fon', color: '#8b5cf6' },
  VIOP: { label: 'VIOP', color: '#ec4899' },
  COMMODITY: { label: 'Emtia', color: '#06b6d4' },
}

export const TREND_TYPES = {
  UPTREND: { label: 'Yükseliş', icon: '↑', color: 'text-primary-400' },
  DOWNTREND: { label: 'Düşüş', icon: '↓', color: 'text-red-400' },
  SIDEWAYS: { label: 'Yatay', icon: '→', color: 'text-dark-400' },
}

export const TIME_RANGES = [
  { label: '1 Hafta', days: 7 },
  { label: '1 Ay', days: 30 },
  { label: '3 Ay', days: 90 },
  { label: '6 Ay', days: 180 },
  { label: '1 Yıl', days: 365 },
]

export const NEWS_CATEGORIES = [
  { slug: 'genel-ekonomi', name: 'Genel Ekonomi' },
  { slug: 'hisse-senedi', name: 'Hisse Senedi' },
  { slug: 'doviz', name: 'Döviz' },
  { slug: 'tahvil-bono', name: 'Tahvil/Bono' },
  { slug: 'altin', name: 'Altın' },
  { slug: 'kripto', name: 'Kripto' },
  { slug: 'dunya-ekonomisi', name: 'Dünya Ekonomisi' },
]
