import { useQuery } from '@tanstack/react-query'
import { marketService } from '../services/marketService'
import Loading from '../components/common/Loading'

export default function BondsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['bonds'],
    queryFn: () => marketService.getBonds({ size: 50 }),
  })

  const bonds = data?.data || []

  return (
    <div className="space-y-6 animate-in">
      <div>
        <h1 className="text-2xl font-bold text-white">Tahvil / Bono</h1>
        <p className="text-dark-400">Devlet tahvilleri ve bonolar</p>
      </div>

      {isLoading ? (
        <Loading />
      ) : (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="table">
              <thead>
                <tr>
                  <th>Enstrüman</th>
                  <th className="text-right">Son Fiyat</th>
                  <th className="text-right">Değişim</th>
                  <th className="text-right">Para Birimi</th>
                  <th className="text-right">Durum</th>
                </tr>
              </thead>
              <tbody>
                {bonds.map((bond) => (
                  <tr key={bond.id}>
                    <td>
                      <div>
                        <p className="font-medium text-white">{bond.symbol}</p>
                        <p className="text-dark-400 text-sm">{bond.name}</p>
                      </div>
                    </td>
                    <td className="text-right font-mono text-white">
                      {bond.currentPrice?.toFixed(4) || '—'}
                    </td>
                    <td className={`text-right font-mono ${
                      bond.changePercent > 0 ? 'price-up' :
                      bond.changePercent < 0 ? 'price-down' : 'price-neutral'
                    }`}>
                      {bond.changePercent ? `${bond.changePercent > 0 ? '+' : ''}${bond.changePercent.toFixed(2)}%` : '—'}
                    </td>
                    <td className="text-right">
                      <span className="badge-info">{bond.currency}</span>
                    </td>
                    <td className="text-right">
                      {bond.isActive ? (
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

          {bonds.length === 0 && (
            <div className="text-center py-12">
              <p className="text-dark-400">Veri bulunamadı</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
