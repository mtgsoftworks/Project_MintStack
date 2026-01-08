import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Layout from './components/common/Layout'
import ProtectedRoute from './components/common/ProtectedRoute'
import Loading from './components/common/Loading'

// Pages
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import NewsPage from './pages/NewsPage'
import NewsDetailPage from './pages/NewsDetailPage'
import CurrencyPage from './pages/CurrencyPage'
import StocksPage from './pages/StocksPage'
import StockDetailPage from './pages/StockDetailPage'
import BondsPage from './pages/BondsPage'
import FundsPage from './pages/FundsPage'
import ViopPage from './pages/ViopPage'
import PortfolioPage from './pages/PortfolioPage'
import PortfolioDetailPage from './pages/PortfolioDetailPage'
import AnalysisPage from './pages/AnalysisPage'
import ProfilePage from './pages/ProfilePage'

function App() {
  const { isLoading, isAuthenticated } = useAuth()

  if (isLoading) {
    return <Loading fullScreen text="Yükleniyor..." />
  }

  return (
    <Routes>
      {/* Public Routes */}
      <Route 
        path="/login" 
        element={isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />} 
      />
      
      {/* Protected Routes with Layout */}
      <Route element={<Layout />}>
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          }
        />
        
        {/* News - Public but with layout */}
        <Route path="/news" element={<NewsPage />} />
        <Route path="/news/:id" element={<NewsDetailPage />} />
        
        {/* Market Data - Public but with layout */}
        <Route path="/market/currencies" element={<CurrencyPage />} />
        <Route path="/market/stocks" element={<StocksPage />} />
        <Route path="/market/stocks/:symbol" element={<StockDetailPage />} />
        <Route path="/market/bonds" element={<BondsPage />} />
        <Route path="/market/funds" element={<FundsPage />} />
        <Route path="/market/viop" element={<ViopPage />} />
        
        {/* Portfolio - Protected */}
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
        
        {/* Analysis - Protected */}
        <Route
          path="/analysis"
          element={
            <ProtectedRoute>
              <AnalysisPage />
            </ProtectedRoute>
          }
        />
        
        {/* Profile - Protected */}
        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          }
        />
      </Route>
      
      {/* Catch all - redirect to home */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
