import '@testing-library/jest-dom'
import React from 'react'
import { afterAll, afterEach, beforeAll, vi } from 'vitest'
import { cleanup } from '@testing-library/react'
import { server } from './mocks/server'

// Mock matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
})

// Mock ResizeObserver
window.ResizeObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}))

// Mock IntersectionObserver
window.IntersectionObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}))

// Mock scrollTo
window.scrollTo = vi.fn()

// Mock Keycloak
vi.mock('keycloak-js', () => {
  return {
    default: vi.fn().mockImplementation(() => ({
      init: vi.fn().mockResolvedValue(true),
      login: vi.fn(),
      logout: vi.fn(),
      token: 'mock-token',
      tokenParsed: {
        sub: 'mock-user-id',
        email: 'test@example.com',
        given_name: 'Test',
        family_name: 'User',
        name: 'Test User',
        realm_access: {
          roles: ['user'],
        },
      },
      subject: 'mock-user-id',
      updateToken: vi.fn().mockResolvedValue(true),
      authenticated: true,
    })),
  }
})

// Mock keycloak export from App
vi.mock('@/App', () => ({
  keycloak: {
    init: vi.fn().mockResolvedValue(true),
    login: vi.fn(),
    logout: vi.fn(),
    token: 'mock-token',
    tokenParsed: {
      sub: 'mock-user-id',
      email: 'test@example.com',
      given_name: 'Test',
      family_name: 'User',
      name: 'Test User',
      realm_access: {
        roles: ['user'],
      },
    },
    subject: 'mock-user-id',
    updateToken: vi.fn().mockResolvedValue(true),
    authenticated: true,
  },
}))

// Mock react-i18next with comprehensive translations
const translations = {
  'nav.home': 'Dashboard',
  'nav.portfolio': 'Portföyler',
  'dashboard.subtitle': 'Dashboard Subtitle',
  'dashboard.widgets.bist100.title': 'BIST 100',
  'dashboard.widgets.bist100.noProvider': '-',
  'dashboard.widgets.bist100.error': 'Error',
  'dashboard.widgets.currencies.title': 'Döviz Kurları',
  'dashboard.widgets.news.title': 'Son Haberler',
  'dashboard.widgets.news.viewAll': 'Tümünü Gör',
  'dashboard.widgets.portfolio.title': 'Portföy Değeri',
  'dashboard.widgets.portfolio.totalPL': 'Toplam K/Z',
  'portfolioPage.title': 'Portföyler',
  'portfolioPage.subtitle': 'Portföylerinizi yönetin',
  'portfolioPage.newPortfolio': 'Yeni Portföy',
  'portfolioPage.summary.totalValue': 'Toplam Değer',
  'portfolioPage.summary.totalPL': 'Toplam K/Z',
  'portfolioPage.summary.portfolioCount': 'Portföy Sayısı',
  'portfolioPage.card.items': '1 varlık',
  'portfolioPage.dialog.title': 'Yeni Portföy',
  'portfolioPage.dialog.description': 'Portföy bilgilerini girin',
  'portfolioPage.dialog.nameLabel': 'Portföy Adı',
  'portfolioPage.dialog.namePlaceholder': 'Portföy adı girin',
  'portfolioPage.dialog.descriptionLabel': 'Açıklama',
  'portfolioPage.dialog.descriptionPlaceholder': 'Açıklama girin',
  'portfolioPage.dialog.create': 'Oluştur',
  'portfolioPage.dialog.cancel': 'İptal',
  'portfolioPage.empty.title': 'Portföy Yok',
  'portfolioPage.empty.description': 'Henüz portföyünüz yok',
  'common.loading': 'Yükleniyor...',
  'profile.title': 'Profil',
  'profile.subtitle': 'Hesap bilgilerinizi ve tercihlerinizi yönetin',
  'profile.changePassword': 'Şifre Değiştir',
  'profile.securitySettings': 'Güvenlik Ayarları',
  'profile.securityAdminOnly': 'Güvenlik ayarları için Keycloak yönetim panelini kullanın.',
  'profile.tabs.general': 'Genel',
  'profile.tabs.notifications': 'Bildirimler',
  'profile.tabs.preferences': 'Tercihler',
  'profile.username': 'Kullanıcı adı',
  'profile.usernameReadonly': 'Kullanıcı adı değiştirilemez',
  'profile.firstName': 'Ad',
  'profile.firstNamePlaceholder': 'Adınız',
  'profile.lastName': 'Soyad',
  'profile.lastNamePlaceholder': 'Soyadınız',
  'profile.email': 'E-posta',
  'profile.emailKeycloakHint': 'E-posta Keycloak hesabından yönetilir',
  'profile.phone': 'Telefon',
  'profile.location': 'Konum',
  'profile.locationPlaceholder': 'Şehir, Ülke',
  'profile.bio': 'Biyografi',
  'profile.bioPlaceholder': 'Kendiniz hakkında kısa bir bilgi yazın',
  'profile.saveChanges': 'Değişiklikleri Kaydet',
  'profile.saving': 'Kaydediliyor...',
  'profile.emailNotifications': 'E-posta Bildirimleri',
  'profile.emailNotificationsDesc': 'Önemli hesap ve piyasa bildirimlerini e-posta ile alın',
  'profile.pushNotifications': 'Anlık Bildirimler',
  'profile.pushNotificationsDesc': 'Tarayıcı üzerinden anlık bildirim alın',
  'profile.priceAlerts': 'Fiyat Alarmları',
  'profile.priceAlertsDesc': 'Fiyat hedefleri gerçekleştiğinde bildirim alın',
  'profile.portfolioUpdates': 'Portföy Güncellemeleri',
  'profile.portfolioUpdatesDesc': 'Portföy performansı değişikliklerini takip edin',
  'profile.compactView': 'Kompakt Görünüm',
  'profile.compactViewDesc': 'Tablo ve kartlarda daha yoğun görünüm kullanın',
  'profile.darkMode': 'Koyu mod',
  'profile.darkModeDesc': 'Koyu tema kullan',
  'profile.defaultCurrency': 'Varsayılan Para Birimi',
  'profile.defaultCurrencyDesc': 'Fiyatlar için varsayılan para birimi',
  'common.error': 'Hata',
  'common.save': 'Kaydet',
  'common.cancel': 'İptal',
  'common.delete': 'Sil',
  'common.edit': 'Düzenle',
}

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key, options) => {
      if (translations[key]) return translations[key]
      if (options?.returnObjects) return []
      return key
    },
    i18n: {
      changeLanguage: () => new Promise(() => { }),
      language: 'tr',
    },
  }),
  initReactI18next: {
    type: '3rdParty',
    init: () => { },
  },
  Trans: ({ children }) => children,
  withTranslation: () => (Component) => {
    const WrappedComponent = (props) => {
      const t = (key) => translations[key] || key
      return React.createElement(Component, { ...props, t })
    }
    WrappedComponent.displayName = `withTranslation(${Component.displayName || Component.name || 'Component'})`
    return WrappedComponent
  },
}))

// Start MSW server before all tests
beforeAll(() => {
  server.listen({ onUnhandledRequest: 'warn' })
})

// Reset handlers after each test
afterEach(() => {
  cleanup()
  server.resetHandlers()
})

// Close server after all tests
afterAll(() => {
  server.close()
})
