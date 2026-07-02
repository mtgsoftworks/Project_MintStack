import { Suspense, lazy } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AdminRoute, Layout, ProtectedRoute } from '@/components/layout'

const DashboardPage = lazy(() => import('@/pages/DashboardPage'))
const NewsPage = lazy(() => import('@/pages/NewsPage'))
const NewsDetailPage = lazy(() => import('@/pages/NewsDetailPage'))
const CurrencyPage = lazy(() => import('@/pages/CurrencyPage'))
const StocksPage = lazy(() => import('@/pages/StocksPage'))
const StockDetailPage = lazy(() => import('@/pages/StockDetailPage'))
const MarketSearchPage = lazy(() => import('@/pages/MarketSearchPage'))
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

const PageLoader = () => (
  <div className="flex min-h-[60vh] items-center justify-center">
    <div className="h-12 w-12 animate-spin rounded-full border-b-2 border-primary" />
  </div>
)

const protectedPage = (page: React.ReactNode) => <ProtectedRoute>{page}</ProtectedRoute>

export function AppRoutes() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />

        <Route element={<Layout />}>
          <Route path="/" element={protectedPage(<DashboardPage />)} />
          <Route path="/news" element={protectedPage(<NewsPage />)} />
          <Route path="/news/:id" element={protectedPage(<NewsDetailPage />)} />
          <Route path="/market/currencies" element={protectedPage(<CurrencyPage />)} />
          <Route path="/market/stocks" element={protectedPage(<StocksPage />)} />
          <Route path="/market/stocks/:symbol" element={protectedPage(<StockDetailPage />)} />
          <Route path="/market/search" element={protectedPage(<MarketSearchPage />)} />
          <Route path="/market/bonds" element={protectedPage(<BondsPage />)} />
          <Route path="/market/funds" element={protectedPage(<FundsPage />)} />
          <Route path="/market/viop" element={protectedPage(<ViopPage />)} />
          <Route path="/portfolio" element={protectedPage(<PortfolioPage />)} />
          <Route path="/portfolio/:id" element={protectedPage(<PortfolioDetailPage />)} />
          <Route path="/analysis" element={protectedPage(<AnalysisPage />)} />
          <Route path="/profile" element={protectedPage(<ProfilePage />)} />
          <Route path="/settings" element={protectedPage(<SettingsPage />)} />
          <Route path="/watchlist" element={protectedPage(<WatchlistPage />)} />
          <Route path="/alerts" element={protectedPage(<AlertsPage />)} />
          <Route path="/notifications" element={protectedPage(<NotificationsPage />)} />
          <Route
            path="/admin"
            element={(
              <AdminRoute>
                <AdminDashboard />
              </AdminRoute>
            )}
          />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  )
}
