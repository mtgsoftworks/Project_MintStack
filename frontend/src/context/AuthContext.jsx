import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react'
import Keycloak from 'keycloak-js'
import { setKeycloakInstance } from '../services/api'

const AuthContext = createContext(null)

const keycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'mintstack-finance',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'finance-frontend',
}

// Create keycloak instance once
const keycloak = new Keycloak(keycloakConfig)

export function AuthProvider({ children }) {
  const [isLoading, setIsLoading] = useState(true)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(null)
  const initStarted = useRef(false)

  useEffect(() => {
    // Prevent double initialization in strict mode
    if (initStarted.current) return
    initStarted.current = true

    const initKeycloak = async () => {
      try {
        // Set keycloak instance for API calls BEFORE init
        setKeycloakInstance(keycloak)
        
        const authenticated = await keycloak.init({
          onLoad: 'check-sso',
          silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
          pkceMethod: 'S256',
          checkLoginIframe: false,
        })

        console.log('Keycloak initialized, authenticated:', authenticated)
        
        setIsAuthenticated(authenticated)

        if (authenticated) {
          console.log('User authenticated, token available:', !!keycloak.token)
          setToken(keycloak.token)
          
          setUser({
            id: keycloak.subject,
            email: keycloak.tokenParsed?.email,
            firstName: keycloak.tokenParsed?.given_name,
            lastName: keycloak.tokenParsed?.family_name,
            fullName: keycloak.tokenParsed?.name,
            roles: keycloak.tokenParsed?.realm_access?.roles || [],
          })

          // Set up token refresh every 30 seconds
          const refreshInterval = setInterval(async () => {
            try {
              const refreshed = await keycloak.updateToken(70)
              if (refreshed) {
                console.log('Token auto-refreshed')
                setToken(keycloak.token)
              }
            } catch (error) {
              console.error('Failed to refresh token:', error)
              clearInterval(refreshInterval)
            }
          }, 30000)

          // Cleanup interval on unmount
          return () => clearInterval(refreshInterval)
        }
      } catch (error) {
        console.error('Keycloak init error:', error)
      } finally {
        setIsLoading(false)
      }
    }

    initKeycloak()
  }, [])

  const login = useCallback(() => {
    keycloak.login()
  }, [])

  const logout = useCallback(() => {
    keycloak.logout({ redirectUri: window.location.origin })
  }, [])

  const hasRole = useCallback((role) => {
    return user?.roles?.includes(role) || false
  }, [user])

  // Get fresh token
  const getToken = useCallback(async () => {
    if (keycloak.authenticated) {
      try {
        await keycloak.updateToken(30)
        return keycloak.token
      } catch (error) {
        console.error('Failed to get fresh token:', error)
        return null
      }
    }
    return null
  }, [])

  const value = {
    isLoading,
    isAuthenticated,
    user,
    token,
    login,
    logout,
    hasRole,
    getToken,
    keycloak,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
