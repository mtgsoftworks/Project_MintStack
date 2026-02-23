import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'

// Use relative URL to go through nginx proxy
export const API_BASE_URL = import.meta.env.VITE_API_URL || '/api/v1'

// Keycloak instance - will be set by App.jsx
let keycloakInstance = null

export const setKeycloakInstance = (instance) => {
  keycloakInstance = instance
}

// Custom base query with auth token
const baseQuery = fetchBaseQuery({
  baseUrl: API_BASE_URL,
  prepareHeaders: (headers, { getState }) => {
    const token = getState().auth?.token

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
      } catch (error) {
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
    'News',
    'NewsCategories',
    'Portfolios',
    'PortfolioItems',
    'PortfolioTransactions',
    'User',
    'Settings',
    'Simulation',
  ],
  endpoints: () => ({}),
})
