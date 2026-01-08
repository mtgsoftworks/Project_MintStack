import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeftIcon, ArrowTrendingUpIcon, ArrowTrendingDownIcon } from '@heroicons/react/24/outline'
import { marketService } from '../services/marketService'
import { analysisService } from '../services/analysisService'
import Loading from '../components/common/Loading'
import PriceChart from '../components/charts/PriceChart'

const TIME_RANGES = [
  { label: '1H', days: 7 },
  { label: '1A', days: 30 },
  { label: '3A', days: 90 },
  { label: '6A', days: 180 },
  { label: '1Y', days: 365 },
]

export default function StockDetailPage() {
  const { symbol } = useParams()
  const [selectedRange, setSelectedRange] = useState(30)

  const { data: stock, isLoading: stockLoading } = useQuery({
    queryKey: ['stock', symbol],
    queryFn: () => marketService.getStock(symbol),
  })

  const { data: history, isLoading: historyLoading } = useQuery({
    queryKey: ['stockHistory', symbol, selectedRange],
    queryFn: () => marketService.getStockHistory(symbol, { days: selectedRange }),
  })

  const { data: trend } = useQuery({
    queryKey: ['trend', symbol, selectedRange],
    queryFn: () => analysisService.getTrend(symbol, selectedRange),
    enabled: !!symbol,
  })

  if (stockLoading) {
    return <Loading />
  }

  if (!stock) {
    return (
      <div className="text-center py-12">
        <p className="text-dark-400">Hisse bulunamadı</p>
        <Link to="/market/stocks" className="btn-primary mt-4">
          Geri Dön
        </Link>
      </div>
    )
  }

  const isUp = stock.changePercent > 0
  const isDown = stock.changePercent < 0

  const chartData = history?.map(h => ({
    date: h.date,
    close: h.close,
  })) || []

  return (
    <div className="space-y-6 animate-in">
      {/* Back Button */}
      <Link
        to="/market/stocks"
        className="inline-flex items-center gap-2 text-dark-400 hover:text-white transition-colors"
      >
        <ArrowLeftIcon className="w-4 h-4" />
        Hisselere Dön
      </Link>

      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-6">
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 rounded-xl bg-dark-800 flex items-center justify-center">
            <span className="text-xl font-bold text-dark-300">{stock.symbol.slice(0, 3)}</span>
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">{stock.symbol}</h1>
            <p className="text-dark-400">{stock.name}</p>
          </div>
        </div>

        <div className="flex flex-col items-end">
          <p className="text-3xl font-bold text-white">
            ₺{stock.currentPrice?.toFixed(2) || '—'}
          </p>
          <div className={`flex items-center gap-2 mt-1 ${
            isUp ? 'price-up' : isDown ? 'price-down' : 'price-neutral'
          }`}>
            {isUp ? (
              <ArrowTrendingUpIcon className="w-5 h-5" />
            ) : isDown ? (
              <ArrowTrendingDownIcon className="w-5 h-5" />
            ) : null}
            <span className="font-medium">
              {stock.change ? `${stock.change > 0 ? '+' : ''}₺${stock.change.toFixed(2)}` : '—'}
              {stock.changePercent ? ` (${stock.changePercent > 0 ? '+' : ''}${stock.changePercent.toFixed(2)}%)` : ''}
            </span>
          </div>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="stat-card">
          <p className="stat-label">Önceki Kapanış</p>
          <p className="stat-value text-xl">₺{stock.previousClose?.toFixed(2) || '—'}</p>
        </div>
        <div className="stat-card">
          <p className="stat-label">Borsa</p>
          <p className="stat-value text-xl">{stock.exchange || 'BIST'}</p>
        </div>
        <div className="stat-card">
          <p className="stat-label">Trend</p>
          <p className={`stat-value text-xl ${
            trend?.trend === 'UPTREND' ? 'text-primary-400' : 
            trend?.trend === 'DOWNTREND' ? 'text-red-400' : 'text-dark-400'
          }`}>
            {trend?.trend === 'UPTREND' ? 'Yükseliş' : 
             trend?.trend === 'DOWNTREND' ? 'Düşüş' : 'Yatay'}
          </p>
        </div>
        <div className="stat-card">
          <p className="stat-label">Volatilite</p>
          <p className="stat-value text-xl">{trend?.volatility?.toFixed(2) || '—'}</p>
        </div>
      </div>

      {/* Chart */}
      <div className="card p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold text-white">Fiyat Grafiği</h2>
          <div className="flex gap-2">
            {TIME_RANGES.map((range) => (
              <button
                key={range.days}
                onClick={() => setSelectedRange(range.days)}
                className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
                  selectedRange === range.days
                    ? 'bg-primary-500 text-white'
                    : 'bg-dark-800 text-dark-400 hover:text-white'
                }`}
              >
                {range.label}
              </button>
            ))}
          </div>
        </div>
        
        {historyLoading ? (
          <Loading />
        ) : chartData.length > 0 ? (
          <PriceChart 
            data={chartData} 
            height={400}
            color={isUp ? '#22c55e' : isDown ? '#ef4444' : '#64748b'}
            formatValue={(v) => `₺${v?.toFixed(2)}`}
          />
        ) : (
          <div className="text-center py-12">
            <p className="text-dark-400">Grafik verisi bulunamadı</p>
          </div>
        )}
      </div>

      {/* Trend Analysis */}
      {trend && (
        <div className="card p-6">
          <h2 className="text-lg font-semibold text-white mb-4">Trend Analizi</h2>
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 text-sm">
            <div>
              <p className="text-dark-400">Başlangıç Fiyatı</p>
              <p className="text-white font-medium">₺{trend.startPrice?.toFixed(2)}</p>
            </div>
            <div>
              <p className="text-dark-400">Bitiş Fiyatı</p>
              <p className="text-white font-medium">₺{trend.endPrice?.toFixed(2)}</p>
            </div>
            <div>
              <p className="text-dark-400">En Yüksek</p>
              <p className="text-white font-medium">₺{trend.highPrice?.toFixed(2)}</p>
            </div>
            <div>
              <p className="text-dark-400">En Düşük</p>
              <p className="text-white font-medium">₺{trend.lowPrice?.toFixed(2)}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
