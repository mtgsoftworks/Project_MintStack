import { Loader2 } from 'lucide-react'
import { cn } from '@/lib/utils'

export function Loading({ 
  size = 'default', 
  text = 'Yükleniyor...', 
  fullScreen = false,
  className 
}) {
  const sizeClasses = {
    sm: 'h-4 w-4',
    default: 'h-8 w-8',
    lg: 'h-12 w-12',
  }

  const content = (
    <div className={cn("flex flex-col items-center justify-center gap-3", className)}>
      <Loader2 className={cn("animate-spin text-primary", sizeClasses[size])} />
      {text && <p className="text-sm text-muted-foreground">{text}</p>}
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
  return (
    <div className="flex h-[calc(100vh-4rem)] items-center justify-center">
      <Loading size="lg" text="Sayfa yükleniyor..." />
    </div>
  )
}

export function LoadingCard() {
  return (
    <div className="flex h-48 items-center justify-center rounded-xl border bg-card">
      <Loading size="default" text="Veriler yükleniyor..." />
    </div>
  )
}

export default Loading
