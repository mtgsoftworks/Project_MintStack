import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  BarChart3,
  Newspaper,
  Wallet,
  ArrowUpRight,
  ArrowDownRight,
} from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { cn, formatCurrency, formatPercent, formatRelativeTime } from '@/lib/utils'
import { useGetCurrenciesQuery, useGetStocksQuery } from '@/store/api/marketApi'
import { useGetNewsQuery } from '@/store/api/newsApi'
import { useGetPortfoliosQuery } from '@/store/api/portfolioApi'
import { useSelector } from 'react-redux'
import { selectIsAuthenticated } from '@/store/slices/authSlice'
import { Link } from 'react-router-dom'

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
  const { data: currencies, isLoading } = useGetCurrenciesQuery()

  const mainCurrencies = currencies?.filter(c =>
    ['USD', 'EUR', 'GBP'].includes(c.currencyCode)
  ) || []

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base font-semibold">Döviz Kurları</CardTitle>
          <Button variant="ghost" size="sm" asChild>
            <Link to="/market/currencies">Tümünü Gör</Link>
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
  const { data, isLoading } = useGetNewsQuery({ page: 0, size: 5 })
  const news = data?.data || []

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base font-semibold">Son Haberler</CardTitle>
          <Button variant="ghost" size="sm" asChild>
            <Link to="/news">Tümünü Gör</Link>
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
  const { data, isLoading } = useGetStocksQuery({ size: 5 })
  const stocks = data?.data || []

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base font-semibold">Popüler Hisseler</CardTitle>
          <Button variant="ghost" size="sm" asChild>
            <Link to="/market/stocks">Tümünü Gör</Link>
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
  const isAuthenticated = useSelector(selectIsAuthenticated)
  const { data: currencies, isLoading: currenciesLoading } = useGetCurrenciesQuery()
  const { data: portfolios, isLoading: portfolioLoading } = useGetPortfoliosQuery(undefined, {
    skip: !isAuthenticated
  })

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

  return (
    <div className="space-y-6 animate-in">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold">Dashboard</h1>
        <p className="text-muted-foreground">
          Piyasa özetine ve portföy durumunuza genel bakış
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="USD/TRY"
          value={usdRate ? formatCurrency(usdRate.sellingRate, 'TRY') : '-'}
          change={usdRate?.changePercent}
          icon={DollarSign}
          trend={usdRate?.changePercent >= 0 ? 'up' : 'down'}
          loading={currenciesLoading}
        />
        <StatCard
          title="BIST 100"
          value="9,450.25"
          change={1.25}
          icon={BarChart3}
          trend="up"
          loading={false}
        />
        {isAuthenticated && (
          <>
            <StatCard
              title="Portföy Değeri"
              value={portfolioSummary ? formatCurrency(portfolioSummary.totalValue, 'TRY') : '-'}
              change={portfolioProfitLossPercent}
              icon={Wallet}
              trend={portfolioProfitLossPercent >= 0 ? 'up' : 'down'}
              loading={portfolioLoading}
            />
            <StatCard
              title="Toplam K/Z"
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
