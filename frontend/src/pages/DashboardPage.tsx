import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  BarChart3,
  Wallet,
  ArrowUpRight,
  ArrowDownRight,
  LucideIcon,
} from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import SimulationDataFlag from '@/components/common/SimulationDataFlag'
import RefreshButton from '@/components/common/RefreshButton'
import RefreshStatus from '@/components/common/RefreshStatus'
import { cn, formatCurrency, formatUserCurrency, formatPercent, formatRelativeTime } from '@/lib/utils'
import { getNewsDisplayTitle, getNewsSourceLabel, isSimulationNews } from '@/lib/news'
import { isSimulatedMarketData } from '@/lib/simulationData'
import { useGetCurrenciesQuery, useGetStocksQuery, useGetMarketIndexQuery } from '@/store/api/marketApi'
import { useGetNewsQuery } from '@/store/api/newsApi'
import { useGetPortfoliosQuery } from '@/store/api/portfolioApi'
import { useAutoRefresh } from '@/hooks/useAutoRefresh'
import { useMarketDataRefresh } from '@/hooks/useMarketDataRefresh'
import { useDispatch, useSelector } from 'react-redux'
import { selectIsAuthenticated } from '@/store/slices/authSlice'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { baseApi } from '@/store/api/baseApi'
import type { FetchBaseQueryError } from '@reduxjs/toolkit/query'

// Query options type matching RTK Query's internal type
type QueryOptions = {
  pollingInterval?: number
  refetchOnFocus?: boolean
  refetchOnReconnect?: boolean
  skip?: boolean
  [key: string]: unknown
}

// Type definitions based on API transformResponse
interface Currency {
  currencyCode: string
  currencyName: string
  sellingRate: number
  buyingRate: number
  changePercent?: number
  isSimulated?: boolean
}

interface NewsItem {
  id: number | string
  title?: string | null
  summary?: string | null
  content?: string | null
  publishedAt: string
  sourceName?: string | null
  isSimulated?: boolean | null
}

interface Stock {
  symbol: string
  name: string
  currentPrice: number
  changePercent: number
  isSimulated?: boolean
}

interface Portfolio {
  id: number | string
  totalValue: number
  totalCost: number
  profitLoss: number
}

function StatCard({ title, value, change, icon: Icon, trend, loading }: {
  title: string
  value: string
  change?: number
  icon: LucideIcon
  trend: 'up' | 'down' | 'neutral'
  loading?: boolean
}) {
  if (loading) {
    return (
      <Card>
        <CardContent className="p-6">
          <Skeleton className="h-4 w-24 mb-4" />
          <Skeleton className="h-8 w-32 mb-2" />
          <Skeleton className="h-4 w-20" />
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="card-hover">
      <CardContent className="p-6">
        <div className="flex items-center justify-between mb-4">
          <span className="text-sm font-medium text-muted-foreground">{title}</span>
          <div className={cn(
            "p-2 rounded-lg",
            trend === 'up' ? "bg-success/10" : trend === 'down' ? "bg-danger/10" : "bg-muted"
          )}>
            <Icon className={cn(
              "h-5 w-5",
              trend === 'up' ? "text-success" : trend === 'down' ? "text-danger" : "text-muted-foreground"
            )} />
          </div>
        </div>
        <div className="stat-value">{value}</div>
        {change !== undefined && (
          <div className={cn(
            "flex items-center gap-1 mt-2 text-sm font-medium",
            change >= 0 ? "text-success" : "text-danger"
          )}>
            {change >= 0 ? (
              <ArrowUpRight className="h-4 w-4" />
            ) : (
              <ArrowDownRight className="h-4 w-4" />
            )}
            {formatPercent(change)}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function CurrencyWidget({ queryOptions }: { queryOptions: QueryOptions }) {
  const { t } = useTranslation()
  const { data: currencies, isLoading } = useGetCurrenciesQuery(undefined, queryOptions)

  const priorityCurrencyCodes = ['USD', 'EUR', 'GBP', 'CHF', 'JPY', 'SAR', 'CAD', 'AUD']
  const mainCurrencies = [...(currencies || [])]
    .sort((a, b) => {
      const leftIndex = priorityCurrencyCodes.indexOf(a.currencyCode)
      const rightIndex = priorityCurrencyCodes.indexOf(b.currencyCode)

      if (leftIndex === -1 && rightIndex === -1) {
        return a.currencyCode.localeCompare(b.currencyCode, 'tr')
      }
      if (leftIndex === -1) {
        return 1
      }
      if (rightIndex === -1) {
        return -1
      }
      return leftIndex - rightIndex
    })
    .slice(0, 5)

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base font-semibold">{t('market.currencies')}</CardTitle>
          <Button variant="ghost" size="sm" asChild>
            <Link to="/market/currencies">{t('common.viewAll')}</Link>
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="space-y-3">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : (
          <div className="space-y-3">
            {mainCurrencies.map((currency) => (
              <div
                key={currency.currencyCode}
                className="flex items-center justify-between p-3 rounded-lg bg-muted/50"
              >
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
                    <span className="text-sm font-bold text-primary">
                      {currency.currencyCode}
                    </span>
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="font-medium">{currency.currencyCode}/TRY</p>
                      {isSimulatedMarketData(currency) && (
                        <SimulationDataFlag className="h-4 px-1 text-[9px]" />
                      )}
                    </div>
                    <p className="text-xs text-muted-foreground">{currency.currencyName}</p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="font-semibold">{formatCurrency(currency.sellingRate, 'TRY')}</p>
                  <p className={cn(
                    "text-xs font-medium",
                    currency.changePercent >= 0 ? "text-success" : "text-danger"
                  )}>
                    {formatPercent(currency.changePercent || 0)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function NewsWidget({ queryOptions }: { queryOptions: QueryOptions }) {
  const { t } = useTranslation()
  const { data, isLoading } = useGetNewsQuery(
    { page: 0, size: 5 },
    queryOptions
  )
  const news: NewsItem[] = data?.data || []

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base font-semibold">{t('news.latest')}</CardTitle>
          <Button variant="ghost" size="sm" asChild>
            <Link to="/news">{t('common.viewAll')}</Link>
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="space-y-2">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-3 w-24" />
              </div>
            ))}
          </div>
        ) : (
          <div className="space-y-4">
            {news.map((item: NewsItem) => {
              const simulationNews = isSimulationNews(item)
              return (
                <Link
                  key={item.id}
                  to={`/news/${item.id}`}
                  className="block group"
                >
                  <div className={cn('space-y-1 rounded-md p-2 -mx-2', simulationNews && 'border border-warning/30 bg-warning/10')}>
                    <p
                      className={cn(
                        'text-sm font-medium group-hover:text-primary transition-colors line-clamp-2',
                        simulationNews && 'text-warning-dark'
                      )}
                    >
                      {getNewsDisplayTitle(item)}
                    </p>
                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                      <span>{getNewsSourceLabel(item)}</span>
                      {simulationNews && <Badge variant="warning">Simulasyon</Badge>}
                      <span>•</span>
                      <span>{formatRelativeTime(item.publishedAt)}</span>
                    </div>
                  </div>
                </Link>
              )
            })}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function StocksWidget({ queryOptions }: { queryOptions: QueryOptions }) {
  const { t } = useTranslation()
  const { data, isLoading } = useGetStocksQuery({ size: 5 }, queryOptions)
  const stocks: Stock[] = data?.data || []

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base font-semibold">{t('dashboard.widgets.stocks.title')}</CardTitle>
          <Button variant="ghost" size="sm" asChild>
            <Link to="/market/stocks">{t('common.viewAll')}</Link>
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : stocks.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-6 text-center text-xs text-muted-foreground space-y-1">
            <p className="font-semibold text-foreground/80">Aktif Hisse Verisi Bulunamadı</p>
            <p className="text-[11px] text-muted-foreground">API anahtarını aktif edin veya veri indirin.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {stocks.map((stock: Stock) => (
              <Link
                key={stock.symbol}
                to={`/market/stocks/${stock.symbol}`}
                className="flex items-center justify-between p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors"
              >
                <div>
                  <div className="flex items-center gap-2">
                    <p className="font-medium">{stock.symbol}</p>
                    {isSimulatedMarketData(stock) && (
                      <SimulationDataFlag className="h-4 px-1 text-[9px]" />
                    )}
                  </div>
                  <p className="text-xs text-muted-foreground truncate max-w-[150px]">
                    {stock.name}
                  </p>
                </div>
                <div className="text-right">
                  <p className="font-semibold">{formatCurrency(stock.currentPrice, 'TRY')}</p>
                  <Badge variant={stock.changePercent >= 0 ? 'success' : 'danger'}>
                    {formatPercent(stock.changePercent || 0)}
                  </Badge>
                </div>
              </Link>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

export default function DashboardPage() {
  const { t } = useTranslation()
  const dispatch = useDispatch()
  const isAuthenticated = useSelector(selectIsAuthenticated)
  const { autoUpdate, refreshRate, queryOptions } = useAutoRefresh()
  const { refreshAndRefetch, isRefreshingMarketData } = useMarketDataRefresh([
    'CURRENCY_RATES',
    'BIST_STOCKS',
    'BIST_INDICES',
    'BONDS',
    'FUNDS',
    'VIOP',
  ])
  const {
    data: currencies,
    isLoading: currenciesLoading,
    isFetching: currenciesFetching,
    refetch: refetchCurrencies,
    fulfilledTimeStamp: currenciesFulfilledAt,
  } = useGetCurrenciesQuery(undefined, queryOptions)
  const {
    data: bistData,
    isLoading: bistLoading,
    isFetching: bistFetching,
    error: bistError,
    refetch: refetchBist,
    fulfilledTimeStamp: bistFulfilledAt,
  } = useGetMarketIndexQuery('XU100.IS', queryOptions)
  const {
    data: portfolios,
    isLoading: portfolioLoading,
    isFetching: portfolioFetching,
    refetch: refetchPortfolios,
    fulfilledTimeStamp: portfoliosFulfilledAt,
  } = useGetPortfoliosQuery(undefined, {
    ...queryOptions,
    skip: !isAuthenticated,
  })
  const isRefreshing = isRefreshingMarketData || currenciesFetching || portfolioFetching || bistFetching
  const lastUpdatedAt = Math.max(
    currenciesFulfilledAt || 0,
    bistFulfilledAt || 0,
    portfoliosFulfilledAt || 0
  ) || null

  const handleRefresh = async () => {
    await refreshAndRefetch(async () => {
      const requests = [Promise.resolve(refetchCurrencies()), Promise.resolve(refetchBist())]
      if (isAuthenticated) {
        requests.push(Promise.resolve(refetchPortfolios()))
      }
      await Promise.all(requests)

      // Refresh dashboard widgets that subscribe to these tags.
      dispatch(baseApi.util.invalidateTags(['Stocks', 'News', 'Currencies', 'Indices', 'Portfolios']))
    })
  }

  // Calculate portfolio summary
  const portfolioSummary: { totalValue: number; totalProfitLoss: number; totalCost: number } | undefined = portfolios?.reduce((acc: { totalValue: number; totalProfitLoss: number; totalCost: number }, portfolio: Portfolio) => ({
    totalValue: acc.totalValue + (portfolio.totalValue || 0),
    totalProfitLoss: acc.totalProfitLoss + (portfolio.profitLoss || 0),
    totalCost: acc.totalCost + (portfolio.totalCost || 0),
  }), { totalValue: 0, totalProfitLoss: 0, totalCost: 0 })

  const portfolioProfitLossPercent = portfolioSummary?.totalCost && portfolioSummary.totalCost > 0
    ? (portfolioSummary.totalProfitLoss / portfolioSummary.totalCost) * 100
    : 0

  // Get USD rate
  const usdRate: Currency | undefined = currencies?.find((c: Currency) => c.currencyCode === 'USD')
  const usdChangePercent = usdRate?.changePercent ?? 0

  // BIST 100 Logic
  let bistValue = '-'
  let bistChange: number | undefined = undefined
  let bistTrend: 'up' | 'down' | 'neutral' = 'neutral'
  let bistTitle = t('dashboard.widgets.bist100.title')
  const bistErrorData = bistError as FetchBaseQueryError | undefined
  const bistErrorMessage = (bistErrorData?.data as { error?: { message?: string }; message?: string } | undefined)?.error?.message
    || (bistErrorData?.data as { message?: string } | undefined)?.message
    || ''

  if (bistLoading) {
    // Keep loading state
  } else if (bistErrorData) {
    if (bistErrorData.status === 404 || bistErrorMessage.includes('Provider')) {
      // Provider missing
      bistValue = t('dashboard.widgets.bist100.noProvider')
      bistTitle = `${t('dashboard.widgets.bist100.title')} (${t('dashboard.widgets.bist100.noSource')})`
    } else {
      // Fetch error
      bistValue = t('dashboard.widgets.bist100.error')
    }
  } else if (bistData?.currentPrice != null) {
    bistValue = formatCurrency(bistData.currentPrice, 'TRY')
    bistChange = bistData.changePercent
    bistTrend = bistData.changePercent >= 0 ? 'up' : 'down'
  } else {
    bistValue = t('dashboard.widgets.bist100.noData')
  }

  return (
    <div className="space-y-6 animate-in">
      {/* Page Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t('nav.home')}</h1>
          <p className="text-muted-foreground">
            {t('dashboard.subtitle')}
          </p>
          <RefreshStatus
            className="mt-1"
            lastUpdatedAt={lastUpdatedAt}
            autoUpdateEnabled={autoUpdate}
            refreshRateSeconds={refreshRate}
            isFetching={isRefreshing}
          />
        </div>
        <RefreshButton
          variant="outline"
          isLoading={isRefreshing}
          onRefresh={handleRefresh}
        >
          {t('common.refresh')}
        </RefreshButton>
      </div>

      {/* Stats Grid */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title={t('dashboard.stats.usdTry')}
          value={usdRate ? formatCurrency(usdRate.sellingRate, 'TRY') : '-'}
          change={usdChangePercent}
          icon={DollarSign}
          trend={usdChangePercent >= 0 ? 'up' : 'down'}
          loading={currenciesLoading}
        />
        <StatCard
          title={bistTitle}
          value={bistValue}
          change={bistChange}
          icon={BarChart3}
          trend={bistTrend}
          loading={bistLoading}
        />
        {isAuthenticated && (
          <>
            <StatCard
              title={t('dashboard.stats.portfolioValue')}
              value={portfolioSummary ? formatUserCurrency(portfolioSummary.totalValue) : '-'}
              change={portfolioProfitLossPercent}
              icon={Wallet}
              trend={portfolioProfitLossPercent >= 0 ? 'up' : 'down'}
              loading={portfolioLoading}
            />
            <StatCard
              title={t('dashboard.stats.totalProfitLoss')}
              value={portfolioSummary ? formatUserCurrency(portfolioSummary.totalProfitLoss) : '-'}
              change={portfolioProfitLossPercent}
              icon={(portfolioSummary?.totalProfitLoss ?? 0) >= 0 ? TrendingUp : TrendingDown}
              trend={(portfolioSummary?.totalProfitLoss ?? 0) >= 0 ? 'up' : 'down'}
              loading={portfolioLoading}
            />
          </>
        )}
      </div>

      {/* Widgets Grid */}
      <div className="grid gap-6 lg:grid-cols-3">
        <CurrencyWidget queryOptions={queryOptions} />
        <StocksWidget queryOptions={queryOptions} />
        <NewsWidget queryOptions={queryOptions} />
      </div>
    </div>
  )
}



