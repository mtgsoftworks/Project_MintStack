import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '@/utils/test-utils'
import AlertsPage from '@/pages/AlertsPage'

vi.mock('@/store/api/alertsApi', () => ({
  useGetAlertsQuery: vi.fn(() => ({
    data: [
      {
        id: 1,
        symbol: 'AAPL',
        instrumentName: 'Apple Inc.',
        alertType: 'PRICE_ABOVE',
        targetValue: 200,
        isActive: true,
        isTriggered: false,
        createdAt: '2026-01-15T10:00:00',
      },
    ],
    isLoading: false,
  })),
  useCreateAlertMutation: vi.fn(() => [vi.fn(), { isLoading: false }]),
  useDeleteAlertMutation: vi.fn(() => [vi.fn(), { isLoading: false }]),
  useDeactivateAlertMutation: vi.fn(() => [vi.fn(), { isLoading: false }]),
}))

describe('AlertsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders without crashing', async () => {
    const { container } = renderWithProviders(<AlertsPage />)
    await waitFor(() => expect(container).toBeInTheDocument())
  })

  it('displays alert info', async () => {
    renderWithProviders(<AlertsPage />)
    await waitFor(() => expect(screen.getByText(/AAPL/i)).toBeInTheDocument())
  })
})
