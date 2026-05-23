import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useExecutePortfolioTradeMutation } from '@/store/api/portfolioApi'
import {
  useGetBondsQuery,
  useGetFundsQuery,
  useGetStocksQuery,
  useGetViopQuery,
} from '@/store/api/marketApi'
import { getApiErrorMessage } from '@/lib/apiError'

const ORDER_TYPES = {
  MARKET: 'MARKET',
  LIMIT: 'LIMIT',
  STOP: 'STOP',
}

const ORDER_TYPE_LABEL_KEYS = {
  [ORDER_TYPES.MARKET]: 'portfolioDetailPage.tradeDialog.orderTypes.market',
  [ORDER_TYPES.LIMIT]: 'portfolioDetailPage.tradeDialog.orderTypes.limit',
  [ORDER_TYPES.STOP]: 'portfolioDetailPage.tradeDialog.orderTypes.stop',
}

export function PortfolioTradeDialog({
  portfolioId,
  mode = 'BUY',
  open,
  onOpenChange,
  defaultSymbol = '',
  defaultInstrumentId = null,
  maxQuantity = null,
}) {
  const { t } = useTranslation()
  const [symbol, setSymbol] = useState(defaultSymbol || '')
  const [selectedInstrumentId, setSelectedInstrumentId] = useState(defaultInstrumentId)
  const [quantity, setQuantity] = useState('')
  const [price, setPrice] = useState('')
  const [orderType, setOrderType] = useState(ORDER_TYPES.MARKET)
  const [limitPrice, setLimitPrice] = useState('')
  const [stopPrice, setStopPrice] = useState('')
  const [tradeDate, setTradeDate] = useState(() => new Date().toISOString().split('T')[0])
  const [notes, setNotes] = useState('')
  const [executeTrade, { isLoading }] = useExecutePortfolioTradeMutation()
  const isSell = mode === 'SELL'
  const isLockedInstrument = Boolean(defaultSymbol || defaultInstrumentId)

  const queryOptions = { skip: !open }
  const { data: stocksResponse } = useGetStocksQuery(
    { page: 0, size: 500, sort: 'symbol,asc' },
    queryOptions
  )
  const { data: bondsResponse } = useGetBondsQuery(
    { page: 0, size: 250, sort: 'symbol,asc' },
    queryOptions
  )
  const { data: fundsResponse } = useGetFundsQuery(
    { page: 0, size: 250, sort: 'symbol,asc' },
    queryOptions
  )
  const { data: viopResponse } = useGetViopQuery(
    { page: 0, size: 250, sort: 'symbol,asc' },
    queryOptions
  )

  const instruments = useMemo(() => {
    const combined = [
      ...(stocksResponse?.data || []),
      ...(bondsResponse?.data || []),
      ...(fundsResponse?.data || []),
      ...(viopResponse?.data || []),
    ]

    const uniqueBySymbol = new Map()
    for (const instrument of combined) {
      const symbolValue = (instrument?.symbol || '').toUpperCase()
      if (!symbolValue || uniqueBySymbol.has(symbolValue)) {
        continue
      }
      uniqueBySymbol.set(symbolValue, {
        id: instrument.id,
        symbol: symbolValue,
        name: instrument.name || symbolValue,
        type: instrument.type || '-',
      })
    }
    return [...uniqueBySymbol.values()].sort((left, right) => left.symbol.localeCompare(right.symbol))
  }, [stocksResponse, bondsResponse, fundsResponse, viopResponse])

  const instrumentBySymbol = useMemo(
    () => new Map(instruments.map((instrument) => [instrument.symbol, instrument])),
    [instruments]
  )
  const instrumentById = useMemo(
    () => new Map(instruments.map((instrument) => [instrument.id, instrument])),
    [instruments]
  )

  useEffect(() => {
    if (!open) {
      return
    }
    setSymbol((defaultSymbol || '').toUpperCase())
    setSelectedInstrumentId(defaultInstrumentId)
    setQuantity(maxQuantity != null ? String(maxQuantity) : '')
    setPrice('')
    setOrderType(ORDER_TYPES.MARKET)
    setLimitPrice('')
    setStopPrice('')
    setNotes('')
    setTradeDate(new Date().toISOString().split('T')[0])
  }, [open, defaultSymbol, defaultInstrumentId, maxQuantity])

  useEffect(() => {
    if (!open) {
      return
    }
    const normalizedSymbol = (symbol || '').toUpperCase().trim()
    if (!normalizedSymbol) {
      return
    }
    const match = instrumentBySymbol.get(normalizedSymbol)
    if (match) {
      setSelectedInstrumentId(match.id)
    }
  }, [open, symbol, instrumentBySymbol])

  const isBuy = mode === 'BUY'
  const resolvedSymbol = (symbol || '').toUpperCase().trim()

  const title = useMemo(
    () => t(isBuy ? 'portfolioDetailPage.tradeDialog.buyTitle' : 'portfolioDetailPage.tradeDialog.sellTitle'),
    [isBuy, t]
  )

  const handleInstrumentInputChange = (value) => {
    const normalizedValue = value.toUpperCase().trim()
    setSymbol(normalizedValue)
    const match = instrumentBySymbol.get(normalizedValue)
    setSelectedInstrumentId(match?.id ?? null)
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    const parsedQuantity = parseFloat(quantity)
    if (!Number.isFinite(parsedQuantity) || parsedQuantity <= 0) {
      toast.error(t('portfolioDetailPage.tradeDialog.invalidQuantity'))
      return
    }

    if (maxQuantity != null && parsedQuantity > maxQuantity) {
      toast.error(t('portfolioDetailPage.tradeDialog.maxSellQuantity', { quantity: maxQuantity }))
      return
    }

    if (!resolvedSymbol && !selectedInstrumentId) {
      toast.error(t('portfolioDetailPage.tradeDialog.symbolRequired'))
      return
    }

    const parsedPrice = price !== '' ? parseFloat(price) : undefined
    if (parsedPrice != null && (!Number.isFinite(parsedPrice) || parsedPrice <= 0)) {
      toast.error(t('portfolioDetailPage.tradeDialog.invalidPrice'))
      return
    }

    const parsedLimit = limitPrice !== '' ? parseFloat(limitPrice) : undefined
    if (parsedLimit != null && (!Number.isFinite(parsedLimit) || parsedLimit <= 0)) {
      toast.error(t('portfolioDetailPage.tradeDialog.invalidLimitPrice'))
      return
    }

    const parsedStop = stopPrice !== '' ? parseFloat(stopPrice) : undefined
    if (parsedStop != null && (!Number.isFinite(parsedStop) || parsedStop <= 0)) {
      toast.error(t('portfolioDetailPage.tradeDialog.invalidStopPrice'))
      return
    }

    if (orderType === ORDER_TYPES.LIMIT && parsedLimit == null) {
      toast.error(t('portfolioDetailPage.tradeDialog.limitPriceRequired'))
      return
    }

    if (orderType === ORDER_TYPES.STOP && parsedStop == null) {
      toast.error(t('portfolioDetailPage.tradeDialog.stopPriceRequired'))
      return
    }

    const selectedInstrument = selectedInstrumentId ? instrumentById.get(selectedInstrumentId) : null
    const resolvedInstrumentId = selectedInstrument?.id ?? selectedInstrumentId ?? null

    try {
      await executeTrade({
        portfolioId,
        instrumentId: resolvedInstrumentId || undefined,
        instrumentSymbol: resolvedInstrumentId ? undefined : resolvedSymbol,
        transactionType: mode,
        orderType,
        quantity: parsedQuantity,
        price: parsedPrice,
        limitPrice: parsedLimit,
        stopPrice: parsedStop,
        transactionDate: tradeDate,
        notes: notes.trim() || undefined,
      }).unwrap()

      toast.success(t(isBuy ? 'portfolioDetailPage.tradeDialog.buySuccess' : 'portfolioDetailPage.tradeDialog.sellSuccess'))
      onOpenChange(false)
    } catch (error) {
      toast.error(getApiErrorMessage(
        error,
        isBuy ? t('portfolioDetailPage.toast.addError') : t('portfolioDetailPage.toast.deleteError')
      ))
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>
            {isBuy
              ? t('portfolioDetailPage.tradeDialog.buyDescription')
              : t('portfolioDetailPage.tradeDialog.sellDescription')}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit}>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="trade-symbol">{t('portfolioDetailPage.tradeDialog.symbol')}</Label>
              <Input
                id="trade-symbol"
                value={resolvedSymbol}
                onChange={(event) => handleInstrumentInputChange(event.target.value)}
                list="trade-instruments-list"
                placeholder={t('portfolioDetailPage.tradeDialog.symbolPlaceholder')}
                required
                disabled={isLockedInstrument}
              />
              <datalist id="trade-instruments-list">
                {instruments.map((instrument) => (
                  <option
                    key={instrument.id || instrument.symbol}
                    value={instrument.symbol}
                  >
                    {`${instrument.name} (${instrument.type})`}
                  </option>
                ))}
              </datalist>
              {!isLockedInstrument && (
                <p className="text-xs text-muted-foreground">
                  {t('portfolioDetailPage.tradeDialog.instrumentCount', { count: instruments.length })}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="trade-quantity">{t('portfolioDetailPage.tradeDialog.quantity')}</Label>
              <Input
                id="trade-quantity"
                type="number"
                step="0.000001"
                min="0.000001"
                value={quantity}
                onChange={(event) => setQuantity(event.target.value)}
                placeholder={t('portfolioDetailPage.addItemDialog.quantityPlaceholder')}
                required
                max={isSell && maxQuantity != null ? String(maxQuantity) : undefined}
              />
              {isSell && maxQuantity != null && (
                <p className="text-xs text-muted-foreground">{t('portfolioDetailPage.tradeDialog.maxSellQuantity', { quantity: maxQuantity })}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label>{t('portfolioDetailPage.tradeDialog.orderType')}</Label>
              <Select value={orderType} onValueChange={setOrderType}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ORDER_TYPES.MARKET}>
                    {t(ORDER_TYPE_LABEL_KEYS[ORDER_TYPES.MARKET])}
                  </SelectItem>
                  <SelectItem value={ORDER_TYPES.LIMIT}>
                    {t(ORDER_TYPE_LABEL_KEYS[ORDER_TYPES.LIMIT])}
                  </SelectItem>
                  <SelectItem value={ORDER_TYPES.STOP}>
                    {t(ORDER_TYPE_LABEL_KEYS[ORDER_TYPES.STOP])}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>

            {orderType === ORDER_TYPES.MARKET && (
              <div className="space-y-2">
                <Label htmlFor="trade-price">{t('portfolioDetailPage.tradeDialog.priceOptional')}</Label>
                <Input
                  id="trade-price"
                  type="number"
                  step="0.000001"
                  min="0.000001"
                  value={price}
                  onChange={(event) => setPrice(event.target.value)}
                  placeholder={t('portfolioDetailPage.tradeDialog.pricePlaceholder')}
                />
              </div>
            )}

            {orderType === ORDER_TYPES.LIMIT && (
              <div className="space-y-2">
                <Label htmlFor="trade-limit-price">{t('portfolioDetailPage.tradeDialog.limitPrice')}</Label>
                <Input
                  id="trade-limit-price"
                  type="number"
                  step="0.000001"
                  min="0.000001"
                  value={limitPrice}
                  onChange={(event) => setLimitPrice(event.target.value)}
                  placeholder={t('portfolioDetailPage.tradeDialog.limitPrice')}
                  required
                />
              </div>
            )}

            {orderType === ORDER_TYPES.STOP && (
              <div className="space-y-2">
                <Label htmlFor="trade-stop-price">{t('portfolioDetailPage.tradeDialog.stopPrice')}</Label>
                <Input
                  id="trade-stop-price"
                  type="number"
                  step="0.000001"
                  min="0.000001"
                  value={stopPrice}
                  onChange={(event) => setStopPrice(event.target.value)}
                  placeholder={t('portfolioDetailPage.tradeDialog.stopPrice')}
                  required
                />
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="trade-date">{t('portfolioDetailPage.tradeDialog.tradeDate')}</Label>
              <Input
                id="trade-date"
                type="date"
                value={tradeDate}
                onChange={(event) => setTradeDate(event.target.value)}
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="trade-notes">{t('portfolioDetailPage.tradeDialog.note')}</Label>
              <Input
                id="trade-notes"
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
                placeholder={t('portfolioDetailPage.tradeDialog.optionalNote')}
              />
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              {t('common.cancel')}
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading
                ? t('profile.saving')
                : isBuy
                  ? t('quickTrade.buy')
                  : t('quickTrade.sell')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
