import { useRef } from 'react'
import { useVirtualizer } from '@tanstack/react-virtual'
import { cn } from '@/lib/utils'

/** Column definition for VirtualTable */
export interface TableColumn<T = Record<string, unknown>> {
  header: React.ReactNode
  accessor: keyof T
  width?: string | number
  className?: string
  render?: (item: T, index: number) => React.ReactNode
}

/** Props for VirtualTable component */
export interface VirtualTableProps<T = Record<string, unknown>> {
  data: T[]
  columns: TableColumn<T>[]
  rowHeight?: number
  containerHeight?: number
  renderRow?: (item: T, index: number) => React.ReactNode
  renderEmptyState?: () => React.ReactNode
  className?: string
  [key: string]: unknown
}

/**
 * VirtualTable component for rendering large tables efficiently
 * Only renders visible rows for better performance with 500+ items
 */
export function VirtualTable<T extends Record<string, unknown>>({
  data,
  columns,
  rowHeight = 56,
  containerHeight = 600,
  renderRow,
  renderEmptyState,
  className,
  ...props
}: VirtualTableProps<T>) {
  const parentRef = useRef<HTMLDivElement>(null)

  const virtualizer = useVirtualizer({
    count: data.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => rowHeight,
    overscan: 5,
  })

  const items = virtualizer.getVirtualItems()
  const totalSize = virtualizer.getTotalSize()

  if (data.length === 0 && renderEmptyState) {
    return renderEmptyState()
  }

  return (
    <div className={cn("w-full", className)} {...props}>
      {/* Table Header - Always visible */}
      <div className="border rounded-t-lg bg-muted/50">
        <div className="flex items-center h-12 px-4 gap-4 font-medium text-sm text-muted-foreground">
          {columns.map((column, index) => (
            <div
              key={index}
              className={cn("flex-shrink-0", column.className)}
              style={{ width: column.width }}
            >
              {column.header}
            </div>
          ))}
        </div>
      </div>

      {/* Virtualized Body */}
      <div
        ref={parentRef}
        className="border border-t-0 rounded-b-lg overflow-auto"
        style={{ height: containerHeight }}
      >
        <div
          style={{
            height: `${totalSize}px`,
            width: '100%',
            position: 'relative',
          }}
        >
          {items.map((virtualRow) => {
            const item = data[virtualRow.index]
            return (
              <div
                key={virtualRow.key}
                style={{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  width: '100%',
                  height: `${virtualRow.size}px`,
                  transform: `translateY(${virtualRow.start}px)`,
                }}
                className="flex items-center px-4 gap-4 border-b last:border-b-0 hover:bg-muted/50 transition-colors"
              >
                {renderRow ? (
                  renderRow(item, virtualRow.index)
                ) : (
                  columns.map((column, colIndex) => (
                    <div
                      key={colIndex}
                      className={cn("flex-shrink-0 truncate", column.className)}
                      style={{ width: column.width }}
                    >
                      {column.render
                        ? column.render(item, virtualRow.index)
                        : String(item[column.accessor] ?? '')}
                    </div>
                  ))
                )}
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}

/** Props for VirtualGrid component */
export interface VirtualGridProps<T = Record<string, unknown>> {
  data: T[]
  renderItem: (item: T, index: number) => React.ReactNode
  renderEmptyState?: () => React.ReactNode
  itemHeight?: number
  columns?: number
  gap?: number
  containerHeight?: number
  className?: string
  [key: string]: unknown
}

/**
 * VirtualGrid component for rendering large grids efficiently
 * Useful for news cards, image galleries, etc.
 */
export function VirtualGrid<T extends Record<string, unknown>>({
  data,
  renderItem,
  renderEmptyState,
  itemHeight = 280,
  columns = 3,
  gap = 16,
  containerHeight = 800,
  className,
  ...props
}: VirtualGridProps<T>) {
  const parentRef = useRef<HTMLDivElement>(null)

  // Calculate rows based on columns
  const rowCount = Math.ceil(data.length / columns)

  const virtualizer = useVirtualizer({
    count: rowCount,
    getScrollElement: () => parentRef.current,
    estimateSize: () => itemHeight + gap,
    overscan: 2,
  })

  const items = virtualizer.getVirtualItems()
  const totalSize = virtualizer.getTotalSize()

  if (data.length === 0 && renderEmptyState) {
    return renderEmptyState()
  }

  return (
    <div
      ref={parentRef}
      className={cn("overflow-auto", className)}
      style={{ height: containerHeight }}
      {...props}
    >
      <div
        style={{
          height: `${totalSize}px`,
          width: '100%',
          position: 'relative',
        }}
      >
        {items.map((virtualRow) => {
          const startIndex = virtualRow.index * columns
          const rowItems = data.slice(startIndex, startIndex + columns)

          return (
            <div
              key={virtualRow.key}
              className="grid"
              style={{
                display: 'grid',
                gridTemplateColumns: `repeat(${columns}, 1fr)`,
                gap: gap,
                position: 'absolute',
                top: 0,
                left: 0,
                width: '100%',
                transform: `translateY(${virtualRow.start}px)`,
              }}
            >
              {rowItems.map((item, colIndex) => (
                <div key={startIndex + colIndex} style={{ height: itemHeight }}>
                  {renderItem(item, startIndex + colIndex)}
                </div>
              ))}
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default VirtualTable
