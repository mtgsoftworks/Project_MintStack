import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus, Trash2, Star, TrendingUp, TrendingDown } from 'lucide-react'
import {
    useCreateWatchlistMutation,
    useDeleteWatchlistMutation,
    useGetWatchlistQuery,
    useGetWatchlistsQuery,
    useRemoveWatchlistInstrumentMutation
} from '@/store/api/watchlistApi'

export default function WatchlistPage() {
    const { t } = useTranslation()
    const [selectedWatchlistId, setSelectedWatchlistId] = useState(null)
    const [showCreateModal, setShowCreateModal] = useState(false)
    const [newWatchlistName, setNewWatchlistName] = useState('')

    const { data: watchlists = [], isLoading: watchlistsLoading } = useGetWatchlistsQuery()
    const { data: selectedWatchlist, isLoading: watchlistDetailLoading } = useGetWatchlistQuery(selectedWatchlistId, {
        skip: !selectedWatchlistId
    })

    const [createWatchlist, { isLoading: creating }] = useCreateWatchlistMutation()
    const [deleteWatchlist, { isLoading: deleting }] = useDeleteWatchlistMutation()
    const [removeWatchlistInstrument, { isLoading: removingItem }] = useRemoveWatchlistInstrumentMutation()

    const mutating = creating || deleting || removingItem

    useEffect(() => {
        if (watchlists.length > 0 && !selectedWatchlistId) {
            setSelectedWatchlistId(watchlists[0].id)
        }
        if (watchlists.length === 0) {
            setSelectedWatchlistId(null)
        }
    }, [watchlists, selectedWatchlistId])

    const handleCreateWatchlist = async () => {
        if (!newWatchlistName.trim()) return
        try {
            const created = await createWatchlist({ name: newWatchlistName }).unwrap()
            setNewWatchlistName('')
            setShowCreateModal(false)
            if (created?.id) {
                setSelectedWatchlistId(created.id)
            }
        } catch (error) {
            console.error('Error creating watchlist:', error)
        }
    }

    const handleDeleteWatchlist = async (id) => {
        if (!window.confirm(t('common.confirm'))) return
        try {
            await deleteWatchlist(id).unwrap()
            if (selectedWatchlistId === id) {
                setSelectedWatchlistId(null)
            }
        } catch (error) {
            console.error('Error deleting watchlist:', error)
        }
    }

    const handleRemoveItem = async (symbol) => {
        if (!selectedWatchlistId) return
        try {
            await removeWatchlistInstrument({ watchlistId: selectedWatchlistId, symbol }).unwrap()
        } catch (error) {
            console.error('Error removing item:', error)
        }
    }

    if (watchlistsLoading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-500" />
            </div>
        )
    }

    return (
        <div className="p-6">
            <div className="flex items-center justify-between mb-6">
                <h1 className="text-2xl font-bold text-foreground">{t('watchlist.title')}</h1>
                <button
                    onClick={() => setShowCreateModal(true)}
                    className="flex items-center gap-2 px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600 transition-colors"
                >
                    <Plus className="w-4 h-4" />
                    {t('watchlist.create')}
                </button>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
                <div className="lg:col-span-1">
                    <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
                        <h2 className="mb-4 font-semibold text-foreground">{t('watchlist.title')}</h2>
                        <div className="space-y-2">
                            {watchlists.map((list) => (
                                <button
                                    key={list.id}
                                    onClick={() => setSelectedWatchlistId(list.id)}
                                    className={`w-full flex items-center justify-between p-3 rounded-lg transition-colors ${
                                        selectedWatchlistId === list.id
                                            ? 'bg-emerald-500/10 text-emerald-600'
                                            : 'text-foreground hover:bg-muted/50'
                                    }`}
                                >
                                    <div className="flex items-center gap-2">
                                        {list.isDefault && <Star className="w-4 h-4 text-yellow-500" />}
                                        <span>{list.name}</span>
                                    </div>
                                    <span className="text-sm text-muted-foreground">{list.itemCount}</span>
                                </button>
                            ))}
                            {watchlists.length === 0 && (
                                <p className="py-4 text-center text-sm text-muted-foreground">{t('watchlist.empty')}</p>
                            )}
                        </div>
                    </div>
                </div>

                <div className="lg:col-span-3">
                    {selectedWatchlist ? (
                        <div className="rounded-xl border border-border bg-card shadow-sm">
                            <div className="flex items-center justify-between p-4 border-b">
                                <h2 className="font-semibold text-foreground">{selectedWatchlist.name}</h2>
                                <button
                                    onClick={() => handleDeleteWatchlist(selectedWatchlist.id)}
                                    disabled={mutating}
                                    className="text-red-500 hover:text-red-600 disabled:opacity-50"
                                >
                                    <Trash2 className="w-5 h-5" />
                                </button>
                            </div>
                            <div className="overflow-x-auto">
                                <table className="w-full">
                                    <thead className="bg-muted/50">
                                        <tr>
                                            <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('portfolio.symbol')}</th>
                                            <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('market.currentPrice')}</th>
                                            <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('market.change')}</th>
                                            <th className="px-4 py-3 text-right text-sm font-medium text-muted-foreground"></th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y">
                                        {selectedWatchlist.items?.map((item) => (
                                            <tr key={item.id} className="hover:bg-muted/40">
                                                <td className="px-4 py-3">
                                                    <div>
                                                        <div className="font-medium text-foreground">{item.symbol}</div>
                                                        <div className="text-sm text-muted-foreground">{item.name}</div>
                                                    </div>
                                                </td>
                                                <td className="px-4 py-3 font-medium">
                                                    TRY {item.currentPrice?.toLocaleString('tr-TR')}
                                                </td>
                                                <td className="px-4 py-3">
                                                    <div className={`flex items-center gap-1 ${item.changePercent >= 0 ? 'text-green-600' : 'text-red-600'}`}>
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
                                                        disabled={mutating}
                                                        className="text-muted-foreground hover:text-red-500 disabled:opacity-50"
                                                    >
                                                        <Trash2 className="w-4 h-4" />
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                        {(!selectedWatchlist.items || selectedWatchlist.items.length === 0) && (
                                            <tr>
                                                <td colSpan={4} className="px-4 py-8 text-center text-muted-foreground">
                                                    {t('watchlist.empty')}
                                                </td>
                                            </tr>
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    ) : watchlistDetailLoading ? (
                        <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground shadow-sm">
                            {t('common.loading')}
                        </div>
                    ) : (
                        <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground shadow-sm">
                            {t('watchlist.empty')}
                        </div>
                    )}
                </div>
            </div>

            {showCreateModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                    <div className="w-full max-w-md rounded-xl border border-border bg-card p-6 text-foreground">
                        <h3 className="text-lg font-semibold mb-4">{t('watchlist.create')}</h3>
                        <input
                            type="text"
                            value={newWatchlistName}
                            onChange={(event) => setNewWatchlistName(event.target.value)}
                            placeholder={t('watchlist.title')}
                            className="mb-4 w-full rounded-lg border border-input bg-background px-4 py-2 text-foreground placeholder:text-muted-foreground"
                        />
                        <div className="flex gap-3 justify-end">
                            <button
                                onClick={() => setShowCreateModal(false)}
                                className="rounded-lg px-4 py-2 text-muted-foreground hover:bg-muted"
                            >
                                {t('common.cancel')}
                            </button>
                            <button
                                onClick={handleCreateWatchlist}
                                disabled={creating}
                                className="px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600 disabled:opacity-50"
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
