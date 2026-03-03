import { describe, it, expect } from 'vitest'
import { portfolioService } from '../portfolioService'

describe('portfolioService', () => {
  it('has getPortfolios method', () => {
    expect(typeof portfolioService.getPortfolios).toBe('function')
  })

  it('has getPortfolio method', () => {
    expect(typeof portfolioService.getPortfolio).toBe('function')
  })

  it('has createPortfolio method', () => {
    expect(typeof portfolioService.createPortfolio).toBe('function')
  })

  it('has deletePortfolio method', () => {
    expect(typeof portfolioService.deletePortfolio).toBe('function')
  })

  it('has getTransactions method', () => {
    expect(typeof portfolioService.getTransactions).toBe('function')
  })
})
