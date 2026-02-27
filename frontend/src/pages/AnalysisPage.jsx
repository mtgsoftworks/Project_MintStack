import { useMemo, useState } from 'react'
import { RefreshCw, TrendingDown, TrendingUp } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import PriceChart from '@/components/charts/PriceChart'
import { cn, formatCurrency, formatPercent } from '@/lib/utils'
import {
  useGetComparisonQuery,
  useGetMovingAverageQuery,
  useGetTrendAnalysisQuery,
} from '@/store/api/analysisApi'

export default function AnalysisPage() {
  const [symbol, setSymbol] = useState('THYAO')
  const [period, setPeriod] = useState('1M')
  const [maType, setMaType] = useState('SMA')
  const [maPeriod, setMaPeriod] = useState('20')
  const [compareInput, setCompareInput] = useState('THYAO,GARAN')

  const compareSymbols = useMemo(
    () =>
      compareInput
        .split(/[\s,;]+/)
        .map((value) => value.trim().toUpperCase())
        .filter(Boolean)
        .slice(0, 5),
    [compareInput]
  )

  const { data: maData, isLoading: maLoading, refetch: refetchMa } = useGetMovingAverageQuery({
    symbol,
    maType,
    maPeriod: Number.parseInt(maPeriod, 10),
  })

  const { data: trendData, isLoading: trendLoading, refetch: refetchTrend } = useGetTrendAnalysisQuery({
    symbol,
    period,
  })

  const {
    data: comparisonData,
    isLoading: comparisonLoading,
    refetch: refetchComparison,
  } = useGetComparisonQuery(
    {
      symbols: compareSymbols,
      period,
    },
    {
      skip: compareSymbols.length < 2,
    }
  )

  const trendDirection =
    trendData?.trend === 'UPTREND' ? 'UP' : trendData?.trend === 'DOWNTREND' ? 'DOWN' : 'SIDEWAYS'

  const comparisonItems = (comparisonData || [])
    .map((item) => {
      const dataPoints = item.data || []
      if (dataPoints.length === 0) {
        return null
      }

      const lastPoint = dataPoints[dataPoints.length - 1]
      const currentPrice = lastPoint.price ?? lastPoint.value ?? 0
      const changePercent = lastPoint.value ?? 0

      return {
        symbol: item.symbol,
        name: item.name,
        currentPrice,
        changePercent,
      }
    })
    .filter(Boolean)

  const handleRefreshAll = () => {
    refetchMa()
    refetchTrend()
    if (compareSymbols.length >= 2) {
      refetchComparison()
    }
  }

  return (
    <div className="space-y-6 animate-in">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Teknik Analiz</h1>
          <p className="text-muted-foreground">Hareketli ortalama, trend ve karsilastirma</p>
        </div>
        <div className="flex items-center gap-2">
          <Input
            value={symbol}
            onChange={(event) => setSymbol(event.target.value.toUpperCase())}
            placeholder="Sembol"
            className="w-32"
          />
          <Select value={period} onValueChange={setPeriod}>
            <SelectTrigger className="w-24">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="1W">1 Hafta</SelectItem>
              <SelectItem value="1M">1 Ay</SelectItem>
              <SelectItem value="3M">3 Ay</SelectItem>
              <SelectItem value="6M">6 Ay</SelectItem>
              <SelectItem value="1Y">1 Yil</SelectItem>
            </SelectContent>
          </Select>
          <Button variant="outline" size="icon" onClick={handleRefreshAll}>
            <RefreshCw className="h-4 w-4" />
          </Button>
        </div>
      </div>

      <Tabs defaultValue="ma" className="space-y-6">
        <TabsList>
          <TabsTrigger value="ma">Hareketli Ortalama</TabsTrigger>
          <TabsTrigger value="trend">Trend Analizi</TabsTrigger>
          <TabsTrigger value="compare">Karsilastirma</TabsTrigger>
        </TabsList>

        <TabsContent value="ma" className="space-y-6">
          <div className="flex items-center gap-4">
            <Select value={maType} onValueChange={setMaType}>
              <SelectTrigger className="w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="SMA">SMA</SelectItem>
                <SelectItem value="EMA">EMA</SelectItem>
                <SelectItem value="WMA">WMA</SelectItem>
              </SelectContent>
            </Select>
            <Select value={maPeriod} onValueChange={setMaPeriod}>
              <SelectTrigger className="w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="10">10 Gun</SelectItem>
                <SelectItem value="20">20 Gun</SelectItem>
                <SelectItem value="50">50 Gun</SelectItem>
                <SelectItem value="100">100 Gun</SelectItem>
                <SelectItem value="200">200 Gun</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="grid gap-6 lg:grid-cols-3">
            <Card className="lg:col-span-2">
              <CardHeader>
                <CardTitle>
                  {symbol} - {maType} ({maPeriod})
                </CardTitle>
                <CardDescription>Fiyat ve hareketli ortalama</CardDescription>
              </CardHeader>
              <CardContent>{maLoading ? <Skeleton className="h-64" /> : <PriceChart data={maData?.data || []} />}</CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Istatistikler</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex justify-between border-b py-2">
                  <span className="text-muted-foreground">Son Fiyat</span>
                  <span className="font-semibold">
                    {maData ? formatCurrency(maData.currentPrice, 'TRY') : '-'}
                  </span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span className="text-muted-foreground">{maType}</span>
                  <span className="font-semibold">{maData ? formatCurrency(maData.maValue, 'TRY') : '-'}</span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span className="text-muted-foreground">Fark</span>
                  <span className={cn('font-semibold', maData?.difference >= 0 ? 'text-success' : 'text-danger')}>
                    {maData ? formatPercent(maData.differencePercent) : '-'}
                  </span>
                </div>
                <div className="flex justify-between py-2">
                  <span className="text-muted-foreground">Sinyal</span>
                  <Badge
                    variant={
                      maData?.signal === 'BUY' ? 'success' : maData?.signal === 'SELL' ? 'danger' : 'secondary'
                    }
                  >
                    {maData?.signal === 'BUY' ? 'AL' : maData?.signal === 'SELL' ? 'SAT' : 'BEKLE'}
                  </Badge>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="trend" className="space-y-6">
          <div className="grid gap-6 lg:grid-cols-3">
            <Card className="lg:col-span-2">
              <CardHeader>
                <CardTitle>{symbol} - Trend Analizi</CardTitle>
                <CardDescription>Destek/direnc seviyeleri ile trend yorumu</CardDescription>
              </CardHeader>
              <CardContent>
                {trendLoading ? <Skeleton className="h-64" /> : <PriceChart data={trendData?.data || []} />}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Trend Bilgileri</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex justify-between border-b py-2">
                  <span className="text-muted-foreground">Trend Yonu</span>
                  <Badge variant={trendDirection === 'UP' ? 'success' : trendDirection === 'DOWN' ? 'danger' : 'secondary'}>
                    {trendDirection === 'UP' ? 'Yukselis' : trendDirection === 'DOWN' ? 'Dusus' : 'Yatay'}
                  </Badge>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span className="text-muted-foreground">Destek</span>
                  <span className="font-semibold">
                    {trendData ? formatCurrency(trendData.support, 'TRY') : '-'}
                  </span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span className="text-muted-foreground">Direnc</span>
                  <span className="font-semibold">
                    {trendData ? formatCurrency(trendData.resistance, 'TRY') : '-'}
                  </span>
                </div>
                <div className="flex justify-between py-2">
                  <span className="text-muted-foreground">Guc</span>
                  <span className="font-semibold">{trendData ? `${trendData.strength}%` : '-'}</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="compare" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Sembol Secimi</CardTitle>
              <CardDescription>Karsilastirma icin en az 2 sembol girin (virgul ile)</CardDescription>
            </CardHeader>
            <CardContent>
              <Input
                value={compareInput}
                onChange={(event) => setCompareInput(event.target.value)}
                placeholder="THYAO,GARAN,AKBNK"
              />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Performans Karsilastirmasi</CardTitle>
              <CardDescription>Secilen semboller: {compareSymbols.join(', ') || '-'}</CardDescription>
            </CardHeader>
            <CardContent>
              {compareSymbols.length < 2 ? (
                <div className="py-8 text-center text-muted-foreground">Karsilastirma icin en az 2 sembol gerekli</div>
              ) : comparisonLoading ? (
                <Skeleton className="h-64" />
              ) : comparisonItems.length > 0 ? (
                <div className="space-y-4">
                  {comparisonItems.map((item) => (
                    <div key={item.symbol} className="flex items-center justify-between rounded-lg bg-muted/50 p-4">
                      <div className="flex items-center gap-3">
                        <div
                          className={cn(
                            'flex h-10 w-10 items-center justify-center rounded-lg',
                            item.changePercent >= 0 ? 'bg-success/10' : 'bg-danger/10'
                          )}
                        >
                          {item.changePercent >= 0 ? (
                            <TrendingUp className="h-5 w-5 text-success" />
                          ) : (
                            <TrendingDown className="h-5 w-5 text-danger" />
                          )}
                        </div>
                        <div>
                          <p className="font-semibold">{item.symbol}</p>
                          <p className="text-xs text-muted-foreground">{item.name}</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="font-semibold">{formatCurrency(item.currentPrice, 'TRY')}</p>
                        <Badge variant={item.changePercent >= 0 ? 'success' : 'danger'}>
                          {formatPercent(item.changePercent)}
                        </Badge>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="py-8 text-center text-muted-foreground">Karsilastirma verisi yok</div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
