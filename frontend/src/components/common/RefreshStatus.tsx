import { RefreshCw } from 'lucide-react'
import { cn, formatDateTimeWithSeconds } from '@/lib/utils'

type RefreshStatusProps = {
  lastUpdatedAt?: number | null
  autoUpdateEnabled: boolean
  refreshRateSeconds: number
  isFetching?: boolean
  className?: string
}

export default function RefreshStatus({
  lastUpdatedAt,
  autoUpdateEnabled,
  refreshRateSeconds,
  isFetching = false,
  className,
}: RefreshStatusProps) {
  return (
    <div className={cn('flex flex-wrap items-center gap-2 text-xs text-muted-foreground', className)}>
      <span>
        Son veri yenileme: {lastUpdatedAt ? formatDateTimeWithSeconds(lastUpdatedAt) : '-'}
      </span>
      <span aria-hidden="true">|</span>
      <span>
        Otomatik yenileme: {autoUpdateEnabled ? `Acik (${refreshRateSeconds} sn)` : 'Kapali'}
      </span>
      {isFetching && (
        <>
          <span aria-hidden="true">|</span>
          <span className="inline-flex items-center gap-1 text-primary">
            <RefreshCw className="h-3.5 w-3.5 animate-spin" />
            Yenileniyor
          </span>
        </>
      )}
    </div>
  )
}
