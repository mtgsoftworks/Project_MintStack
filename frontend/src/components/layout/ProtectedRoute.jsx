import { Navigate, useLocation } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { selectIsAuthenticated, selectIsInitialized, selectRoles } from '@/store/slices/authSlice'
import { Loading } from './Loading'

export function ProtectedRoute({ children, requiredRoles = [] }) {
  const location = useLocation()
  const isAuthenticated = useSelector(selectIsAuthenticated)
  const isInitialized = useSelector(selectIsInitialized)
  const userRoles = useSelector(selectRoles)

  // Wait for auth initialization
  if (!isInitialized) {
    return <Loading fullScreen text="Oturum kontrol ediliyor..." />
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
