import { type ComponentProps, useEffect, useRef, useState } from 'react'
import { RefreshCw } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

const DEFAULT_MIN_SPIN_MS = 800

type RefreshButtonProps = ComponentProps<typeof Button> & {
  onRefresh?: () => void | Promise<unknown>
  isLoading?: boolean
  minSpinMs?: number
  iconClassName?: string
}

export default function RefreshButton({
  onRefresh,
  isLoading = false,
  minSpinMs = DEFAULT_MIN_SPIN_MS,
  iconClassName,
  className,
  children,
  disabled,
  ...buttonProps
}: RefreshButtonProps) {
  const [isManualSpinning, setIsManualSpinning] = useState(false)
  const spinStartedAtRef = useRef<number>(0)
  const spinTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const clearSpinTimer = () => {
    if (!spinTimerRef.current) {
      return
    }
    clearTimeout(spinTimerRef.current)
    spinTimerRef.current = null
  }

  useEffect(() => {
    return () => clearSpinTimer()
  }, [])

  const stopManualSpinWithMinimum = () => {
    const elapsed = Date.now() - spinStartedAtRef.current
    const remaining = Math.max(minSpinMs - elapsed, 0)

    clearSpinTimer()
    if (remaining === 0) {
      setIsManualSpinning(false)
      return
    }

    spinTimerRef.current = setTimeout(() => {
      setIsManualSpinning(false)
      spinTimerRef.current = null
    }, remaining)
  }

  const isSpinning = isLoading || isManualSpinning
  const isDisabled = Boolean(disabled) || isSpinning

  const handleClick = async () => {
    if (!onRefresh || isDisabled) {
      return
    }

    spinStartedAtRef.current = Date.now()
    setIsManualSpinning(true)

    try {
      await Promise.resolve(onRefresh())
    } finally {
      stopManualSpinWithMinimum()
    }
  }

  return (
    <Button
      type="button"
      className={className}
      disabled={isDisabled}
      aria-busy={isSpinning}
      onClick={handleClick}
      {...buttonProps}
    >
      <RefreshCw className={cn('h-4 w-4', children ? 'mr-2' : '', isSpinning && 'animate-spin', iconClassName)} />
      {children}
    </Button>
  )
}
