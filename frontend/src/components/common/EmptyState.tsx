import React from 'react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import {
  FolderOpen,
  Bell,
  LineChart,
  Package,
  AlertCircle,
  Plus,
  Search
} from 'lucide-react'

type IconName = 'folder' | 'bell' | 'chart' | 'package' | 'alert' | 'search'

interface EmptyStateProps {
  icon?: IconName
  title: string
  description?: string
  actionLabel?: string
  onAction?: () => void
  className?: string
}

const icons: Record<IconName, React.ComponentType<{ className?: string }>> = {
  folder: FolderOpen,
  bell: Bell,
  chart: LineChart,
  package: Package,
  alert: AlertCircle,
  search: Search
}

export function EmptyState({
  icon = 'folder',
  title,
  description,
  actionLabel,
  onAction,
  className
}: EmptyStateProps) {
  const IconComponent = icons[icon] || FolderOpen

  return (
    <div className={cn(
      "flex flex-col items-center justify-center py-12 px-4 text-center",
      className
    )}>
      <div className="rounded-full bg-muted p-4 mb-4">
        <IconComponent className="h-8 w-8 text-muted-foreground" />
      </div>
      <h3 className="text-lg font-semibold mb-2">{title}</h3>
      {description && (
        <p className="text-sm text-muted-foreground max-w-sm mb-4">
          {description}
        </p>
      )}
      {actionLabel && onAction && (
        <Button onClick={onAction}>
          <Plus className="h-4 w-4 mr-2" />
          {actionLabel}
        </Button>
      )}
    </div>
  )
}

export default EmptyState
