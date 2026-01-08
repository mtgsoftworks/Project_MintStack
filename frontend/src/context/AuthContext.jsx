import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import Keycloak from 'keycloak-js'

const AuthContext = createContext(null)

const keycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'mintstack-finance',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'finance-frontend',
}

const keycloak = new Keycloak(keycloakConfig)

export function AuthProvider({ children }) {
  const [isLoading, setIsLoading] = useState(true)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(null)

  useEffect(() => {
    const initKeycloak = async () => {
      try {
        const authenticated = await keycloak.init({
          onLoad: 'check-sso',
          silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
          pkceMethod: 'S256',
          checkLoginIframe: false,
        })

        setIsAuthenticated(authenticated)

        if (authenticated) {
          setToken(keycloak.token)
          setUser({
            id: keycloak.subject,
            email: keycloak.tokenParsed?.email,
            firstName: keycloak.tokenParsed?.given_name,
            lastName: keycloak.tokenParsed?.family_name,
            fullName: keycloak.tokenParsed?.name,
            roles: keycloak.tokenParsed?.realm_access?.roles || [],
          })

          // Set up token refresh
          setInterval(() => {
            keycloak.updateToken(70).then((refreshed) => {
              if (refreshed) {
                setToken(keycloak.token)
              }
            }).catch(() => {
              console.error('Failed to refresh token')
            })
          }, 60000)
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

  const value = {
    isLoading,
    isAuthenticated,
    user,
    token,
    login,
    logout,
    hasRole,
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
