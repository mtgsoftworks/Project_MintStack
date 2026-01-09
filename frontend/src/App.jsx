import { useEffect } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { useDispatch } from 'react-redux'
import Keycloak from 'keycloak-js'
import { setAuth, setInitialized } from '@/store/slices/authSlice'
import { Layout, ProtectedRoute, LoadingPage } from '@/components/layout'

// Pages
import DashboardPage from '@/pages/DashboardPage'
import NewsPage from '@/pages/NewsPage'
import NewsDetailPage from '@/pages/NewsDetailPage'
import CurrencyPage from '@/pages/CurrencyPage'
import StocksPage from '@/pages/StocksPage'
import StockDetailPage from '@/pages/StockDetailPage'
import BondsPage from '@/pages/BondsPage'
import FundsPage from '@/pages/FundsPage'
import ViopPage from '@/pages/ViopPage'
import PortfolioPage from '@/pages/PortfolioPage'
import PortfolioDetailPage from '@/pages/PortfolioDetailPage'
import AnalysisPage from '@/pages/AnalysisPage'
import ProfilePage from '@/pages/ProfilePage'
import LoginPage from '@/pages/LoginPage'

// Keycloak configuration
const keycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'mintstack',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'mintstack-frontend',
}

// Initialize Keycloak
const keycloak = new Keycloak(keycloakConfig)
window.keycloak = keycloak

function App() {
  const dispatch = useDispatch()

  useEffect(() => {
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

          dispatch(setAuth({
            token: keycloak.token,
            user,
            roles: user.roles,
          }))

          // Set up token refresh
          setInterval(() => {
            keycloak
              .updateToken(30)
              .then((refreshed) => {
                if (refreshed) {
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
  }, [dispatch])

  return (
    <Routes>
      {/* Public route */}
      <Route path="/login" element={<LoginPage />} />

      {/* Protected routes with Layout */}
      <Route element={<Layout />}>
        {/* Dashboard */}
        <Route path="/" element={<DashboardPage />} />

        {/* News */}
        <Route path="/news" element={<NewsPage />} />
        <Route path="/news/:id" element={<NewsDetailPage />} />

        {/* Market Data (public - no auth required) */}
        <Route path="/market/currencies" element={<CurrencyPage />} />
        <Route path="/market/stocks" element={<StocksPage />} />
        <Route path="/market/stocks/:symbol" element={<StockDetailPage />} />
        <Route path="/market/bonds" element={<BondsPage />} />
        <Route path="/market/funds" element={<FundsPage />} />
        <Route path="/market/viop" element={<ViopPage />} />

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
      </Route>

      {/* Catch all */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
