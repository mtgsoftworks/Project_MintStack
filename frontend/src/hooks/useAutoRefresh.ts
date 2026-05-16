import { useMemo } from 'react'
import { useSelector } from 'react-redux'
import { selectAutoUpdate, selectRefreshRate } from '@/store/slices/uiSlice'

export function useAutoRefresh() {
  const autoUpdate = useSelector(selectAutoUpdate)
  const refreshRate = useSelector(selectRefreshRate)
  const pollingInterval = autoUpdate && refreshRate > 0 ? refreshRate * 1000 : 0

  const queryOptions = useMemo(
    () => ({
      pollingInterval,
      refetchOnFocus: true,
      refetchOnReconnect: true,
    }),
    [pollingInterval]
  )

  return {
    autoUpdate,
    refreshRate,
    pollingInterval,
    queryOptions,
  }
}
