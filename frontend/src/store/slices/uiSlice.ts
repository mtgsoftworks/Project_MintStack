import { createSlice, PayloadAction } from '@reduxjs/toolkit'

export type Theme = 'light' | 'dark'
export type Currency = 'TRY' | 'USD' | 'EUR' | 'GBP'

export interface UIState {
  sidebarCollapsed: boolean
  sidebarMobileOpen: boolean
  theme: Theme
  currency: Currency
  timezone: string
  autoUpdate: boolean
  refreshRate: number
  loading: boolean
  globalError: string | null
}

const getStoredTheme = (): Theme => {
  const stored = localStorage.getItem('theme')
  if (stored === 'light' || stored === 'dark') return stored
  return 'light'
}

const getStoredCurrency = (): Currency => {
  const stored = localStorage.getItem('currency')
  if (stored === 'TRY' || stored === 'USD' || stored === 'EUR' || stored === 'GBP') return stored
  return 'TRY'
}

const getStoredRefreshRate = (): number => {
  const stored = localStorage.getItem('refreshRate')
  if (stored) {
    const parsed = parseInt(stored, 10)
    if (!isNaN(parsed) && parsed > 0) return parsed
  }
  return 60
}

const initialState: UIState = {
  sidebarCollapsed: false,
  sidebarMobileOpen: false,
  theme: getStoredTheme(),
  currency: getStoredCurrency(),
  timezone: localStorage.getItem('timezone') || 'Europe/Istanbul',
  autoUpdate: localStorage.getItem('autoUpdate') !== 'false',
  refreshRate: getStoredRefreshRate(),
  loading: false,
  globalError: null,
}

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    toggleSidebar: (state) => {
      state.sidebarCollapsed = !state.sidebarCollapsed
    },
    setSidebarCollapsed: (state, action: PayloadAction<boolean>) => {
      state.sidebarCollapsed = action.payload
    },
    toggleMobileSidebar: (state) => {
      state.sidebarMobileOpen = !state.sidebarMobileOpen
    },
    setMobileSidebarOpen: (state, action: PayloadAction<boolean>) => {
      state.sidebarMobileOpen = action.payload
    },
    setTheme: (state, action: PayloadAction<Theme>) => {
      state.theme = action.payload
      localStorage.setItem('theme', action.payload)
    },
    toggleTheme: (state) => {
      state.theme = state.theme === 'light' ? 'dark' : 'light'
      localStorage.setItem('theme', state.theme)
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload
    },
    setGlobalError: (state, action: PayloadAction<string | null>) => {
      state.globalError = action.payload
    },
    clearGlobalError: (state) => {
      state.globalError = null
    },
    setCurrency: (state, action: PayloadAction<Currency>) => {
      state.currency = action.payload
      localStorage.setItem('currency', action.payload)
    },
    setTimezone: (state, action: PayloadAction<string>) => {
      state.timezone = action.payload
      localStorage.setItem('timezone', action.payload)
    },
    setAutoUpdate: (state, action: PayloadAction<boolean>) => {
      state.autoUpdate = action.payload
      localStorage.setItem('autoUpdate', action.payload.toString())
    },
    setRefreshRate: (state, action: PayloadAction<number>) => {
      state.refreshRate = action.payload
      localStorage.setItem('refreshRate', action.payload.toString())
    },
  },
})

export const {
  toggleSidebar,
  setSidebarCollapsed,
  toggleMobileSidebar,
  setMobileSidebarOpen,
  setTheme,
  toggleTheme,
  setLoading,
  setGlobalError,
  clearGlobalError,
  setCurrency,
  setTimezone,
  setAutoUpdate,
  setRefreshRate,
} = uiSlice.actions

// Selectors
export const selectSidebarCollapsed = (state: { ui: UIState }) => state.ui.sidebarCollapsed
export const selectSidebarMobileOpen = (state: { ui: UIState }) => state.ui.sidebarMobileOpen
export const selectTheme = (state: { ui: UIState }): Theme => state.ui.theme
export const selectCurrency = (state: { ui: UIState }): Currency => state.ui.currency
export const selectTimezone = (state: { ui: UIState }) => state.ui.timezone
export const selectAutoUpdate = (state: { ui: UIState }) => state.ui.autoUpdate
export const selectRefreshRate = (state: { ui: UIState }) => state.ui.refreshRate
export const selectLoading = (state: { ui: UIState }) => state.ui.loading
export const selectGlobalError = (state: { ui: UIState }): string | null => state.ui.globalError

export default uiSlice.reducer
