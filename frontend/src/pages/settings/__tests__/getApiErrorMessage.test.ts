import { describe, expect, it } from 'vitest'
import { getApiErrorMessage } from '../getApiErrorMessage'

describe('getApiErrorMessage', () => {
  it('returns backend message for conflict-like payloads', () => {
    const error = {
      status: 409,
      data: {
        message: 'Simulation cannot be enabled: API key is required.',
      },
    }

    expect(getApiErrorMessage(error, 'fallback')).toBe(
      'Simulation cannot be enabled: API key is required.'
    )
  })

  it('falls back when no backend payload exists', () => {
    expect(getApiErrorMessage({}, 'fallback')).toBe('fallback')
  })
})
