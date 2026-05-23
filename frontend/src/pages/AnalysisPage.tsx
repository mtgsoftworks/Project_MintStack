import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
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
import { useGetFundsQuery, useGetStocksQuery } from '@/store/api/marketApi'
import { useMarketDataRefresh } from '@/hooks/useMarketDataRefresh'

const formatNumber = (value, digits = 2) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return '-'
  }
  return numeric.toFixed(digits)
}

const getRsiStatus = (rsi) => {
  if (!Number.isFinite(Number(rsi))) {
    return { labelKey: 'analysis.status.noData', variant: 'secondary' }
  }
  if (rsi < 30) {
    return { labelKey: 'analysis.status.oversold', variant: 'success' }
  }
  if (rsi > 70) {
    return { labelKey: 'analysis.status.overbought', variant: 'danger' }
  }
  return { labelKey: 'analysis.status.neutral', variant: 'secondary' }
}

const getSignalBadge = (signal) => {
  switch (signal) {
    case 'BUY':
    case 'BULLISH':
      return { labelKey: 'analysis.signal.buy', variant: 'success' }
    case 'SELL':
    case 'BEARISH':
      return { labelKey: 'analysis.signal.sell', variant: 'danger' }
    default:
      return { label: signal || null, labelKey: signal ? null : 'analysis.signal.hold', variant: 'secondary' }
  }
}

const getStochasticSignal = (signal) => {
  switch (signal) {
    case 'OVERSOLD':
      return { labelKey: 'analysis.status.oversold', variant: 'success' }
    case 'OVERBOUGHT':
      return { labelKey: 'analysis.status.overbought', variant: 'danger' }
    case 'BULLISH':
      return { labelKey: 'analysis.status.bullish', variant: 'success' }
    case 'BEARISH':
      return { labelKey: 'analysis.status.bearish', variant: 'danger' }
    default:
      return { labelKey: 'analysis.status.neutral', variant: 'secondary' }
  }
}

export default function AnalysisPage() {
  const { t } = useTranslation()
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
  const { refreshAndRefetch, isRefreshingMarketData } = useMarketDataRefresh(['BIST_STOCKS', 'FUNDS'])

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
  const availableInstruments = useMemo(() => {
    const combined = [
      ...(stocksResponse?.data || []),
      ...(fundsResponse?.data || []),
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
  }, [stocksResponse, fundsResponse])

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

  const allIndicators = allIndicatorsData?.success ? allIndicatorsData.data : null
  const macd = macdData?.success ? macdData.data : null
  const macdHistogram = Number(macd?.histogram)
  const hasMacdData = Boolean(macd) && Number.isFinite(macdHistogram)
  const overallSignal = getSignalBadge(allIndicators?.overallSignal)
  const rsiStatus = getRsiStatus(rsiData?.data)
  const stochasticSignal = getStochasticSignal(stochasticData?.data?.signal)
  const isRefreshing =
    isRefreshingMarketData
    || stocksFetching
    || fundsFetching
    || maFetching
    || trendFetching
    || comparisonFetching
    || allIndicatorsFetching
    || rsiFetching
    || macdFetching
    || bollingerFetching
    || stochasticFetching

  const handleRefreshAll = async () => {
    await refreshAndRefetch(async () => {
      refetchStocks()
      refetchFunds()
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
    })
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

  const renderBadgeLabel = (badge) => badge.labelKey ? t(badge.labelKey) : badge.label
  const buildSymbolTitle = (title: string) => (symbol ? `${symbol} - ${title}` : title)
  const combinedSignalDescription = symbol
    ? t('analysis.combinedDescriptionForSymbol', { symbol })
    : t('analysis.combinedDescription')

  return (
    <div className="space-y-6 animate-in">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t('analysis.title')}</h1>
          <p className="text-muted-foreground">{t('analysis.subtitle')}</p>
        </div>
        <div className="flex items-center gap-2">
          <Select value={symbol} onValueChange={setSymbol} disabled={availableInstruments.length === 0}>
            <SelectTrigger className="w-[280px]">
              <SelectValue placeholder={t('analysis.selectSymbol')} />
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
              <SelectItem value="1W">{t('analysis.periods.oneWeek')}</SelectItem>
              <SelectItem value="1M">{t('analysis.periods.oneMonth')}</SelectItem>
              <SelectItem value="3M">{t('analysis.periods.threeMonths')}</SelectItem>
              <SelectItem value="6M">{t('analysis.periods.sixMonths')}</SelectItem>
              <SelectItem value="1Y">{t('analysis.periods.oneYear')}</SelectItem>
            </SelectContent>
          </Select>
          <RefreshButton variant="outline" size="icon" onRefresh={handleRefreshAll} isLoading={isRefreshing} />
        </div>
      </div>

      <Card className="border-dashed">
        <CardHeader>
          <CardTitle>{t('analysis.generalSignal')}</CardTitle>
          <CardDescription>{combinedSignalDescription}</CardDescription>
        </CardHeader>
        <CardContent>
          {allIndicatorsLoading ? (
            <Skeleton className="h-16" />
          ) : (
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-muted-foreground">{t('analysis.totalSignal')}</p>
                <p className="text-sm text-muted-foreground">{allIndicatorsData?.message || t('analysis.analysisReady')}</p>
              </div>
              <Badge variant={overallSignal.variant}>{renderBadgeLabel(overallSignal)}</Badge>
            </div>
          )}
        </CardContent>
      </Card>

      <Tabs defaultValue="ma" className="space-y-6">
        <TabsList className="h-auto flex-wrap justify-start">
          <TabsTrigger value="ma">{t('analysis.tabs.movingAverage')}</TabsTrigger>
          <TabsTrigger value="trend">{t('analysis.tabs.trend')}</TabsTrigger>
          <TabsTrigger value="rsi">RSI</TabsTrigger>
          <TabsTrigger value="macd">MACD</TabsTrigger>
          <TabsTrigger value="bollinger">Bollinger</TabsTrigger>
          <TabsTrigger value="stochastic">Stochastic</TabsTrigger>
          <TabsTrigger value="compare">{t('analysis.tabs.compare')}</TabsTrigger>
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
                <SelectItem value="10">{t('analysis.days', { count: 10 })}</SelectItem>
                <SelectItem value="20">{t('analysis.days', { count: 20 })}</SelectItem>
                <SelectItem value="50">{t('analysis.days', { count: 50 })}</SelectItem>
                <SelectItem value="100">{t('analysis.days', { count: 100 })}</SelectItem>
                <SelectItem value="200">{t('analysis.days', { count: 200 })}</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="grid gap-6 lg:grid-cols-3">
            <Card className="lg:col-span-2">
              <CardHeader>
                <CardTitle>
                  {buildSymbolTitle(`${maType} (${maPeriod})`)}
                </CardTitle>
                <CardDescription>{t('analysis.priceAndMovingAverage')}</CardDescription>
              </CardHeader>
              <CardContent>{maLoading ? <Skeleton className="h-64" /> : <PriceChart data={maData?.data || []} />}</CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>{t('analysis.statistics')}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex justify-between border-b py-2">
                  <span className="text-muted-foreground">{t('analysis.lastPrice')}</span>
                  <span className="font-semibold">{maData ? formatCurrency(maData.currentPrice, 'TRY') : '-'}</span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span className="text-muted-foreground">{maType}</span>
                  <span className="font-semibold">{maData ? formatCurrency(maData.maValue, 'TRY') : '-'}</span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span className="text-muted-foreground">{t('analysis.difference')}</span>
                  <span className={cn('font-semibold', maData?.difference >= 0 ? 'text-success' : 'text-danger')}>
                    {maData ? formatPercent(maData.differencePercent) : '-'}
                  </span>
                </div>
                <div className="flex justify-between py-2">
                  <span className="text-muted-foreground">{t('analysis.signal.label')}</span>
                  <Badge
                    variant={
                      maData?.signal === 'BUY' ? 'success' : maData?.signal === 'SELL' ? 'danger' : 'secondary'
                    }
                  >
                    {maData?.signal === 'BUY'
                      ? t('analysis.signal.buy')
                      : maData?.signal === 'SELL'
                        ? t('analysis.signal.sell')
                        : t('analysis.signal.hold')}
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
                <CardTitle>{buildSymbolTitle(t('analysis.tabs.trend'))}</CardTitle>
                <CardDescription>{t('analysis.trendDescription')}</CardDescription>
              </CardHeader>
              <CardContent>
                {trendLoading ? <Skeleton className="h-64" /> : <PriceChart data={trendData?.data || []} />}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>{t('analysis.trendInfo')}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex justify-between border-b py-2">
                  <span className="text-muted-foreground">{t('analysis.trendDirection')}</span>
                  <Badge variant={trendDirection === 'UP' ? 'success' : trendDirection === 'DOWN' ? 'danger' : 'secondary'}>
                    {trendDirection === 'UP'
                      ? t('analysis.status.bullish')
                      : trendDirection === 'DOWN'
                        ? t('analysis.status.bearish')
                        : t('analysis.status.sideways')}
                  </Badge>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span className="text-muted-foreground">{t('analysis.support')}</span>
                  <span className="font-semibold">{trendData ? formatCurrency(trendData.support, 'TRY') : '-'}</span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span className="text-muted-foreground">{t('analysis.resistance')}</span>
                  <span className="font-semibold">{trendData ? formatCurrency(trendData.resistance, 'TRY') : '-'}</span>
                </div>
                <div className="flex justify-between py-2">
                  <span className="text-muted-foreground">{t('analysis.strength')}</span>
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
                <CardDescription>{t('analysis.rsiDescription')}</CardDescription>
              </div>
              <Select value={rsiPeriod} onValueChange={setRsiPeriod}>
                <SelectTrigger className="w-28">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="7">{t('analysis.days', { count: 7 })}</SelectItem>
                  <SelectItem value="14">{t('analysis.days', { count: 14 })}</SelectItem>
                  <SelectItem value="21">{t('analysis.days', { count: 21 })}</SelectItem>
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
                      <p className="text-sm text-muted-foreground">{t('analysis.rsiValue')}</p>
                      <p className="text-4xl font-bold">{formatNumber(rsiData?.data)}</p>
                    </div>
                    <Badge variant={rsiStatus.variant}>{renderBadgeLabel(rsiStatus)}</Badge>
                  </div>
                  <p className="text-sm text-muted-foreground">{rsiData?.message || t('analysis.rsiReady')}</p>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="macd" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>{buildSymbolTitle('MACD')}</CardTitle>
              <CardDescription>{t('analysis.macdDescription')}</CardDescription>
            </CardHeader>
            <CardContent>
              {macdLoading ? (
                <Skeleton className="h-36" />
              ) : !hasMacdData ? (
                <div className="rounded-lg border border-dashed p-6 text-center text-sm text-muted-foreground">
                  {macdData?.message || t('analysis.status.noData')}
                </div>
              ) : (
                <div className="grid gap-4 md:grid-cols-3">
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">{t('analysis.macdLine')}</p>
                    <p className="text-2xl font-semibold">{formatNumber(macd.macdLine, 4)}</p>
                  </div>
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">{t('analysis.signalLine')}</p>
                    <p className="text-2xl font-semibold">{formatNumber(macd.signalLine, 4)}</p>
                  </div>
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">Histogram</p>
                    <p className="text-2xl font-semibold">{formatNumber(macd.histogram, 4)}</p>
                    <Badge variant={macdHistogram >= 0 ? 'success' : 'danger'} className="mt-2">
                      {macdHistogram >= 0
                        ? t('analysis.bullishMomentum')
                        : t('analysis.bearishMomentum')}
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
                <SelectItem value="10">{t('analysis.days', { count: 10 })}</SelectItem>
                <SelectItem value="20">{t('analysis.days', { count: 20 })}</SelectItem>
                <SelectItem value="50">{t('analysis.days', { count: 50 })}</SelectItem>
              </SelectContent>
            </Select>
            <Select value={bollingerStdDev} onValueChange={setBollingerStdDev}>
              <SelectTrigger className="w-36">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="1.5">{t('analysis.stdDev', { value: '1.5' })}</SelectItem>
                <SelectItem value="2.0">{t('analysis.stdDev', { value: '2.0' })}</SelectItem>
                <SelectItem value="2.5">{t('analysis.stdDev', { value: '2.5' })}</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>{buildSymbolTitle(t('analysis.bollingerBands'))}</CardTitle>
              <CardDescription>{t('analysis.bollingerDescription')}</CardDescription>
            </CardHeader>
            <CardContent>
              {bollingerLoading ? (
                <Skeleton className="h-36" />
              ) : (
                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">{t('analysis.upperBand')}</p>
                    <p className="text-xl font-semibold">{formatNumber(bollingerData?.data?.upperBand, 4)}</p>
                  </div>
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">{t('analysis.middleBand')}</p>
                    <p className="text-xl font-semibold">{formatNumber(bollingerData?.data?.middleBand, 4)}</p>
                  </div>
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">{t('analysis.lowerBand')}</p>
                    <p className="text-xl font-semibold">{formatNumber(bollingerData?.data?.lowerBand, 4)}</p>
                  </div>
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">{t('analysis.bandwidth')}</p>
                    <p className="text-xl font-semibold">{formatNumber(bollingerData?.data?.bandwidth, 2)}</p>
                  </div>
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">%B</p>
                    <p className="text-xl font-semibold">{formatNumber(bollingerData?.data?.percentB, 2)}</p>
                  </div>
                  <div className="rounded-lg border p-4">
                    <p className="text-xs text-muted-foreground">{t('analysis.comment')}</p>
                    <p className="text-sm font-medium">{bollingerData?.message || t('analysis.bollingerReady')}</p>
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
              <CardDescription>{t('analysis.stochasticDescription')}</CardDescription>
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
                      <p className="text-xs text-muted-foreground">{t('analysis.signal.label')}</p>
                      <Badge variant={stochasticSignal.variant}>{renderBadgeLabel(stochasticSignal)}</Badge>
                    </div>
                  </div>
                  <p className="text-sm text-muted-foreground">{stochasticData?.message || t('analysis.stochasticReady')}</p>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="compare" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>{t('analysis.symbolSelection')}</CardTitle>
              <CardDescription>{t('analysis.compareDescription')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex flex-wrap items-center gap-2">
                <Select value={compareCandidate} onValueChange={setCompareCandidate} disabled={availableInstruments.length === 0}>
                  <SelectTrigger className="w-[320px]">
                    <SelectValue placeholder={t('analysis.selectSymbol')} />
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
                  {t('common.add')}
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
                      aria-label={t('analysis.removeSymbol', { symbol: itemSymbol })}
                    >
                      x
                    </button>
                  </Badge>
                ))}
              </div>
              <p className="text-xs text-muted-foreground">{t('analysis.selectedCount', { count: compareSymbols.length })}</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>{t('analysis.performanceComparison')}</CardTitle>
              <CardDescription>{t('analysis.selectedSymbols', { symbols: compareSymbols.join(', ') || '-' })}</CardDescription>
            </CardHeader>
            <CardContent>
              {compareSymbols.length < 2 ? (
                <div className="py-8 text-center text-muted-foreground">{t('analysis.compareRequiresTwo')}</div>
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
                <div className="py-8 text-center text-muted-foreground">{t('analysis.compareNoData')}</div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
