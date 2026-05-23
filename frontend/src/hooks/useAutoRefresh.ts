import { useMemo } from 'react'
import { useSelector } from 'react-redux'
import { selectAutoUpdate, selectRefreshRate } from '@/store/slices/uiSlice'

export function useAutoRefresh() {
  const autoUpdate = useSelector(selectAutoUpdate)
  const refreshRate = useSelector(selectRefreshRate)
  const pollingInterval = autoUpdate && refreshRate > 0 ? refreshRate * 1000 : 0

  // The global App refresh loop calls the backend refresh endpoint first, then
  // invalidates RTK Query tags. Page queries should not run an independent
  // frontend-only polling loop on the same interval.
  const queryOptions = useMemo(
    () => ({
      pollingInterval: 0,
      refetchOnFocus: true,
      refetchOnReconnect: true,
    }),
    []
  )

  return {
    autoUpdate,
    refreshRate,
    pollingInterval,
    queryOptions,
  }
}
