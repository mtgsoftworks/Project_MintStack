import { useEffect, useMemo, useState } from 'react'
import { TrendingDown, TrendingUp } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import PriceChart from '@/components/charts/PriceChart'
import RefreshButton from '@/components/common/RefreshButton'
import { cn, formatCurrency, formatPercent } from '@/lib/utils'
import {
  useGetAllTechnicalIndicatorsQuery,
  useGetBollingerBandsQuery,
  useGetComparisonQuery,
  useGetMacdQuery,
  useGetMovingAverageQuery,
  useGetRsiQuery,
  useGetStochasticQuery,
  useGetTrendAnalysisQuery,
} from '@/store/api/analysisApi'
import { useGetCryptoQuery, useGetFundsQuery, useGetStocksQuery } from '@/store/api/marketApi'

const formatNumber = (value, digits = 2) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return '-'
  }
  return numeric.toFixed(digits)
}

const getRsiStatus = (rsi) => {
  if (!Number.isFinite(Number(rsi))) {
    return { label: 'Veri Yok', variant: 'secondary' }
  }
  if (rsi < 30) {
    return { label: 'Asiri Satim', variant: 'success' }
  }
  if (rsi > 70) {
    return { label: 'Asiri Alim', variant: 'danger' }
  }
  return { label: 'Notr', variant: 'secondary' }
}

const getSignalBadge = (signal) => {
  switch (signal) {
    case 'BUY':
    case 'BULLISH':
      return { label: 'AL', variant: 'success' }
    case 'SELL':
    case 'BEARISH':
      return { label: 'SAT', variant: 'danger' }
    default:
      return { label: signal || 'BEKLE', variant: 'secondary' }
  }
}

const getStochasticSignal = (signal) => {
  switch (signal) {
    case 'OVERSOLD':
      return { label: 'Asiri Satim', variant: 'success' }
    case 'OVERBOUGHT':
      return { label: 'Asiri Alim', variant: 'danger' }
    case 'BULLISH':
      return { label: 'Yukselis', variant: 'success' }
    case 'BEARISH':
      return { label: 'Dusus', variant: 'danger' }
    default:
      return { label: 'Notr', variant: 'secondary' }
  }
}

export default function AnalysisPage() {
  const [symbol, setSymbol] = useState('')
  const [period, setPeriod] = useState('1M')
  const [maType, setMaType] = useState('SMA')
  const [maPeriod, setMaPeriod] = useState('20')
  const [compareSymbols, setCompareSymbols] = useState<string[]>([])
  const [compareCandidate, setCompareCandidate] = useState('')
  const [rsiPeriod, setRsiPeriod] = useState('14')
  const [bollingerPeriod, setBollingerPeriod] = useState('20')
  const [bollingerStdDev, setBollingerStdDev] = useState('2.0')
  const [stochasticKPeriod, setStochasticKPeriod] = useState('14')
  const [stochasticDPeriod, setStochasticDPeriod] = useState('3')

  const {
    data: stocksResponse,
    isFetching: stocksFetching,
    refetch: refetchStocks,
  } = useGetStocksQuery({ page: 0, size: 300, sort: 'symbol,asc' })
  const {
    data: fundsResponse,
    isFetching: fundsFetching,
    refetch: refetchFunds,
  } = useGetFundsQuery({ page: 0, size: 300, sort: 'symbol,asc' })
  const {
    data: cryptoResponse,
    isFetching: cryptoFetching,
    refetch: refetchCrypto,
  } = useGetCryptoQuery({ page: 0, size: 300, sort: 'symbol,asc' })

  const availableInstruments = useMemo(() => {
    const combined = [
      ...(stocksResponse?.data || []),
      ...(fundsResponse?.data || []),
      ...(cryptoResponse?.data || []),
    ]

    const uniqueBySymbol = new Map()
    for (const item of combined) {
      const symbolValue = (item?.symbol || '').toUpperCase()
      if (!symbolValue || uniqueBySymbol.has(symbolValue)) {
        continue
      }
      uniqueBySymbol.set(symbolValue, {
        symbol: symbolValue,
        name: item?.name || symbolValue,
        type: item?.type || '-',
      })
    }

    return [...uniqueBySymbol.values()].sort((left, right) => left.symbol.localeCompare(right.symbol))
  }, [stocksResponse, fundsResponse, cryptoResponse])

  const availableSymbols = useMemo(
    () => availableInstruments.map((item) => item.symbol),
    [availableInstruments]
  )

  useEffect(() => {
    if (availableSymbols.length === 0) {
      return
    }
    if (!symbol || !availableSymbols.includes(symbol)) {
      setSymbol(availableSymbols[0])
    }
  }, [availableSymbols, symbol])

  useEffect(() => {
    if (availableSymbols.length < 2) {
      return
    }
    if (compareSymbols.length === 0) {
      setCompareSymbols([availableSymbols[0], availableSymbols[1]])
    }
  }, [availableSymbols, compareSymbols.length])

  useEffect(() => {
    const nextCandidate = availableSymbols.find((candidate) => !compareSymbols.includes(candidate)) || ''
    if (!compareCandidate || !availableSymbols.includes(compareCandidate) || compareSymbols.includes(compareCandidate)) {
      setCompareCandidate(nextCandidate)
    }
  }, [availableSymbols, compareCandidate, compareSymbols])

  useEffect(() => {
    setCompareSymbols((previous) => {
      const filtered = previous.filter((item) => availableSymbols.includes(item)).slice(0, 5)
      return filtered.length === previous.length ? previous : filtered
    })
  }, [availableSymbols])

  const { data: maData, isLoading: maLoading, isFetching: maFetching, refetch: refetchMa } = useGetMovingAverageQuery(
    {
      symbol,
      maType,
      maPeriod: Number.parseInt(maPeriod, 10),
    },
    { skip: !symbol }
  )

  const { data: trendData, isLoading: trendLoading, isFetching: trendFetching, refetch: refetchTrend } = useGetTrendAnalysisQuery(
    {
      symbol,
      period,
    },
    { skip: !symbol }
  )

  const {
    data: comparisonData,
    isLoading: comparisonLoading,
    isFetching: comparisonFetching,
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

  const { data: allIndicatorsData, isLoading: allIndicatorsLoading, isFetching: allIndicatorsFetching, refetch: refetchAllIndicators } =
    useGetAllTechnicalIndicatorsQuery(
      {
        symbol,
      },
      { skip: !symbol }
    )

  const { data: rsiData, isLoading: rsiLoading, isFetching: rsiFetching, refetch: refetchRsi } = useGetRsiQuery(
    {
      symbol,
      period: Number.parseInt(rsiPeriod, 10),
    },
    { skip: !symbol }
  )

  const { data: macdData, isLoading: macdLoading, isFetching: macdFetching, refetch: refetchMacd } = useGetMacdQuery(
    {
      symbol,
      fastPeriod: 12,
      slowPeriod: 26,
      signalPeriod: 9,
    },
    { skip: !symbol }
  )

  const { data: bollingerData, isLoading: bollingerLoading, isFetching: bollingerFetching, refetch: refetchBollinger } = useGetBollingerBandsQuery(
    {
      symbol,
      period: Number.parseInt(bollingerPeriod, 10),
      stdDev: Number.parseFloat(bollingerStdDev),
    },
    { skip: !symbol }
  )

  const { data: stochasticData, isLoading: stochasticLoading, isFetching: stochasticFetching, refetch: refetchStochastic } = useGetStochasticQuery(
    {
      symbol,
      kPeriod: Number.parseInt(stochasticKPeriod, 10),
      dPeriod: Number.parseInt(stochasticDPeriod, 10),
    },
    { skip: !symbol }
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

  const overallSignal = getSignalBadge(allIndicatorsData?.data?.overallSignal)
  const rsiStatus = getRsiStatus(rsiData?.data)
  const stochasticSignal = getStochasticSignal(stochasticData?.data?.signal)
  const isRefreshing =
    stocksFetching
    || fundsFetching
    || cryptoFetching
    || maFetching
    || trendFetching
    || comparisonFetching
    || allIndicatorsFetching
    || rsiFetching
    || macdFetching
    || bollingerFetching
    || stochasticFetching

  const handleRefreshAll = () => {
    refetchStocks()
    refetchFunds()
    refetchCrypto()
    refetchMa()
    refetchTrend()
    refetchRsi()
    refetchMacd()
    refetchBollinger()
    refetchStochastic()
    refetchAllIndicators()
    if (compareSymbols.length >= 2) {
      refetchComparison()
    }
  }

  const canAddCompareSymbol = Boolean(compareCandidate) && compareSymbols.length < 5

  const handleAddCompareSymbol = () => {
    if (!compareCandidate || compareSymbols.includes(compareCandidate) || compareSymbols.length >= 5) {
      return
    }
    setCompareSymbols((previous) => [...previous, compareCandidate])
  }

  const handleRemoveCompareSymbol = (value) => {
    setCompareSymbols((previous) => previous.filter((item) => item !== value))
  }

  const buildSymbolTitle = (title: string) => (symbol ? `${symbol} - ${title}` : title)
  const combinedSignalDescription = symbol
    ? `${symbol} icin tum indikatorlerin birlesik yorumu`
    : 'Secili sembol icin tum indikatorlerin birlesik yorumu'

  return (
    <div className="space-y-6 animate-in">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Teknik Analiz</h1>
          <p className="text-muted-foreground">MA, trend, RSI, MACD, Bollinger, Stochastic ve karsilastirma</p>
        </div>
        <div className="flex items-center gap-2">
          <Select value={symbol} onValueChange={setSymbol} disabled={availableInstruments.length === 0}>
            <SelectTrigger className="w-[280px]">
              <SelectValue placeholder="Sembol secin" />
            </SelectTrigger>
            <SelectContent>
              {availableInstruments.map((instrument) => (
                <SelectItem key={instrument.symbol} value={instrument.symbol}>
                  {instrument.symbol} - {instrument.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
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
          <RefreshButton variant="outline" size="icon" onRefresh={handleRefreshAll} isLoading={isRefreshing} />
        </div>
      </div>

      <Card className="border-dashed">
        <CardHeader>
          <CardTitle>Genel Teknik Sinyal</CardTitle>
          <CardDescription>{combinedSignalDescription}</CardDescription>
        </CardHeader>
        <CardContent>
          {allIndicatorsLoading ? (
            <Skeleton className="h-16" />
          ) : (
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-muted-foreground">Toplam Sinyal</p>
                <p className="text-sm text-muted-foreground">{allIndicatorsData?.message || 'Analiz bilgisi hazir'}</p>
              </div>
              <Badge variant={overallSignal.variant}>{overallSignal.label}</Badge>
            </div>
          )}
        </CardContent>
      </Card>

      <Tabs defaultValue="ma" className="space-y-6">
        <TabsList className="h-auto flex-wrap justify-start">
          <TabsTrigger value="ma">Hareketli Ortalama</TabsTrigger>
          <TabsTrigger value="trend">Trend Analizi</TabsTrigger>
          <TabsTrigger value="rsi">RSI</TabsTrigger>
          <TabsTrigger value="macd">MACD</TabsTrigger>
          <TabsTrigger value="bollinger">Bollinger</TabsTrigger>
          <TabsTrigger value="stochastic">Stochastic</TabsTrigger>
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
                  {buildSymbolTitle(`${maType} (${maPeriod})`)}
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
                  <span className="font-semibold">{maData ? formatCurrency(maData.currentPrice, 'TRY') : '-'}</span>
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
                <CardTitle>{buildSymbolTitle('Trend Analizi')}</CardTitle>
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
                  <span className="font-semibold">{trendData ? formatCurrency(trendData.support, 'TRY') : '-'}</span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span className="text-muted-foreground">Direnc</span>
                  <span className="font-semibold">{trendData ? formatCurrency(trendData.resistance, 'TRY') : '-'}</span>
                </div>
                <div className="flex justify-between py-2">
                  <span className="text-muted-foreground">Guc</span>
                  <span className="font-semibold">{trendData ? `${trendData.strength}%` : '-'}</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="rsi" className="space-y-6">
          <Card>
            <CardHeader className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
              <div>
                <CardTitle>{buildSymbolTitle('RSI')}</CardTitle>
                <CardDescription>Goreceli Guc Endeksi (0-100)</CardDescription>
              </div>
              <Select value={rsiPeriod} onValueChange={setRsiPeriod}>
                <SelectTrigger className="w-28">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="7">7 Gun</SelectItem>
                  <SelectItem value="14">14 Gun</SelectItem>
                  <SelectItem value="21">21 Gun</SelectItem>
                </SelectContent>
              </Select>
            </CardHeader>
            <CardContent>
              {rsiLoading ? (
                <Skeleton className="h-28" />
              ) : (
                <div className="space-y-4">
                  <div className="flex items-end justify-between">
                    <div>
                      <p className="text-sm text-muted-foreground">RSI Degeri</p>
                      <p className="text-4xl font-bold">{formatNumber(rsiData?.data)}</p>
                    </div>
                    <Badge variant={rsiStatus.variant}>{rsiStatus.label}</Badge>
                  </div>
                  <p className="text-sm text-muted-foreground">{rsiData?.message || 'RSI yorumu hazir'}</p>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="macd" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>{buildSymbolTitle('MACD')}</CardTitle>
              <CardDescription>12-26-9 standart ayarlari ile momentum analizi</CardDescription>
            </CardHeader>
            <CardContent>
              {macdLoading ? (
                <Skeleton className="h-36" />
              ) : (
                <div className="grid gap-4 md:grid-cols-3">
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">MACD Cizgisi</p>
                    <p className="text-2xl font-semibold">{formatNumber(macdData?.data?.macdLine, 4)}</p>
                  </div>
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">Sinyal Cizgisi</p>
                    <p className="text-2xl font-semibold">{formatNumber(macdData?.data?.signalLine, 4)}</p>
                  </div>
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">Histogram</p>
                    <p className="text-2xl font-semibold">{formatNumber(macdData?.data?.histogram, 4)}</p>
                    <Badge variant={Number(macdData?.data?.histogram) >= 0 ? 'success' : 'danger'} className="mt-2">
                      {Number(macdData?.data?.histogram) >= 0 ? 'Yukselis Momentumu' : 'Dusus Momentumu'}
                    </Badge>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="bollinger" className="space-y-6">
          <div className="flex flex-wrap items-center gap-4">
            <Select value={bollingerPeriod} onValueChange={setBollingerPeriod}>
              <SelectTrigger className="w-36">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="10">10 Gun</SelectItem>
                <SelectItem value="20">20 Gun</SelectItem>
                <SelectItem value="50">50 Gun</SelectItem>
              </SelectContent>
            </Select>
            <Select value={bollingerStdDev} onValueChange={setBollingerStdDev}>
              <SelectTrigger className="w-36">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="1.5">Std Sapma 1.5</SelectItem>
                <SelectItem value="2.0">Std Sapma 2.0</SelectItem>
                <SelectItem value="2.5">Std Sapma 2.5</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>{buildSymbolTitle('Bollinger Bantlari')}</CardTitle>
              <CardDescription>Bant genisligi ve %B ile volatilite analizi</CardDescription>
            </CardHeader>
            <CardContent>
              {bollingerLoading ? (
                <Skeleton className="h-36" />
              ) : (
                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">Ust Bant</p>
                    <p className="text-xl font-semibold">{formatNumber(bollingerData?.data?.upperBand, 4)}</p>
                  </div>
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">Orta Bant</p>
                    <p className="text-xl font-semibold">{formatNumber(bollingerData?.data?.middleBand, 4)}</p>
                  </div>
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">Alt Bant</p>
                    <p className="text-xl font-semibold">{formatNumber(bollingerData?.data?.lowerBand, 4)}</p>
                  </div>
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">Bant Genisligi</p>
                    <p className="text-xl font-semibold">{formatNumber(bollingerData?.data?.bandwidth, 2)}</p>
                  </div>
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">%B</p>
                    <p className="text-xl font-semibold">{formatNumber(bollingerData?.data?.percentB, 2)}</p>
                  </div>
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">Yorum</p>
                    <p className="text-sm font-medium">{bollingerData?.message || 'Bant analizi hazir'}</p>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="stochastic" className="space-y-6">
          <div className="flex flex-wrap items-center gap-4">
            <Select value={stochasticKPeriod} onValueChange={setStochasticKPeriod}>
              <SelectTrigger className="w-36">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="9">K 9</SelectItem>
                <SelectItem value="14">K 14</SelectItem>
                <SelectItem value="21">K 21</SelectItem>
              </SelectContent>
            </Select>
            <Select value={stochasticDPeriod} onValueChange={setStochasticDPeriod}>
              <SelectTrigger className="w-36">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="3">D 3</SelectItem>
                <SelectItem value="5">D 5</SelectItem>
                <SelectItem value="7">D 7</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>{buildSymbolTitle('Stochastic')}</CardTitle>
              <CardDescription>%K ve %D ile asiri alim/satim tespiti</CardDescription>
            </CardHeader>
            <CardContent>
              {stochasticLoading ? (
                <Skeleton className="h-28" />
              ) : (
                <div className="space-y-4">
                  <div className="grid gap-4 md:grid-cols-3">
                    <div className="rounded-lg border p-4">
                      <p className="text-xs text-muted-foreground">%K</p>
                      <p className="text-2xl font-semibold">{formatNumber(stochasticData?.data?.percentK, 2)}</p>
                    </div>
                    <div className="rounded-lg border p-4">
                      <p className="text-xs text-muted-foreground">%D</p>
                      <p className="text-2xl font-semibold">{formatNumber(stochasticData?.data?.percentD, 2)}</p>
                    </div>
                    <div className="rounded-lg border p-4">
                      <p className="text-xs text-muted-foreground">Sinyal</p>
                      <Badge variant={stochasticSignal.variant}>{stochasticSignal.label}</Badge>
                    </div>
                  </div>
                  <p className="text-sm text-muted-foreground">{stochasticData?.message || 'Stochastic yorumu hazir'}</p>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="compare" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Sembol Secimi</CardTitle>
              <CardDescription>Karsilastirma icin en az 2, en fazla 5 sembol secin</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex flex-wrap items-center gap-2">
                <Select value={compareCandidate} onValueChange={setCompareCandidate} disabled={availableInstruments.length === 0}>
                  <SelectTrigger className="w-[320px]">
                    <SelectValue placeholder="Sembol secin" />
                  </SelectTrigger>
                  <SelectContent>
                    {availableInstruments
                      .filter((instrument) => !compareSymbols.includes(instrument.symbol))
                      .map((instrument) => (
                        <SelectItem key={instrument.symbol} value={instrument.symbol}>
                          {instrument.symbol} - {instrument.name}
                        </SelectItem>
                      ))}
                  </SelectContent>
                </Select>
                <Button type="button" onClick={handleAddCompareSymbol} disabled={!canAddCompareSymbol}>
                  Ekle
                </Button>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                {compareSymbols.map((itemSymbol) => (
                  <Badge key={itemSymbol} variant="secondary" className="flex items-center gap-2 py-1">
                    {itemSymbol}
                    <button
                      type="button"
                      className="text-muted-foreground transition-colors hover:text-foreground"
                      onClick={() => handleRemoveCompareSymbol(itemSymbol)}
                      aria-label={`${itemSymbol} kaldir`}
                    >
                      x
                    </button>
                  </Badge>
                ))}
              </div>
              <p className="text-xs text-muted-foreground">Secili: {compareSymbols.length}/5</p>
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
