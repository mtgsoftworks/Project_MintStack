import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus, Trash2, Star, TrendingUp, TrendingDown } from 'lucide-react'
import watchlistService from '@/services/watchlistService'

export default function WatchlistPage() {
    const { t } = useTranslation()
    const [watchlists, setWatchlists] = useState([])
    const [selectedWatchlist, setSelectedWatchlist] = useState(null)
    const [loading, setLoading] = useState(true)
    const [showCreateModal, setShowCreateModal] = useState(false)
    const [newWatchlistName, setNewWatchlistName] = useState('')

    useEffect(() => {
        loadWatchlists()
    }, [])

    const loadWatchlists = async () => {
        try {
            setLoading(true)
            const response = await watchlistService.getAll()
            setWatchlists(response.data || [])
            if (response.data?.length > 0 && !selectedWatchlist) {
                loadWatchlistDetails(response.data[0].id)
            }
        } catch (error) {
            console.error('Error loading watchlists:', error)
        } finally {
            setLoading(false)
        }
    }

    const loadWatchlistDetails = async (id) => {
        try {
            const response = await watchlistService.getById(id)
            setSelectedWatchlist(response.data)
        } catch (error) {
            console.error('Error loading watchlist details:', error)
        }
    }

    const handleCreateWatchlist = async () => {
        if (!newWatchlistName.trim()) return
        try {
            await watchlistService.create({ name: newWatchlistName })
            setNewWatchlistName('')
            setShowCreateModal(false)
            loadWatchlists()
        } catch (error) {
            console.error('Error creating watchlist:', error)
        }
    }

    const handleDeleteWatchlist = async (id) => {
        if (!confirm(t('common.confirm'))) return
        try {
            await watchlistService.delete(id)
            setSelectedWatchlist(null)
            loadWatchlists()
        } catch (error) {
            console.error('Error deleting watchlist:', error)
        }
    }

    const handleRemoveItem = async (symbol) => {
        if (!selectedWatchlist) return
        try {
            await watchlistService.removeInstrument(selectedWatchlist.id, symbol)
            loadWatchlistDetails(selectedWatchlist.id)
        } catch (error) {
            console.error('Error removing item:', error)
        }
    }

    if (loading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-500" />
            </div>
        )
    }

    return (
        <div className="p-6">
            <div className="flex items-center justify-between mb-6">
                <h1 className="text-2xl font-bold text-gray-900">{t('watchlist.title')}</h1>
                <button
                    onClick={() => setShowCreateModal(true)}
                    className="flex items-center gap-2 px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600 transition-colors"
                >
                    <Plus className="w-4 h-4" />
                    {t('watchlist.create')}
                </button>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
                {/* Watchlist Sidebar */}
                <div className="lg:col-span-1">
                    <div className="bg-white rounded-xl shadow-sm p-4">
                        <h2 className="font-semibold text-gray-700 mb-4">{t('watchlist.title')}</h2>
                        <div className="space-y-2">
                            {watchlists.map((list) => (
                                <button
                                    key={list.id}
                                    onClick={() => loadWatchlistDetails(list.id)}
                                    className={`w-full flex items-center justify-between p-3 rounded-lg transition-colors ${selectedWatchlist?.id === list.id
                                            ? 'bg-emerald-50 text-emerald-600'
                                            : 'hover:bg-gray-50'
                                        }`}
                                >
                                    <div className="flex items-center gap-2">
                                        {list.isDefault && <Star className="w-4 h-4 text-yellow-500" />}
                                        <span>{list.name}</span>
                                    </div>
                                    <span className="text-sm text-gray-500">{list.itemCount}</span>
                                </button>
                            ))}
                            {watchlists.length === 0 && (
                                <p className="text-gray-500 text-sm text-center py-4">{t('watchlist.empty')}</p>
                            )}
                        </div>
                    </div>
                </div>

                {/* Watchlist Items */}
                <div className="lg:col-span-3">
                    {selectedWatchlist ? (
                        <div className="bg-white rounded-xl shadow-sm">
                            <div className="flex items-center justify-between p-4 border-b">
                                <h2 className="font-semibold text-gray-900">{selectedWatchlist.name}</h2>
                                <button
                                    onClick={() => handleDeleteWatchlist(selectedWatchlist.id)}
                                    className="text-red-500 hover:text-red-600"
                                >
                                    <Trash2 className="w-5 h-5" />
                                </button>
                            </div>
                            <div className="overflow-x-auto">
                                <table className="w-full">
                                    <thead className="bg-gray-50">
                                        <tr>
                                            <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">{t('portfolio.symbol')}</th>
                                            <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">{t('market.currentPrice')}</th>
                                            <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">{t('market.change')}</th>
                                            <th className="px-4 py-3 text-right text-sm font-medium text-gray-600"></th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y">
                                        {selectedWatchlist.items?.map((item) => (
                                            <tr key={item.id} className="hover:bg-gray-50">
                                                <td className="px-4 py-3">
                                                    <div>
                                                        <div className="font-medium text-gray-900">{item.symbol}</div>
                                                        <div className="text-sm text-gray-500">{item.name}</div>
                                                    </div>
                                                </td>
                                                <td className="px-4 py-3 font-medium">
                                                    ₺{item.currentPrice?.toLocaleString('tr-TR')}
                                                </td>
                                                <td className="px-4 py-3">
                                                    <div className={`flex items-center gap-1 ${item.changePercent >= 0 ? 'text-green-600' : 'text-red-600'
                                                        }`}>
                                                        {item.changePercent >= 0 ? (
                                                            <TrendingUp className="w-4 h-4" />
                                                        ) : (
                                                            <TrendingDown className="w-4 h-4" />
                                                        )}
                                                        <span>%{item.changePercent?.toFixed(2)}</span>
                                                    </div>
                                                </td>
                                                <td className="px-4 py-3 text-right">
                                                    <button
                                                        onClick={() => handleRemoveItem(item.symbol)}
                                                        className="text-gray-400 hover:text-red-500"
                                                    >
                                                        <Trash2 className="w-4 h-4" />
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                        {(!selectedWatchlist.items || selectedWatchlist.items.length === 0) && (
                                            <tr>
                                                <td colSpan={4} className="px-4 py-8 text-center text-gray-500">
                                                    {t('watchlist.empty')}
                                                </td>
                                            </tr>
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    ) : (
                        <div className="bg-white rounded-xl shadow-sm p-8 text-center text-gray-500">
                            {t('watchlist.empty')}
                        </div>
                    )}
                </div>
            </div>

            {/* Create Modal */}
            {showCreateModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-xl p-6 w-full max-w-md">
                        <h3 className="text-lg font-semibold mb-4">{t('watchlist.create')}</h3>
                        <input
                            type="text"
                            value={newWatchlistName}
                            onChange={(e) => setNewWatchlistName(e.target.value)}
                            placeholder={t('watchlist.title')}
                            className="w-full px-4 py-2 border rounded-lg mb-4"
                        />
                        <div className="flex gap-3 justify-end">
                            <button
                                onClick={() => setShowCreateModal(false)}
                                className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg"
                            >
                                {t('common.cancel')}
                            </button>
                            <button
                                onClick={handleCreateWatchlist}
                                className="px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600"
                            >
                                {t('common.save')}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
