import { useState } from 'react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useExecutePortfolioTradeMutation } from '@/store/api/portfolioApi'

function resolveApiErrorMessage(error, fallbackMessage) {
  const responseData = error?.data
  if (!responseData) {
    return fallbackMessage
  }
  if (typeof responseData === 'string') {
    return responseData
  }
  return responseData.message || responseData.error || responseData.details || fallbackMessage
}

export default function PortfolioQuickTradeCell({
  instrument,
  selectedPortfolioId,
  className = '',
}) {
  const [quantity, setQuantity] = useState('')
  const [executeTrade, { isLoading }] = useExecutePortfolioTradeMutation()
  const symbol = (instrument?.symbol || '').toUpperCase()
  const price = Number(instrument?.currentPrice || 0)
  const canTrade = Boolean(symbol && Number.isFinite(price) && price > 0)

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
      toast.error(resolveApiErrorMessage(error, 'Portfoy islemi basarisiz oldu'))
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
        disabled={isLoading || !canTrade}
        onClick={() => handleTrade('BUY')}
      >
        Al
      </Button>
      <Button
        size="sm"
        variant="outline"
        disabled={isLoading || !canTrade}
        onClick={() => handleTrade('SELL')}
      >
        Sat
      </Button>
    </div>
  )
}
