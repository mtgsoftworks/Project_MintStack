import { useEffect, useRef } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import type { AppDispatch } from '@/store'
import { setAuth, setInitialized } from '@/store/slices/authSlice'
import { selectTheme } from '@/store/slices/uiSlice'
import websocketService from '@/services/websocketService'
import { keycloak } from '@/auth/keycloak'
import { AppRoutes } from '@/routes/AppRoutes'

function App() {
  const dispatch = useDispatch<AppDispatch>()
  const theme = useSelector(selectTheme)
  const tokenRefreshIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // Apply theme class to document root
  useEffect(() => {
    const root = document.documentElement
    root.classList.remove('light', 'dark')
    root.classList.add(theme)
  }, [theme])

  // NOTE: Market data refresh is handled by backend schedulers (MarketDataScheduler).
  // Client-side refresh removed to prevent DoS via multiple open tabs.
  // WebSocket price updates are sufficient for real-time data.

  useEffect(() => {
    if (import.meta.env.DEV && import.meta.env.VITE_E2E_BYPASS_AUTH === 'true') {
      const e2eUser = {
        id: 'e2e-user',
        username: 'e2e-user',
        name: 'E2E User',
        email: 'e2e@mintstack.local',
        roles: ['user', 'admin'],
      }

      dispatch(setAuth({
        token: 'e2e-bypass-token',
        user: e2eUser,
        roles: e2eUser.roles,
      }))
      websocketService.setAuthToken('e2e-bypass-token')

      return () => {
        if (tokenRefreshIntervalRef.current) {
          clearInterval(tokenRefreshIntervalRef.current)
        }
      }
    }

    // Initialize Keycloak
    keycloak
      .init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
        pkceMethod: 'S256',
      })
      .then((authenticated) => {
        if (authenticated) {
          // User is authenticated
          const user = {
            id: keycloak.subject,
            username: keycloak.tokenParsed?.preferred_username,
            name: keycloak.tokenParsed?.name,
            email: keycloak.tokenParsed?.email,
            roles: keycloak.tokenParsed?.realm_access?.roles || [],
          }

          const token = keycloak.token ?? null
          dispatch(setAuth({
            token,
            user,
            roles: user.roles,
          }))
          websocketService.setAuthToken(token)

          // Connect WebSocket for real-time price updates
          try {
            websocketService.connect({ token: keycloak.token })
          } catch (error) {
            console.warn('WebSocket connection failed:', error)
          }

          // Set up token refresh
          tokenRefreshIntervalRef.current = setInterval(() => {
            keycloak
              .updateToken(30)
              .then((refreshed) => {
                if (refreshed) {
                  const newToken = keycloak.token ?? null
                  websocketService.setAuthToken(newToken)
                  dispatch(setAuth({
                    token: newToken,
                    user,
                    roles: user.roles,
                  }))
                }
              })
              .catch(() => {
                console.error('Token refresh failed')
              })
          }, 60000) // Check every minute
        }

        dispatch(setInitialized(true))
      })
      .catch((error) => {
        console.error('Keycloak init failed:', error)
        dispatch(setInitialized(true))
      })

    // Cleanup on unmount
    return () => {
      if (tokenRefreshIntervalRef.current) {
        clearInterval(tokenRefreshIntervalRef.current)
      }
      websocketService.disconnect()
    }
  }, [dispatch])

  return <AppRoutes />
}

export default App
