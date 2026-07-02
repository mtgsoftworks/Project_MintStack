import { useMemo, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ArrowLeft, TrendingUp, TrendingDown, Plus, Minus } from 'lucide-react'
import { toast } from 'sonner'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import SimulationDataFlag from '@/components/common/SimulationDataFlag'
import RefreshButton from '@/components/common/RefreshButton'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { isSimulatedMarketData } from '@/lib/simulationData'
import { getApiErrorMessage } from '@/lib/apiError'
import { cn, formatCurrency, formatPercent, formatNumber } from '@/lib/utils'
import { useGetStockQuery, useGetStockHistoryQuery } from '@/store/api/marketApi'
import { useMarketDataRefresh } from '@/hooks/useMarketDataRefresh'
import {
  useExecutePortfolioTradeMutation,
  useGetPortfoliosQuery,
} from '@/store/api/portfolioApi'
import PriceChart from '@/components/charts/PriceChart'

const PERIOD_DAYS = {
  '1D': 0,
  '1W': 7,
  '1M': 30,
  '3M': 90,
  '1Y': 365,
}

function formatDateInput(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function hasFiniteChange(value) {
  const numeric = Number(value)
  return value !== null && value !== undefined && Number.isFinite(numeric) && numeric !== 0
}

export default function StockDetailPage() {
  const { t, i18n } = useTranslation()
  const { symbol } = useParams()
  const [period, setPeriod] = useState('1M')
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false)
  const [selectedPortfolioId, setSelectedPortfolioId] = useState('')
  const [quantity, setQuantity] = useState('1')
  const { refreshAndRefetch, isRefreshingMarketData } = useMarketDataRefresh(['BIST_STOCKS'])
  const changeRangeParams = useMemo(() => {
    const days = PERIOD_DAYS[period] ?? 30
    const endDate = new Date()
    const startDate = new Date(endDate)
    startDate.setDate(startDate.getDate() - days)
    return {
      changeStartDate: formatDateInput(startDate),
      changeEndDate: formatDateInput(endDate),
    }
  }, [period])

  const { data: stock, isLoading: stockLoading, isFetching: stockFetching, refetch } = useGetStockQuery({
    symbol,
    ...changeRangeParams,
  })
  const { data: history, isLoading: historyLoading, refetch: refetchHistory } = useGetStockHistoryQuery({ symbol, period })
  const { data: portfolios = [], isLoading: portfoliosLoading } = useGetPortfoliosQuery(undefined)
  const [executeTrade, { isLoading: isSubmittingTrade }] = useExecutePortfolioTradeMutation()
  const numberLocale = i18n.language === 'en' ? 'en-US' : 'tr-TR'

  const openAddDialog = () => {
    if (!stock?.symbol) {
      toast.error(t('stockDetail.instrumentMissing'))
      return
    }

    if (portfoliosLoading) {
      toast.error(t('stockDetail.portfoliosLoading'))
      return
    }

    if (!portfolios.length) {
      toast.error(t('stockDetail.noPortfolio'))
      return
    }

    const defaultPortfolio = portfolios.find((portfolio) => portfolio.isDefault) || portfolios[0]
    setSelectedPortfolioId(defaultPortfolio?.id || '')
    setQuantity('1')
    setIsAddDialogOpen(true)
  }

  const handleAddToPortfolio = async (event) => {
    event.preventDefault()

    const parsedQuantity = Number.parseFloat(quantity)
    if (!Number.isFinite(parsedQuantity) || parsedQuantity <= 0) {
      toast.error(t('stockDetail.invalidQuantity'))
      return
    }

    if (!selectedPortfolioId) {
      toast.error(t('stockDetail.selectPortfolioError'))
      return
    }

    const selectedPortfolio = portfolios.find((portfolio) => portfolio.id === selectedPortfolioId)
    const currentPrice = Number(stock.currentPrice || 0)
    const cashBalance = Number(selectedPortfolio?.cashBalance || 0)
    const estimatedGross = parsedQuantity * currentPrice
    if (Number.isFinite(currentPrice) && currentPrice > 0 && estimatedGross > cashBalance) {
      toast.error(t('stockDetail.insufficientCash', {
        required: estimatedGross.toLocaleString(numberLocale),
        current: cashBalance.toLocaleString(numberLocale),
      }))
      return
    }

    try {
      await executeTrade({
        portfolioId: selectedPortfolioId,
        instrumentId: stock.id || undefined,
        instrumentSymbol: stock.id ? undefined : stock.symbol,
        transactionType: 'BUY',
        orderType: 'MARKET',
        quantity: parsedQuantity,
        price: currentPrice > 0 ? currentPrice : undefined,
      }).unwrap()

      toast.success(t('stockDetail.addSuccess', { symbol: stock.symbol }))
      setIsAddDialogOpen(false)
    } catch (error) {
      toast.error(getApiErrorMessage(error, t('stockDetail.addError')))
    }
  }

  if (stockLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2">
            <Skeleton className="h-96" />
          </div>
          <Skeleton className="h-96" />
        </div>
      </div>
    )
  }

  if (!stock) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-12">
          <p className="mb-4 text-muted-foreground">{t('stockDetail.notFound')}</p>
          <Button asChild>
            <Link to="/market/stocks">
              <ArrowLeft className="mr-2 h-4 w-4" />
              {t('stockDetail.backToStocks')}
            </Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const hasChange = hasFiniteChange(stock.changePercent)
  const change = hasChange ? Number(stock.changePercent) : null
  const isUp = hasChange && change >= 0

  return (
    <div className="space-y-6 animate-in">
      <Button variant="ghost" asChild>
        <Link to="/market/stocks">
          <ArrowLeft className="mr-2 h-4 w-4" />
          {t('stockDetail.backToStocks')}
        </Link>
      </Button>

      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-4">
          <div
            className={cn(
              'flex h-14 w-14 items-center justify-center rounded-xl',
              !hasChange ? 'bg-muted' : isUp ? 'bg-success/10' : 'bg-danger/10'
            )}
          >
            {!hasChange ? (
              <Minus className="h-7 w-7 text-muted-foreground" />
            ) : isUp ? (
              <TrendingUp className="h-7 w-7 text-success" />
            ) : (
              <TrendingDown className="h-7 w-7 text-danger" />
            )}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-bold">{stock.symbol}</h1>
              {isSimulatedMarketData(stock) && <SimulationDataFlag />}
            </div>
            <p className="text-muted-foreground">{stock.name}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <RefreshButton
            variant="outline"
            isLoading={stockFetching || isRefreshingMarketData}
            onRefresh={() => refreshAndRefetch(() => Promise.all([refetch(), refetchHistory()]))}
          >
            {t('common.refresh')}
          </RefreshButton>
          <Button onClick={openAddDialog}>
            <Plus className="mr-2 h-4 w-4" />
            {t('stockDetail.addToPortfolio')}
          </Button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-3xl font-bold">
                  {formatCurrency(stock.currentPrice, 'TRY')}
                </CardTitle>
                <div className="mt-1 flex items-center gap-2">
                  <Badge variant={!hasChange ? 'secondary' : isUp ? 'success' : 'danger'} className="text-sm">
                    {hasChange && isUp ? '+' : ''}
                    {hasChange ? formatCurrency(stock.change, 'TRY') : '-'}
                  </Badge>
                  <Badge variant={!hasChange ? 'secondary' : isUp ? 'success' : 'danger'} className="text-sm">
                    {hasChange ? formatPercent(change) : '-'}
                  </Badge>
                </div>
              </div>
              <Tabs value={period} onValueChange={setPeriod}>
                <TabsList>
                  <TabsTrigger value="1D">1G</TabsTrigger>
                  <TabsTrigger value="1W">1H</TabsTrigger>
                  <TabsTrigger value="1M">1A</TabsTrigger>
                  <TabsTrigger value="3M">3A</TabsTrigger>
                  <TabsTrigger value="1Y">1Y</TabsTrigger>
                </TabsList>
              </Tabs>
            </div>
          </CardHeader>
          <CardContent>
            {historyLoading ? <Skeleton className="h-64" /> : <PriceChart data={history || []} />}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t('stockDetail.details')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex justify-between border-b py-2">
              <span className="text-muted-foreground">{t('stockDetail.previousClose')}</span>
              <span className="font-medium">{formatCurrency(stock.previousClose, 'TRY')}</span>
            </div>
            <div className="flex justify-between border-b py-2">
              <span className="text-muted-foreground">{t('stockDetail.open')}</span>
              <span className="font-medium">
                {formatCurrency(stock.openPrice || stock.previousClose, 'TRY')}
              </span>
            </div>
            <div className="flex justify-between border-b py-2">
              <span className="text-muted-foreground">{t('stockDetail.high')}</span>
              <span className="font-medium">
                {formatCurrency(stock.highPrice || stock.currentPrice, 'TRY')}
              </span>
            </div>
            <div className="flex justify-between border-b py-2">
              <span className="text-muted-foreground">{t('stockDetail.low')}</span>
              <span className="font-medium">
                {formatCurrency(stock.lowPrice || stock.currentPrice, 'TRY')}
              </span>
            </div>
            <div className="flex justify-between border-b py-2">
              <span className="text-muted-foreground">{t('stockDetail.volume')}</span>
              <span className="font-medium">{stock.volume?.toLocaleString(numberLocale) || '-'}</span>
            </div>
            <div className="flex justify-between border-b py-2">
              <span className="text-muted-foreground">{t('stockDetail.marketCap')}</span>
              <span className="font-medium">
                {stock.marketCap ? formatCurrency(stock.marketCap, 'TRY') : '-'}
              </span>
            </div>
            <div className="flex justify-between py-2">
              <span className="text-muted-foreground">{t('stockDetail.week52Range')}</span>
              <span className="text-sm font-medium">
                {stock.week52Low != null && stock.week52High != null
                  ? `${formatNumber(stock.week52Low)} - ${formatNumber(stock.week52High)}`
                  : '-'}
              </span>
            </div>
          </CardContent>
        </Card>
      </div>

      <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('stockDetail.addToPortfolio')}</DialogTitle>
            <DialogDescription>{t('stockDetail.addDescription', { symbol: stock.symbol })}</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleAddToPortfolio}>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="add-portfolio">{t('stockDetail.portfolio')}</Label>
                <Select value={selectedPortfolioId} onValueChange={setSelectedPortfolioId}>
                  <SelectTrigger id="add-portfolio">
                    <SelectValue placeholder={t('stockDetail.selectPortfolio')} />
                  </SelectTrigger>
                  <SelectContent>
                    {portfolios.map((portfolio) => (
                      <SelectItem key={portfolio.id} value={portfolio.id}>
                        {portfolio.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="add-quantity">{t('stockDetail.quantity')}</Label>
                <Input
                  id="add-quantity"
                  type="number"
                  min="0.000001"
                  step="0.000001"
                  value={quantity}
                  onChange={(event) => setQuantity(event.target.value)}
                  required
                />
              </div>
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setIsAddDialogOpen(false)}>
                {t('stockDetail.cancel')}
              </Button>
              <Button type="submit" disabled={isSubmittingTrade}>
                {isSubmittingTrade ? t('stockDetail.saving') : t('stockDetail.add')}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
