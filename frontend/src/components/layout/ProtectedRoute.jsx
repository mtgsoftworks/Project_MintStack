import { Navigate, useLocation } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { selectIsAuthenticated, selectIsInitialized, selectRoles } from '@/store/slices/authSlice'
import { Loading } from './Loading'
import { useTranslation } from 'react-i18next'

export function ProtectedRoute({ children, requiredRoles = [] }) {
  const location = useLocation()
  const isAuthenticated = useSelector(selectIsAuthenticated)
  const isInitialized = useSelector(selectIsInitialized)
  const userRoles = useSelector(selectRoles)
  const { t } = useTranslation()

  // Wait for auth initialization
  if (!isInitialized) {
    return <Loading fullScreen text={t('common.sessionCheck')} />
  }

  // Check if user is authenticated
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  // Check for required roles
  if (requiredRoles.length > 0) {
    const hasRequiredRole = requiredRoles.some(role => userRoles.includes(role))
    if (!hasRequiredRole) {
      return <Navigate to="/unauthorized" replace />
    }
  }

  return children
}

export function AdminRoute({ children }) {
  return (
    <ProtectedRoute requiredRoles={['admin']}>
      {children}
    </ProtectedRoute>
  )
}

export default ProtectedRoute
