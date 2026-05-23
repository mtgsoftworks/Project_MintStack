import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { TrendingUp, TrendingDown, Search } from 'lucide-react'
import { toast } from 'sonner'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import SimulationDataFlag from '@/components/common/SimulationDataFlag'
import RefreshButton from '@/components/common/RefreshButton'
import RefreshStatus from '@/components/common/RefreshStatus'
import WatchlistQuickAddButton from '@/components/market/WatchlistQuickAddButton'
import MarketChangeRangeSelector from '@/components/market/MarketChangeRangeSelector'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { isSimulatedMarketData } from '@/lib/simulationData'
import { getApiErrorMessage } from '@/lib/apiError'
import { cn, formatCurrency, formatNumber, formatPercent, formatDateTime } from '@/lib/utils'
import { useGetCurrenciesQuery } from '@/store/api/marketApi'
import { useAutoRefresh } from '@/hooks/useAutoRefresh'
import { useMarketDataRefresh } from '@/hooks/useMarketDataRefresh'
import { getMarketChangeRangeLabel, useMarketChangeRange } from '@/hooks/useMarketChangeRange'
import {
  useExecutePortfolioTradeMutation,
  useGetPortfolioQuery,
  useGetPortfoliosQuery,
} from '@/store/api/portfolioApi'

function CurrencyTableSkeleton() {
  return (
    <div className="space-y-3">
      {[...Array(10)].map((_, i) => (
        <Skeleton key={i} className="h-14 w-full" />
      ))}
    </div>
  )
}

function getDisplayRate(primaryRate, fallbackRate) {
  const primary = Number(primaryRate)
  if (Number.isFinite(primary) && primary > 0) {
    return primaryRate
  }
  const fallback = Number(fallbackRate)
  return Number.isFinite(fallback) && fallback > 0 ? fallbackRate : null
}

function hasMeaningfulChange(value) {
  const numeric = Number(value)
  return value !== null && value !== undefined && Number.isFinite(numeric) && numeric !== 0
}

export default function CurrencyPage() {
  const { t, i18n } = useTranslation()
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedPortfolioId, setSelectedPortfolioId] = useState('')
  const [tradeQuantities, setTradeQuantities] = useState({})
  const { autoUpdate, refreshRate, queryOptions } = useAutoRefresh()
  const { refreshAndRefetch, isRefreshingMarketData } = useMarketDataRefresh(['CURRENCY_RATES'])
  const changeRange = useMarketChangeRange()
  const changeRangeLabel = getMarketChangeRangeLabel(t, changeRange)
  const {
    data: currencies,
    isLoading,
    isFetching,
    refetch,
    fulfilledTimeStamp,
  } = useGetCurrenciesQuery(changeRange.queryParams, queryOptions)
  const { data: portfolios = [] } = useGetPortfoliosQuery()
  const { data: selectedPortfolio, isFetching: isPortfolioFetching } = useGetPortfolioQuery(
    selectedPortfolioId,
    { skip: !selectedPortfolioId }
  )
  const [executeTrade, { isLoading: isSubmittingTrade }] = useExecutePortfolioTradeMutation()
  const numberLocale = i18n.language === 'en' ? 'en-US' : 'tr-TR'

  useEffect(() => {
    if (!selectedPortfolioId && portfolios.length > 0) {
      const defaultPortfolio = portfolios.find((portfolio) => portfolio.isDefault) || portfolios[0]
      setSelectedPortfolioId(defaultPortfolio?.id || '')
    }
  }, [portfolios, selectedPortfolioId])

  // Get data source from first currency (all should have same source)
  const dataSource = currencies?.[0]?.source || null
  const getSourceLabel = (source) => {
    const labels = {
      'TCMB': 'TCMB',
      'ALPHA_VANTAGE': 'Alpha Vantage',
      'YAHOO_FINANCE': 'Yahoo Finance',
      'FINNHUB': 'Finnhub'
    }
    return labels[source] || source
  }

  const filteredCurrencies = currencies?.filter((currency) =>
    hasMeaningfulChange(currency.changePercent) && (
      currency.currencyCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
      currency.currencyName?.toLowerCase().includes(searchQuery.toLowerCase())
    )
  ) || []
  const hasSimulatedCurrencies = filteredCurrencies.some((currency) => isSimulatedMarketData(currency))

  const handleQuantityChange = (code, value) => {
    setTradeQuantities((current) => ({ ...current, [code]: value }))
  }

  const handleCurrencyTrade = async (currency, transactionType) => {
    const quantity = Number(tradeQuantities[currency.currencyCode])
    if (!selectedPortfolioId) {
      toast.error(t('quickTrade.selectPortfolio'))
      return
    }
    if (!Number.isFinite(quantity) || quantity <= 0) {
      toast.error(t('currencyPage.trade.invalidQuantity'))
      return
    }
    if (!selectedPortfolio) {
      toast.error(t('quickTrade.portfolioLoading'))
      return
    }

    const executionPrice = Number(transactionType === 'BUY' ? currency.sellingRate : currency.buyingRate)
    if (!Number.isFinite(executionPrice) || executionPrice <= 0) {
      toast.error(t('currencyPage.trade.invalidPrice'))
      return
    }

    const instrumentSymbol = `${currency.currencyCode}TRY`
    const holdingQuantity = (selectedPortfolio.items || [])
      .filter((item) => {
        const itemSymbol = (item.instrumentSymbol || item.symbol || '').toUpperCase()
        return itemSymbol === instrumentSymbol
      })
      .reduce((total, item) => {
        const itemQuantity = Number(item.quantity || 0)
        return Number.isFinite(itemQuantity) ? total + itemQuantity : total
      }, 0)

    if (transactionType === 'SELL') {
      if (holdingQuantity <= 0) {
        toast.error(t('currencyPage.trade.notHeld', { symbol: `${currency.currencyCode}/TRY` }))
        return
      }
      if (quantity > holdingQuantity) {
        toast.error(t('currencyPage.trade.insufficientPosition', { quantity: holdingQuantity }))
        return
      }
    }

    if (transactionType === 'BUY') {
      const cashBalance = Number(selectedPortfolio.cashBalance || 0)
      const estimatedGross = quantity * executionPrice
      if (Number.isFinite(cashBalance) && estimatedGross > cashBalance) {
        toast.error(t('currencyPage.trade.insufficientCash', {
          required: estimatedGross.toLocaleString(numberLocale),
          current: cashBalance.toLocaleString(numberLocale),
        }))
        return
      }
    }

    try {
      await executeTrade({
        portfolioId: selectedPortfolioId,
        instrumentSymbol,
        transactionType,
        orderType: 'MARKET',
        quantity,
        price: executionPrice,
        transactionDate: new Date().toISOString().slice(0, 10),
        notes: t('currencyPage.trade.note', {
          symbol: `${currency.currencyCode}/TRY`,
          side: t(transactionType === 'BUY' ? 'currencyPage.trade.buySide' : 'currencyPage.trade.sellSide'),
        }),
      }).unwrap()
      toast.success(t(transactionType === 'BUY' ? 'currencyPage.trade.buySuccess' : 'currencyPage.trade.sellSuccess'))
      handleQuantityChange(currency.currencyCode, '')
    } catch (error) {
      toast.error(getApiErrorMessage(error, t('currencyPage.trade.failed')))
    }
  }

  return (
    <div className="space-y-6 animate-in">
      {/* Page Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold">{t('currencyPage.title')}</h1>
            {hasSimulatedCurrencies && <SimulationDataFlag />}
          </div>
          <p className="text-muted-foreground">
            {dataSource 
              ? t('currencyPage.subtitleWithSource', { source: getSourceLabel(dataSource) })
              : t('currencyPage.subtitle')}
          </p>
          <RefreshStatus
            className="mt-1"
            lastUpdatedAt={fulfilledTimeStamp}
            autoUpdateEnabled={autoUpdate}
            refreshRateSeconds={refreshRate}
            isFetching={isFetching || isRefreshingMarketData}
          />
        </div>
        <div className="flex flex-wrap items-center justify-end gap-2">
          {portfolios.length > 0 && (
            <select
              className="h-10 rounded-md border border-input bg-background px-3 text-sm"
              value={selectedPortfolioId}
              onChange={(event) => setSelectedPortfolioId(event.target.value)}
            >
              {portfolios.map((portfolio) => (
                <option key={portfolio.id} value={portfolio.id}>
                  {portfolio.name}
                </option>
              ))}
            </select>
          )}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder={t('currencyPage.searchPlaceholder')}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 w-64"
            />
          </div>
          <MarketChangeRangeSelector {...changeRange} />
          <RefreshButton
            variant="outline"
            size="icon"
            isLoading={isFetching || isRefreshingMarketData}
            onRefresh={() => refreshAndRefetch(refetch)}
          />
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {['USD', 'EUR', 'GBP', 'CHF'].map((code) => {
          const currency = currencies?.find(c => c.currencyCode === code)
          const hasChange = hasMeaningfulChange(currency?.changePercent)
          if (!hasChange) {
            return null
          }
          const change = hasChange ? Number(currency.changePercent) : null
          const simulatedCurrency = isSimulatedMarketData(currency)

          return (
            <Card key={code} className="card-hover">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className={cn(
                      "flex h-10 w-10 items-center justify-center rounded-lg",
                      !hasChange ? "bg-muted" : change >= 0 ? "bg-success/10" : "bg-danger/10"
                    )}>
                      <span className={cn(
                        "text-sm font-bold",
                        !hasChange ? "text-muted-foreground" : change >= 0 ? "text-success" : "text-danger"
                      )}>
                        {code}
                      </span>
                    </div>
                    <div>
                      <p className="font-semibold">
                        {currency ? formatCurrency(currency.sellingRate, 'TRY') : '-'}
                      </p>
                      <div className="flex items-center gap-1">
                        <p className="text-xs text-muted-foreground">{code}/TRY</p>
                        {simulatedCurrency && <SimulationDataFlag className="h-4 px-1 text-[9px]" />}
                      </div>
                    </div>
                  </div>
                  <Badge variant={!hasChange ? 'secondary' : change >= 0 ? 'success' : 'danger'}>
                    {hasChange && (change >= 0
                      ? <TrendingUp className="mr-1 h-3 w-3" />
                      : <TrendingDown className="mr-1 h-3 w-3" />)}
                    {hasChange ? formatPercent(change) : '-'}
                  </Badge>
                </div>
              </CardContent>
            </Card>
          )
        })}
      </div>

      {/* Currency Table */}
      <Card>
        <CardHeader>
          <CardTitle>{t('currencyPage.table.title')}</CardTitle>
          <CardDescription>
            {t('currencyPage.table.description', {
              date: currencies?.[0]?.rateDate ? formatDateTime(currencies[0].rateDate) : '-',
            })}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <CurrencyTableSkeleton />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('currencyPage.table.headers.currency')}</TableHead>
                  <TableHead className="text-right">{t('currencyPage.table.headers.buying')}</TableHead>
                  <TableHead className="text-right">{t('currencyPage.table.headers.selling')}</TableHead>
                  <TableHead className="text-right">{t('currencyPage.table.headers.effectiveBuying')}</TableHead>
                  <TableHead className="text-right">{t('currencyPage.table.headers.effectiveSelling')}</TableHead>
                  <TableHead className="text-right">{t('marketChangeRange.changeHeader', { range: changeRangeLabel })}</TableHead>
                  <TableHead className="text-right">{t('currencyPage.table.headers.portfolio')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredCurrencies.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                      {t('currencyPage.empty')}
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredCurrencies.map((currency) => {
                    const hasChange = hasMeaningfulChange(currency.changePercent)
                    const isPositive = hasChange && Number(currency.changePercent) >= 0

                    return (
                    <TableRow key={currency.currencyCode} className="cursor-pointer">
                      <TableCell>
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
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
                            <p className="text-xs text-muted-foreground">
                              {currency.currencyName}
                            </p>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="text-right font-medium">
                        {formatNumber(currency.buyingRate, 4)}
                      </TableCell>
                      <TableCell className="text-right font-medium">
                        {formatNumber(currency.sellingRate, 4)}
                      </TableCell>
                      <TableCell className="text-right">
                        {formatNumber(getDisplayRate(currency.effectiveBuyingRate, currency.buyingRate), 4)}
                      </TableCell>
                      <TableCell className="text-right">
                        {formatNumber(getDisplayRate(currency.effectiveSellingRate, currency.sellingRate), 4)}
                      </TableCell>
                      <TableCell className="text-right">
                        <Badge variant={!hasChange ? 'secondary' : isPositive ? 'success' : 'danger'}>
                          {hasChange ? formatPercent(currency.changePercent) : '-'}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <WatchlistQuickAddButton symbol={`${currency.currencyCode}TRY`} />
                          <Input
                            className="h-9 w-24 text-right"
                            type="number"
                            min="0"
                            step="0.01"
                            placeholder={t('quickTrade.quantity')}
                            value={tradeQuantities[currency.currencyCode] || ''}
                            onChange={(event) => handleQuantityChange(currency.currencyCode, event.target.value)}
                          />
                          <Button
                            size="sm"
                            variant="success"
                            disabled={isSubmittingTrade || isPortfolioFetching}
                            onClick={() => handleCurrencyTrade(currency, 'BUY')}
                          >
                            {t('quickTrade.buy')}
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            disabled={isSubmittingTrade || isPortfolioFetching}
                            onClick={() => handleCurrencyTrade(currency, 'SELL')}
                          >
                            {t('quickTrade.sell')}
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                    )
                  })
                )}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
