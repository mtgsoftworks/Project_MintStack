import { vi, describe, it, expect } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '@/utils/test-utils'
import { GeneralSettingsTab } from '@/pages/settings/GeneralSettingsTab'

const t = (key, options) => {
  if (options?.returnObjects) {
    return ['item-1', 'item-2']
  }
  return key
}

const baseProps = {
  t,
  i18n: { language: 'tr', changeLanguage: vi.fn() },
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
  onThemeChange: vi.fn(),
  onLanguageChange: vi.fn(),
  onCurrencyChange: vi.fn(),
  onTimezoneChange: vi.fn(),
  onAutoUpdateChange: vi.fn(),
  onRefreshRateChange: vi.fn(),
  onNotificationToggle: vi.fn(),
  isAdmin: true,
  onFullReset: vi.fn(),
  onClearCache: vi.fn(),
  onSaveSettings: vi.fn(),
}

describe('GeneralSettingsTab', () => {
  it('calls save handler when save button is clicked', async () => {
    const user = userEvent.setup()
    const onSaveSettings = vi.fn()

    renderWithProviders(
      <GeneralSettingsTab
        {...baseProps}
        onSaveSettings={onSaveSettings}
      />
    )

    await user.click(screen.getByText('common.save'))
    expect(onSaveSettings).toHaveBeenCalledTimes(1)
  })

  it('closes danger dialog when cancel is clicked', async () => {
    const user = userEvent.setup()

    renderWithProviders(<GeneralSettingsTab {...baseProps} />)

    await user.click(screen.getByText('settingsPage.dangerZone.reset.button'))
    expect(screen.getByText('settingsPage.dangerZone.reset.dialog.title')).toBeInTheDocument()

    await user.click(screen.getByText('common.cancel'))

    await waitFor(() => {
      expect(screen.queryByText('settingsPage.dangerZone.reset.dialog.title')).not.toBeInTheDocument()
    })
  })
})
