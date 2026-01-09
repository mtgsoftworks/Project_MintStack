import { useState } from 'react'
import { Search, RefreshCw, TrendingUp, TrendingDown, BarChart3 } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { cn, formatCurrency, formatPercent, formatNumber } from '@/lib/utils'
import {
  useGetMovingAverageQuery,
  useGetTrendAnalysisQuery,
  useGetComparisonQuery,
} from '@/store/api/analysisApi'
import PriceChart from '@/components/charts/PriceChart'

export default function AnalysisPage() {
  const [symbol, setSymbol] = useState('THYAO')
  const [period, setPeriod] = useState('1M')
  const [maType, setMaType] = useState('SMA')
  const [maPeriod, setMaPeriod] = useState('20')
  const [compareSymbols, setCompareSymbols] = useState(['THYAO', 'GARAN'])

  const { data: maData, isLoading: maLoading, refetch: refetchMa } = useGetMovingAverageQuery({
    symbol,
    period,
    maType,
    maPeriod: parseInt(maPeriod),
  })

  const { data: trendData, isLoading: trendLoading } = useGetTrendAnalysisQuery({
    symbol,
    period,
  })

  const { data: comparisonData, isLoading: comparisonLoading } = useGetComparisonQuery({
    symbols: compareSymbols,
    period,
  })

  return (
    <div className="space-y-6 animate-in">
      {/* Page Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Teknik Analiz</h1>
          <p className="text-muted-foreground">
            Hareketli ortalama, trend analizi ve karşılaştırma
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Input
            value={symbol}
            onChange={(e) => setSymbol(e.target.value.toUpperCase())}
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
              <SelectItem value="1Y">1 Yıl</SelectItem>
            </SelectContent>
          </Select>
          <Button variant="outline" size="icon" onClick={() => refetchMa()}>
            <RefreshCw className="h-4 w-4" />
          </Button>
        </div>
      </div>

      <Tabs defaultValue="ma" className="space-y-6">
        <TabsList>
          <TabsTrigger value="ma">Hareketli Ortalama</TabsTrigger>
          <TabsTrigger value="trend">Trend Analizi</TabsTrigger>
          <TabsTrigger value="compare">Karşılaştırma</TabsTrigger>
        </TabsList>

        {/* Moving Average Tab */}
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
                <SelectItem value="10">10 Gün</SelectItem>
                <SelectItem value="20">20 Gün</SelectItem>
                <SelectItem value="50">50 Gün</SelectItem>
                <SelectItem value="100">100 Gün</SelectItem>
                <SelectItem value="200">200 Gün</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="grid gap-6 lg:grid-cols-3">
            <Card className="lg:col-span-2">
              <CardHeader>
                <CardTitle>{symbol} - {maType} ({maPeriod})</CardTitle>
                <CardDescription>
                  {maType === 'SMA' && 'Basit Hareketli Ortalama'}
                  {maType === 'EMA' && 'Üstel Hareketli Ortalama'}
                  {maType === 'WMA' && 'Ağırlıklı Hareketli Ortalama'}
                </CardDescription>
              </CardHeader>
              <CardContent>
                {maLoading ? (
                  <Skeleton className="h-64" />
                ) : (
                  <PriceChart data={maData?.data || []} />
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>İstatistikler</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex justify-between py-2 border-b">
                  <span className="text-muted-foreground">Son Fiyat</span>
                  <span className="font-semibold">
                    {maData ? formatCurrency(maData.currentPrice, 'TRY') : '-'}
                  </span>
                </div>
                <div className="flex justify-between py-2 border-b">
                  <span className="text-muted-foreground">{maType} Değeri</span>
                  <span className="font-semibold">
                    {maData ? formatCurrency(maData.maValue, 'TRY') : '-'}
                  </span>
                </div>
                <div className="flex justify-between py-2 border-b">
                  <span className="text-muted-foreground">Fark</span>
                  <span className={cn(
                    "font-semibold",
                    maData?.difference >= 0 ? "text-success" : "text-danger"
                  )}>
                    {maData ? formatPercent(maData.differencePercent) : '-'}
                  </span>
                </div>
                <div className="flex justify-between py-2">
                  <span className="text-muted-foreground">Sinyal</span>
                  <Badge variant={maData?.signal === 'BUY' ? 'success' : maData?.signal === 'SELL' ? 'danger' : 'secondary'}>
                    {maData?.signal === 'BUY' ? 'AL' : maData?.signal === 'SELL' ? 'SAT' : 'BEKLE'}
                  </Badge>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        {/* Trend Analysis Tab */}
        <TabsContent value="trend" className="space-y-6">
          <div className="grid gap-6 lg:grid-cols-3">
            <Card className="lg:col-span-2">
              <CardHeader>
                <CardTitle>{symbol} - Trend Analizi</CardTitle>
                <CardDescription>
                  Fiyat trendi ve destek/direnç seviyeleri
                </CardDescription>
              </CardHeader>
              <CardContent>
                {trendLoading ? (
                  <Skeleton className="h-64" />
                ) : (
                  <PriceChart data={trendData?.data || []} />
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Trend Bilgileri</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex justify-between py-2 border-b">
                  <span className="text-muted-foreground">Trend Yönü</span>
                  <Badge variant={trendData?.trend === 'UP' ? 'success' : trendData?.trend === 'DOWN' ? 'danger' : 'secondary'}>
                    {trendData?.trend === 'UP' ? 'Yükseliş' : trendData?.trend === 'DOWN' ? 'Düşüş' : 'Yatay'}
                  </Badge>
                </div>
                <div className="flex justify-between py-2 border-b">
                  <span className="text-muted-foreground">Destek</span>
                  <span className="font-semibold">
                    {trendData ? formatCurrency(trendData.support, 'TRY') : '-'}
                  </span>
                </div>
                <div className="flex justify-between py-2 border-b">
                  <span className="text-muted-foreground">Direnç</span>
                  <span className="font-semibold">
                    {trendData ? formatCurrency(trendData.resistance, 'TRY') : '-'}
                  </span>
                </div>
                <div className="flex justify-between py-2">
                  <span className="text-muted-foreground">Güç</span>
                  <span className="font-semibold">
                    {trendData ? `${trendData.strength}%` : '-'}
                  </span>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        {/* Comparison Tab */}
        <TabsContent value="compare" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Performans Karşılaştırması</CardTitle>
              <CardDescription>
                Seçilen semboller: {compareSymbols.join(', ')}
              </CardDescription>
            </CardHeader>
            <CardContent>
              {comparisonLoading ? (
                <Skeleton className="h-64" />
              ) : comparisonData ? (
                <div className="space-y-4">
                  {comparisonData.map((item) => (
                    <div key={item.symbol} className="flex items-center justify-between p-4 rounded-lg bg-muted/50">
                      <div className="flex items-center gap-3">
                        <div className={cn(
                          "flex h-10 w-10 items-center justify-center rounded-lg",
                          item.changePercent >= 0 ? "bg-success/10" : "bg-danger/10"
                        )}>
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
                <div className="text-center py-8 text-muted-foreground">
                  Karşılaştırma verisi yok
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
