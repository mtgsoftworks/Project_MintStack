import { vi, describe, it, expect } from 'vitest'
import { marketService } from '../marketService'

describe('marketService', () => {
  it('has getCurrencies method', () => {
    expect(typeof marketService.getCurrencies).toBe('function')
  })

  it('has getStocks method', () => {
    expect(typeof marketService.getStocks).toBe('function')
  })

  it('has getStock method', () => {
    expect(typeof marketService.getStock).toBe('function')
  })

  it('has getBonds method', () => {
    expect(typeof marketService.getBonds).toBe('function')
  })

  it('has getFunds method', () => {
    expect(typeof marketService.getFunds).toBe('function')
  })
})
