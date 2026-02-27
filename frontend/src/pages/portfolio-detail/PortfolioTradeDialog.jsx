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
  maxQuantity = null,
}) {
  const { t } = useTranslation()
  const [symbol, setSymbol] = useState(defaultSymbol)
  const [quantity, setQuantity] = useState('')
  const [price, setPrice] = useState('')
  const [orderType, setOrderType] = useState(ORDER_TYPES.MARKET)
  const [limitPrice, setLimitPrice] = useState('')
  const [stopPrice, setStopPrice] = useState('')
  const [tradeDate, setTradeDate] = useState(() => new Date().toISOString().split('T')[0])
  const [notes, setNotes] = useState('')
  const [executeTrade, { isLoading }] = useExecutePortfolioTradeMutation()

  useEffect(() => {
    if (!open) {
      return
    }
    setSymbol(defaultSymbol || '')
    setQuantity(maxQuantity != null ? String(maxQuantity) : '')
    setPrice('')
    setOrderType(ORDER_TYPES.MARKET)
    setLimitPrice('')
    setStopPrice('')
    setNotes('')
    setTradeDate(new Date().toISOString().split('T')[0])
  }, [open, defaultSymbol, maxQuantity])

  const isBuy = mode === 'BUY'
  const resolvedSymbol = (defaultSymbol || symbol || '').toUpperCase().trim()

  const title = useMemo(() => (isBuy ? 'Alis Islemi' : 'Satis Islemi'), [isBuy])

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

    if (!resolvedSymbol) {
      toast.error('Sembol zorunludur')
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

    try {
      await executeTrade({
        portfolioId,
        instrumentSymbol: resolvedSymbol,
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
    } catch {
      toast.error(isBuy ? t('portfolioDetailPage.toast.addError') : t('portfolioDetailPage.toast.deleteError'))
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
                onChange={(event) => setSymbol(event.target.value.toUpperCase())}
                placeholder={t('portfolioDetailPage.addItemDialog.symbolPlaceholder')}
                required
                disabled={Boolean(defaultSymbol)}
              />
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
              />
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
