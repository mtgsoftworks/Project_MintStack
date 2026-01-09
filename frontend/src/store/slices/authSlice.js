import { createSlice } from '@reduxjs/toolkit'

const initialState = {
  isAuthenticated: false,
  isInitialized: false,
  token: null,
  user: null,
  roles: [],
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setAuth: (state, action) => {
      const { token, user, roles } = action.payload
      state.isAuthenticated = true
      state.isInitialized = true
      state.token = token
      state.user = user
      state.roles = roles || []
    },
    setToken: (state, action) => {
      state.token = action.payload
    },
    setUser: (state, action) => {
      state.user = action.payload
    },
    setInitialized: (state, action) => {
      state.isInitialized = action.payload
    },
    logout: (state) => {
      state.isAuthenticated = false
      state.token = null
      state.user = null
      state.roles = []
    },
    updateProfile: (state, action) => {
      if (state.user) {
        state.user = { ...state.user, ...action.payload }
      }
    },
  },
})

export const { 
  setAuth, 
  setToken, 
  setUser, 
  setInitialized, 
  logout, 
  updateProfile 
} = authSlice.actions

// Selectors
export const selectIsAuthenticated = (state) => state.auth.isAuthenticated
export const selectIsInitialized = (state) => state.auth.isInitialized
export const selectToken = (state) => state.auth.token
export const selectUser = (state) => state.auth.user
export const selectRoles = (state) => state.auth.roles
export const selectIsAdmin = (state) => state.auth.roles.includes('admin')

export default authSlice.reducer
