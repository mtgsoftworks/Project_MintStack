import { useCallback, useMemo } from 'react'
import { toast } from 'sonner'
import { useRefreshMarketDataMutation } from '@/store/api/marketApi'
import { getApiErrorMessage } from '@/pages/settings/getApiErrorMessage'

export function useMarketDataRefresh(dataTypes: string[]) {
  const [refreshMarketData, { isLoading }] = useRefreshMarketDataMutation()
  const normalizedDataTypes = useMemo(
    () => [...new Set((dataTypes || []).filter(Boolean))],
    [dataTypes.join('|')]
  )

  const refreshAndRefetch = useCallback(
    async (refetchAfterRefresh?: () => void | Promise<unknown>) => {
      try {
        await refreshMarketData({ dataTypes: normalizedDataTypes }).unwrap()
      } catch (error) {
        toast.error(getApiErrorMessage(error, 'Veri yenileme basarisiz oldu'))
      } finally {
        if (refetchAfterRefresh) {
          await Promise.resolve(refetchAfterRefresh())
        }
      }
    },
    [normalizedDataTypes, refreshMarketData]
  )

  return {
    refreshAndRefetch,
    isRefreshingMarketData: isLoading,
  }
}
