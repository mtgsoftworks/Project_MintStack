import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { PlusIcon, TrashIcon, ArrowTrendingUpIcon, ArrowTrendingDownIcon } from '@heroicons/react/24/outline'
import { portfolioService } from '../services/portfolioService'
import Loading from '../components/common/Loading'
import toast from 'react-hot-toast'

export default function PortfolioPage() {
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [newPortfolio, setNewPortfolio] = useState({ name: '', description: '' })
  const queryClient = useQueryClient()

  const { data: portfolios, isLoading, error } = useQuery({
    queryKey: ['portfolios'],
    queryFn: portfolioService.getPortfolios,
    retry: 1,
  })

  const createMutation = useMutation({
    mutationFn: portfolioService.createPortfolio,
    onSuccess: () => {
      queryClient.invalidateQueries(['portfolios'])
      setShowCreateModal(false)
      setNewPortfolio({ name: '', description: '' })
      toast.success('Portföy oluşturuldu')
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Bir hata oluştu')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: portfolioService.deletePortfolio,
    onSuccess: () => {
      queryClient.invalidateQueries(['portfolios'])
      toast.success('Portföy silindi')
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Bir hata oluştu')
    },
  })

  const handleCreate = (e) => {
    e.preventDefault()
    createMutation.mutate(newPortfolio)
  }

  const handleDelete = (id, name) => {
    if (window.confirm(`"${name}" portföyünü silmek istediğinize emin misiniz?`)) {
      deleteMutation.mutate(id)
    }
  }

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('tr-TR', {
      style: 'currency',
      currency: 'TRY',
    }).format(value || 0)
  }

  return (
    <div className="space-y-6 animate-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Portföylerim</h1>
          <p className="text-dark-400">Yatırım portföylerinizi yönetin</p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="btn-primary flex items-center gap-2"
        >
          <PlusIcon className="w-5 h-5" />
          Yeni Portföy
        </button>
      </div>

      {isLoading ? (
        <Loading />
      ) : error ? (
        <div className="card p-12 text-center">
          <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-red-500/10 flex items-center justify-center">
            <span className="text-3xl">⚠️</span>
          </div>
          <h3 className="text-xl font-semibold text-white mb-2">Portföyler yüklenemedi</h3>
          <p className="text-dark-400 mb-4">Lütfen sayfayı yenileyin veya daha sonra tekrar deneyin</p>
          <button
            onClick={() => window.location.reload()}
            className="btn-primary"
          >
            Sayfayı Yenile
          </button>
        </div>
      ) : !portfolios || portfolios.length === 0 ? (
        <div className="card p-12 text-center">
          <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-dark-800 flex items-center justify-center">
            <PlusIcon className="w-8 h-8 text-dark-500" />
          </div>
          <h3 className="text-xl font-semibold text-white mb-2">Henüz portföy yok</h3>
          <p className="text-dark-400 mb-4">İlk portföyünüzü oluşturarak başlayın</p>
          <button
            onClick={() => setShowCreateModal(true)}
            className="btn-primary"
          >
            Portföy Oluştur
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {portfolios?.map((portfolio) => {
            const isUp = portfolio.profitLoss > 0
            const isDown = portfolio.profitLoss < 0

            return (
              <div key={portfolio.id} className="card-hover p-6 group">
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <Link
                      to={`/portfolio/${portfolio.id}`}
                      className="text-lg font-semibold text-white hover:text-primary-400 transition-colors"
                    >
                      {portfolio.name}
                    </Link>
                    {portfolio.isDefault && (
                      <span className="ml-2 badge-info text-xs">Varsayılan</span>
                    )}
                    {portfolio.description && (
                      <p className="text-dark-400 text-sm mt-1 line-clamp-1">
                        {portfolio.description}
                      </p>
                    )}
                  </div>
                  <button
                    onClick={() => handleDelete(portfolio.id, portfolio.name)}
                    className="p-2 rounded-lg text-dark-500 hover:text-red-400 hover:bg-dark-800 transition-colors opacity-0 group-hover:opacity-100"
                  >
                    <TrashIcon className="w-5 h-5" />
                  </button>
                </div>

                <div className="space-y-3">
                  <div>
                    <p className="text-dark-500 text-xs">Toplam Değer</p>
                    <p className="text-2xl font-bold text-white">
                      {formatCurrency(portfolio.totalValue)}
                    </p>
                  </div>

                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-dark-500 text-xs">Kar/Zarar</p>
                      <div className={`flex items-center gap-1 ${
                        isUp ? 'price-up' : isDown ? 'price-down' : 'price-neutral'
                      }`}>
                        {isUp ? (
                          <ArrowTrendingUpIcon className="w-4 h-4" />
                        ) : isDown ? (
                          <ArrowTrendingDownIcon className="w-4 h-4" />
                        ) : null}
                        <span className="font-medium">
                          {formatCurrency(portfolio.profitLoss)}
                        </span>
                        {portfolio.profitLossPercent !== null && (
                          <span className="text-sm">
                            ({portfolio.profitLossPercent > 0 ? '+' : ''}{portfolio.profitLossPercent?.toFixed(2)}%)
                          </span>
                        )}
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="text-dark-500 text-xs">Enstrüman</p>
                      <p className="text-white font-medium">{portfolio.itemCount || 0}</p>
                    </div>
                  </div>
                </div>

                <Link
                  to={`/portfolio/${portfolio.id}`}
                  className="mt-4 block text-center py-2 text-sm text-primary-400 hover:text-primary-300 border-t border-dark-800"
                >
                  Detayları Gör →
                </Link>
              </div>
            )
          })}
        </div>
      )}

      {/* Create Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="card p-6 w-full max-w-md animate-in">
            <h2 className="text-xl font-semibold text-white mb-4">Yeni Portföy</h2>
            <form onSubmit={handleCreate} className="space-y-4">
              <div>
                <label className="label">Portföy Adı</label>
                <input
                  type="text"
                  value={newPortfolio.name}
                  onChange={(e) => setNewPortfolio({ ...newPortfolio, name: e.target.value })}
                  className="input"
                  placeholder="Örn: Ana Portföy"
                  required
                />
              </div>
              <div>
                <label className="label">Açıklama (Opsiyonel)</label>
                <textarea
                  value={newPortfolio.description}
                  onChange={(e) => setNewPortfolio({ ...newPortfolio, description: e.target.value })}
                  className="input"
                  rows={3}
                  placeholder="Portföy hakkında kısa bir açıklama..."
                />
              </div>
              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="btn-secondary flex-1"
                >
                  İptal
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="btn-primary flex-1"
                >
                  {createMutation.isPending ? 'Oluşturuluyor...' : 'Oluştur'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
