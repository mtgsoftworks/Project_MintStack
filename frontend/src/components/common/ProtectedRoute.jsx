import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import Loading from './Loading'

export default function ProtectedRoute({ children, requiredRole }) {
  const { isAuthenticated, isLoading, hasRole, login } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return <Loading fullScreen text="Oturum kontrol ediliyor..." />
  }

  if (!isAuthenticated) {
    // Redirect to login with return url
    login()
    return <Loading fullScreen text="Giriş sayfasına yönlendiriliyorsunuz..." />
  }

  if (requiredRole && !hasRole(requiredRole)) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh]">
        <div className="card p-8 text-center">
          <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-red-500/10 flex items-center justify-center">
            <svg className="w-8 h-8 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <h2 className="text-xl font-semibold text-white mb-2">Erişim Reddedildi</h2>
          <p className="text-dark-400 mb-4">Bu sayfaya erişim yetkiniz bulunmamaktadır.</p>
          <button
            onClick={() => window.history.back()}
            className="btn-secondary"
          >
            Geri Dön
          </button>
        </div>
      </div>
    )
  }

  return children
}
