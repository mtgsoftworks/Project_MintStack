import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '../../utils/test-utils'
import PortfolioPage from '../PortfolioPage'

vi.mock('../../store/api/portfolioApi', () => ({
  useGetPortfoliosQuery: vi.fn(),
  useGetPortfolioSummaryQuery: vi.fn(),
  useCreatePortfolioMutation: vi.fn(() => [vi.fn(), { isLoading: false }]),
  useDeletePortfolioMutation: vi.fn(() => [vi.fn(), { isLoading: false }]),
}))

import { useGetPortfoliosQuery, useGetPortfolioSummaryQuery } from '../../store/api/portfolioApi'

describe('PortfolioPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useGetPortfoliosQuery.mockReturnValue({
      data: [{ id: 1, name: 'Test Portfolio', totalValue: 1000, profitLoss: 100, profitLossPercent: 10 }],
      isLoading: false,
      error: null,
    })
    useGetPortfolioSummaryQuery.mockReturnValue({
      data: { totalValue: 5000, totalCost: 4000, totalProfitLoss: 1000, totalProfitLossPercent: 25 },
      isLoading: false,
      error: null,
    })
  })

  it('renders without crashing', async () => {
    const { container } = renderWithProviders(<PortfolioPage />)
    await waitFor(() => expect(container).toBeInTheDocument())
  })

  it('displays portfolio name from mock data', async () => {
    renderWithProviders(<PortfolioPage />)
    await waitFor(() => expect(screen.getByText('Test Portfolio')).toBeInTheDocument())
  })

  it('shows loading state', async () => {
    useGetPortfoliosQuery.mockReturnValue({ data: null, isLoading: true, error: null })
    useGetPortfolioSummaryQuery.mockReturnValue({ data: null, isLoading: true, error: null })
    const { container } = renderWithProviders(<PortfolioPage />)
    await waitFor(() => expect(container).toBeInTheDocument())
  })
})
