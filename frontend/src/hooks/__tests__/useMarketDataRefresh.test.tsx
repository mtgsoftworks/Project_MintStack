import { PropsWithChildren } from 'react'
import { Provider } from 'react-redux'
import { configureStore } from '@reduxjs/toolkit'
import { act, renderHook } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import authReducer from '@/store/slices/authSlice'
import { useMarketDataRefresh } from '../useMarketDataRefresh'

const mocks = vi.hoisted(() => ({
  refreshMarketData: vi.fn(),
  unwrap: vi.fn(),
  toast: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
  },
}))

vi.mock('sonner', () => ({
  toast: mocks.toast,
}))

vi.mock('@/store/api/marketApi', () => ({
  useRefreshMarketDataMutation: () => [
    mocks.refreshMarketData,
    { isLoading: false },
  ],
}))

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, values?: Record<string, unknown>) => {
      if (values?.count != null) {
        return `${key}:${values.count}`
      }
      return key
    },
  }),
}))

function createWrapper(isAuthenticated: boolean) {
  const store = configureStore({
    reducer: {
      auth: authReducer,
    },
    preloadedState: {
      auth: {
        isAuthenticated,
        isInitialized: true,
        token: isAuthenticated ? 'token' : null,
        user: isAuthenticated ? { username: 'demo' } : null,
        roles: [],
      },
    },
  })

  return function Wrapper({ children }: PropsWithChildren) {
    return <Provider store={store}>{children}</Provider>
  }
}

describe('useMarketDataRefresh', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.unwrap.mockResolvedValue({
      data: {
        refreshedDataTypes: ['CURRENCY_RATES'],
        skippedDataTypes: [],
      },
    })
    mocks.refreshMarketData.mockReturnValue({ unwrap: mocks.unwrap })
  })

  it('calls backend refresh before refetching when user is authenticated', async () => {
    const refetch = vi.fn()
    const { result } = renderHook(
      () => useMarketDataRefresh(['CURRENCY_RATES']),
      { wrapper: createWrapper(true) }
    )

    await act(async () => {
      await result.current.refreshAndRefetch(refetch)
    })

    expect(mocks.refreshMarketData).toHaveBeenCalledWith({ dataTypes: ['CURRENCY_RATES'] })
    expect(refetch).toHaveBeenCalledTimes(1)
    expect(mocks.toast.success).toHaveBeenCalledWith('marketRefresh.success')
  })

  it('does not call backend refresh for anonymous users and still refetches visible data', async () => {
    const refetch = vi.fn()
    const { result } = renderHook(
      () => useMarketDataRefresh(['CURRENCY_RATES']),
      { wrapper: createWrapper(false) }
    )

    await act(async () => {
      await result.current.refreshAndRefetch(refetch)
    })

    expect(mocks.refreshMarketData).not.toHaveBeenCalled()
    expect(refetch).toHaveBeenCalledTimes(1)
    expect(mocks.toast.info).toHaveBeenCalledWith('marketRefresh.loginRequired')
  })
})
