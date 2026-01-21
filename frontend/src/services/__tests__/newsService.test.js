import { describe, it, expect } from 'vitest'
import { newsService } from '../newsService'

describe('newsService', () => {
  it('has getLatestNews method', () => {
    expect(typeof newsService.getLatestNews).toBe('function')
  })

  it('has getNews method', () => {
    expect(typeof newsService.getNews).toBe('function')
  })

  it('has getNewsById method', () => {
    expect(typeof newsService.getNewsById).toBe('function')
  })

  it('has getCategories method', () => {
    expect(typeof newsService.getCategories).toBe('function')
  })
})
