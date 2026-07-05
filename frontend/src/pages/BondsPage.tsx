import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Search, TrendingUp, TrendingDown, Minus } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import SimulationDataFlag from '@/components/common/SimulationDataFlag'
import RefreshButton from '@/components/common/RefreshButton'
import RefreshStatus from '@/components/common/RefreshStatus'
import PortfolioQuickTradeCell from '@/components/market/PortfolioQuickTradeCell'
import WatchlistQuickAddButton from '@/components/market/WatchlistQuickAddButton'
import MarketChangeRangeSelector from '@/components/market/MarketChangeRangeSelector'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { isSimulatedMarketData } from '@/lib/simulationData'
import { cn, formatCurrency, formatPercent, formatDate } from '@/lib/utils'
import { useGetBondsQuery } from '@/store/api/marketApi'
import { useAutoRefresh } from '@/hooks/useAutoRefresh'
import { useMarketDataRefresh } from '@/hooks/useMarketDataRefresh'
import { useDefaultPortfolioSelection } from '@/hooks/useDefaultPortfolioSelection'
import { getMarketChangeRangeLabel, useMarketChangeRange } from '@/hooks/useMarketChangeRange'

function BondTableSkeleton() {
  return (
    <div className="space-y-3">
      {[...Array(10)].map((_, i) => (
        <Skeleton key={i} className="h-14 w-full" />
      ))}
    </div>
  )
}

function hasMeaningfulChange(value) {
  const numeric = Number(value)
  return value !== null && value !== undefined && Number.isFinite(numeric)
}

export default function BondsPage() {
  const { t } = useTranslation()
  const [searchQuery, setSearchQuery] = useState('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(50)
  const { portfolios, selectedPortfolioId, setSelectedPortfolioId } = useDefaultPortfolioSelection()
  const { autoUpdate, refreshRate, queryOptions } = useAutoRefresh()
  const { refreshAndRefetch, isRefreshingMarketData } = useMarketDataRefresh(['BONDS'])
  const changeRange = useMarketChangeRange()
  const changeRangeLabel = getMarketChangeRangeLabel(t, changeRange)
  const {
    data,
    isLoading,
    isFetching,
    refetch,
    fulfilledTimeStamp,
  } = useGetBondsQuery(
    {
      page,
      size: pageSize,
      sort: 'symbol,asc',
      search: searchQuery.trim() || undefined,
      ...changeRange.queryParams,
    },
    queryOptions
  )

  const bonds = data?.data || []
  const totalPages = data?.pagination?.totalPages || 0
  const totalElements = data?.pagination?.totalElements || 0

  useEffect(() => {
    setPage(0)
  }, [searchQuery, pageSize, changeRange.queryParams.changeStartDate, changeRange.queryParams.changeEndDate])

  const filteredBonds = bonds.filter((bond) => {
    if (!bond || !bond.symbol) return false
    if (!searchQuery) return true
    const query = searchQuery.toLowerCase()
    return bond.symbol.toLowerCase().includes(query) || bond.name?.toLowerCase().includes(query)
  }).sort((left, right) => left.symbol.localeCompare(right.symbol))

  return (
    <div className="space-y-6 animate-in">
      {/* Page Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t('bondsPage.title')}</h1>
          <p className="text-muted-foreground">
            {t('bondsPage.subtitle')}
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
              placeholder={t('bondsPage.searchPlaceholder')}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 w-64"
            />
          </div>
          <MarketChangeRangeSelector {...changeRange} />
          <Select value={String(pageSize)} onValueChange={(value) => setPageSize(Number(value))}>
            <SelectTrigger className="w-28">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {[20, 50, 100, 200].map((size) => (
                <SelectItem key={size} value={String(size)}>
                  {size}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <RefreshButton
            variant="outline"
            size="icon"
            isLoading={isFetching || isRefreshingMarketData}
            onRefresh={() => refreshAndRefetch(refetch)}
          />
        </div>
      </div>

      {/* Bonds Table */}
      <Card>
        <CardHeader>
          <CardTitle>{t('bondsPage.listTitle')}</CardTitle>
          <CardDescription>
            {t('bondsPage.listDescription', { count: totalElements })}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <BondTableSkeleton />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('bondsPage.headers.symbol')}</TableHead>
                  <TableHead>{t('bondsPage.headers.name')}</TableHead>
                  <TableHead className="text-right">{t('bondsPage.headers.price')}</TableHead>
                  <TableHead className="text-right">{t('bondsPage.headers.yield')}</TableHead>
                  <TableHead className="text-right">{t('marketChangeRange.changeHeader', { range: changeRangeLabel })}</TableHead>
                  <TableHead className="text-right">{t('bondsPage.headers.maturity')}</TableHead>
                  <TableHead className="text-right">Islem</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredBonds.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                      {t('bondsPage.empty')}
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredBonds.map((bond) => {
                    const hasChange = hasMeaningfulChange(bond.changePercent)
                    const isPositive = hasChange && bond.changePercent >= 0

                    return (
                    <TableRow key={bond.symbol}>
                      <TableCell>
                        <div className="flex items-center gap-3">
                          <div className={cn(
                            "flex h-10 w-10 items-center justify-center rounded-lg",
                            !hasChange ? "bg-muted" : isPositive ? "bg-success/10" : "bg-danger/10"
                          )}>
                            {!hasChange ? (
                              <Minus className="h-5 w-5 text-muted-foreground" />
                            ) : isPositive ? (
                              <TrendingUp className="h-5 w-5 text-success" />
                            ) : (
                              <TrendingDown className="h-5 w-5 text-danger" />
                            )}
                          </div>
                          <div className="flex items-center gap-2">
                            <span className="font-semibold">{bond.symbol}</span>
                            {isSimulatedMarketData(bond) && <SimulationDataFlag className="h-4 px-1 text-[9px]" />}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <span className="text-muted-foreground truncate max-w-[200px] block">
                          {bond.name}
                        </span>
                      </TableCell>
                      <TableCell className="text-right font-semibold">
                        {formatCurrency(bond.currentPrice, 'TRY')}
                      </TableCell>
                      <TableCell className="text-right">
                        {bond.yieldRate != null ? `%${Number(bond.yieldRate).toFixed(2)}` : '-'}
                      </TableCell>
                      <TableCell className="text-right">
                        <Badge variant={!hasChange ? 'secondary' : isPositive ? 'success' : 'danger'}>
                          {hasChange ? formatPercent(bond.changePercent) : '-'}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right text-muted-foreground">
                        {bond.maturityDate ? formatDate(bond.maturityDate) : '-'}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <WatchlistQuickAddButton symbol={bond.symbol} />
                          <PortfolioQuickTradeCell instrument={bond} selectedPortfolioId={selectedPortfolioId} />
                        </div>
                      </TableCell>
                    </TableRow>
                    )
                  })
                )}
              </TableBody>
            </Table>
          )}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-6">
              <Button
                variant="outline"
                onClick={() => setPage((value) => Math.max(0, value - 1))}
                disabled={page === 0 || isFetching}
              >
                {t('common.previous')}
              </Button>
              <span className="text-sm text-muted-foreground px-4">
                {page + 1} / {totalPages}
              </span>
              <Button
                variant="outline"
                onClick={() => setPage((value) => value + 1)}
                disabled={page >= totalPages - 1 || isFetching}
              >
                {t('common.next')}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
