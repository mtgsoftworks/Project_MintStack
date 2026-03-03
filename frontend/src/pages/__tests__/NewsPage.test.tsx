import { vi, describe, it, expect, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '@/utils/test-utils'
import NewsPage from '@/pages/NewsPage'

vi.mock('@/store/api/newsApi', () => ({
  useGetNewsQuery: vi.fn(() => ({
    data: {
      data: [
        {
          id: '1',
          title: 'Test Haber Başlığı',
          summary: 'Test haber özeti',
          sourceName: 'Test Source',
          publishedAt: '2026-01-15T10:00:00',
          categoryName: 'Finans',
          imageUrl: null,
          isFeatured: false,
        },
      ],
      pagination: { currentPage: 0, totalPages: 1, totalElements: 1 },
    },
    isLoading: false,
    error: null,
  })),
  useGetNewsCategoriesQuery: vi.fn(() => ({
    data: [{ id: '1', name: 'Finans', slug: 'finans' }],
    isLoading: false,
  })),
  useSearchNewsQuery: vi.fn(() => ({
    data: { data: [], totalPages: 0 },
    isLoading: false,
  })),
  useGetNewsByCategoryQuery: vi.fn(() => ({
    data: { data: [], totalPages: 0 },
    isLoading: false,
  })),
}))

describe('NewsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders without crashing', () => {
    const { container } = renderWithProviders(<NewsPage />)
    expect(container).toBeInTheDocument()
  })

  it('displays news title', () => {
    renderWithProviders(<NewsPage />)
    expect(screen.getByText(/Test Haber Başlığı/i)).toBeInTheDocument()
  })
})
