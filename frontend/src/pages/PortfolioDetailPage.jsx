import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowLeftIcon, PlusIcon, TrashIcon, ArrowTrendingUpIcon, ArrowTrendingDownIcon } from '@heroicons/react/24/outline'
import { portfolioService } from '../services/portfolioService'
import { marketService } from '../services/marketService'
import Loading from '../components/common/Loading'
import PieChart from '../components/charts/PieChart'
import toast from 'react-hot-toast'

export default function PortfolioDetailPage() {
  const { id } = useParams()
  const [showAddModal, setShowAddModal] = useState(false)
  const [newItem, setNewItem] = useState({
    instrumentId: '',
    quantity: '',
    purchasePrice: '',
    purchaseDate: new Date().toISOString().split('T')[0],
  })
  const queryClient = useQueryClient()

  const { data: portfolio, isLoading } = useQuery({
    queryKey: ['portfolio', id],
    queryFn: () => portfolioService.getPortfolio(id),
  })

  const { data: stocksData } = useQuery({
    queryKey: ['allStocks'],
    queryFn: () => marketService.getStocks({ size: 100 }),
  })

  const addMutation = useMutation({
    mutationFn: (data) => portfolioService.addItem(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries(['portfolio', id])
      setShowAddModal(false)
      setNewItem({
        instrumentId: '',
        quantity: '',
        purchasePrice: '',
        purchaseDate: new Date().toISOString().split('T')[0],
      })
      toast.success('Enstrüman eklendi')
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Bir hata oluştu')
    },
  })

  const removeMutation = useMutation({
    mutationFn: (itemId) => portfolioService.removeItem(id, itemId),
    onSuccess: () => {
      queryClient.invalidateQueries(['portfolio', id])
      toast.success('Enstrüman kaldırıldı')
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Bir hata oluştu')
    },
  })

  const handleAdd = (e) => {
    e.preventDefault()
    addMutation.mutate({
      ...newItem,
      quantity: parseFloat(newItem.quantity),
      purchasePrice: parseFloat(newItem.purchasePrice),
    })
  }

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('tr-TR', {
      style: 'currency',
      currency: 'TRY',
    }).format(value || 0)
  }

  if (isLoading) {
    return <Loading />
  }

  if (!portfolio) {
    return (
      <div className="text-center py-12">
        <p className="text-dark-400">Portföy bulunamadı</p>
        <Link to="/portfolio" className="btn-primary mt-4">
          Portföylere Dön
        </Link>
      </div>
    )
  }

  const isUp = portfolio.profitLoss > 0
  const isDown = portfolio.profitLoss < 0

  const chartData = portfolio.items?.map(item => ({
    name: item.instrumentSymbol,
    value: item.currentValue || 0,
    percent: ((item.currentValue || 0) / (portfolio.totalValue || 1) * 100).toFixed(1),
  })) || []

  return (
    <div className="space-y-6 animate-in">
      {/* Back Button */}
      <Link
        to="/portfolio"
        className="inline-flex items-center gap-2 text-dark-400 hover:text-white transition-colors"
      >
        <ArrowLeftIcon className="w-4 h-4" />
        Portföylere Dön
      </Link>

      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-6">
        <div>
          <h1 className="text-2xl font-bold text-white">{portfolio.name}</h1>
          {portfolio.description && (
            <p className="text-dark-400 mt-1">{portfolio.description}</p>
          )}
        </div>
        <button
          onClick={() => setShowAddModal(true)}
          className="btn-primary flex items-center gap-2"
        >
          <PlusIcon className="w-5 h-5" />
          Enstrüman Ekle
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="stat-card">
          <p className="stat-label">Toplam Değer</p>
          <p className="stat-value">{formatCurrency(portfolio.totalValue)}</p>
        </div>
        <div className="stat-card">
          <p className="stat-label">Toplam Maliyet</p>
          <p className="stat-value">{formatCurrency(portfolio.totalCost)}</p>
        </div>
        <div className="stat-card">
          <p className="stat-label">Kar/Zarar</p>
          <p className={`stat-value ${isUp ? 'text-primary-400' : isDown ? 'text-red-400' : ''}`}>
            {formatCurrency(portfolio.profitLoss)}
          </p>
        </div>
        <div className="stat-card">
          <p className="stat-label">Kar/Zarar %</p>
          <div className={`flex items-center gap-2 ${
            isUp ? 'text-primary-400' : isDown ? 'text-red-400' : 'text-dark-400'
          }`}>
            {isUp ? <ArrowTrendingUpIcon className="w-5 h-5" /> : isDown ? <ArrowTrendingDownIcon className="w-5 h-5" /> : null}
            <span className="stat-value">
              {portfolio.profitLossPercent > 0 ? '+' : ''}{portfolio.profitLossPercent?.toFixed(2)}%
            </span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Items Table */}
        <div className="lg:col-span-2 card overflow-hidden">
          <div className="p-4 border-b border-dark-800">
            <h2 className="text-lg font-semibold text-white">Enstrümanlar</h2>
          </div>
          {portfolio.items?.length === 0 ? (
            <div className="p-12 text-center">
              <p className="text-dark-400 mb-4">Henüz enstrüman eklenmemiş</p>
              <button
                onClick={() => setShowAddModal(true)}
                className="btn-primary"
              >
                Enstrüman Ekle
              </button>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="table">
                <thead>
                  <tr>
                    <th>Enstrüman</th>
                    <th className="text-right">Miktar</th>
                    <th className="text-right">Alış Fiyatı</th>
                    <th className="text-right">Güncel Fiyat</th>
                    <th className="text-right">Kar/Zarar</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {portfolio.items.map((item) => {
                    const itemUp = item.profitLoss > 0
                    const itemDown = item.profitLoss < 0

                    return (
                      <tr key={item.id}>
                        <td>
                          <div>
                            <p className="font-medium text-white">{item.instrumentSymbol}</p>
                            <p className="text-dark-400 text-sm">{item.instrumentName}</p>
                          </div>
                        </td>
                        <td className="text-right font-mono text-dark-200">
                          {item.quantity}
                        </td>
                        <td className="text-right font-mono text-dark-200">
                          {formatCurrency(item.purchasePrice)}
                        </td>
                        <td className="text-right font-mono text-white">
                          {formatCurrency(item.currentPrice)}
                        </td>
                        <td className={`text-right font-mono ${
                          itemUp ? 'price-up' : itemDown ? 'price-down' : ''
                        }`}>
                          {formatCurrency(item.profitLoss)}
                          <span className="text-xs ml-1">
                            ({item.profitLossPercent > 0 ? '+' : ''}{item.profitLossPercent?.toFixed(1)}%)
                          </span>
                        </td>
                        <td>
                          <button
                            onClick={() => removeMutation.mutate(item.id)}
                            className="p-2 rounded-lg text-dark-500 hover:text-red-400 transition-colors"
                          >
                            <TrashIcon className="w-4 h-4" />
                          </button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Distribution Chart */}
        <div className="card p-6">
          <h2 className="text-lg font-semibold text-white mb-4">Dağılım</h2>
          {chartData.length > 0 ? (
            <PieChart data={chartData} height={250} />
          ) : (
            <p className="text-dark-400 text-center py-8">Veri yok</p>
          )}
        </div>
      </div>

      {/* Add Item Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="card p-6 w-full max-w-md animate-in">
            <h2 className="text-xl font-semibold text-white mb-4">Enstrüman Ekle</h2>
            <form onSubmit={handleAdd} className="space-y-4">
              <div>
                <label className="label">Enstrüman</label>
                <select
                  value={newItem.instrumentId}
                  onChange={(e) => setNewItem({ ...newItem, instrumentId: e.target.value })}
                  className="input"
                  required
                >
                  <option value="">Seçin...</option>
                  {stocksData?.data?.map((stock) => (
                    <option key={stock.id} value={stock.id}>
                      {stock.symbol} - {stock.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Miktar</label>
                  <input
                    type="number"
                    step="0.000001"
                    value={newItem.quantity}
                    onChange={(e) => setNewItem({ ...newItem, quantity: e.target.value })}
                    className="input"
                    required
                  />
                </div>
                <div>
                  <label className="label">Alış Fiyatı (₺)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={newItem.purchasePrice}
                    onChange={(e) => setNewItem({ ...newItem, purchasePrice: e.target.value })}
                    className="input"
                    required
                  />
                </div>
              </div>
              <div>
                <label className="label">Alış Tarihi</label>
                <input
                  type="date"
                  value={newItem.purchaseDate}
                  onChange={(e) => setNewItem({ ...newItem, purchaseDate: e.target.value })}
                  className="input"
                  required
                />
              </div>
              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="btn-secondary flex-1"
                >
                  İptal
                </button>
                <button
                  type="submit"
                  disabled={addMutation.isPending}
                  className="btn-primary flex-1"
                >
                  {addMutation.isPending ? 'Ekleniyor...' : 'Ekle'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
