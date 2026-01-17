import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'

const API_BASE_URL = import.meta.env.VITE_API_URL || import.meta.env.REACT_APP_API_URL || 'http://localhost:18080/api/v1'

// Custom base query with auth token
const baseQuery = fetchBaseQuery({
  baseUrl: API_BASE_URL,
  prepareHeaders: (headers, { getState }) => {
    // Get token from auth state or keycloak
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
    // Try to refresh token via keycloak
    const keycloak = window.keycloak
    if (keycloak && keycloak.authenticated) {
      try {
        const refreshed = await keycloak.updateToken(30)
        if (refreshed) {
          // Retry with new token
          api.dispatch({ type: 'auth/setToken', payload: keycloak.token })
          result = await baseQuery(args, api, extraOptions)
        }
      } catch (error) {
        // Token refresh failed, logout
        api.dispatch({ type: 'auth/logout' })
      }
    }
  }

  return result
}

// Create the base API
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
  ],
  endpoints: () => ({}),
})
