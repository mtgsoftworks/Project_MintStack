import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '@/utils/test-utils'
import SettingsPage from '@/pages/SettingsPage'

vi.mock('@/pages/settings/ApiKeysTab', () => ({
  ApiKeysTab: () => <div>api-keys-content</div>,
}))

vi.mock('@/pages/settings/DataSourcesTab', () => ({
  DataSourcesTab: () => <div>data-sources-content</div>,
}))

vi.mock('@/pages/settings/GeneralSettingsTab', () => ({
  GeneralSettingsTab: () => <div>general-settings-content</div>,
}))

vi.mock('@/pages/settings/SimulationTab', () => ({
  SimulationTab: () => <div>simulation-content</div>,
}))

vi.mock('@/pages/settings/providerInfo', () => ({
  getProviderInfo: () => ({}),
}))

vi.mock('@/pages/settings/hooks/useGeneralSettings', () => ({
  useGeneralSettings: () => ({
    theme: 'light',
    currency: 'TRY',
    timezone: 'Europe/Istanbul',
    autoUpdate: true,
    refreshRate: 60,
    notificationSettings: {
      priceAlerts: true,
      portfolioUpdates: true,
      emailNotifications: true,
      pushNotifications: false,
    },
    isClearingCache: false,
    isSavingSettings: false,
    handleThemeChange: vi.fn(),
    handleLanguageChange: vi.fn(),
    handleCurrencyChange: vi.fn(),
    handleTimezoneChange: vi.fn(),
    handleAutoUpdateChange: vi.fn(),
    handleRefreshRateChange: vi.fn(),
    handleNotificationToggle: vi.fn(),
    handleFullReset: vi.fn(),
    handleClearCache: vi.fn(),
    handleSaveSettings: vi.fn(),
  }),
}))

vi.mock('@/pages/settings/hooks/useApiDataSourceSettings', () => ({
  useApiDataSourceSettings: () => ({
    apiConfigs: [],
    providerCapabilities: {},
    preferencesData: { data: [] },
    isLoading: false,
    isAdding: false,
    isTesting: false,
    isDialogOpen: false,
    editingConfig: null,
    formData: { provider: 'ALPHA_VANTAGE', apiKey: '', secretKey: '', modelName: '', baseUrl: '', isActive: true },
    isValidated: false,
    isRefreshingDataSources: false,
    handleOpenDialog: vi.fn(),
    handleDialogOpenChange: vi.fn(),
    handleFormFieldChange: vi.fn(),
    handleTestKey: vi.fn(),
    handleAddSubmit: vi.fn(),
    handleDelete: vi.fn(),
    handleSelectDataPreference: vi.fn(),
    handleRefreshDataSources: vi.fn(),
  }),
}))

vi.mock('@/pages/settings/hooks/useSimulationSettings', () => ({
  useSimulationSettings: () => ({
    simConfig: { enabled: false },
    simStatus: null,
    handleToggleSimulation: vi.fn(),
    handleUpdateSimulationConfig: vi.fn(),
    handleResetSimulation: vi.fn(),
  }),
}))

describe('SettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders settings tabs and default content', () => {
    renderWithProviders(<SettingsPage />, {
      preloadedState: {
        auth: {
          isAuthenticated: true,
          isInitialized: true,
          token: 'mock-token',
          user: { username: 'user' },
          roles: ['user'],
        },
      },
    })

    expect(screen.getByText('settingsPage.tabs.general')).toBeInTheDocument()
    expect(screen.getAllByText('settings.apiKeys.title').length).toBeGreaterThan(0)
    expect(screen.getByText('settings.dataSources.title')).toBeInTheDocument()
    expect(screen.queryByText('settings.simulation.title')).not.toBeInTheDocument()
    expect(screen.getByText('api-keys-content')).toBeInTheDocument()
  })

  it('shows simulation tab for admin users', () => {
    renderWithProviders(<SettingsPage />, {
      preloadedState: {
        auth: {
          isAuthenticated: true,
          isInitialized: true,
          token: 'mock-token',
          user: { username: 'admin' },
          roles: ['admin'],
        },
      },
    })

    expect(screen.getByText('settings.simulation.title')).toBeInTheDocument()
  })
})
