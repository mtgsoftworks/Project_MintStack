import { Loader2 } from 'lucide-react'
import { cn } from '@/lib/utils'
import { useTranslation } from 'react-i18next'

export function Loading({ 
  size = 'default', 
  text,
  fullScreen = false,
  className 
}) {
  const { t } = useTranslation()
  const resolvedText = text ?? t('common.loading')
  const sizeClasses = {
    sm: 'h-4 w-4',
    default: 'h-8 w-8',
    lg: 'h-12 w-12',
  }

  const content = (
    <div className={cn("flex flex-col items-center justify-center gap-3", className)}>
      <Loader2 className={cn("animate-spin text-primary", sizeClasses[size])} />
      {resolvedText && <p className="text-sm text-muted-foreground">{resolvedText}</p>}
    </div>
  )

  if (fullScreen) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-background">
        {content}
      </div>
    )
  }

  return content
}

export function LoadingPage() {
  const { t } = useTranslation()
  return (
    <div className="flex h-[calc(100vh-4rem)] items-center justify-center">
      <Loading size="lg" text={t('common.loadingPage')} />
    </div>
  )
}

export function LoadingCard() {
  const { t } = useTranslation()
  return (
    <div className="flex h-48 items-center justify-center rounded-xl border bg-card">
      <Loading size="default" text={t('common.loadingData')} />
    </div>
  )
}

export default Loading
