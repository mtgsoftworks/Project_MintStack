import { Star } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { getApiErrorMessage } from '@/lib/apiError'
import { useAddToDefaultWatchlistMutation } from '@/store/api/watchlistApi'

export default function WatchlistQuickAddButton({
  symbol,
  className = '',
  compact = true,
}) {
  const { t } = useTranslation()
  const [addToDefaultWatchlist, { isLoading }] = useAddToDefaultWatchlistMutation()

  const handleAdd = async () => {
    const normalizedSymbol = (symbol || '').trim().toUpperCase()
    if (!normalizedSymbol) {
      toast.error(t('watchlist.invalidSymbol'))
      return
    }

    try {
      const watchlist = await addToDefaultWatchlist({ symbol: normalizedSymbol }).unwrap()
      toast.success(t('watchlist.addedToList', { symbol: normalizedSymbol, list: watchlist?.name || 'watchlist' }))
    } catch (error) {
      toast.error(getApiErrorMessage(error, t('watchlist.addFailed')))
    }
  }

  return (
    <Button
      type="button"
      size={compact ? 'icon' : 'sm'}
      variant="outline"
      className={className}
      disabled={isLoading}
      onClick={handleAdd}
      title={t('watchlist.addDefaultTitle')}
    >
      <Star className="h-4 w-4" />
      {!compact && <span className="ml-2">Watchlist</span>}
    </Button>
  )
}
