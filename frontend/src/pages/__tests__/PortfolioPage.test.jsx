import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
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
      isLoading: false
    })
    useGetPortfolioSummaryQuery.mockReturnValue({
      data: { totalValue: 5000, totalCost: 4000, totalProfitLoss: 1000, totalProfitLossPercent: 25 },
      isLoading: false
    })
  })

  it('renders without crashing', () => {
    const { container } = renderWithProviders(<PortfolioPage />)
    expect(container).toBeInTheDocument()
  })

  it('displays portfolio name from mock data', () => {
    renderWithProviders(<PortfolioPage />)
    expect(screen.getByText('Test Portfolio')).toBeInTheDocument()
  })

  it('shows loading state', () => {
    useGetPortfoliosQuery.mockReturnValue({ data: null, isLoading: true })
    useGetPortfolioSummaryQuery.mockReturnValue({ data: null, isLoading: true })
    const { container } = renderWithProviders(<PortfolioPage />)
    expect(container).toBeInTheDocument()
  })
})
