import { createSlice } from '@reduxjs/toolkit'

const initialState = {
  sidebarCollapsed: false,
  sidebarMobileOpen: false,
  theme: localStorage.getItem('theme') || 'light',
  currency: localStorage.getItem('currency') || 'TRY',
  timezone: localStorage.getItem('timezone') || 'Europe/Istanbul',
  autoUpdate: localStorage.getItem('autoUpdate') !== 'false',
  refreshRate: parseInt(localStorage.getItem('refreshRate')) || 60,
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
    setSidebarCollapsed: (state, action) => {
      state.sidebarCollapsed = action.payload
    },
    toggleMobileSidebar: (state) => {
      state.sidebarMobileOpen = !state.sidebarMobileOpen
    },
    setMobileSidebarOpen: (state, action) => {
      state.sidebarMobileOpen = action.payload
    },
    setTheme: (state, action) => {
      state.theme = action.payload
      // Persist to localStorage
      localStorage.setItem('theme', action.payload)
    },
    toggleTheme: (state) => {
      state.theme = state.theme === 'light' ? 'dark' : 'light'
      localStorage.setItem('theme', state.theme)
    },
    setLoading: (state, action) => {
      state.loading = action.payload
    },
    setGlobalError: (state, action) => {
      state.globalError = action.payload
    },
    clearGlobalError: (state) => {
      state.globalError = null
    },
    setCurrency: (state, action) => {
      state.currency = action.payload
      localStorage.setItem('currency', action.payload)
    },
    setTimezone: (state, action) => {
      state.timezone = action.payload
      localStorage.setItem('timezone', action.payload)
    },
    setAutoUpdate: (state, action) => {
      state.autoUpdate = action.payload
      localStorage.setItem('autoUpdate', action.payload.toString())
    },
    setRefreshRate: (state, action) => {
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
export const selectSidebarCollapsed = (state) => state.ui.sidebarCollapsed
export const selectSidebarMobileOpen = (state) => state.ui.sidebarMobileOpen
export const selectTheme = (state) => state.ui.theme
export const selectCurrency = (state) => state.ui.currency
export const selectTimezone = (state) => state.ui.timezone
export const selectAutoUpdate = (state) => state.ui.autoUpdate
export const selectRefreshRate = (state) => state.ui.refreshRate
export const selectLoading = (state) => state.ui.loading
export const selectGlobalError = (state) => state.ui.globalError

export default uiSlice.reducer
