import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '@/utils/test-utils'
import SettingsPage from '@/pages/SettingsPage'

vi.mock('@/store/api/userApi', () => ({
  useGetPreferencesQuery: vi.fn(() => ({
    data: {
      language: 'tr',
      theme: 'light',
      refreshRate: 30,
      autoUpdate: true,
    },
    isLoading: false,
  })),
  useUpdatePreferencesMutation: vi.fn(() => [vi.fn(), { isLoading: false }]),
}))

describe('SettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders without crashing', () => {
    const { container } = renderWithProviders(<SettingsPage />)
    expect(container).toBeInTheDocument()
  })
})
