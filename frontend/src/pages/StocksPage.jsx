import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { MagnifyingGlassIcon, ArrowTrendingUpIcon, ArrowTrendingDownIcon } from '@heroicons/react/24/outline'
import { marketService } from '../services/marketService'
import Loading from '../components/common/Loading'

export default function StocksPage() {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['stocks', search, page],
    queryFn: () => marketService.getStocks({ search, page, size: 20 }),
  })

  const stocks = data?.data || []
  const pagination = data?.pagination

  return (
    <div className="space-y-6 animate-in">
      <div>
        <h1 className="text-2xl font-bold text-white">Hisse Senetleri</h1>
        <p className="text-dark-400">BIST hisse senetleri</p>
      </div>

      {/* Search */}
      <div className="relative max-w-md">
        <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-dark-500" />
        <input
          type="text"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            setPage(0)
          }}
          placeholder="Hisse ara (örn: THYAO, Türk Hava Yolları)..."
          className="input pl-10"
        />
      </div>

      {/* Table */}
      {isLoading ? (
        <Loading />
      ) : (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="table">
              <thead>
                <tr>
                  <th>Hisse</th>
                  <th className="text-right">Son Fiyat</th>
                  <th className="text-right">Değişim</th>
                  <th className="text-right">Önceki Kapanış</th>
                  <th className="text-right">Borsa</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {stocks.map((stock) => {
                  const isUp = stock.changePercent > 0
                  const isDown = stock.changePercent < 0
                  
                  return (
                    <tr key={stock.id}>
                      <td>
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-lg bg-dark-800 flex items-center justify-center">
                            <span className="text-xs font-bold text-dark-300">
                              {stock.symbol.slice(0, 3)}
                            </span>
                          </div>
                          <div>
                            <p className="font-medium text-white">{stock.symbol}</p>
                            <p className="text-dark-400 text-sm">{stock.name}</p>
                          </div>
                        </div>
                      </td>
                      <td className="text-right font-mono text-white font-medium">
                        {stock.currentPrice?.toFixed(2) || '—'}
                      </td>
                      <td className="text-right">
                        <div className={`flex items-center justify-end gap-1 ${
                          isUp ? 'price-up' : isDown ? 'price-down' : 'price-neutral'
                        }`}>
                          {isUp ? (
                            <ArrowTrendingUpIcon className="w-4 h-4" />
                          ) : isDown ? (
                            <ArrowTrendingDownIcon className="w-4 h-4" />
                          ) : null}
                          <span className="font-mono">
                            {stock.changePercent ? `${stock.changePercent > 0 ? '+' : ''}${stock.changePercent.toFixed(2)}%` : '—'}
                          </span>
                        </div>
                      </td>
                      <td className="text-right font-mono text-dark-400">
                        {stock.previousClose?.toFixed(2) || '—'}
                      </td>
                      <td className="text-right">
                        <span className="badge-info">{stock.exchange || 'BIST'}</span>
                      </td>
                      <td className="text-right">
                        <Link
                          to={`/market/stocks/${stock.symbol}`}
                          className="text-primary-400 hover:text-primary-300 text-sm"
                        >
                          Detay →
                        </Link>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          {stocks.length === 0 && (
            <div className="text-center py-12">
              <p className="text-dark-400">Sonuç bulunamadı</p>
            </div>
          )}

          {/* Pagination */}
          {pagination && pagination.totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t border-dark-800">
              <p className="text-dark-400 text-sm">
                Toplam {pagination.totalElements} sonuç
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={pagination.first}
                  className="btn-secondary text-sm"
                >
                  Önceki
                </button>
                <button
                  onClick={() => setPage(p => p + 1)}
                  disabled={pagination.last}
                  className="btn-secondary text-sm"
                >
                  Sonraki
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
