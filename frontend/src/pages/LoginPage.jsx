import { useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { Loader2, TrendingUp } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { selectIsAuthenticated, selectIsInitialized } from '@/store/slices/authSlice'

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const isAuthenticated = useSelector(selectIsAuthenticated)
  const isInitialized = useSelector(selectIsInitialized)

  const from = location.state?.from?.pathname || '/'

  useEffect(() => {
    if (isAuthenticated) {
      navigate(from, { replace: true })
    }
  }, [isAuthenticated, navigate, from])

  const handleLogin = () => {
    if (window.keycloak) {
      window.keycloak.login({
        redirectUri: window.location.origin + from,
      })
    }
  }

  if (!isInitialized) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <p className="text-muted-foreground">Yükleniyor...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-background to-muted p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center space-y-4">
          {/* Logo */}
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-primary to-primary-600">
            <TrendingUp className="h-8 w-8 text-white" />
          </div>
          <div>
            <CardTitle className="text-2xl">MintStack Finance</CardTitle>
            <CardDescription className="text-base mt-2">
              Finansal verilerinizi yönetin, piyasaları takip edin
            </CardDescription>
          </div>
        </CardHeader>
        <CardContent className="space-y-6">
          <Button
            onClick={handleLogin}
            className="w-full h-12 text-base"
            size="lg"
          >
            Giriş Yap
          </Button>

          <p className="text-center text-xs text-muted-foreground">
            Giriş yaparak{' '}
            <a href="#" className="underline hover:text-primary">
              Kullanım Şartları
            </a>{' '}
            ve{' '}
            <a href="#" className="underline hover:text-primary">
              Gizlilik Politikası
            </a>
            &apos;nı kabul etmiş olursunuz.
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
