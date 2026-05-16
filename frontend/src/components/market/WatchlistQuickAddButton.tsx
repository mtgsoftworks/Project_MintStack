import { Star } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { getApiErrorMessage } from '@/lib/apiError'
import { useAddToDefaultWatchlistMutation } from '@/store/api/watchlistApi'

export default function WatchlistQuickAddButton({
  symbol,
  className = '',
  compact = true,
}) {
  const [addToDefaultWatchlist, { isLoading }] = useAddToDefaultWatchlistMutation()

  const handleAdd = async () => {
    const normalizedSymbol = (symbol || '').trim().toUpperCase()
    if (!normalizedSymbol) {
      toast.error('Gecerli sembol bulunamadi')
      return
    }

    try {
      const watchlist = await addToDefaultWatchlist({ symbol: normalizedSymbol }).unwrap()
      toast.success(`${normalizedSymbol} ${watchlist?.name || 'watchlist'} listesine eklendi`)
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Watchlist ekleme basarisiz oldu'))
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
      title="Varsayilan watchlist'e ekle"
    >
      <Star className="h-4 w-4" />
      {!compact && <span className="ml-2">Watchlist</span>}
    </Button>
  )
}
