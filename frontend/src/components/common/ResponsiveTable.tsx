import { type ReactNode } from 'react'
import { cn } from '@/lib/utils'
import { ScrollArea, ScrollBar } from '@/components/ui/scroll-area'

interface ResponsiveTableProps {
    children: ReactNode
    className?: string
}

interface ResponsiveTableWrapperProps {
    children: ReactNode
    className?: string
}

export function ResponsiveTable({ children, className }: ResponsiveTableProps) {
  return (
    <ScrollArea className={cn("w-full", className)}>
      <div className="min-w-[600px]">
        {children}
      </div>
      <ScrollBar orientation="horizontal" />
    </ScrollArea>
  )
}

export function ResponsiveTableWrapper({ children, className }: ResponsiveTableWrapperProps) {
  return (
    <div className={cn(
      "relative w-full overflow-auto rounded-md border",
      "scrollbar-thin scrollbar-thumb-muted scrollbar-track-transparent",
      className
    )}>
      <div className="min-w-[600px]">
        {children}
      </div>
    </div>
  )
}

export default ResponsiveTable
