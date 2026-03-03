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

const ORDER_TYPES = {
  MARKET: 'MARKET',
  LIMIT: 'LIMIT',
  STOP: 'STOP',
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

  const title = useMemo(() => (isBuy ? 'Alis Islemi' : 'Satis Islemi'), [isBuy])

  const handleInstrumentInputChange = (value) => {
    const normalizedValue = value.toUpperCase().trim()
    setSymbol(normalizedValue)
    const match = instrumentBySymbol.get(normalizedValue)
    setSelectedInstrumentId(match?.id ?? null)
  }

  const resolveApiErrorMessage = (error, fallbackMessage) => {
    const responseData = error?.data
    if (!responseData) {
      return fallbackMessage
    }
    if (typeof responseData === 'string') {
      return responseData
    }
    return (
      responseData.message ||
      responseData.error ||
      responseData.details ||
      fallbackMessage
    )
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    const parsedQuantity = parseFloat(quantity)
    if (!Number.isFinite(parsedQuantity) || parsedQuantity <= 0) {
      toast.error('Gecerli miktar girin')
      return
    }

    if (maxQuantity != null && parsedQuantity > maxQuantity) {
      toast.error(`Maksimum satis miktari: ${maxQuantity}`)
      return
    }

    if (!resolvedSymbol && !selectedInstrumentId) {
      toast.error('Sembol zorunludur')
      return
    }
    if (!selectedInstrumentId && instruments.length > 0) {
      toast.error('Listeden gecerli bir varlik secin')
      return
    }

    const parsedPrice = price !== '' ? parseFloat(price) : undefined
    if (parsedPrice != null && (!Number.isFinite(parsedPrice) || parsedPrice <= 0)) {
      toast.error('Gecerli fiyat girin')
      return
    }

    const parsedLimit = limitPrice !== '' ? parseFloat(limitPrice) : undefined
    if (parsedLimit != null && (!Number.isFinite(parsedLimit) || parsedLimit <= 0)) {
      toast.error('Gecerli limit fiyat girin')
      return
    }

    const parsedStop = stopPrice !== '' ? parseFloat(stopPrice) : undefined
    if (parsedStop != null && (!Number.isFinite(parsedStop) || parsedStop <= 0)) {
      toast.error('Gecerli stop fiyat girin')
      return
    }

    if (orderType === ORDER_TYPES.LIMIT && parsedLimit == null) {
      toast.error('Limit emir icin limit fiyat zorunludur')
      return
    }

    if (orderType === ORDER_TYPES.STOP && parsedStop == null) {
      toast.error('Stop emir icin stop fiyat zorunludur')
      return
    }

    const selectedInstrument = selectedInstrumentId ? instrumentById.get(selectedInstrumentId) : null

    try {
      await executeTrade({
        portfolioId,
        instrumentId: selectedInstrument?.id,
        instrumentSymbol: selectedInstrument ? undefined : resolvedSymbol,
        transactionType: mode,
        orderType,
        quantity: parsedQuantity,
        price: parsedPrice,
        limitPrice: parsedLimit,
        stopPrice: parsedStop,
        transactionDate: tradeDate,
        notes: notes.trim() || undefined,
      }).unwrap()

      toast.success(isBuy ? 'Alis islemi kaydedildi' : 'Satis islemi kaydedildi')
      onOpenChange(false)
    } catch (error) {
      toast.error(resolveApiErrorMessage(
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
              ? 'Portfoye alim emri girin. Nakit bakiye ve komisyon kontrol edilir.'
              : 'Pozisyonunuzdan kisimli veya tam satis emri girin.'}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit}>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="trade-symbol">Sembol</Label>
              <Input
                id="trade-symbol"
                value={resolvedSymbol}
                onChange={(event) => handleInstrumentInputChange(event.target.value)}
                list="trade-instruments-list"
                placeholder="Sembol yazin ve listeden secin"
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
                  {instruments.length} varlik listelendi. Sembol yazarak secim yapabilirsiniz.
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="trade-quantity">Miktar</Label>
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
                <p className="text-xs text-muted-foreground">Maksimum satis miktari: {maxQuantity}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label>Emir Tipi</Label>
              <Select value={orderType} onValueChange={setOrderType}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ORDER_TYPES.MARKET}>MARKET</SelectItem>
                  <SelectItem value={ORDER_TYPES.LIMIT}>LIMIT</SelectItem>
                  <SelectItem value={ORDER_TYPES.STOP}>STOP</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {orderType === ORDER_TYPES.MARKET && (
              <div className="space-y-2">
                <Label htmlFor="trade-price">Fiyat (Opsiyonel)</Label>
                <Input
                  id="trade-price"
                  type="number"
                  step="0.000001"
                  min="0.000001"
                  value={price}
                  onChange={(event) => setPrice(event.target.value)}
                  placeholder="Bos birakirsaniz anlik fiyat kullanilir"
                />
              </div>
            )}

            {orderType === ORDER_TYPES.LIMIT && (
              <div className="space-y-2">
                <Label htmlFor="trade-limit-price">Limit Fiyat</Label>
                <Input
                  id="trade-limit-price"
                  type="number"
                  step="0.000001"
                  min="0.000001"
                  value={limitPrice}
                  onChange={(event) => setLimitPrice(event.target.value)}
                  placeholder="Limit fiyat"
                  required
                />
              </div>
            )}

            {orderType === ORDER_TYPES.STOP && (
              <div className="space-y-2">
                <Label htmlFor="trade-stop-price">Stop Fiyat</Label>
                <Input
                  id="trade-stop-price"
                  type="number"
                  step="0.000001"
                  min="0.000001"
                  value={stopPrice}
                  onChange={(event) => setStopPrice(event.target.value)}
                  placeholder="Stop fiyat"
                  required
                />
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="trade-date">Islem Tarihi</Label>
              <Input
                id="trade-date"
                type="date"
                value={tradeDate}
                onChange={(event) => setTradeDate(event.target.value)}
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="trade-notes">Not</Label>
              <Input
                id="trade-notes"
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
                placeholder="Opsiyonel not"
              />
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              {t('common.cancel')}
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? 'Kaydediliyor...' : isBuy ? 'Al' : 'Sat'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
