import { useMemo } from 'react'
import { Star } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { getApiErrorMessage } from '@/lib/apiError'
import {
  useGetWatchlistsQuery,
  useAddToDefaultWatchlistMutation,
  useRemoveWatchlistInstrumentMutation,
} from '@/store/api/watchlistApi'

interface WatchlistQuickAddButtonProps {
  symbol: string
  className?: string
  compact?: boolean
}

interface WatchlistItem {
  symbol?: string
  instrumentSymbol?: string
}

interface Watchlist {
  id: string | number
  name?: string
  isDefault?: boolean
  items?: WatchlistItem[]
}

export default function WatchlistQuickAddButton({
  symbol,
  className = '',
  compact = true,
}: WatchlistQuickAddButtonProps) {
  const { t } = useTranslation()
  const normalizedSymbol = useMemo(() => (symbol || '').trim().toUpperCase(), [symbol])

  const { data: watchlists = [] } = useGetWatchlistsQuery(undefined)
  const [addToDefaultWatchlist, { isLoading: isAdding }] = useAddToDefaultWatchlistMutation()
  const [removeWatchlistInstrument, { isLoading: isRemoving }] = useRemoveWatchlistInstrumentMutation()

  // Find if symbol is in any watchlist, and retrieve that watchlist info
  const targetWatchlistInfo = useMemo(() => {
    if (!normalizedSymbol || !Array.isArray(watchlists)) {
      return null
    }
    for (const wl of watchlists as Watchlist[]) {
      const items = wl.items || []
      const found = items.some((item) => {
        const itemSymbol = (item.instrumentSymbol || item.symbol || '').trim().toUpperCase()
        return itemSymbol === normalizedSymbol
      })
      if (found) {
        return { watchlistId: wl.id, watchlistName: wl.name || 'İzleme Listesi' }
      }
    }
    return null
  }, [normalizedSymbol, watchlists])

  const isStarred = Boolean(targetWatchlistInfo)
  const isLoading = isAdding || isRemoving

  const handleToggle = async (event: React.MouseEvent) => {
    event.stopPropagation()
    event.preventDefault()

    if (!normalizedSymbol) {
      toast.error(t('watchlist.invalidSymbol'))
      return
    }

    try {
      if (isStarred && targetWatchlistInfo) {
        // Remove from watchlist
        await removeWatchlistInstrument({
          watchlistId: targetWatchlistInfo.watchlistId,
          symbol: normalizedSymbol,
        }).unwrap()
        toast.info(`${normalizedSymbol} izleme listesinden çıkarıldı`)
      } else {
        // Add to default watchlist
        const watchlist = await addToDefaultWatchlist({ symbol: normalizedSymbol }).unwrap()
        const listName = watchlist?.name || 'Varsayılan Liste'
        toast.success(`${normalizedSymbol} izleme listesine eklendi ⭐`, {
          description: `"${listName}" listenize kaydedildi.`,
        })
      }
    } catch (error) {
      toast.error(getApiErrorMessage(error, isStarred ? 'Listeden çıkarılamadı' : t('watchlist.addFailed')))
    }
  }

  return (
    <Button
      type="button"
      size={compact ? 'icon' : 'sm'}
      variant={isStarred ? 'secondary' : 'outline'}
      className={cn(
        'transition-all duration-200',
        isStarred && 'border-amber-400/50 bg-amber-400/10 hover:bg-amber-400/20 text-amber-500',
        className
      )}
      disabled={isLoading}
      onClick={handleToggle}
      title={isStarred ? `${normalizedSymbol} listenden çıkar` : `${normalizedSymbol} izleme listesine ekle`}
    >
      <Star
        className={cn(
          'h-4 w-4 transition-transform duration-200',
          isStarred ? 'fill-amber-400 text-amber-400 scale-110' : 'text-muted-foreground'
        )}
      />
      {!compact && (
        <span className="ml-2 font-medium">
          {isStarred ? 'Takipte' : 'Takip Et'}
        </span>
      )}
    </Button>
  )
}
