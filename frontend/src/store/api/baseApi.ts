import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'

// Use relative URL to go through nginx proxy
export const API_BASE_URL = import.meta.env.VITE_API_URL || '/api/v1'

// Keycloak instance - will be set by App.jsx
let keycloakInstance: any = null

export const setKeycloakInstance = (instance: any) => {
  keycloakInstance = instance
}

// Custom base query with auth token
const baseQuery = fetchBaseQuery({
  baseUrl: API_BASE_URL,
  prepareHeaders: (headers, { getState }) => {
    const token = (getState() as any)?.auth?.token

    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }

    return headers
  },
})

// Base query with re-auth on 401
const baseQueryWithReauth = async (args, api, extraOptions) => {
  let result = await baseQuery(args, api, extraOptions)

  if (result?.error?.status === 401) {
    if (keycloakInstance && keycloakInstance.authenticated) {
      try {
        const refreshed = await keycloakInstance.updateToken(30)
        if (refreshed) {
          api.dispatch({ type: 'auth/setToken', payload: keycloakInstance.token })
          result = await baseQuery(args, api, extraOptions)
        }
      } catch (_error) {
        api.dispatch({ type: 'auth/logout' })
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
  ],
  endpoints: () => ({}),
}) as any

