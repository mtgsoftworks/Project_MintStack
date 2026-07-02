import { useNavigate } from 'react-router-dom'
import { ShieldX, Home, LogIn } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { keycloak } from '@/auth/keycloak'

export default function UnauthorizedPage() {
  const navigate = useNavigate()

  const handleGoHome = () => {
    navigate('/')
  }

  const handleLogin = () => {
    keycloak.login()
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-background to-muted p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center space-y-4">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-destructive/10">
            <ShieldX className="h-8 w-8 text-destructive" />
          </div>
          <div>
            <CardTitle className="text-2xl">Erişim Reddedildi</CardTitle>
            <CardDescription className="text-base mt-2">
              Bu sayfaya erişim izniniz bulunmuyor.
            </CardDescription>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <Button
            onClick={handleGoHome}
            className="w-full"
            variant="default"
          >
            <Home className="mr-2 h-4 w-4" />
            Ana Sayfaya Dön
          </Button>
          <Button
            onClick={handleLogin}
            className="w-full"
            variant="outline"
          >
            <LogIn className="mr-2 h-4 w-4" />
            Farklı Hesapla Giriş Yap
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
