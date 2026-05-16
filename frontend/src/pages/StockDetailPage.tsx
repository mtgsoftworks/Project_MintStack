import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { ArrowLeft, TrendingUp, TrendingDown, Plus } from 'lucide-react'
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
import {
  useExecutePortfolioTradeMutation,
  useGetPortfoliosQuery,
} from '@/store/api/portfolioApi'
import PriceChart from '@/components/charts/PriceChart'

export default function StockDetailPage() {
  const { symbol } = useParams()
  const [period, setPeriod] = useState('1M')
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false)
  const [selectedPortfolioId, setSelectedPortfolioId] = useState('')
  const [quantity, setQuantity] = useState('1')

  const { data: stock, isLoading: stockLoading, isFetching: stockFetching, refetch } = useGetStockQuery(symbol)
  const { data: history, isLoading: historyLoading } = useGetStockHistoryQuery({ symbol, period })
  const { data: portfolios = [], isLoading: portfoliosLoading } = useGetPortfoliosQuery()
  const [executeTrade, { isLoading: isSubmittingTrade }] = useExecutePortfolioTradeMutation()

  const openAddDialog = () => {
    if (!stock?.symbol) {
      toast.error('Enstruman bilgisi alinamadi')
      return
    }

    if (portfoliosLoading) {
      toast.error('Portfoyler yukleniyor, tekrar deneyin')
      return
    }

    if (!portfolios.length) {
      toast.error('Portfoy bulunamadi. Once bir portfoy olusturun')
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
      toast.error('Gecerli miktar girin')
      return
    }

    if (!selectedPortfolioId) {
      toast.error('Portfoy secin')
      return
    }

    const selectedPortfolio = portfolios.find((portfolio) => portfolio.id === selectedPortfolioId)
    const currentPrice = Number(stock.currentPrice || 0)
    const cashBalance = Number(selectedPortfolio?.cashBalance || 0)
    const estimatedGross = parsedQuantity * currentPrice
    if (Number.isFinite(currentPrice) && currentPrice > 0 && estimatedGross > cashBalance) {
      toast.error(`Yetersiz nakit bakiye. Tahmini gerekli: ${estimatedGross.toLocaleString('tr-TR')}, mevcut: ${cashBalance.toLocaleString('tr-TR')}`)
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

      toast.success('Portfoye eklendi')
      setIsAddDialogOpen(false)
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Portfoye eklenemedi'))
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
          <p className="mb-4 text-muted-foreground">Hisse senedi bulunamadi.</p>
          <Button asChild>
            <Link to="/market/stocks">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Hisselere Don
            </Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const change = stock.changePercent || 0
  const isUp = change >= 0

  return (
    <div className="space-y-6 animate-in">
      <Button variant="ghost" asChild>
        <Link to="/market/stocks">
          <ArrowLeft className="mr-2 h-4 w-4" />
          Hisselere Don
        </Link>
      </Button>

      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-4">
          <div
            className={cn(
              'flex h-14 w-14 items-center justify-center rounded-xl',
              isUp ? 'bg-success/10' : 'bg-danger/10'
            )}
          >
            {isUp ? (
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
          <RefreshButton variant="outline" isLoading={stockFetching} onRefresh={refetch}>
            Yenile
          </RefreshButton>
          <Button onClick={openAddDialog}>
            <Plus className="mr-2 h-4 w-4" />
            Portfoye Ekle
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
                  <Badge variant={isUp ? 'success' : 'danger'} className="text-sm">
                    {isUp ? '+' : ''}
                    {formatCurrency(stock.currentPrice - stock.previousClose, 'TRY')}
                  </Badge>
                  <Badge variant={isUp ? 'success' : 'danger'} className="text-sm">
                    {formatPercent(change)}
                  </Badge>
                </div>
              </div>
              <Tabs value={period} onValueChange={setPeriod}>
                <TabsList>
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
            <CardTitle>Detaylar</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex justify-between border-b py-2">
              <span className="text-muted-foreground">Onceki Kapanis</span>
              <span className="font-medium">{formatCurrency(stock.previousClose, 'TRY')}</span>
            </div>
            <div className="flex justify-between border-b py-2">
              <span className="text-muted-foreground">Acilis</span>
              <span className="font-medium">
                {formatCurrency(stock.openPrice || stock.previousClose, 'TRY')}
              </span>
            </div>
            <div className="flex justify-between border-b py-2">
              <span className="text-muted-foreground">En Yuksek</span>
              <span className="font-medium">
                {formatCurrency(stock.highPrice || stock.currentPrice, 'TRY')}
              </span>
            </div>
            <div className="flex justify-between border-b py-2">
              <span className="text-muted-foreground">En Dusuk</span>
              <span className="font-medium">
                {formatCurrency(stock.lowPrice || stock.currentPrice, 'TRY')}
              </span>
            </div>
            <div className="flex justify-between border-b py-2">
              <span className="text-muted-foreground">Hacim</span>
              <span className="font-medium">{stock.volume?.toLocaleString('tr-TR') || '-'}</span>
            </div>
            <div className="flex justify-between border-b py-2">
              <span className="text-muted-foreground">Piyasa Degeri</span>
              <span className="font-medium">
                {stock.marketCap ? formatCurrency(stock.marketCap, 'TRY') : '-'}
              </span>
            </div>
            <div className="flex justify-between py-2">
              <span className="text-muted-foreground">52 Hafta Araligi</span>
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
            <DialogTitle>Portfoye Ekle</DialogTitle>
            <DialogDescription>{stock.symbol} icin alim emri olusturulur.</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleAddToPortfolio}>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="add-portfolio">Portfoy</Label>
                <Select value={selectedPortfolioId} onValueChange={setSelectedPortfolioId}>
                  <SelectTrigger id="add-portfolio">
                    <SelectValue placeholder="Portfoy secin" />
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
                <Label htmlFor="add-quantity">Miktar</Label>
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
                Vazgec
              </Button>
              <Button type="submit" disabled={isSubmittingTrade}>
                {isSubmittingTrade ? 'Kaydediliyor...' : 'Ekle'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
