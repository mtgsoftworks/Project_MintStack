import { createApi, fetchBaseQuery, BaseQueryFn } from '@reduxjs/toolkit/query/react'
import type { RootState } from '../index'

// Use relative URL to go through nginx proxy
export const API_BASE_URL = import.meta.env.VITE_API_URL || '/api/v1'

// Keycloak instance interface
interface KeycloakInstance {
  authenticated?: boolean
  token?: string
  updateToken?: (minValidity: number) => Promise<boolean>
  login?: () => void
  logout?: () => void
}

// Keycloak instance - will be set by App.tsx
let keycloakInstance: KeycloakInstance | null = null

export const setKeycloakInstance = (instance: KeycloakInstance): void => {
  keycloakInstance = instance
}

// Custom base query with auth token
const baseQuery = fetchBaseQuery({
  baseUrl: API_BASE_URL,
  prepareHeaders: (headers, { getState }) => {
    const state = getState() as RootState
    const token = state.auth?.token

    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }

    return headers
  },
})

// Base query with re-auth on 401
const baseQueryWithReauth: BaseQueryFn = async (args, api, extraOptions) => {
  let result = await baseQuery(args, api, extraOptions)

  if (result?.error?.status === 401) {
    if (keycloakInstance && keycloakInstance.authenticated && keycloakInstance.updateToken) {
      try {
        const refreshed = await keycloakInstance.updateToken(30)
        if (refreshed && keycloakInstance.token) {
          api.dispatch({ type: 'auth/setToken', payload: keycloakInstance.token })
          result = await baseQuery(args, api, extraOptions)
        }
      } catch (_error) {
        api.dispatch({ type: 'auth/logout' })
        if (keycloakInstance?.login) {
          keycloakInstance.login()
        }
      }
    }
  }

  return result
}

export const baseApi = createApi({
  reducerPath: 'api',
  baseQuery: baseQueryWithReauth,
  tagTypes: [
    'Currencies',
    'Stocks',
    'Bonds',
    'Funds',
    'Viop',
    'Indices',
    'News',
    'NewsCategories',
    'Portfolios',
    'PortfolioItems',
    'PortfolioTransactions',
    'User',
    'Settings',
    'Simulation',
    'DataSources',
    'Alerts',
    'Watchlists',
    'AdminDashboard',
    'AdminUsers',
    'Glossary',
  ],
  endpoints: () => ({}),
})

// Export type for use in other API definitions
export type BaseApiType = typeof baseApi

