import { useQuery } from '@tanstack/react-query'
import { marketService } from '../services/marketService'
import Loading from '../components/common/Loading'

export default function ViopPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['viop'],
    queryFn: () => marketService.getViop({ size: 50 }),
  })

  const viop = data?.data || []

  return (
    <div className="space-y-6 animate-in">
      <div>
        <h1 className="text-2xl font-bold text-white">VIOP</h1>
        <p className="text-dark-400">Vadeli İşlem ve Opsiyon Piyasası</p>
      </div>

      {isLoading ? (
        <Loading />
      ) : (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="table">
              <thead>
                <tr>
                  <th>Kontrat</th>
                  <th className="text-right">Son Fiyat</th>
                  <th className="text-right">Değişim</th>
                  <th className="text-right">Borsa</th>
                  <th className="text-right">Durum</th>
                </tr>
              </thead>
              <tbody>
                {viop.map((contract) => (
                  <tr key={contract.id}>
                    <td>
                      <div>
                        <p className="font-medium text-white">{contract.symbol}</p>
                        <p className="text-dark-400 text-sm">{contract.name}</p>
                      </div>
                    </td>
                    <td className="text-right font-mono text-white">
                      {contract.currentPrice?.toFixed(2) || '—'}
                    </td>
                    <td className={`text-right font-mono ${
                      contract.changePercent > 0 ? 'price-up' :
                      contract.changePercent < 0 ? 'price-down' : 'price-neutral'
                    }`}>
                      {contract.changePercent ? `${contract.changePercent > 0 ? '+' : ''}${contract.changePercent.toFixed(2)}%` : '—'}
                    </td>
                    <td className="text-right">
                      <span className="badge-info">{contract.exchange || 'VIOP'}</span>
                    </td>
                    <td className="text-right">
                      {contract.isActive ? (
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

          {viop.length === 0 && (
            <div className="text-center py-12">
              <p className="text-dark-400">Veri bulunamadı</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
