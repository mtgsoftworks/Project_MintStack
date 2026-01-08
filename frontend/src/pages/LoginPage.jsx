import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { ChartBarIcon, ShieldCheckIcon, BoltIcon, DevicePhoneMobileIcon } from '@heroicons/react/24/outline'

export default function LoginPage() {
  const { login, keycloak } = useAuth()
  const [rememberMe, setRememberMe] = useState(false)

  const handleLogin = () => {
    // Pass rememberMe option to Keycloak login
    login({ 
      prompt: 'login',
      // Keycloak'ta "Remember Me" için
      loginHint: rememberMe ? 'remember' : undefined
    })
  }

  const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180'
  const keycloakRealm = import.meta.env.VITE_KEYCLOAK_REALM || 'mintstack-finance'
  const twoFactorUrl = `${keycloakUrl}/realms/${keycloakRealm}/account/totp`

  const features = [
    {
      icon: ChartBarIcon,
      title: 'Gerçek Zamanlı Veriler',
      description: 'TCMB, BIST ve global piyasa verileri',
    },
    {
      icon: ShieldCheckIcon,
      title: 'Güvenli Kimlik Doğrulama',
      description: '2FA ve Keycloak ile güvenli giriş',
    },
    {
      icon: BoltIcon,
      title: 'Hızlı Analiz',
      description: 'Teknik göstergeler ve karşılaştırma araçları',
    },
  ]

  return (
    <div className="min-h-screen flex">
      {/* Left side - Branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-primary-900 via-dark-900 to-dark-950 p-12 flex-col justify-between">
        <div>
          <div className="flex items-center gap-3 mb-12">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center">
              <span className="text-white font-bold text-2xl">M</span>
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white">MintStack</h1>
              <p className="text-sm text-primary-300">Finance Portal</p>
            </div>
          </div>

          <h2 className="text-4xl font-bold text-white mb-4">
            Finansal Piyasaları<br />
            <span className="text-gradient">Keşfedin</span>
          </h2>
          <p className="text-dark-300 text-lg max-w-md">
            Döviz kurları, hisse senetleri, tahviller ve daha fazlasını tek platformda takip edin.
          </p>
        </div>

        <div className="space-y-6">
          {features.map((feature) => (
            <div key={feature.title} className="flex items-start gap-4">
              <div className="w-10 h-10 rounded-lg bg-primary-500/10 flex items-center justify-center flex-shrink-0">
                <feature.icon className="w-5 h-5 text-primary-400" />
              </div>
              <div>
                <h3 className="text-white font-medium">{feature.title}</h3>
                <p className="text-dark-400 text-sm">{feature.description}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Right side - Login */}
      <div className="flex-1 flex items-center justify-center p-8 bg-dark-950">
        <div className="w-full max-w-md">
          {/* Mobile logo */}
          <div className="lg:hidden flex items-center justify-center gap-3 mb-8">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center">
              <span className="text-white font-bold text-2xl">M</span>
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white">MintStack</h1>
              <p className="text-sm text-dark-400">Finance Portal</p>
            </div>
          </div>

          <div className="card p-8">
            <div className="text-center mb-8">
              <h2 className="text-2xl font-bold text-white mb-2">Hoş Geldiniz</h2>
              <p className="text-dark-400">Devam etmek için giriş yapın</p>
            </div>

            {/* Remember Me Checkbox */}
            <label className="flex items-center gap-3 mb-6 cursor-pointer group">
              <div className="relative">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="sr-only peer"
                />
                <div className="w-5 h-5 rounded border-2 border-dark-600 bg-dark-800 
                              peer-checked:bg-primary-600 peer-checked:border-primary-600
                              peer-focus:ring-2 peer-focus:ring-primary-500/50
                              transition-all duration-200">
                  <svg
                    className={`w-3 h-3 text-white absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 
                               transition-opacity ${rememberMe ? 'opacity-100' : 'opacity-0'}`}
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    strokeWidth={3}
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                  </svg>
                </div>
              </div>
              <span className="text-dark-300 text-sm group-hover:text-dark-200 transition-colors">
                Beni hatırla
              </span>
            </label>

            <button
              onClick={handleLogin}
              className="w-full btn-primary py-3 text-lg font-semibold mb-4"
            >
              Keycloak ile Giriş Yap
            </button>

            {/* 2FA Setup Link */}
            <a
              href={twoFactorUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="w-full flex items-center justify-center gap-2 py-3 px-4 mb-6
                        border border-dark-700 rounded-lg text-dark-300 
                        hover:border-primary-500/50 hover:text-primary-400 hover:bg-dark-800/50
                        transition-all duration-200"
            >
              <DevicePhoneMobileIcon className="w-5 h-5" />
              <span className="text-sm font-medium">2FA Aktive Et</span>
            </a>

            <div className="relative mb-6">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-dark-700" />
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-dark-900 text-dark-500">veya</span>
              </div>
            </div>

            <div className="text-center">
              <p className="text-dark-400 text-sm">
                Hesabınız yok mu?{' '}
                <button onClick={handleLogin} className="text-primary-400 hover:text-primary-300">
                  Kayıt olun
                </button>
              </p>
            </div>
          </div>

          <p className="text-center text-dark-500 text-xs mt-6">
            Giriş yaparak{' '}
            <a href="#" className="text-primary-400 hover:underline">Kullanım Şartları</a>
            {' '}ve{' '}
            <a href="#" className="text-primary-400 hover:underline">Gizlilik Politikası</a>
            'nı kabul etmiş olursunuz.
          </p>
        </div>
      </div>
    </div>
  )
}
