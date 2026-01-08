import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { MagnifyingGlassIcon, ArrowPathIcon } from '@heroicons/react/24/outline'
import { marketService } from '../services/marketService'
import Loading from '../components/common/Loading'

export default function CurrencyPage() {
  const [search, setSearch] = useState('')

  const { data: currencies, isLoading, refetch, isFetching } = useQuery({
    queryKey: ['currencies'],
    queryFn: marketService.getCurrencies,
  })

  const filteredCurrencies = currencies?.filter(
    (c) =>
      c.currencyCode.toLowerCase().includes(search.toLowerCase()) ||
      c.currencyName?.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="space-y-6 animate-in">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Döviz Kurları</h1>
          <p className="text-dark-400">TCMB güncel döviz kurları</p>
        </div>
        <button
          onClick={() => refetch()}
          disabled={isFetching}
          className="btn-secondary flex items-center gap-2"
        >
          <ArrowPathIcon className={`w-4 h-4 ${isFetching ? 'animate-spin' : ''}`} />
          Yenile
        </button>
      </div>

      {/* Search */}
      <div className="relative max-w-md">
        <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-dark-500" />
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Döviz ara..."
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
                  <th>Döviz</th>
                  <th className="text-right">Alış</th>
                  <th className="text-right">Satış</th>
                  <th className="text-right">Efektif Alış</th>
                  <th className="text-right">Efektif Satış</th>
                  <th className="text-right">Kaynak</th>
                </tr>
              </thead>
              <tbody>
                {filteredCurrencies?.map((currency) => (
                  <tr key={currency.currencyCode}>
                    <td>
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-full bg-dark-800 flex items-center justify-center">
                          <span className="text-sm font-medium text-dark-300">
                            {currency.currencyCode}
                          </span>
                        </div>
                        <div>
                          <p className="font-medium text-white">{currency.currencyCode}/TRY</p>
                          <p className="text-dark-400 text-sm">{currency.currencyName}</p>
                        </div>
                      </div>
                    </td>
                    <td className="text-right font-mono text-dark-200">
                      {currency.buyingRate?.toFixed(4)}
                    </td>
                    <td className="text-right font-mono text-white font-medium">
                      {currency.sellingRate?.toFixed(4)}
                    </td>
                    <td className="text-right font-mono text-dark-400">
                      {currency.effectiveBuyingRate?.toFixed(4) || '—'}
                    </td>
                    <td className="text-right font-mono text-dark-400">
                      {currency.effectiveSellingRate?.toFixed(4) || '—'}
                    </td>
                    <td className="text-right">
                      <span className="badge-info">{currency.source}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          
          {filteredCurrencies?.length === 0 && (
            <div className="text-center py-12">
              <p className="text-dark-400">Sonuç bulunamadı</p>
            </div>
          )}
        </div>
      )}

      {/* Last Update Info */}
      {currencies?.[0]?.fetchedAt && (
        <p className="text-dark-500 text-sm text-center">
          Son güncelleme: {new Date(currencies[0].fetchedAt).toLocaleString('tr-TR')}
        </p>
      )}
    </div>
  )
}
