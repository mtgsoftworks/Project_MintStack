import { useState } from 'react'
import { useTranslation } from 'react-i18next'
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
  const { t, i18n } = useTranslation()
  const [quantity, setQuantity] = useState('')
  const [executeTrade, { isLoading }] = useExecutePortfolioTradeMutation()
  const { data: selectedPortfolio, isFetching: isPortfolioFetching } = useGetPortfolioQuery(
    selectedPortfolioId,
    { skip: !selectedPortfolioId }
  )
  const symbol = (instrument?.symbol || '').toUpperCase()
  const price = Number(instrument?.currentPrice || 0)
  const canTrade = Boolean(symbol && Number.isFinite(price) && price > 0)
  const numberLocale = i18n.language === 'en' ? 'en-US' : 'tr-TR'
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
      toast.error(t('quickTrade.selectPortfolio'))
      return
    }
    if (!canTrade) {
      toast.error(t('quickTrade.invalidPrice'))
      return
    }
    if (!Number.isFinite(parsedQuantity) || parsedQuantity <= 0) {
      toast.error(t('quickTrade.invalidQuantity'))
      return
    }
    if (!selectedPortfolio) {
      toast.error(t('quickTrade.portfolioLoading'))
      return
    }

    if (transactionType === 'SELL') {
      if (holdingQuantity <= 0) {
        toast.error(t('quickTrade.notHeld', { symbol }))
        return
      }
      if (parsedQuantity > holdingQuantity) {
        toast.error(t('quickTrade.insufficientPosition', { quantity: holdingQuantity }))
        return
      }
    }

    if (transactionType === 'BUY') {
      const estimatedGross = parsedQuantity * price
      if (Number.isFinite(cashBalance) && estimatedGross > cashBalance) {
        toast.error(t('quickTrade.insufficientCash', {
          required: estimatedGross.toLocaleString(numberLocale),
          current: cashBalance.toLocaleString(numberLocale),
        }))
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
        notes: t(transactionType === 'BUY' ? 'quickTrade.buyNote' : 'quickTrade.sellNote', { symbol }),
      }).unwrap()

      toast.success(t(transactionType === 'BUY' ? 'quickTrade.buySuccess' : 'quickTrade.sellSuccess'))
      setQuantity('')
    } catch (error) {
      toast.error(getApiErrorMessage(error, t('quickTrade.failed')))
    }
  }

  return (
    <div className={`flex justify-end gap-2 ${className}`}>
      <Input
        className="h-9 w-24 text-right"
        type="number"
        min="0"
        step="0.000001"
        placeholder={t('quickTrade.quantity')}
        value={quantity}
        onChange={(event) => setQuantity(event.target.value)}
      />
      <Button
        size="sm"
        variant="success"
        disabled={isLoading || isPortfolioFetching || !canTrade}
        onClick={() => handleTrade('BUY')}
      >
        {t('quickTrade.buy')}
      </Button>
      <Button
        size="sm"
        variant="outline"
        disabled={isLoading || isPortfolioFetching || !canTrade}
        onClick={() => handleTrade('SELL')}
      >
        {t('quickTrade.sell')}
      </Button>
    </div>
  )
}
