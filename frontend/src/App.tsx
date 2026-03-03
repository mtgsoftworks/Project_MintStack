/* eslint-disable react-refresh/only-export-components */
import { useEffect, useRef, useCallback, Suspense, lazy } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import Keycloak from 'keycloak-js'
import { setAuth, setInitialized } from '@/store/slices/authSlice'
import { setKeycloakInstance } from '@/services/api'
import { setKeycloakInstance as setApiKeycloakInstance } from '@/store/api/baseApi'
import { selectAutoUpdate, selectRefreshRate, selectTheme } from '@/store/slices/uiSlice'
import { AdminRoute, Layout, ProtectedRoute } from '@/components/layout'
import websocketService from '@/services/websocketService'

// Lazy-loaded Pages (code splitting)
const DashboardPage = lazy(() => import('@/pages/DashboardPage'))
const NewsPage = lazy(() => import('@/pages/NewsPage'))
const NewsDetailPage = lazy(() => import('@/pages/NewsDetailPage'))
const CurrencyPage = lazy(() => import('@/pages/CurrencyPage'))
const StocksPage = lazy(() => import('@/pages/StocksPage'))
const StockDetailPage = lazy(() => import('@/pages/StockDetailPage'))
const BondsPage = lazy(() => import('@/pages/BondsPage'))
const FundsPage = lazy(() => import('@/pages/FundsPage'))
const ViopPage = lazy(() => import('@/pages/ViopPage'))
const PortfolioPage = lazy(() => import('@/pages/PortfolioPage'))
const PortfolioDetailPage = lazy(() => import('@/pages/PortfolioDetailPage'))
const AnalysisPage = lazy(() => import('@/pages/AnalysisPage'))
const ProfilePage = lazy(() => import('@/pages/ProfilePage'))
const SettingsPage = lazy(() => import('@/pages/SettingsPage'))
const WatchlistPage = lazy(() => import('@/pages/WatchlistPage'))
const AlertsPage = lazy(() => import('@/pages/AlertsPage'))
const AdminDashboard = lazy(() => import('@/pages/AdminDashboard'))
const NotificationsPage = lazy(() => import('@/pages/NotificationsPage'))
const LoginPage = lazy(() => import('@/pages/LoginPage'))
const UnauthorizedPage = lazy(() => import('@/pages/UnauthorizedPage'))

// Loading fallback component
const PageLoader = () => (
  <div className="flex items-center justify-center min-h-[60vh]">
    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
  </div>
)

// Keycloak configuration
const keycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'mintstack-finance',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'finance-frontend',
}

const keycloak = new Keycloak(keycloakConfig)
setKeycloakInstance(keycloak)
setApiKeycloakInstance(keycloak)

export { keycloak }

function App() {
  const dispatch = useDispatch()
  const autoUpdate = useSelector(selectAutoUpdate)
  const refreshRate = useSelector(selectRefreshRate)
  const theme = useSelector(selectTheme)
  const dataRefreshIntervalRef = useRef(null)
  const tokenRefreshIntervalRef = useRef(null)

  // Apply theme class to document root
  useEffect(() => {
    const root = document.documentElement
    root.classList.remove('light', 'dark')
    root.classList.add(theme)
  }, [theme])

  // Function to trigger data refresh via WebSocket
  const triggerDataRefresh = useCallback(() => {
    if (websocketService.isConnected) {
      websocketService.requestPriceUpdate()
    }
  }, [])

  // Setup data refresh interval based on user settings
  useEffect(() => {
    // Clear existing interval
    if (dataRefreshIntervalRef.current) {
      clearInterval(dataRefreshIntervalRef.current)
      dataRefreshIntervalRef.current = null
    }

    // Only set up interval if auto-update is enabled
    if (autoUpdate && refreshRate > 0) {
      console.log(`[App] Auto-update enabled with ${refreshRate}s refresh rate`)
      dataRefreshIntervalRef.current = setInterval(() => {
        console.log('[App] Triggering scheduled data refresh')
        triggerDataRefresh()
      }, refreshRate * 1000)
    } else {
      console.log('[App] Auto-update disabled')
    }

    return () => {
      if (dataRefreshIntervalRef.current) {
        clearInterval(dataRefreshIntervalRef.current)
      }
    }
  }, [autoUpdate, refreshRate, triggerDataRefresh])

  useEffect(() => {
    if (import.meta.env.VITE_E2E_BYPASS_AUTH === 'true') {
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

      window.keycloak = {
        authenticated: true,
        logout: () => {
          window.location.href = '/login'
        },
      }

      return () => {
        if (tokenRefreshIntervalRef.current) {
          clearInterval(tokenRefreshIntervalRef.current)
        }
      }
    }

    // Initialize Keycloak
    keycloak
      .init({
        onLoad: 'login-required',
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

          dispatch(setAuth({
            token: keycloak.token,
            user,
            roles: user.roles,
          }))
          websocketService.setAuthToken(keycloak.token)

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
                  websocketService.setAuthToken(keycloak.token)
                  dispatch(setAuth({
                    token: keycloak.token,
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

  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />

        {/* Protected routes with Layout */}
        <Route element={<Layout />}>
          {/* Dashboard */}
          <Route path="/" element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          } />

          {/* News */}
          <Route path="/news" element={
            <ProtectedRoute>
              <NewsPage />
            </ProtectedRoute>
          } />
          <Route path="/news/:id" element={
            <ProtectedRoute>
              <NewsDetailPage />
            </ProtectedRoute>
          } />

          {/* Market Data */}
          <Route path="/market/currencies" element={
            <ProtectedRoute>
              <CurrencyPage />
            </ProtectedRoute>
          } />
          <Route path="/market/stocks" element={
            <ProtectedRoute>
              <StocksPage />
            </ProtectedRoute>
          } />
          <Route path="/market/stocks/:symbol" element={
            <ProtectedRoute>
              <StockDetailPage />
            </ProtectedRoute>
          } />
          <Route path="/market/bonds" element={
            <ProtectedRoute>
              <BondsPage />
            </ProtectedRoute>
          } />
          <Route path="/market/funds" element={
            <ProtectedRoute>
              <FundsPage />
            </ProtectedRoute>
          } />
          <Route path="/market/viop" element={
            <ProtectedRoute>
              <ViopPage />
            </ProtectedRoute>
          } />

          {/* Protected routes */}
          <Route
            path="/portfolio"
            element={
              <ProtectedRoute>
                <PortfolioPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/portfolio/:id"
            element={
              <ProtectedRoute>
                <PortfolioDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/analysis"
            element={
              <ProtectedRoute>
                <AnalysisPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/settings"
            element={
              <ProtectedRoute>
                <SettingsPage />
              </ProtectedRoute>
            }
          />

          {/* Watchlist */}
          <Route
            path="/watchlist"
            element={
              <ProtectedRoute>
                <WatchlistPage />
              </ProtectedRoute>
            }
          />

          {/* Alerts */}
          <Route
            path="/alerts"
            element={
              <ProtectedRoute>
                <AlertsPage />
              </ProtectedRoute>
            }
          />

          {/* Notifications */}
          <Route
            path="/notifications"
            element={
              <ProtectedRoute>
                <NotificationsPage />
              </ProtectedRoute>
            }
          />

          {/* Admin */}
          <Route
            path="/admin"
            element={
              <AdminRoute>
                <AdminDashboard />
              </AdminRoute>
            }
          />
        </Route>

        {/* Catch all */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  )
}

export default App
