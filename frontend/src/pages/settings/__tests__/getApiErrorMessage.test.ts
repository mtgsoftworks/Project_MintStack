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

  it('does not return raw error objects to renderers', () => {
    const error = {
      status: 400,
      data: {
        error: {
          status: 400,
          error: 'Bad Request',
        },
      },
    }

    expect(getApiErrorMessage(error, 'fallback')).toBe('Bad Request')
  })
})
