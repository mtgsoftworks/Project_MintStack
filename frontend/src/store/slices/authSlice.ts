import { createSlice, PayloadAction } from '@reduxjs/toolkit'

export type UserRole = 'admin' | 'user' | 'moderator' | string

export interface User {
  id?: string | number
  name?: string
  email?: string
  avatar?: string
  [key: string]: unknown
}

export interface AuthState {
  isAuthenticated: boolean
  isInitialized: boolean
  token: string | null
  user: User | null
  roles: UserRole[]
}

interface SetAuthPayload {
  token: string
  user: User
  roles?: UserRole[]
}

interface UpdateProfilePayload {
  [key: string]: unknown
}

const initialState: AuthState = {
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
    setAuth: (state, action: PayloadAction<SetAuthPayload>) => {
      const { token, user, roles } = action.payload
      state.isAuthenticated = true
      state.isInitialized = true
      state.token = token
      state.user = user
      state.roles = roles || []
    },
    setToken: (state, action: PayloadAction<string | null>) => {
      state.token = action.payload
    },
    setUser: (state, action: PayloadAction<User | null>) => {
      state.user = action.payload
    },
    setInitialized: (state, action: PayloadAction<boolean>) => {
      state.isInitialized = action.payload
    },
    logout: (state) => {
      state.isAuthenticated = false
      state.token = null
      state.user = null
      state.roles = []
    },
    updateProfile: (state, action: PayloadAction<UpdateProfilePayload>) => {
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
  updateProfile,
} = authSlice.actions

// Selectors
export const selectIsAuthenticated = (state: { auth: AuthState }) => state.auth.isAuthenticated
export const selectIsInitialized = (state: { auth: AuthState }) => state.auth.isInitialized
export const selectToken = (state: { auth: AuthState }): string | null => state.auth.token
export const selectUser = (state: { auth: AuthState }): User | null => state.auth.user
export const selectRoles = (state: { auth: AuthState }): UserRole[] => state.auth.roles
export const selectIsAdmin = (state: { auth: AuthState }): boolean => state.auth.roles.includes('admin')

export default authSlice.reducer
