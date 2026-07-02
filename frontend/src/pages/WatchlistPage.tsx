import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Plus,
  Trash2,
  Star,
  TrendingUp,
  TrendingDown,
  AlertTriangle,
  RefreshCw,
  GripVertical,
  Save,
} from 'lucide-react'
import { toast } from 'sonner'
import {
  useAddWatchlistInstrumentMutation,
  useCreateWatchlistMutation,
  useDeleteWatchlistMutation,
  useGetWatchlistQuery,
  useGetWatchlistsQuery,
  useRemoveWatchlistInstrumentMutation,
  useReorderWatchlistItemsMutation,
  useUpdateWatchlistItemMutation,
  useUpdateWatchlistMutation,
} from '@/store/api/watchlistApi'
import { getApiErrorMessage } from '@/pages/settings/getApiErrorMessage'
import { useInstrumentOptions } from '@/hooks/useInstrumentOptions'

const DEFAULT_COLUMNS = ['SYMBOL', 'NAME', 'TYPE', 'PRICE', 'CHANGE', 'NOTES'] as const

interface WatchlistItem {
  id: string
  symbol: string
  name?: string
  type?: string
  displayOrder?: number | string
  notes?: string | null
  addedAt?: string
  currentPrice?: string | number
  previousClose?: string | number
  changePercent?: number
}

interface Watchlist {
  id: string | number
  name: string
  description?: string
  tag?: string
  notes?: string
  isDefault?: boolean
  itemCount?: number
  columnPreferences?: string[]
  items?: WatchlistItem[]
}

interface WatchlistFormState {
  name: string
  description: string
  tag: string
  notes: string
  columnPreferences: string[]
}
const OPTIONAL_COLUMNS: { key: string; labelKey: string }[] = [
  { key: 'TYPE', labelKey: 'watchlist.columns.type' },
  { key: 'PRICE', labelKey: 'watchlist.columns.price' },
  { key: 'CHANGE', labelKey: 'watchlist.columns.change' },
  { key: 'ADDED_AT', labelKey: 'watchlist.columns.addedAt' },
  { key: 'NOTES', labelKey: 'watchlist.columns.note' },
]

export default function WatchlistPage() {
  const { t } = useTranslation()
  const [selectedWatchlistId, setSelectedWatchlistId] = useState<string | number | null>(null)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [newInstrumentSymbol, setNewInstrumentSymbol] = useState('')
  const [draggedItemId, setDraggedItemId] = useState<string | null>(null)
  const [newWatchlistForm, setNewWatchlistForm] = useState({
    name: '',
    description: '',
    tag: '',
    notes: '',
  })
  const [metadataForm, setMetadataForm] = useState<WatchlistFormState>({
    name: '',
    description: '',
    tag: '',
    notes: '',
    columnPreferences: [...DEFAULT_COLUMNS],
  })
  const [itemNotesDraft, setItemNotesDraft] = useState<Record<string, string>>({})

  const {
    data: watchlists = [] as Watchlist[],
    isLoading: watchlistsLoading,
    error: watchlistsError,
    refetch: refetchWatchlists,
  } = useGetWatchlistsQuery(undefined)

  const {
    data: selectedWatchlist,
    isLoading: watchlistDetailLoading,
    isFetching: watchlistDetailFetching,
    error: watchlistDetailError,
    refetch: refetchWatchlistDetail,
  } = useGetWatchlistQuery(
    (selectedWatchlistId ?? '') as string | number,
    { skip: !selectedWatchlistId }
  )

  const [createWatchlist, { isLoading: creating }] = useCreateWatchlistMutation()
  const [updateWatchlist, { isLoading: updatingWatchlist }] = useUpdateWatchlistMutation()
  const [deleteWatchlist, { isLoading: deleting }] = useDeleteWatchlistMutation()
  const [addWatchlistInstrument, { isLoading: addingItem }] = useAddWatchlistInstrumentMutation()
  const [removeWatchlistInstrument, { isLoading: removingItem }] = useRemoveWatchlistInstrumentMutation()
  const [reorderWatchlistItems, { isLoading: reordering }] = useReorderWatchlistItemsMutation()
  const [updateWatchlistItem, { isLoading: updatingItem }] = useUpdateWatchlistItemMutation()

  const { instrumentOptions, isFetching: instrumentsFetching } = useInstrumentOptions()

  const mutating = creating || deleting || addingItem || removingItem || reordering || updatingItem
  const watchlistsErrorMessage = watchlistsError ? getApiErrorMessage(watchlistsError, t('common.error')) : null
  const watchlistDetailErrorMessage = watchlistDetailError ? getApiErrorMessage(watchlistDetailError, t('common.error')) : null

  useEffect(() => {
    if (watchlists.length === 0) {
      setSelectedWatchlistId(null)
      return
    }

    const selectionExists = watchlists.some((list: Watchlist) => list.id === selectedWatchlistId)
    if (!selectedWatchlistId || !selectionExists) {
      setSelectedWatchlistId(watchlists[0].id)
    }
  }, [watchlists, selectedWatchlistId])

  useEffect(() => {
    if (!selectedWatchlist) {
      return
    }

    setMetadataForm({
      name: selectedWatchlist.name || '',
      description: selectedWatchlist.description || '',
      tag: selectedWatchlist.tag || '',
      notes: selectedWatchlist.notes || '',
      columnPreferences: selectedWatchlist.columnPreferences?.length
        ? selectedWatchlist.columnPreferences
        : DEFAULT_COLUMNS,
    })

    const drafts: Record<string, string> = {}
    for (const item of selectedWatchlist.items || []) {
      drafts[item.id] = item.notes || ''
    }
    setItemNotesDraft(drafts)
  }, [selectedWatchlist])

  const orderedItems = useMemo(() => {
    if (!selectedWatchlist?.items) {
      return []
    }

    return [...selectedWatchlist.items].sort((left, right) => {
      const leftOrder = Number(left.displayOrder || 0)
      const rightOrder = Number(right.displayOrder || 0)
      if (leftOrder !== rightOrder) {
        return leftOrder - rightOrder
      }
      return String(left.symbol || '').localeCompare(String(right.symbol || ''), 'tr')
    })
  }, [selectedWatchlist])

  const hasColumn = (columnKey: string) => metadataForm.columnPreferences.includes(columnKey)

  const handleCreateWatchlist = async () => {
    const name = newWatchlistForm.name.trim()
    if (!name) {
      toast.error(t('watchlist.nameRequired'))
      return
    }

    try {
      const created = await createWatchlist({
        name,
        description: newWatchlistForm.description.trim() || null,
        tag: newWatchlistForm.tag.trim() || null,
        notes: newWatchlistForm.notes.trim() || null,
        columnPreferences: DEFAULT_COLUMNS,
      }).unwrap()

      setNewWatchlistForm({ name: '', description: '', tag: '', notes: '' })
      setShowCreateModal(false)

      if (created?.id) {
        setSelectedWatchlistId(created.id)
      }

      toast.success(t('watchlist.created'))
    } catch (error) {
      toast.error(getApiErrorMessage(error, t('common.error')))
    }
  }

  const handleSaveMetadata = async () => {
    if (!selectedWatchlistId) {
      return
    }

    const normalizedName = metadataForm.name.trim()
    if (!normalizedName) {
      toast.error(t('watchlist.nameRequired'))
      return
    }

    try {
      await updateWatchlist({
        id: selectedWatchlistId,
        name: normalizedName,
        description: metadataForm.description.trim() || null,
        tag: metadataForm.tag.trim() || null,
        notes: metadataForm.notes.trim() || null,
        columnPreferences: metadataForm.columnPreferences,
      }).unwrap()

      toast.success(t('watchlist.settingsSaved'))
    } catch (error) {
      toast.error(getApiErrorMessage(error, t('common.error')))
    }
  }

  const handleDeleteWatchlist = async (id: string | number) => {
    if (!window.confirm(t('common.confirm'))) {
      return
    }

    try {
      await deleteWatchlist(id).unwrap()
      if (selectedWatchlistId === id) {
        setSelectedWatchlistId(null)
      }
      toast.success(t('watchlist.deleted'))
    } catch (error) {
      toast.error(getApiErrorMessage(error, t('common.error')))
    }
  }

  const handleRemoveItem = async (symbol: string) => {
    if (!selectedWatchlistId) {
      return
    }
    const watchlistId: string | number = selectedWatchlistId

    try {
      await (removeWatchlistInstrument as Function)({ watchlistId, symbol }).unwrap()
    } catch (error) {
      toast.error(getApiErrorMessage(error, t('common.error')))
    }
  }

  const handleAddItem = async () => {
    const symbol = newInstrumentSymbol.trim().toUpperCase()
    if (!selectedWatchlistId || !symbol) {
      toast.error(t('watchlist.selectListAndSymbol'))
      return
    }
    const watchlistId: string | number = selectedWatchlistId

    try {
      await (addWatchlistInstrument as Function)({ watchlistId, symbol }).unwrap()
      setNewInstrumentSymbol('')
      toast.success(t('watchlist.assetAdded'))
    } catch (error) {
      toast.error(getApiErrorMessage(error, t('common.error')))
    }
  }

  const handleToggleColumn = (columnKey: string, enabled: boolean) => {
    setMetadataForm((previous) => {
      const current = [...previous.columnPreferences]
      if (enabled) {
        if (current.includes(columnKey)) {
          return previous
        }
        return {
          ...previous,
          columnPreferences: [...current, columnKey],
        }
      }

      return {
        ...previous,
        columnPreferences: current.filter((column) => column !== columnKey),
      }
    })
  }

  const handleItemNoteChange = (itemId: string, value: string) => {
    setItemNotesDraft((previous) => ({
      ...previous,
      [itemId]: value,
    }))
  }

  const handleItemNoteBlur = async (item: WatchlistItem) => {
    if (!selectedWatchlistId) {
      return
    }
    const watchlistId: string | number = selectedWatchlistId

    const draftValue = (itemNotesDraft[item.id] ?? '').trim()
    const currentValue = (item.notes || '').trim()

    if (draftValue === currentValue) {
      return
    }

    try {
      await (updateWatchlistItem as Function)({
        watchlistId,
        itemId: item.id,
        notes: draftValue || null,
      }).unwrap()
      toast.success(t('watchlist.noteUpdated', { symbol: item.symbol }))
    } catch (error) {
      toast.error(getApiErrorMessage(error, t('common.error')))
    }
  }

  const handleRowDrop = async (targetItemId: string) => {
    if (!selectedWatchlistId || !draggedItemId || draggedItemId === targetItemId) {
      setDraggedItemId(null)
      return
    }
    const watchlistId: string | number = selectedWatchlistId

    const fromIndex = orderedItems.findIndex((item) => item.id === draggedItemId)
    const toIndex = orderedItems.findIndex((item) => item.id === targetItemId)

    if (fromIndex === -1 || toIndex === -1) {
      setDraggedItemId(null)
      return
    }

    const reorderedItems = [...orderedItems]
    const [movedItem] = reorderedItems.splice(fromIndex, 1)
    reorderedItems.splice(toIndex, 0, movedItem)

    try {
      await (reorderWatchlistItems as Function)({
        watchlistId,
        itemIds: reorderedItems.map((item) => item.id),
      }).unwrap()
    } catch (error) {
      toast.error(getApiErrorMessage(error, t('common.error')))
    } finally {
      setDraggedItemId(null)
    }
  }

  const getChangePercent = (item: WatchlistItem): number | null => {
    if (typeof item.changePercent === 'number') {
      return item.changePercent
    }

    const currentPrice = Number(item.currentPrice)
    const previousClose = Number(item.previousClose)
    if (!Number.isFinite(currentPrice) || !Number.isFinite(previousClose) || previousClose === 0) {
      return null
    }

    return ((currentPrice - previousClose) / previousClose) * 100
  }

  const formatPrice = (price: string | number | undefined) => {
    const numericPrice = Number(price)
    return Number.isFinite(numericPrice) ? `TRY ${numericPrice.toLocaleString('tr-TR')}` : '-'
  }

  const formatAddedAt = (value: string | undefined | null) => {
    if (!value) {
      return '-'
    }

    const parsed = new Date(value)
    if (Number.isNaN(parsed.getTime())) {
      return '-'
    }

    return parsed.toLocaleDateString('tr-TR')
  }

  if (watchlistsLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-500" />
      </div>
    )
  }

  if (watchlistsError) {
    return (
      <div className="p-6">
        <div className="rounded-xl border border-danger/40 bg-danger/5 p-6 text-danger shadow-sm">
          <div className="mb-3 flex items-center gap-2 font-semibold">
            <AlertTriangle className="h-4 w-4" />
            {watchlistsErrorMessage || t('common.error')}
          </div>
          <button
            onClick={() => refetchWatchlists()}
            className="inline-flex items-center gap-2 rounded-lg border border-danger/40 px-3 py-2 text-sm font-medium transition-colors hover:bg-danger/10"
          >
            <RefreshCw className="h-4 w-4" />
            {t('common.refresh')}
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-foreground">{t('watchlist.title')}</h1>
        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-2 rounded-lg bg-emerald-500 px-4 py-2 text-white transition-colors hover:bg-emerald-600"
        >
          <Plus className="h-4 w-4" />
          {t('watchlist.create')}
        </button>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-4">
        <div className="lg:col-span-1">
          <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
            <h2 className="mb-4 font-semibold text-foreground">{t('watchlist.title')}</h2>
            <div className="space-y-2">
              {watchlists.map((list: Watchlist) => (
                <button
                  key={list.id}
                  onClick={() => setSelectedWatchlistId(list.id)}
                  className={`w-full rounded-lg p-3 transition-colors ${
                    selectedWatchlistId === list.id
                      ? 'bg-emerald-500/10 text-emerald-600'
                      : 'text-foreground hover:bg-muted/50'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      {list.isDefault && <Star className="h-4 w-4 text-yellow-500" />}
                      <span>{list.name}</span>
                    </div>
                    <span className="text-sm text-muted-foreground">{list.itemCount}</span>
                  </div>
                  {list.tag && (
                    <div className="mt-2 text-left">
                      <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">#{list.tag}</span>
                    </div>
                  )}
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
            <div className="space-y-4">
              <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
                <div className="mb-3 flex items-center justify-between">
                  <h2 className="font-semibold text-foreground">{selectedWatchlist.name}</h2>
                  <button
                    onClick={handleSaveMetadata}
                    disabled={updatingWatchlist}
                    className="inline-flex items-center gap-2 rounded-lg border border-input px-3 py-2 text-sm hover:bg-muted disabled:opacity-50"
                  >
                    <Save className="h-4 w-4" />
                    {t('watchlist.saveSettings')}
                  </button>
                </div>

                <div className="grid gap-3 md:grid-cols-3">
                  <div>
                    <label className="mb-1 block text-xs text-muted-foreground">{t('watchlist.listName')}</label>
                    <input
                      value={metadataForm.name}
                      onChange={(event) => setMetadataForm((prev) => ({ ...prev, name: event.target.value }))}
                      className="w-full rounded-lg border border-input bg-background px-3 py-2 text-sm"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs text-muted-foreground">{t('watchlist.description')}</label>
                    <input
                      value={metadataForm.description}
                      onChange={(event) => setMetadataForm((prev) => ({ ...prev, description: event.target.value }))}
                      className="w-full rounded-lg border border-input bg-background px-3 py-2 text-sm"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs text-muted-foreground">{t('watchlist.tag')}</label>
                    <input
                      value={metadataForm.tag}
                      onChange={(event) => setMetadataForm((prev) => ({ ...prev, tag: event.target.value }))}
                      placeholder={t('watchlist.tagPlaceholder')}
                      className="w-full rounded-lg border border-input bg-background px-3 py-2 text-sm"
                    />
                  </div>
                </div>

                <div className="mt-3">
                  <label className="mb-1 block text-xs text-muted-foreground">{t('watchlist.listNote')}</label>
                  <textarea
                    value={metadataForm.notes}
                    onChange={(event) => setMetadataForm((prev) => ({ ...prev, notes: event.target.value }))}
                    rows={2}
                    className="w-full rounded-lg border border-input bg-background px-3 py-2 text-sm"
                    placeholder={t('watchlist.listNotePlaceholder')}
                  />
                </div>

                <div className="mt-3">
                  <p className="mb-2 text-xs text-muted-foreground">{t('watchlist.customColumns')}</p>
                  <div className="flex flex-wrap gap-3">
                    {OPTIONAL_COLUMNS.map((column) => (
                      <label key={column.key} className="inline-flex items-center gap-2 text-sm">
                        <input
                          type="checkbox"
                          checked={hasColumn(column.key)}
                          onChange={(event) => handleToggleColumn(column.key, event.target.checked)}
                        />
                        {t(column.labelKey)}
                      </label>
                    ))}
                  </div>
                </div>
              </div>

              <div className="rounded-xl border border-border bg-card shadow-sm">
                <div className="flex flex-col gap-4 border-b p-4 xl:flex-row xl:items-center xl:justify-between">
                  <div>
                    <h3 className="font-semibold text-foreground">{selectedWatchlist.name}</h3>
                    <p className="text-sm text-muted-foreground">
                      {t('watchlist.rowHelp')}
                    </p>
                  </div>
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
                    <div className="min-w-[260px]">
                      <input
                        type="text"
                        list="watchlist-symbol-options"
                        value={newInstrumentSymbol}
                        onChange={(event) => setNewInstrumentSymbol(event.target.value.toUpperCase())}
                        placeholder={instrumentsFetching ? t('watchlist.symbolsLoading') : t('watchlist.symbolPlaceholder')}
                        className="w-full rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground"
                        disabled={addingItem}
                      />
                      <datalist id="watchlist-symbol-options">
                        {instrumentOptions.map((instrument) => (
                          <option key={instrument.symbol} value={instrument.symbol}>
                            {`${instrument.name} (${instrument.type})`}
                          </option>
                        ))}
                      </datalist>
                    </div>
                    <button
                      onClick={handleAddItem}
                      disabled={mutating || !newInstrumentSymbol.trim()}
                      className="inline-flex items-center justify-center gap-2 rounded-lg bg-emerald-500 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-emerald-600 disabled:opacity-50"
                    >
                      <Plus className="h-4 w-4" />
                      {t('watchlist.addAsset')}
                    </button>
                    <button
                      onClick={() => handleDeleteWatchlist(selectedWatchlist.id)}
                      disabled={mutating}
                      className="inline-flex items-center justify-center rounded-lg border border-red-200 px-3 py-2 text-red-500 hover:bg-red-50 disabled:opacity-50 dark:border-red-900/60 dark:hover:bg-red-950/30"
                      title={t('watchlist.deleteTitle')}
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead className="bg-muted/50">
                      <tr>
                        <th className="w-10 px-2 py-3" />
                        <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('portfolio.symbol')}</th>
                        {hasColumn('TYPE') && <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('watchlist.columns.type')}</th>}
                        {hasColumn('PRICE') && <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('market.currentPrice')}</th>}
                        {hasColumn('CHANGE') && <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('market.change')}</th>}
                        {hasColumn('ADDED_AT') && <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('watchlist.columns.addedAt')}</th>}
                        {hasColumn('NOTES') && <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('watchlist.columns.note')}</th>}
                        <th className="px-4 py-3 text-right text-sm font-medium text-muted-foreground" />
                      </tr>
                    </thead>
                    <tbody className="divide-y">
                      {orderedItems.map((item) => {
                        const changePercent = getChangePercent(item)
                        const isPositive = changePercent != null && changePercent >= 0
                        const changeClass = changePercent == null
                          ? 'text-muted-foreground'
                          : isPositive ? 'text-green-600' : 'text-red-600'

                        return (
                          <tr
                            key={item.id}
                            draggable
                            onDragStart={() => setDraggedItemId(item.id)}
                            onDragOver={(event) => event.preventDefault()}
                            onDrop={() => handleRowDrop(item.id)}
                            className="hover:bg-muted/40"
                          >
                            <td className="px-2 py-3 text-muted-foreground">
                              <GripVertical className="h-4 w-4" />
                            </td>
                            <td className="px-4 py-3">
                              <div>
                                <div className="font-medium text-foreground">{item.symbol}</div>
                                <div className="text-sm text-muted-foreground">{item.name}</div>
                              </div>
                            </td>
                            {hasColumn('TYPE') && <td className="px-4 py-3 text-sm text-muted-foreground">{item.type || '-'}</td>}
                            {hasColumn('PRICE') && (
                              <td className="px-4 py-3 font-medium">{formatPrice(item.currentPrice)}</td>
                            )}
                            {hasColumn('CHANGE') && (
                              <td className="px-4 py-3">
                                <div className={`flex items-center gap-1 ${changeClass}`}>
                                  {changePercent == null ? (
                                    <span>-</span>
                                  ) : isPositive ? (
                                    <>
                                      <TrendingUp className="h-4 w-4" />
                                      <span>%{changePercent.toFixed(2)}</span>
                                    </>
                                  ) : (
                                    <>
                                      <TrendingDown className="h-4 w-4" />
                                      <span>%{changePercent.toFixed(2)}</span>
                                    </>
                                  )}
                                </div>
                              </td>
                            )}
                            {hasColumn('ADDED_AT') && (
                              <td className="px-4 py-3 text-sm text-muted-foreground">{formatAddedAt(item.addedAt)}</td>
                            )}
                            {hasColumn('NOTES') && (
                              <td className="px-4 py-3">
                                <input
                                  value={itemNotesDraft[item.id] || ''}
                                  onChange={(event) => handleItemNoteChange(item.id, event.target.value)}
                                  onBlur={() => handleItemNoteBlur(item)}
                                  disabled={updatingItem}
                                  placeholder={t('watchlist.addNote')}
                                  className="w-full min-w-[180px] rounded-lg border border-input bg-background px-2 py-1 text-xs"
                                />
                              </td>
                            )}
                            <td className="px-4 py-3 text-right">
                              <button
                                onClick={() => handleRemoveItem(item.symbol)}
                                disabled={mutating}
                                className="text-muted-foreground hover:text-red-500 disabled:opacity-50"
                              >
                                <Trash2 className="h-4 w-4" />
                              </button>
                            </td>
                          </tr>
                        )
                      })}

                      {orderedItems.length === 0 && (
                        <tr>
                          <td colSpan={8} className="px-4 py-8 text-center text-muted-foreground">
                            <div className="space-y-2">
                              <p>{t('watchlist.empty')}</p>
                              <p className="text-sm">{t('watchlist.emptyAddFirst')}</p>
                            </div>
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          ) : watchlistDetailLoading || watchlistDetailFetching ? (
            <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground shadow-sm">
              {t('common.loading')}
            </div>
          ) : watchlistDetailError ? (
            <div className="rounded-xl border border-danger/40 bg-danger/5 p-8 text-center text-danger shadow-sm">
              <div className="mb-3 flex items-center justify-center gap-2 font-medium">
                <AlertTriangle className="h-4 w-4" />
                {watchlistDetailErrorMessage || t('common.error')}
              </div>
              <button
                onClick={() => refetchWatchlistDetail()}
                className="inline-flex items-center gap-2 rounded-lg border border-danger/40 px-3 py-2 text-sm font-medium transition-colors hover:bg-danger/10"
              >
                <RefreshCw className="h-4 w-4" />
                {t('common.refresh')}
              </button>
            </div>
          ) : (
            <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground shadow-sm">
              {t('watchlist.empty')}
            </div>
          )}
        </div>
      </div>

      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="w-full max-w-lg rounded-xl border border-border bg-card p-6 text-foreground">
            <h3 className="mb-4 text-lg font-semibold">{t('watchlist.create')}</h3>
            <div className="space-y-3">
              <input
                type="text"
                value={newWatchlistForm.name}
                onChange={(event) => setNewWatchlistForm((prev) => ({ ...prev, name: event.target.value }))}
                placeholder={t('watchlist.listName')}
                className="w-full rounded-lg border border-input bg-background px-4 py-2 text-foreground placeholder:text-muted-foreground"
              />
              <input
                type="text"
                value={newWatchlistForm.description}
                onChange={(event) => setNewWatchlistForm((prev) => ({ ...prev, description: event.target.value }))}
                placeholder={t('watchlist.shortDescription')}
                className="w-full rounded-lg border border-input bg-background px-4 py-2 text-foreground placeholder:text-muted-foreground"
              />
              <input
                type="text"
                value={newWatchlistForm.tag}
                onChange={(event) => setNewWatchlistForm((prev) => ({ ...prev, tag: event.target.value }))}
                placeholder={t('watchlist.tag')}
                className="w-full rounded-lg border border-input bg-background px-4 py-2 text-foreground placeholder:text-muted-foreground"
              />
              <textarea
                value={newWatchlistForm.notes}
                onChange={(event) => setNewWatchlistForm((prev) => ({ ...prev, notes: event.target.value }))}
                placeholder={t('watchlist.listNote')}
                rows={2}
                className="w-full rounded-lg border border-input bg-background px-4 py-2 text-foreground placeholder:text-muted-foreground"
              />
            </div>

            <div className="mt-4 flex justify-end gap-3">
              <button
                onClick={() => setShowCreateModal(false)}
                className="rounded-lg px-4 py-2 text-muted-foreground hover:bg-muted"
              >
                {t('common.cancel')}
              </button>
              <button
                onClick={handleCreateWatchlist}
                disabled={creating}
                className="rounded-lg bg-emerald-500 px-4 py-2 text-white hover:bg-emerald-600 disabled:opacity-50"
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
