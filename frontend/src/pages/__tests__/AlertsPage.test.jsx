import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '@/utils/test-utils'
import AlertsPage from '@/pages/AlertsPage'

vi.mock('@/services/alertService', () => ({
  default: {
    getAll: vi.fn(() => Promise.resolve({
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
    })),
    create: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({})),
    deactivate: vi.fn(() => Promise.resolve({})),
  },
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
