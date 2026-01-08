import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { analysisService } from '../services/analysisService'
import { marketService } from '../services/marketService'
import Loading from '../components/common/Loading'
import PriceChart from '../components/charts/PriceChart'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts'

export default function AnalysisPage() {
  const [selectedSymbols, setSelectedSymbols] = useState(['THYAO'])
  const [period, setPeriod] = useState(30)
  const [startDate, setStartDate] = useState(
    new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  )
  const [endDate, setEndDate] = useState(new Date().toISOString().split('T')[0])

  const { data: stocksData } = useQuery({
    queryKey: ['allStocks'],
    queryFn: () => marketService.getStocks({ size: 100 }),
  })

  const { data: maData, isLoading: maLoading } = useQuery({
    queryKey: ['multipleMA', selectedSymbols[0]],
    queryFn: () => analysisService.getMultipleMA(selectedSymbols[0]),
    enabled: selectedSymbols.length > 0,
  })

  const { data: trendData, isLoading: trendLoading } = useQuery({
    queryKey: ['trend', selectedSymbols[0], period],
    queryFn: () => analysisService.getTrend(selectedSymbols[0], period),
    enabled: selectedSymbols.length > 0,
  })

  const { data: compareData, isLoading: compareLoading } = useQuery({
    queryKey: ['compare', selectedSymbols, startDate, endDate],
    queryFn: () => analysisService.compare(selectedSymbols, startDate, endDate),
    enabled: selectedSymbols.length > 1,
  })

  const COLORS = ['#22c55e', '#f59e0b', '#3b82f6', '#ef4444', '#8b5cf6']

  return (
    <div className="space-y-6 animate-in">
      <div>
        <h1 className="text-2xl font-bold text-white">Teknik Analiz</h1>
        <p className="text-dark-400">Hareketli ortalamalar, trend analizi ve karşılaştırma</p>
      </div>

      {/* Controls */}
      <div className="card p-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div>
            <label className="label">Enstrüman Seçin</label>
            <select
              value={selectedSymbols[0] || ''}
              onChange={(e) => setSelectedSymbols([e.target.value])}
              className="input"
            >
              {stocksData?.data?.map((stock) => (
                <option key={stock.id} value={stock.symbol}>
                  {stock.symbol} - {stock.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="label">Karşılaştır (Opsiyonel)</label>
            <select
              value={selectedSymbols[1] || ''}
              onChange={(e) => {
                if (e.target.value) {
                  setSelectedSymbols([selectedSymbols[0], e.target.value])
                } else {
                  setSelectedSymbols([selectedSymbols[0]])
                }
              }}
              className="input"
            >
              <option value="">Seçin...</option>
              {stocksData?.data
                ?.filter((s) => s.symbol !== selectedSymbols[0])
                .map((stock) => (
                  <option key={stock.id} value={stock.symbol}>
                    {stock.symbol}
                  </option>
                ))}
            </select>
          </div>

          <div>
            <label className="label">Başlangıç Tarihi</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="input"
            />
          </div>

          <div>
            <label className="label">Bitiş Tarihi</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="input"
            />
          </div>
        </div>
      </div>

      {/* Trend Analysis */}
      {trendLoading ? (
        <Loading />
      ) : trendData && (
        <div className="card p-6">
          <h2 className="text-lg font-semibold text-white mb-4">
            {selectedSymbols[0]} - Trend Analizi ({period} Gün)
          </h2>
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4 mb-6">
            <div>
              <p className="text-dark-400 text-sm">Trend</p>
              <p className={`font-semibold ${
                trendData.trend === 'UPTREND' ? 'text-primary-400' :
                trendData.trend === 'DOWNTREND' ? 'text-red-400' : 'text-dark-400'
              }`}>
                {trendData.trend === 'UPTREND' ? '↑ Yükseliş' :
                 trendData.trend === 'DOWNTREND' ? '↓ Düşüş' : '→ Yatay'}
              </p>
            </div>
            <div>
              <p className="text-dark-400 text-sm">Güç</p>
              <p className="text-white">{trendData.trendStrength}</p>
            </div>
            <div>
              <p className="text-dark-400 text-sm">Değişim</p>
              <p className={`${
                trendData.changePercent > 0 ? 'text-primary-400' :
                trendData.changePercent < 0 ? 'text-red-400' : 'text-dark-400'
              }`}>
                {trendData.changePercent > 0 ? '+' : ''}{trendData.changePercent?.toFixed(2)}%
              </p>
            </div>
            <div>
              <p className="text-dark-400 text-sm">Volatilite</p>
              <p className="text-white">{trendData.volatility?.toFixed(4)}</p>
            </div>
            <div>
              <p className="text-dark-400 text-sm">En Yüksek</p>
              <p className="text-white">₺{trendData.highPrice?.toFixed(2)}</p>
            </div>
            <div>
              <p className="text-dark-400 text-sm">En Düşük</p>
              <p className="text-white">₺{trendData.lowPrice?.toFixed(2)}</p>
            </div>
          </div>
        </div>
      )}

      {/* Moving Averages Chart */}
      {maLoading ? (
        <Loading />
      ) : maData?.data && (
        <div className="card p-6">
          <h2 className="text-lg font-semibold text-white mb-4">
            {selectedSymbols[0]} - Hareketli Ortalamalar
          </h2>
          <ResponsiveContainer width="100%" height={400}>
            <LineChart data={maData.data}>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
              <XAxis
                dataKey="date"
                stroke="#64748b"
                tick={{ fill: '#64748b', fontSize: 12 }}
              />
              <YAxis
                stroke="#64748b"
                tick={{ fill: '#64748b', fontSize: 12 }}
                domain={['auto', 'auto']}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: '#1e293b',
                  border: '1px solid #334155',
                  borderRadius: '8px',
                }}
              />
              <Legend />
              <Line
                type="monotone"
                dataKey="price"
                stroke="#ffffff"
                strokeWidth={2}
                dot={false}
                name="Fiyat"
              />
              <Line
                type="monotone"
                dataKey="ma7"
                stroke="#22c55e"
                strokeWidth={1}
                dot={false}
                name="MA7"
              />
              <Line
                type="monotone"
                dataKey="ma25"
                stroke="#f59e0b"
                strokeWidth={1}
                dot={false}
                name="MA25"
              />
              <Line
                type="monotone"
                dataKey="ma99"
                stroke="#3b82f6"
                strokeWidth={1}
                dot={false}
                name="MA99"
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Comparison Chart */}
      {selectedSymbols.length > 1 && (
        <div className="card p-6">
          <h2 className="text-lg font-semibold text-white mb-4">
            Karşılaştırma: {selectedSymbols.join(' vs ')}
          </h2>
          {compareLoading ? (
            <Loading />
          ) : compareData?.instruments && (
            <ResponsiveContainer width="100%" height={400}>
              <LineChart>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis
                  dataKey="date"
                  stroke="#64748b"
                  tick={{ fill: '#64748b', fontSize: 12 }}
                  allowDuplicatedCategory={false}
                />
                <YAxis
                  stroke="#64748b"
                  tick={{ fill: '#64748b', fontSize: 12 }}
                  tickFormatter={(v) => `${v.toFixed(0)}%`}
                />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#1e293b',
                    border: '1px solid #334155',
                    borderRadius: '8px',
                  }}
                  formatter={(value) => [`${value.toFixed(2)}%`, 'Değişim']}
                />
                <Legend />
                {compareData.instruments.map((instrument, index) => (
                  <Line
                    key={instrument.symbol}
                    data={instrument.data}
                    type="monotone"
                    dataKey="value"
                    stroke={COLORS[index % COLORS.length]}
                    strokeWidth={2}
                    dot={false}
                    name={instrument.symbol}
                  />
                ))}
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>
      )}
    </div>
  )
}
