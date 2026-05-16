import { useState } from 'react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { getApiErrorMessage } from '@/lib/apiError'
import { useExecutePortfolioTradeMutation, useGetPortfolioQuery } from '@/store/api/portfolioApi'

export default function PortfolioQuickTradeCell({
  instrument,
  selectedPortfolioId,
  className = '',
}) {
  const [quantity, setQuantity] = useState('')
  const [executeTrade, { isLoading }] = useExecutePortfolioTradeMutation()
  const { data: selectedPortfolio, isFetching: isPortfolioFetching } = useGetPortfolioQuery(
    selectedPortfolioId,
    { skip: !selectedPortfolioId }
  )
  const symbol = (instrument?.symbol || '').toUpperCase()
  const price = Number(instrument?.currentPrice || 0)
  const canTrade = Boolean(symbol && Number.isFinite(price) && price > 0)
  const holdingQuantity = (selectedPortfolio?.items || [])
    .filter((item) => {
      const itemSymbol = (item.instrumentSymbol || item.symbol || '').toUpperCase()
      return item.instrumentId === instrument?.id || itemSymbol === symbol
    })
    .reduce((total, item) => {
      const itemQuantity = Number(item.quantity || 0)
      return Number.isFinite(itemQuantity) ? total + itemQuantity : total
    }, 0)
  const cashBalance = Number(selectedPortfolio?.cashBalance || 0)

  const handleTrade = async (transactionType) => {
    const parsedQuantity = Number(quantity)

    if (!selectedPortfolioId) {
      toast.error('Once bir portfoy secin veya olusturun')
      return
    }
    if (!canTrade) {
      toast.error('Bu varlik icin gecerli fiyat bulunamadi')
      return
    }
    if (!Number.isFinite(parsedQuantity) || parsedQuantity <= 0) {
      toast.error('Gecerli miktar girin')
      return
    }
    if (!selectedPortfolio) {
      toast.error('Portfoy bilgisi yukleniyor, tekrar deneyin')
      return
    }

    if (transactionType === 'SELL') {
      if (holdingQuantity <= 0) {
        toast.error(`${symbol} portfoyde yok; satis yapilamaz`)
        return
      }
      if (parsedQuantity > holdingQuantity) {
        toast.error(`Yetersiz pozisyon. Mevcut: ${holdingQuantity}`)
        return
      }
    }

    if (transactionType === 'BUY') {
      const estimatedGross = parsedQuantity * price
      if (Number.isFinite(cashBalance) && estimatedGross > cashBalance) {
        toast.error(`Yetersiz nakit bakiye. Tahmini gerekli: ${estimatedGross.toLocaleString('tr-TR')}, mevcut: ${cashBalance.toLocaleString('tr-TR')}`)
        return
      }
    }

    try {
      await executeTrade({
        portfolioId: selectedPortfolioId,
        instrumentId: instrument?.id || undefined,
        instrumentSymbol: instrument?.id ? undefined : symbol,
        transactionType,
        orderType: 'MARKET',
        quantity: parsedQuantity,
        price,
        transactionDate: new Date().toISOString().slice(0, 10),
        notes: `${symbol} ${transactionType === 'BUY' ? 'alim' : 'satis'} islemi`,
      }).unwrap()

      toast.success(transactionType === 'BUY' ? 'Varlik portfoye eklendi' : 'Satis emri islendi')
      setQuantity('')
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Portfoy islemi basarisiz oldu'))
    }
  }

  return (
    <div className={`flex justify-end gap-2 ${className}`}>
      <Input
        className="h-9 w-24 text-right"
        type="number"
        min="0"
        step="0.000001"
        placeholder="Miktar"
        value={quantity}
        onChange={(event) => setQuantity(event.target.value)}
      />
      <Button
        size="sm"
        variant="success"
        disabled={isLoading || isPortfolioFetching || !canTrade}
        onClick={() => handleTrade('BUY')}
      >
        Al
      </Button>
      <Button
        size="sm"
        variant="outline"
        disabled={isLoading || isPortfolioFetching || !canTrade}
        onClick={() => handleTrade('SELL')}
      >
        Sat
      </Button>
    </div>
  )
}
