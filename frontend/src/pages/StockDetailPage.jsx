import { useParams, Link } from 'react-router-dom'
import { ArrowLeft, TrendingUp, TrendingDown, RefreshCw, Plus } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { cn, formatCurrency, formatPercent, formatNumber } from '@/lib/utils'
import { useGetStockQuery, useGetStockHistoryQuery } from '@/store/api/marketApi'
import PriceChart from '@/components/charts/PriceChart'
import { useState } from 'react'

export default function StockDetailPage() {
  const { symbol } = useParams()
  const [period, setPeriod] = useState('1M')
  
  const { data: stock, isLoading: stockLoading, refetch } = useGetStockQuery(symbol)
  const { data: history, isLoading: historyLoading } = useGetStockHistoryQuery({ symbol, period })

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
          <p className="text-muted-foreground mb-4">Hisse senedi bulunamadı.</p>
          <Button asChild>
            <Link to="/market/stocks">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Hisselere Dön
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
      {/* Back Button */}
      <Button variant="ghost" asChild>
        <Link to="/market/stocks">
          <ArrowLeft className="mr-2 h-4 w-4" />
          Hisselere Dön
        </Link>
      </Button>

      {/* Stock Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-4">
          <div className={cn(
            "flex h-14 w-14 items-center justify-center rounded-xl",
            isUp ? "bg-success/10" : "bg-danger/10"
          )}>
            {isUp ? (
              <TrendingUp className="h-7 w-7 text-success" />
            ) : (
              <TrendingDown className="h-7 w-7 text-danger" />
            )}
          </div>
          <div>
            <h1 className="text-2xl font-bold">{stock.symbol}</h1>
            <p className="text-muted-foreground">{stock.name}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => refetch()}>
            <RefreshCw className="mr-2 h-4 w-4" />
            Yenile
          </Button>
          <Button>
            <Plus className="mr-2 h-4 w-4" />
            Portföye Ekle
          </Button>
        </div>
      </div>

      {/* Price Info */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Chart */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-3xl font-bold">
                  {formatCurrency(stock.currentPrice, 'TRY')}
                </CardTitle>
                <div className="flex items-center gap-2 mt-1">
                  <Badge variant={isUp ? 'success' : 'danger'} className="text-sm">
                    {isUp ? '+' : ''}{formatCurrency(stock.currentPrice - stock.previousClose, 'TRY')}
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
            {historyLoading ? (
              <Skeleton className="h-64" />
            ) : (
              <PriceChart data={history || []} />
            )}
          </CardContent>
        </Card>

        {/* Stock Details */}
        <Card>
          <CardHeader>
            <CardTitle>Detaylar</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">Önceki Kapanış</span>
              <span className="font-medium">{formatCurrency(stock.previousClose, 'TRY')}</span>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">Açılış</span>
              <span className="font-medium">{formatCurrency(stock.openPrice || stock.previousClose, 'TRY')}</span>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">En Yüksek</span>
              <span className="font-medium">{formatCurrency(stock.highPrice || stock.currentPrice, 'TRY')}</span>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">En Düşük</span>
              <span className="font-medium">{formatCurrency(stock.lowPrice || stock.currentPrice, 'TRY')}</span>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">Hacim</span>
              <span className="font-medium">{stock.volume?.toLocaleString('tr-TR') || '-'}</span>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">Piyasa Değeri</span>
              <span className="font-medium">{stock.marketCap ? formatCurrency(stock.marketCap, 'TRY') : '-'}</span>
            </div>
            <div className="flex justify-between py-2">
              <span className="text-muted-foreground">52 Hafta Aralığı</span>
              <span className="font-medium text-sm">
                {stock.week52Low ? `${formatNumber(stock.week52Low)} - ${formatNumber(stock.week52High)}` : '-'}
              </span>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
