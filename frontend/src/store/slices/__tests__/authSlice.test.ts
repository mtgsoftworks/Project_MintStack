import { describe, it, expect } from 'vitest'
import authReducer, {
  setAuth,
  setToken,
  setUser,
  setInitialized,
  logout,
  updateProfile,
  selectIsAuthenticated,
  selectIsInitialized,
  selectToken,
  selectUser,
  selectRoles,
  selectIsAdmin,
} from '../authSlice'

describe('authSlice', () => {
  describe('initial state', () => {
    it('should return the initial state', () => {
      expect(authReducer(undefined, { type: 'unknown' })).toEqual({
        isAuthenticated: false,
        isInitialized: false,
        token: null,
        user: null,
        roles: [],
      })
    })
  })

  describe('setAuth action', () => {
    it('should set authentication data', () => {
      const initialState = {
        isAuthenticated: false,
        isInitialized: false,
        token: null,
        user: null,
        roles: [],
      }

      const payload = {
        token: 'test-token',
        user: { id: 1, name: 'Test User', email: 'test@example.com' },
        roles: ['user', 'admin'],
      }

      const newState = authReducer(initialState, setAuth(payload))

      expect(newState.isAuthenticated).toBe(true)
      expect(newState.isInitialized).toBe(true)
      expect(newState.token).toBe('test-token')
      expect(newState.user).toEqual(payload.user)
      expect(newState.roles).toEqual(['user', 'admin'])
    })

    it('should set authentication data without roles', () => {
      const initialState = {
        isAuthenticated: false,
        isInitialized: false,
        token: null,
        user: null,
        roles: [],
      }

      const payload = {
        token: 'test-token',
        user: { id: 1, name: 'Test User' },
      }

      const newState = authReducer(initialState, setAuth(payload))

      expect(newState.isAuthenticated).toBe(true)
      expect(newState.isInitialized).toBe(true)
      expect(newState.roles).toEqual([])
    })
  })

  describe('setToken action', () => {
    it('should set the token', () => {
      const initialState = {
        isAuthenticated: true,
        isInitialized: true,
        token: 'old-token',
        user: { id: 1 },
        roles: ['user'],
      }

      const newState = authReducer(initialState, setToken('new-token'))

      expect(newState.token).toBe('new-token')
      expect(newState.isAuthenticated).toBe(true)
    })
  })

  describe('setUser action', () => {
    it('should set the user', () => {
      const initialState = {
        isAuthenticated: true,
        isInitialized: true,
        token: 'token',
        user: { id: 1, name: 'Old Name' },
        roles: ['user'],
      }

      const newUser = { id: 1, name: 'New Name', email: 'new@example.com' }
      const newState = authReducer(initialState, setUser(newUser))

      expect(newState.user).toEqual(newUser)
    })
  })

  describe('setInitialized action', () => {
    it('should set isInitialized to true', () => {
      const initialState = {
        isAuthenticated: false,
        isInitialized: false,
        token: null,
        user: null,
        roles: [],
      }

      const newState = authReducer(initialState, setInitialized(true))

      expect(newState.isInitialized).toBe(true)
    })

    it('should set isInitialized to false', () => {
      const initialState = {
        isAuthenticated: true,
        isInitialized: true,
        token: 'token',
        user: { id: 1 },
        roles: ['user'],
      }

      const newState = authReducer(initialState, setInitialized(false))

      expect(newState.isInitialized).toBe(false)
    })
  })

  describe('logout action', () => {
    it('should reset state to logged out', () => {
      const initialState = {
        isAuthenticated: true,
        isInitialized: true,
        token: 'test-token',
        user: { id: 1, name: 'Test User' },
        roles: ['user', 'admin'],
      }

      const newState = authReducer(initialState, logout())

      expect(newState.isAuthenticated).toBe(false)
      expect(newState.token).toBe(null)
      expect(newState.user).toBe(null)
      expect(newState.roles).toEqual([])
    })

    it('should preserve isInitialized after logout', () => {
      const initialState = {
        isAuthenticated: true,
        isInitialized: true,
        token: 'test-token',
        user: { id: 1 },
        roles: ['user'],
      }

      const newState = authReducer(initialState, logout())

      expect(newState.isInitialized).toBe(true)
    })
  })

  describe('updateProfile action', () => {
    it('should update user profile', () => {
      const initialState = {
        isAuthenticated: true,
        isInitialized: true,
        token: 'token',
        user: { id: 1, name: 'Test User', email: 'test@example.com' },
        roles: ['user'],
      }

      const updates = { name: 'Updated Name', phone: '123456' }
      const newState = authReducer(initialState, updateProfile(updates))

      expect(newState.user.name).toBe('Updated Name')
      expect(newState.user.phone).toBe('123456')
      expect(newState.user.email).toBe('test@example.com')
    })

    it('should not update if user is null', () => {
      const initialState = {
        isAuthenticated: false,
        isInitialized: true,
        token: null,
        user: null,
        roles: [],
      }

      const newState = authReducer(initialState, updateProfile({ name: 'Test' }))

      expect(newState.user).toBe(null)
    })
  })
})

describe('authSlice selectors', () => {
  const mockState = {
    auth: {
      isAuthenticated: true,
      isInitialized: true,
      token: 'test-token',
      user: { id: 1, name: 'Test User' },
      roles: ['user', 'admin'],
    },
  }

  it('selectIsAuthenticated should return isAuthenticated', () => {
    expect(selectIsAuthenticated(mockState)).toBe(true)
  })

  it('selectIsInitialized should return isInitialized', () => {
    expect(selectIsInitialized(mockState)).toBe(true)
  })

  it('selectToken should return token', () => {
    expect(selectToken(mockState)).toBe('test-token')
  })

  it('selectUser should return user', () => {
    expect(selectUser(mockState)).toEqual({ id: 1, name: 'Test User' })
  })

  it('selectRoles should return roles', () => {
    expect(selectRoles(mockState)).toEqual(['user', 'admin'])
  })

  it('selectIsAdmin should return true when admin role exists', () => {
    expect(selectIsAdmin(mockState)).toBe(true)
  })

  it('selectIsAdmin should return false when admin role does not exist', () => {
    const nonAdminState = {
      auth: {
        ...mockState.auth,
        roles: ['user'],
      },
    }
    expect(selectIsAdmin(nonAdminState)).toBe(false)
  })
})
