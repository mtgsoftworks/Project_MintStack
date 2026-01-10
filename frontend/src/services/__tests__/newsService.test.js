import { describe, it, expect, beforeEach, vi } from 'vitest'
import { newsService } from '../newsService'
import api from '../api'

// Mock the api module
vi.mock('../api', () => ({
    default: {
        get: vi.fn(),
    },
}))

describe('newsService', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    describe('getLatestNews', () => {
        it('should fetch latest news', async () => {
            const mockNews = [
                { id: '1', title: 'BIST 100 Rekor', summary: 'Borsa yükselişte' },
                { id: '2', title: 'Dolar Güncelleme', summary: 'Dolar stabil' },
            ]

            api.get.mockResolvedValue({ data: { data: mockNews } })

            const result = await newsService.getLatestNews()

            expect(api.get).toHaveBeenCalledWith('/news/latest')
            expect(result).toEqual(mockNews)
        })

        it('should handle errors', async () => {
            api.get.mockRejectedValue(new Error('Network error'))

            await expect(newsService.getLatestNews()).rejects.toThrow('Network error')
        })
    })

    describe('getAllNews', () => {
        it('should fetch all news with pagination', async () => {
            const mockResponse = {
                content: [{ id: '1', title: 'Test Haber' }],
                totalElements: 100,
                totalPages: 10,
            }

            api.get.mockResolvedValue({ data: { data: mockResponse } })

            const result = await newsService.getAllNews({ page: 0, size: 10 })

            expect(api.get).toHaveBeenCalledWith('/news', {
                params: { page: 0, size: 10 },
            })
            expect(result).toEqual(mockResponse)
        })
    })

    describe('getNewsById', () => {
        it('should fetch specific news by id', async () => {
            const mockNews = {
                id: '1',
                title: 'Test Haber',
                content: 'Detaylı içerik...',
                publishedAt: '2024-01-01T10:00:00Z',
            }

            api.get.mockResolvedValue({ data: { data: mockNews } })

            const result = await newsService.getNewsById('1')

            expect(api.get).toHaveBeenCalledWith('/news/1')
            expect(result).toEqual(mockNews)
        })
    })

    describe('getNewsByCategory', () => {
        it('should fetch news filtered by category', async () => {
            const mockResponse = {
                content: [{ id: '1', title: 'Ekonomi Haberi', categorySlug: 'ekonomi' }],
                totalElements: 50,
            }

            api.get.mockResolvedValue({ data: { data: mockResponse } })

            const result = await newsService.getNewsByCategory('ekonomi', { page: 0, size: 10 })

            expect(api.get).toHaveBeenCalledWith('/news/category/ekonomi', {
                params: { page: 0, size: 10 },
            })
            expect(result).toEqual(mockResponse)
        })
    })

    describe('searchNews', () => {
        it('should search news by query', async () => {
            const mockResponse = {
                content: [{ id: '1', title: 'BIST 100 Analizi' }],
                totalElements: 5,
            }

            api.get.mockResolvedValue({ data: { data: mockResponse } })

            const result = await newsService.searchNews('BIST', { page: 0, size: 10 })

            expect(api.get).toHaveBeenCalledWith('/news/search', {
                params: { q: 'BIST', page: 0, size: 10 },
            })
            expect(result).toEqual(mockResponse)
        })
    })

    describe('getFeaturedNews', () => {
        it('should fetch featured news', async () => {
            const mockNews = [
                { id: '1', title: 'Öne Çıkan Haber', isFeatured: true },
            ]

            api.get.mockResolvedValue({ data: { data: mockNews } })

            const result = await newsService.getFeaturedNews()

            expect(api.get).toHaveBeenCalledWith('/news/featured')
            expect(result).toEqual(mockNews)
        })
    })

    describe('getCategories', () => {
        it('should fetch news categories', async () => {
            const mockCategories = [
                { id: '1', name: 'Ekonomi', slug: 'ekonomi' },
                { id: '2', name: 'Borsa', slug: 'borsa' },
            ]

            api.get.mockResolvedValue({ data: { data: mockCategories } })

            const result = await newsService.getCategories()

            expect(api.get).toHaveBeenCalledWith('/news/categories')
            expect(result).toEqual(mockCategories)
        })
    })
})
