import { 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  Area,
  AreaChart,
} from 'recharts'
import { cn, formatCurrency, formatDate } from '@/lib/utils'

// Metronic color palette for charts
const CHART_COLORS = {
  primary: '#0095E8',
  success: '#50CD89',
  danger: '#F1416C',
  warning: '#FFC700',
  info: '#7239EA',
  secondary: '#A1A5B7',
  gradient: {
    primary: ['#0095E8', '#00D9E8'],
    success: ['#50CD89', '#89E8B8'],
  }
}

function CustomTooltip({ active, payload, label }) {
  if (active && payload && payload.length) {
    return (
      <div className="rounded-lg border bg-card p-3 shadow-lg">
        <p className="text-xs text-muted-foreground mb-1">{formatDate(label)}</p>
        {payload.map((item, index) => (
          <p key={index} className="text-sm font-semibold" style={{ color: item.color }}>
            {item.name}: {formatCurrency(item.value, 'TRY')}
          </p>
        ))}
      </div>
    )
  }
  return null
}

export function PriceChart({ 
  data = [], 
  dataKey = 'price',
  showArea = true,
  showGrid = true,
  color = CHART_COLORS.primary,
  height = 300,
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

  // Format data for chart
  const chartData = data.map(item => ({
    ...item,
    date: item.date || item.timestamp,
    price: item.price || item.close || item.value,
  }))

  const minValue = Math.min(...chartData.map(d => d.price)) * 0.99
  const maxValue = Math.max(...chartData.map(d => d.price)) * 1.01

  if (showArea) {
    return (
      <div className={cn("w-full", className)} style={{ height }}>
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id="colorPrice" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor={color} stopOpacity={0.3} />
                <stop offset="95%" stopColor={color} stopOpacity={0} />
              </linearGradient>
            </defs>
            {showGrid && (
              <CartesianGrid 
                strokeDasharray="3 3" 
                stroke="hsl(var(--border))" 
                vertical={false}
              />
            )}
            <XAxis 
              dataKey="date" 
              tickFormatter={(value) => {
                const date = new Date(value)
                return `${date.getDate()}/${date.getMonth() + 1}`
              }}
              stroke="hsl(var(--muted-foreground))"
              fontSize={12}
              tickLine={false}
              axisLine={false}
            />
            <YAxis 
              domain={[minValue, maxValue]}
              tickFormatter={(value) => value.toLocaleString('tr-TR')}
              stroke="hsl(var(--muted-foreground))"
              fontSize={12}
              tickLine={false}
              axisLine={false}
              width={60}
            />
            <Tooltip content={<CustomTooltip />} />
            <Area 
              type="monotone" 
              dataKey="price" 
              name="Fiyat"
              stroke={color} 
              strokeWidth={2}
              fill="url(#colorPrice)" 
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    )
  }

  return (
    <div className={cn("w-full", className)} style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
          {showGrid && (
            <CartesianGrid 
              strokeDasharray="3 3" 
              stroke="hsl(var(--border))" 
              vertical={false}
            />
          )}
          <XAxis 
            dataKey="date" 
            tickFormatter={(value) => {
              const date = new Date(value)
              return `${date.getDate()}/${date.getMonth() + 1}`
            }}
            stroke="hsl(var(--muted-foreground))"
            fontSize={12}
            tickLine={false}
            axisLine={false}
          />
          <YAxis 
            domain={[minValue, maxValue]}
            tickFormatter={(value) => value.toLocaleString('tr-TR')}
            stroke="hsl(var(--muted-foreground))"
            fontSize={12}
            tickLine={false}
            axisLine={false}
            width={60}
          />
          <Tooltip content={<CustomTooltip />} />
          <Line 
            type="monotone" 
            dataKey="price" 
            name="Fiyat"
            stroke={color} 
            strokeWidth={2}
            dot={false}
            activeDot={{ r: 6, strokeWidth: 2 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}

export default PriceChart
