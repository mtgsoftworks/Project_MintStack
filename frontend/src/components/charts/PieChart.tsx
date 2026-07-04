import { useMemo } from 'react'
import {
  PieChart as RechartsPieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Legend,
  Tooltip,
} from 'recharts'
import { cn, formatCurrency, formatPercent } from '@/lib/utils'

interface PieChartDataItem {
  name: string
  value: number
  [key: string]: string | number
}

interface PieChartProps {
  data?: PieChartDataItem[]
  height?: number
  innerRadius?: number
  outerRadius?: number
  showLegend?: boolean
  centerTitle?: string
  centerSubTitle?: string
  className?: string
}

interface CustomTooltipPayloadItem {
  name: string
  value: number
  payload: PieChartDataItem & { percent: number }
}

interface CustomLegendPayloadItem {
  color: string
  value: string
}

// Modern vibrant palette
const CHART_COLORS = [
  '#0095E8', // Primary Blue
  '#50CD89', // Success Green
  '#FFC700', // Warning Yellow
  '#F1416C', // Danger Red
  '#7239EA', // Info Purple
  '#009EF7', // Light Blue
  '#F6C000', // Gold
  '#181C32', // Dark
]

function CustomTooltip({ active, payload }: { active?: boolean; payload?: CustomTooltipPayloadItem[] }) {
  if (active && payload && payload.length) {
    const data = payload[0]
    return (
      <div className="rounded-xl border bg-card/95 p-3 shadow-xl backdrop-blur">
        <p className="text-sm font-bold mb-1">{data.name}</p>
        <p className="text-sm font-semibold text-primary">
          {formatCurrency(data.value, 'TRY')}
        </p>
        <p className="text-xs text-muted-foreground mt-0.5">
          Oran: {formatPercent(data.payload.percent * 100)}
        </p>
      </div>
    )
  }
  return null
}

function CustomLegend({ payload = [] }: { payload?: CustomLegendPayloadItem[] }) {
  return (
    <div className="flex flex-wrap justify-center gap-3 pt-3">
      {payload.map((entry: CustomLegendPayloadItem, index: number) => (
        <div key={`legend-${index}`} className="flex items-center gap-2 px-2.5 py-1 rounded-full border bg-muted/40 text-xs">
          <div 
            className="w-2.5 h-2.5 rounded-full shrink-0" 
            style={{ backgroundColor: entry.color }}
          />
          <span className="font-semibold text-foreground">{entry.value}</span>
        </div>
      ))}
    </div>
  )
}

export function PieChart({
  data = [],
  height = 380,
  innerRadius = 90,
  outerRadius = 145,
  showLegend = true,
  centerTitle,
  centerSubTitle,
  className = '',
}: PieChartProps) {
  const total = useMemo(() => {
    return (data || []).reduce((sum: number, item: PieChartDataItem) => sum + (item.value || 0), 0)
  }, [data])

  const chartData: (PieChartDataItem & { percent: number })[] = useMemo(() => {
    return (data || []).map((item: PieChartDataItem) => ({
      ...item,
      percent: total > 0 ? item.value / total : 0,
    }))
  }, [data, total])

  if (!data || data.length === 0) {
    return (
      <div 
        className={cn("flex items-center justify-center text-muted-foreground", className)}
        style={{ height }}
      >
        Grafik verisi bulunamadı
      </div>
    )
  }

  return (
    <div className={cn("relative w-full flex flex-col items-center justify-center", className)} style={{ height }}>
      {/* Donut Center Overlay Label */}
      <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none pb-8 z-10">
        <p className="text-xs text-muted-foreground font-medium uppercase tracking-wider">
          {centerSubTitle || 'Portföy Dağılımı'}
        </p>
        <p className="text-xl sm:text-2xl font-extrabold text-foreground tracking-tight mt-0.5">
          {centerTitle || formatCurrency(total, 'TRY')}
        </p>
      </div>

      <ResponsiveContainer width="100%" height="100%">
        <RechartsPieChart>
          <Pie
            data={chartData}
            cx="50%"
            cy="50%"
            innerRadius={innerRadius}
            outerRadius={outerRadius}
            paddingAngle={3}
            dataKey="value"
            nameKey="name"
          >
            {chartData.map((_entry, index: number) => (
              <Cell 
                key={`cell-${index}`} 
                fill={CHART_COLORS[index % CHART_COLORS.length]}
                stroke="hsl(var(--background))"
                strokeWidth={3}
                className="hover:opacity-80 transition-opacity cursor-pointer"
              />
            ))}
          </Pie>
          <Tooltip content={<CustomTooltip />} />
          {showLegend && <Legend content={<CustomLegend />} />}
        </RechartsPieChart>
      </ResponsiveContainer>
    </div>
  )
}

export default PieChart
