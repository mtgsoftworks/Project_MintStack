import { describe, it, expect, beforeEach, vi } from 'vitest'
import { portfolioService } from '../portfolioService'
import api from '../api'

// Mock the api module
vi.mock('../api', () => ({
    default: {
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        delete: vi.fn(),
    },
}))

describe('portfolioService', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    describe('getPortfolios', () => {
        it('should fetch user portfolios', async () => {
            const mockPortfolios = [
                { id: '1', name: 'Ana Portföy', totalValue: 100000 },
                { id: '2', name: 'Yedek Portföy', totalValue: 50000 },
            ]

            api.get.mockResolvedValue({ data: { data: mockPortfolios } })

            const result = await portfolioService.getPortfolios()

            expect(api.get).toHaveBeenCalledWith('/portfolios')
            expect(result).toEqual(mockPortfolios)
        })

        it('should handle errors', async () => {
            api.get.mockRejectedValue(new Error('Network error'))

            await expect(portfolioService.getPortfolios()).rejects.toThrow('Network error')
        })
    })

    describe('getPortfolio', () => {
        it('should fetch specific portfolio by id', async () => {
            const mockPortfolio = {
                id: '1',
                name: 'Ana Portföy',
                items: [],
                totalValue: 100000
            }

            api.get.mockResolvedValue({ data: { data: mockPortfolio } })

            const result = await portfolioService.getPortfolio('1')

            expect(api.get).toHaveBeenCalledWith('/portfolios/1')
            expect(result).toEqual(mockPortfolio)
        })
    })

    describe('createPortfolio', () => {
        it('should create new portfolio', async () => {
            const newPortfolio = { name: 'Yeni Portföy', description: 'Test' }
            const mockResponse = { id: '3', ...newPortfolio }

            api.post.mockResolvedValue({ data: { data: mockResponse } })

            const result = await portfolioService.createPortfolio(newPortfolio)

            expect(api.post).toHaveBeenCalledWith('/portfolios', newPortfolio)
            expect(result).toEqual(mockResponse)
        })
    })

    describe('updatePortfolio', () => {
        it('should update existing portfolio', async () => {
            const updateData = { name: 'Güncellenmiş Portföy' }
            const mockResponse = { id: '1', ...updateData }

            api.put.mockResolvedValue({ data: { data: mockResponse } })

            const result = await portfolioService.updatePortfolio('1', updateData)

            expect(api.put).toHaveBeenCalledWith('/portfolios/1', updateData)
            expect(result).toEqual(mockResponse)
        })
    })

    describe('deletePortfolio', () => {
        it('should delete portfolio', async () => {
            api.delete.mockResolvedValue({ data: { success: true } })

            await portfolioService.deletePortfolio('1')

            expect(api.delete).toHaveBeenCalledWith('/portfolios/1')
        })
    })

    describe('addItem', () => {
        it('should add item to portfolio', async () => {
            const newItem = {
                symbol: 'THYAO',
                quantity: 100,
                averageCost: 250
            }
            const mockResponse = { id: 'item-1', ...newItem }

            api.post.mockResolvedValue({ data: { data: mockResponse } })

            const result = await portfolioService.addItem('1', newItem)

            expect(api.post).toHaveBeenCalledWith('/portfolios/1/items', newItem)
            expect(result).toEqual(mockResponse)
        })
    })

    describe('removeItem', () => {
        it('should remove item from portfolio', async () => {
            api.delete.mockResolvedValue({ data: { success: true } })

            await portfolioService.removeItem('1', 'item-1')

            expect(api.delete).toHaveBeenCalledWith('/portfolios/1/items/item-1')
        })
    })

    describe('getPortfolioSummary', () => {
        it('should fetch portfolio summary', async () => {
            const mockSummary = {
                totalValue: 150000,
                totalCost: 120000,
                totalProfit: 30000,
                profitPercentage: 25,
            }

            api.get.mockResolvedValue({ data: { data: mockSummary } })

            const result = await portfolioService.getSummary('1')

            expect(api.get).toHaveBeenCalledWith('/portfolios/1/summary')
            expect(result).toEqual(mockSummary)
        })
    })

    describe('getTransactions', () => {
        it('should fetch portfolio transactions with pagination', async () => {
            const mockResponse = {
                success: true,
                data: [
                    {
                        id: 'txn-1',
                        instrumentSymbol: 'THYAO',
                        transactionType: 'BUY',
                        quantity: 5,
                        price: 95,
                    },
                ],
                pagination: {
                    page: 0,
                    size: 20,
                    totalElements: 1,
                    totalPages: 1,
                },
            }

            api.get.mockResolvedValue({ data: mockResponse })

            const result = await portfolioService.getTransactions('1', { page: 0, size: 20 })

            expect(api.get).toHaveBeenCalledWith('/portfolios/1/transactions', {
                params: { page: 0, size: 20 },
            })
            expect(result).toEqual(mockResponse)
        })
    })
})
