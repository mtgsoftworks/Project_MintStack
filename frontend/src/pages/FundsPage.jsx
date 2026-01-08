import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { MagnifyingGlassIcon } from '@heroicons/react/24/outline'
import { marketService } from '../services/marketService'
import Loading from '../components/common/Loading'

export default function FundsPage() {
  const [search, setSearch] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['funds', search],
    queryFn: () => marketService.getFunds({ search, size: 50 }),
  })

  const funds = data?.data || []

  return (
    <div className="space-y-6 animate-in">
      <div>
        <h1 className="text-2xl font-bold text-white">Yatırım Fonları</h1>
        <p className="text-dark-400">Borsa yatırım fonları</p>
      </div>

      <div className="relative max-w-md">
        <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-dark-500" />
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Fon ara..."
          className="input pl-10"
        />
      </div>

      {isLoading ? (
        <Loading />
      ) : (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="table">
              <thead>
                <tr>
                  <th>Fon</th>
                  <th className="text-right">Son Fiyat</th>
                  <th className="text-right">Değişim</th>
                  <th className="text-right">Para Birimi</th>
                  <th className="text-right">Durum</th>
                </tr>
              </thead>
              <tbody>
                {funds.map((fund) => (
                  <tr key={fund.id}>
                    <td>
                      <div>
                        <p className="font-medium text-white">{fund.symbol}</p>
                        <p className="text-dark-400 text-sm">{fund.name}</p>
                      </div>
                    </td>
                    <td className="text-right font-mono text-white">
                      {fund.currentPrice?.toFixed(4) || '—'}
                    </td>
                    <td className={`text-right font-mono ${
                      fund.changePercent > 0 ? 'price-up' :
                      fund.changePercent < 0 ? 'price-down' : 'price-neutral'
                    }`}>
                      {fund.changePercent ? `${fund.changePercent > 0 ? '+' : ''}${fund.changePercent.toFixed(2)}%` : '—'}
                    </td>
                    <td className="text-right">
                      <span className="badge-info">{fund.currency}</span>
                    </td>
                    <td className="text-right">
                      {fund.isActive ? (
                        <span className="badge-success">Aktif</span>
                      ) : (
                        <span className="badge-danger">Pasif</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {funds.length === 0 && (
            <div className="text-center py-12">
              <p className="text-dark-400">Sonuç bulunamadı</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
