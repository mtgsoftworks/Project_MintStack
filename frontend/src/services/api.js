import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1'

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 second timeout
})

// Store keycloak instance reference
let keycloakInstance = null

// Set keycloak instance
export const setKeycloakInstance = (keycloak) => {
  keycloakInstance = keycloak
}

// Get current token (for debugging)
export const getCurrentToken = () => {
  return keycloakInstance?.token
}

// Request interceptor to add auth token
api.interceptors.request.use(
  async (config) => {
    if (keycloakInstance) {
      // Check if authenticated
      if (keycloakInstance.authenticated) {
        // Try to refresh token if it's about to expire (within 30 seconds)
        try {
          const refreshed = await keycloakInstance.updateToken(30)
          if (refreshed) {
            console.log('Token refreshed successfully')
          }
        } catch (error) {
          console.warn('Token refresh failed, using existing token:', error)
        }
        
        // Add token to request if available
        if (keycloakInstance.token) {
          config.headers.Authorization = `Bearer ${keycloakInstance.token}`
        }
      }
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    
    // Handle 401 errors
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      
      console.warn('Received 401 - attempting token refresh')
      
      if (keycloakInstance && keycloakInstance.authenticated) {
        try {
          // Force token refresh
          await keycloakInstance.updateToken(-1) // Force refresh
          
          // Retry the request with new token
          if (keycloakInstance.token) {
            originalRequest.headers.Authorization = `Bearer ${keycloakInstance.token}`
            return api(originalRequest)
          }
        } catch (refreshError) {
          console.error('Token refresh failed:', refreshError)
          // Redirect to login if refresh fails
          if (!window.location.pathname.includes('/login')) {
            keycloakInstance.login()
          }
        }
      }
    }
    
    return Promise.reject(error)
  }
)

// Legacy function for backwards compatibility
export const setAuthToken = (token) => {
  // Not needed anymore since we use keycloak instance directly
}

export default api
