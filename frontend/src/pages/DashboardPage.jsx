import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  BarChart3,
  Wallet,
  ArrowUpRight,
  ArrowDownRight,
  RefreshCw,
} from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { cn, formatCurrency, formatPercent, formatRelativeTime } from '@/lib/utils'
import { useGetCurrenciesQuery, useGetStocksQuery, useGetMarketIndexQuery } from '@/store/api/marketApi'
import { useGetNewsQuery } from '@/store/api/newsApi'
import { useGetPortfoliosQuery } from '@/store/api/portfolioApi'
import { useDispatch, useSelector } from 'react-redux'
import { selectIsAuthenticated } from '@/store/slices/authSlice'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { baseApi } from '@/store/api/baseApi'

function StatCard({ title, value, change, icon: Icon, trend, loading }) {
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

function CurrencyWidget() {
  const { t } = useTranslation()
  const { data: currencies, isLoading } = useGetCurrenciesQuery()

  const mainCurrencies = currencies?.filter(c =>
    ['USD', 'EUR', 'GBP'].includes(c.currencyCode)
  ) || []

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
            {[1, 2, 3].map((i) => (
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
                    <p className="font-medium">{currency.currencyCode}/TRY</p>
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

function NewsWidget() {
  const { t } = useTranslation()
  const { data, isLoading } = useGetNewsQuery(
    { page: 0, size: 5 },
    {
      pollingInterval: 10000,
      refetchOnFocus: true,
      refetchOnReconnect: true,
    }
  )
  const news = data?.data || []

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
            {news.map((item) => (
              <Link
                key={item.id}
                to={`/news/${item.id}`}
                className="block group"
              >
                <div className="space-y-1">
                  <p className="text-sm font-medium group-hover:text-primary transition-colors line-clamp-2">
                    {item.title}
                  </p>
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <span>{item.sourceName}</span>
                    <span>•</span>
                    <span>{formatRelativeTime(item.publishedAt)}</span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function StocksWidget() {
  const { t } = useTranslation()
  const { data, isLoading } = useGetStocksQuery({ size: 5 })
  const stocks = data?.data || []

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
        ) : (
          <div className="space-y-3">
            {stocks.map((stock) => (
              <Link
                key={stock.symbol}
                to={`/market/stocks/${stock.symbol}`}
                className="flex items-center justify-between p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors"
              >
                <div>
                  <p className="font-medium">{stock.symbol}</p>
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
  const {
    data: currencies,
    isLoading: currenciesLoading,
    isFetching: currenciesFetching,
    refetch: refetchCurrencies,
  } = useGetCurrenciesQuery()
  const {
    data: bistData,
    isLoading: bistLoading,
    isFetching: bistFetching,
    error: bistError,
    refetch: refetchBist,
  } = useGetMarketIndexQuery(
    'XU100.IS',
    {
      pollingInterval: 5000,
      refetchOnFocus: true,
      refetchOnReconnect: true,
    }
  )
  const {
    data: portfolios,
    isLoading: portfolioLoading,
    isFetching: portfolioFetching,
    refetch: refetchPortfolios,
  } = useGetPortfoliosQuery(undefined, {
    skip: !isAuthenticated
  })
  const isRefreshing = currenciesFetching || portfolioFetching || bistFetching

  const handleRefresh = () => {
    refetchCurrencies()
    refetchBist()
    if (isAuthenticated) {
      refetchPortfolios()
    }

    // Refresh dashboard widgets that subscribe to these tags.
    dispatch(baseApi.util.invalidateTags(['Stocks', 'News', 'Currencies', 'Indices', 'Portfolios']))
  }

  // Calculate portfolio summary
  const portfolioSummary = portfolios?.reduce((acc, portfolio) => ({
    totalValue: acc.totalValue + (portfolio.totalValue || 0),
    totalProfitLoss: acc.totalProfitLoss + (portfolio.profitLoss || 0),
    totalCost: acc.totalCost + (portfolio.totalCost || 0),
  }), { totalValue: 0, totalProfitLoss: 0, totalCost: 0 })

  const portfolioProfitLossPercent = portfolioSummary?.totalCost > 0
    ? (portfolioSummary.totalProfitLoss / portfolioSummary.totalCost) * 100
    : 0

  // Get USD rate
  const usdRate = currencies?.find(c => c.currencyCode === 'USD')

  // BIST 100 Logic
  let bistValue = '-'
  let bistChange = undefined
  let bistTrend = 'neutral'
  let bistTitle = t('dashboard.widgets.bist100.title')
  const bistErrorMessage = bistError?.data?.error?.message || bistError?.data?.message || ''

  if (bistLoading) {
    // Keep loading state
  } else if (bistError) {
    if (bistError.status === 404 || bistErrorMessage.includes('Provider')) {
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
    bistValue = t('dashboard.widgets.bist100.noData', { defaultValue: 'Veri yok' })
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
        </div>
        <Button
          variant="outline"
          onClick={handleRefresh}
          disabled={isRefreshing}
        >
          <RefreshCw className={cn('h-4 w-4 mr-2', isRefreshing && 'animate-spin')} />
          {t('common.refresh')}
        </Button>
      </div>

      {/* Stats Grid */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title={t('dashboard.stats.usdTry')}
          value={usdRate ? formatCurrency(usdRate.sellingRate, 'TRY') : '-'}
          change={usdRate?.changePercent}
          icon={DollarSign}
          trend={usdRate?.changePercent >= 0 ? 'up' : 'down'}
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
              value={portfolioSummary ? formatCurrency(portfolioSummary.totalValue, 'TRY') : '-'}
              change={portfolioProfitLossPercent}
              icon={Wallet}
              trend={portfolioProfitLossPercent >= 0 ? 'up' : 'down'}
              loading={portfolioLoading}
            />
            <StatCard
              title={t('dashboard.stats.totalProfitLoss')}
              value={portfolioSummary ? formatCurrency(portfolioSummary.totalProfitLoss, 'TRY') : '-'}
              change={portfolioProfitLossPercent}
              icon={portfolioSummary?.totalProfitLoss >= 0 ? TrendingUp : TrendingDown}
              trend={portfolioSummary?.totalProfitLoss >= 0 ? 'up' : 'down'}
              loading={portfolioLoading}
            />
          </>
        )}
      </div>

      {/* Widgets Grid */}
      <div className="grid gap-6 lg:grid-cols-3">
        <CurrencyWidget />
        <StocksWidget />
        <NewsWidget />
      </div>
    </div>
  )
}
