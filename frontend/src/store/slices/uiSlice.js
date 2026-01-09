import { createSlice } from '@reduxjs/toolkit'

const initialState = {
  sidebarCollapsed: false,
  sidebarMobileOpen: false,
  theme: localStorage.getItem('theme') || 'light',
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
} = uiSlice.actions

// Selectors
export const selectSidebarCollapsed = (state) => state.ui.sidebarCollapsed
export const selectSidebarMobileOpen = (state) => state.ui.sidebarMobileOpen
export const selectTheme = (state) => state.ui.theme
export const selectLoading = (state) => state.ui.loading
export const selectGlobalError = (state) => state.ui.globalError

export default uiSlice.reducer
