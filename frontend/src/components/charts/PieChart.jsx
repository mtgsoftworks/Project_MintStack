import { 
  PieChart as RechartsPieChart, 
  Pie, 
  Cell, 
  ResponsiveContainer,
  Legend,
  Tooltip,
} from 'recharts'
import { cn, formatCurrency, formatPercent } from '@/lib/utils'

// Metronic color palette for pie charts
const CHART_COLORS = [
  '#0095E8', // Primary Blue
  '#50CD89', // Success Green
  '#F1416C', // Danger Red
  '#FFC700', // Warning Yellow
  '#7239EA', // Info Purple
  '#009EF7', // Light Blue
  '#E4E6EF', // Gray
  '#181C32', // Dark
]

function CustomTooltip({ active, payload }) {
  if (active && payload && payload.length) {
    const data = payload[0]
    return (
      <div className="rounded-lg border bg-card p-3 shadow-lg">
        <p className="text-sm font-semibold mb-1">{data.name}</p>
        <p className="text-sm text-muted-foreground">
          {formatCurrency(data.value, 'TRY')}
        </p>
        <p className="text-xs text-muted-foreground">
          {formatPercent(data.payload.percent * 100)}
        </p>
      </div>
    )
  }
  return null
}

function CustomLegend({ payload }) {
  return (
    <div className="flex flex-wrap justify-center gap-3 mt-4">
      {payload.map((entry, index) => (
        <div key={`legend-${index}`} className="flex items-center gap-2">
          <div 
            className="w-3 h-3 rounded-full" 
            style={{ backgroundColor: entry.color }}
          />
          <span className="text-sm text-muted-foreground">{entry.value}</span>
        </div>
      ))}
    </div>
  )
}

export function PieChart({ 
  data = [], 
  height = 300,
  innerRadius = 60,
  outerRadius = 100,
  showLegend = true,
  className,
}) {
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

  // Calculate total and percentages
  const total = data.reduce((sum, item) => sum + (item.value || 0), 0)
  const chartData = data.map(item => ({
    ...item,
    percent: total > 0 ? item.value / total : 0,
  }))

  return (
    <div className={cn("w-full", className)} style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        <RechartsPieChart>
          <Pie
            data={chartData}
            cx="50%"
            cy="50%"
            innerRadius={innerRadius}
            outerRadius={outerRadius}
            paddingAngle={2}
            dataKey="value"
            nameKey="name"
          >
            {chartData.map((entry, index) => (
              <Cell 
                key={`cell-${index}`} 
                fill={CHART_COLORS[index % CHART_COLORS.length]}
                stroke="hsl(var(--background))"
                strokeWidth={2}
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
