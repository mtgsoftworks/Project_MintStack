import { vi, describe, it, expect } from 'vitest'
import { baseApi, API_BASE_URL } from '../baseApi'

vi.mock('@/App', () => ({
  keycloak: {
    authenticated: true,
    updateToken: vi.fn().mockResolvedValue(true),
    token: 'mock-keycloak-token',
  },
}))

describe('baseApi', () => {
  describe('configuration', () => {
    it('should be configured correctly', () => {
      expect(baseApi).toBeDefined()
      expect(baseApi.reducerPath).toBe('api')
    })

    it('should have the correct reducerPath', () => {
      expect(baseApi.reducerPath).toBe('api')
    })
  })

  describe('API_BASE_URL', () => {
    it('should have a default URL', () => {
      expect(API_BASE_URL).toBeDefined()
      expect(typeof API_BASE_URL).toBe('string')
    })

    it('should default to relative api URL when no env vars set', () => {
      expect(API_BASE_URL).toBe('/api/v1')
    })
  })

  describe('API structure', () => {
    it('should have endpoints object', () => {
      expect(baseApi.endpoints).toBeDefined()
      expect(typeof baseApi.endpoints).toBe('object')
    })

    it('should have a reducer function', () => {
      expect(typeof baseApi.reducer).toBe('function')
    })

    it('should have middleware', () => {
      expect(baseApi.middleware).toBeDefined()
    })

    it('should have internalActions', () => {
      expect(baseApi.internalActions).toBeDefined()
    })

    it('should have util object', () => {
      expect(baseApi.util).toBeDefined()
    })

    it('should have injectEndpoints method', () => {
      expect(typeof baseApi.injectEndpoints).toBe('function')
    })

    it('should have enhanceEndpoints method', () => {
      expect(typeof baseApi.enhanceEndpoints).toBe('function')
    })
  })
})
