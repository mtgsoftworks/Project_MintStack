import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { TrendingUp, TrendingDown, Search, RefreshCw } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import SimulationDataFlag from '@/components/common/SimulationDataFlag'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { isSimulatedMarketData } from '@/lib/simulationData'
import { cn, formatCurrency, formatNumber, formatPercent, formatDateTime } from '@/lib/utils'
import { useGetCurrenciesQuery } from '@/store/api/marketApi'

function CurrencyTableSkeleton() {
  return (
    <div className="space-y-3">
      {[...Array(10)].map((_, i) => (
        <Skeleton key={i} className="h-14 w-full" />
      ))}
    </div>
  )
}

export default function CurrencyPage() {
  const { t } = useTranslation()
  const [searchQuery, setSearchQuery] = useState('')
  const { data: currencies, isLoading, isFetching, refetch } = useGetCurrenciesQuery()

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
    currency.currencyCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
    currency.currencyName?.toLowerCase().includes(searchQuery.toLowerCase())
  ) || []
  const hasSimulatedCurrencies = filteredCurrencies.some((currency) => isSimulatedMarketData(currency))

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
              ? t('currencyPage.subtitleWithSource', { source: getSourceLabel(dataSource), defaultValue: `${getSourceLabel(dataSource)} güncel döviz kurları` })
              : t('currencyPage.subtitle')}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder={t('currencyPage.searchPlaceholder')}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 w-64"
            />
          </div>
          <Button
            variant="outline"
            size="icon"
            onClick={() => refetch()}
            disabled={isFetching}
          >
            <RefreshCw className={cn("h-4 w-4", isFetching && "animate-spin")} />
          </Button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {['USD', 'EUR', 'GBP', 'CHF'].map((code) => {
          const currency = currencies?.find(c => c.currencyCode === code)
          const change = currency?.changePercent || 0
          const simulatedCurrency = isSimulatedMarketData(currency)

          return (
            <Card key={code} className="card-hover">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className={cn(
                      "flex h-10 w-10 items-center justify-center rounded-lg",
                      change >= 0 ? "bg-success/10" : "bg-danger/10"
                    )}>
                      <span className={cn(
                        "text-sm font-bold",
                        change >= 0 ? "text-success" : "text-danger"
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
                  <Badge variant={change >= 0 ? 'success' : 'danger'}>
                    {change >= 0 ? <TrendingUp className="mr-1 h-3 w-3" /> : <TrendingDown className="mr-1 h-3 w-3" />}
                    {formatPercent(change)}
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
                  <TableHead className="text-right">{t('currencyPage.table.headers.change')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredCurrencies.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                      {t('currencyPage.empty')}
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredCurrencies.map((currency) => (
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
                        {currency.effectiveBuyingRate ? formatNumber(currency.effectiveBuyingRate, 4) : '-'}
                      </TableCell>
                      <TableCell className="text-right">
                        {currency.effectiveSellingRate ? formatNumber(currency.effectiveSellingRate, 4) : '-'}
                      </TableCell>
                      <TableCell className="text-right">
                        <Badge variant={currency.changePercent >= 0 ? 'success' : 'danger'}>
                          {formatPercent(currency.changePercent || 0)}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
