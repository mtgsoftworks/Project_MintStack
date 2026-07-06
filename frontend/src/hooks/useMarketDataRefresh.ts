import { useCallback, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useSelector } from 'react-redux'
import { toast } from 'sonner'
import { useRefreshMarketDataMutation } from '@/store/api/marketApi'
import { getApiErrorMessage } from '@/pages/settings/getApiErrorMessage'
import { selectIsAuthenticated } from '@/store/slices/authSlice'

export function useMarketDataRefresh(dataTypes: string[]) {
  const { t } = useTranslation()
  const isAuthenticated = useSelector(selectIsAuthenticated)
  const [refreshMarketData, { isLoading }] = useRefreshMarketDataMutation()
  const normalizedDataTypes = useMemo(
    () => [...new Set((dataTypes || []).filter(Boolean))],
    [dataTypes]
  )

  const refreshAndRefetch = useCallback(
    async (refetchAfterRefresh?: () => void | Promise<unknown>) => {
      try {
        if (isAuthenticated) {
          const result = await refreshMarketData({ dataTypes: normalizedDataTypes }).unwrap()
          const refreshData = result?.data ?? result
          const skippedCount = refreshData?.skippedDataTypes?.length || 0
          const refreshedCount = refreshData?.refreshedDataTypes?.length || 0

          if (skippedCount > 0 && refreshedCount === 0) {
            toast.error(t('marketRefresh.failed'))
          } else if (skippedCount > 0) {
            toast.warning(t('marketRefresh.partial', { count: skippedCount }))
          } else {
            toast.success(t('marketRefresh.success'))
          }
        } else {
          toast.info(t('marketRefresh.loginRequired'))
        }
      } catch (error) {
        toast.error(getApiErrorMessage(error, t('marketRefresh.failed')))
      } finally {
        if (refetchAfterRefresh) {
          try {
            // Wrap in timeout to ensure query is registered in RTK Query state
            setTimeout(() => {
              Promise.resolve(refetchAfterRefresh()).catch(() => {
                // Ignore refetch errors - query might not be started yet
              })
            }, 100)
          } catch (refetchError) {
            // Silently ignore refetch errors
          }
        }
      }
    },
    [isAuthenticated, normalizedDataTypes, refreshMarketData, t]
  )

  return {
    refreshAndRefetch,
    isRefreshingMarketData: isLoading,
  }
}
