import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router-dom'
import { Search, TrendingUp, TrendingDown, ArrowUpDown, List, LayoutGrid, Minus } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import SimulationDataFlag from '@/components/common/SimulationDataFlag'
import RefreshButton from '@/components/common/RefreshButton'
import RefreshStatus from '@/components/common/RefreshStatus'
import PortfolioQuickTradeCell from '@/components/market/PortfolioQuickTradeCell'
import WatchlistQuickAddButton from '@/components/market/WatchlistQuickAddButton'
import MarketChangeRangeSelector from '@/components/market/MarketChangeRangeSelector'
import { Switch } from '@/components/ui/switch'
import { Label } from '@/components/ui/label'
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
import { cn, formatCurrency, formatPercent } from '@/lib/utils'
import { useGetStocksQuery } from '@/store/api/marketApi'
import { useAutoRefresh } from '@/hooks/useAutoRefresh'
import { useMarketDataRefresh } from '@/hooks/useMarketDataRefresh'
import { getMarketChangeRangeLabel, useMarketChangeRange } from '@/hooks/useMarketChangeRange'
import { useVirtualizer } from '@tanstack/react-virtual'
import { useDefaultPortfolioSelection } from '@/hooks/useDefaultPortfolioSelection'

function StockTableSkeleton() {
  return (
    <div className="space-y-3">
      {[...Array(10)].map((_, i) => (
        <Skeleton key={i} className="h-14 w-full" />
      ))}
    </div>
  )
}

function hasFiniteChange(value) {
  return value !== null && value !== undefined && Number.isFinite(Number(value))
}

// Virtual scrolling stock row component
function VirtualStockRow({ stock, selectedPortfolioId }) {
  const hasChange = hasFiniteChange(stock.changePercent)
  const isPositive = hasChange && Number(stock.changePercent) >= 0

  return (
    <div className="flex items-center px-4 gap-4 h-14 border-b hover:bg-muted/50 transition-colors">
      <div className="w-[180px] flex-shrink-0">
        <Link
          to={`/market/stocks/${stock.symbol}`}
          className="flex items-center gap-3 hover:text-primary transition-colors"
        >
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
            <span className="font-semibold">{stock.symbol}</span>
            {isSimulatedMarketData(stock) && <SimulationDataFlag className="h-4 px-1 text-[9px]" />}
          </div>
        </Link>
      </div>
      <div className="flex-1 min-w-0">
        <span className="text-muted-foreground truncate block">
          {stock.name}
        </span>
      </div>
      <div className="w-[120px] text-right font-semibold flex-shrink-0">
        {formatCurrency(stock.currentPrice, 'TRY')}
      </div>
      <div className="w-[100px] text-right flex-shrink-0">
        <Badge variant={!hasChange ? 'secondary' : isPositive ? 'success' : 'danger'}>
          {hasChange ? formatPercent(stock.changePercent) : '-'}
        </Badge>
      </div>
      <div className="w-[100px] text-right text-muted-foreground flex-shrink-0">
        {formatCurrency(stock.previousClose, 'TRY')}
      </div>
      <div className="w-[100px] text-right text-muted-foreground flex-shrink-0">
        {stock.volume?.toLocaleString('tr-TR') || '-'}
      </div>
      <div className="w-[280px] flex-shrink-0">
        <div className="flex justify-end gap-2">
          <WatchlistQuickAddButton symbol={stock.symbol} />
          <PortfolioQuickTradeCell instrument={stock} selectedPortfolioId={selectedPortfolioId} />
        </div>
      </div>
    </div>
  )
}

export default function StocksPage() {
  const { t } = useTranslation()
  const [searchParams] = useSearchParams()
  const searchParam = searchParams.get('search') || ''
  const [page, setPage] = useState(0)
  const [searchQuery, setSearchQuery] = useState(() => searchParam)
  const [sortBy, setSortBy] = useState('symbol')
  const [sortOrder, setSortOrder] = useState('asc')
  const [pageSize, setPageSize] = useState(50)
  const [useVirtualScroll, setUseVirtualScroll] = useState(false)
  const parentRef = useRef(null)
  const { portfolios, selectedPortfolioId, setSelectedPortfolioId } = useDefaultPortfolioSelection()
  const { autoUpdate, refreshRate, queryOptions } = useAutoRefresh()
  const { refreshAndRefetch, isRefreshingMarketData } = useMarketDataRefresh(['BIST_STOCKS'])
  const changeRange = useMarketChangeRange()
  const changeRangeLabel = getMarketChangeRangeLabel(t, changeRange)

  useEffect(() => {
    setSearchQuery(searchParam)
    setPage(0)
  }, [searchParam])

  useEffect(() => {
    setPage(0)
  }, [searchQuery, pageSize, sortBy, sortOrder, changeRange.queryParams.changeStartDate, changeRange.queryParams.changeEndDate])

  // Load more items when virtual scrolling is enabled
  const requestSize = useVirtualScroll ? Math.max(pageSize, 100) : pageSize

  const {
    data,
    isLoading,
    isFetching,
    refetch,
    fulfilledTimeStamp,
  } = useGetStocksQuery(
    {
      page: useVirtualScroll ? 0 : page,
      size: requestSize,
      sort: `${sortBy},${sortOrder}`,
      search: searchQuery.trim() || undefined,
      ...changeRange.queryParams,
    },
    queryOptions
  )

  const stocks = data?.data || []
  const totalPages = data?.pagination?.totalPages || 0
  const totalElements = data?.pagination?.totalElements || 0

  const filteredStocks = stocks.filter((stock) =>
    stock.symbol.toLowerCase().includes(searchQuery.toLowerCase()) ||
    stock.name?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  // Virtual scrolling setup
  const virtualizer = useVirtualizer({
    count: filteredStocks.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 56,
    overscan: 10,
  })

  const handleSort = (field) => {
    if (sortBy === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')
    } else {
      setSortBy(field)
      setSortOrder('asc')
    }
  }

  return (
    <div className="space-y-6 animate-in">
      {/* Page Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t('stocksPage.title')}</h1>
          <p className="text-muted-foreground">
            {t('stocksPage.subtitle')}
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
          {/* Virtual Scroll Toggle */}
          <div className="flex items-center space-x-2">
            <Switch
              id="virtual-mode"
              checked={useVirtualScroll}
              onCheckedChange={setUseVirtualScroll}
            />
            <Label htmlFor="virtual-mode" className="text-sm cursor-pointer">
              {useVirtualScroll ? (
                <span className="flex items-center gap-1">
                  <LayoutGrid className="h-4 w-4" />
                  {t('stocksPage.virtualMode')}
                </span>
              ) : (
                <span className="flex items-center gap-1">
                  <List className="h-4 w-4" />
                  {t('stocksPage.paginatedMode')}
                </span>
              )}
            </Label>
          </div>

          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder={t('stocksPage.searchPlaceholder')}
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

      {/* Stocks Table */}
      <Card>
        <CardHeader>
          <CardTitle>{t('stocksPage.listTitle')}</CardTitle>
          <CardDescription>
            {t('stocksPage.listDescription', { count: data?.pagination?.totalElements || 0 })}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <StockTableSkeleton />
          ) : useVirtualScroll ? (
            /* Virtual Scrolling Mode */
            <div className="border rounded-lg">
              {/* Header */}
              <div className="flex items-center px-4 h-12 border-b bg-muted/50 text-sm font-medium text-muted-foreground">
                <div className="w-[180px] flex-shrink-0">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="-ml-3 h-8"
                    onClick={() => handleSort('symbol')}
                  >
                    {t('stocksPage.headers.symbol')}
                    <ArrowUpDown className="ml-2 h-4 w-4" />
                  </Button>
                </div>
                <div className="flex-1">{t('stocksPage.headers.company')}</div>
                <div className="w-[120px] text-right flex-shrink-0">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="-mr-3 h-8"
                    onClick={() => handleSort('currentPrice')}
                  >
                    {t('stocksPage.headers.price')}
                    <ArrowUpDown className="ml-2 h-4 w-4" />
                  </Button>
                </div>
                <div className="w-[100px] text-right flex-shrink-0">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="-mr-3 h-8"
                    onClick={() => handleSort('changePercent')}
                  >
                    {t('marketChangeRange.changeHeader', { range: changeRangeLabel })}
                    <ArrowUpDown className="ml-2 h-4 w-4" />
                  </Button>
                </div>
                <div className="w-[100px] text-right flex-shrink-0">{t('stocksPage.headers.previousClose')}</div>
                <div className="w-[100px] text-right flex-shrink-0">{t('stocksPage.headers.volume')}</div>
                <div className="w-[280px] text-right flex-shrink-0">{t('stocksPage.headers.action')}</div>
              </div>

              {/* Virtual List */}
              {filteredStocks.length === 0 ? (
                <div className="text-center py-8 text-muted-foreground">
                  {t('stocksPage.empty')}
                </div>
              ) : (
                <div
                  ref={parentRef}
                  className="overflow-auto"
                  style={{ height: '500px' }}
                >
                  <div
                    style={{
                      height: `${virtualizer.getTotalSize()}px`,
                      width: '100%',
                      position: 'relative',
                    }}
                  >
                    {virtualizer.getVirtualItems().map((virtualRow) => {
                      const stock = filteredStocks[virtualRow.index]
                      return (
                        <div
                          key={virtualRow.key}
                          style={{
                            position: 'absolute',
                            top: 0,
                            left: 0,
                            width: '100%',
                            height: `${virtualRow.size}px`,
                            transform: `translateY(${virtualRow.start}px)`,
                          }}
                        >
                          <VirtualStockRow stock={stock} selectedPortfolioId={selectedPortfolioId} />
                        </div>
                      )
                    })}
                  </div>
                </div>
              )}

              {/* Info */}
              <div className="px-4 py-2 border-t bg-muted/30 text-xs text-muted-foreground">
                {t('stocksPage.virtualInfo', { visible: filteredStocks.length,
                  total: totalElements })}
              </div>
            </div>
          ) : (
            /* Regular Paginated Mode */
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="-ml-3 h-8"
                        onClick={() => handleSort('symbol')}
                      >
                        {t('stocksPage.headers.symbol')}
                        <ArrowUpDown className="ml-2 h-4 w-4" />
                      </Button>
                    </TableHead>
                    <TableHead>{t('stocksPage.headers.company')}</TableHead>
                    <TableHead className="text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        className="-mr-3 h-8"
                        onClick={() => handleSort('currentPrice')}
                      >
                        {t('stocksPage.headers.price')}
                        <ArrowUpDown className="ml-2 h-4 w-4" />
                      </Button>
                    </TableHead>
                    <TableHead className="text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        className="-mr-3 h-8"
                        onClick={() => handleSort('changePercent')}
                      >
                        {t('marketChangeRange.changeHeader', { range: changeRangeLabel })}
                        <ArrowUpDown className="ml-2 h-4 w-4" />
                      </Button>
                    </TableHead>
                    <TableHead className="text-right">{t('stocksPage.headers.previousClose')}</TableHead>
                    <TableHead className="text-right">{t('stocksPage.headers.volume')}</TableHead>
                    <TableHead className="text-right">{t('stocksPage.headers.action')}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredStocks.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                        {t('stocksPage.empty')}
                      </TableCell>
                    </TableRow>
                  ) : (
                    filteredStocks.map((stock) => {
                      const hasChange = hasFiniteChange(stock.changePercent)
                      const isPositive = hasChange && Number(stock.changePercent) >= 0

                      return (
                      <TableRow key={stock.symbol}>
                        <TableCell>
                          <Link
                            to={`/market/stocks/${stock.symbol}`}
                            className="flex items-center gap-3 hover:text-primary transition-colors"
                          >
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
                              <span className="font-semibold">{stock.symbol}</span>
                              {isSimulatedMarketData(stock) && (
                                <SimulationDataFlag className="h-4 px-1 text-[9px]" />
                              )}
                            </div>
                          </Link>
                        </TableCell>
                        <TableCell>
                          <span className="text-muted-foreground truncate max-w-[200px] block">
                            {stock.name}
                          </span>
                        </TableCell>
                        <TableCell className="text-right font-semibold">
                          {formatCurrency(stock.currentPrice, 'TRY')}
                        </TableCell>
                        <TableCell className="text-right">
                          <Badge variant={!hasChange ? 'secondary' : isPositive ? 'success' : 'danger'}>
                            {hasChange ? formatPercent(stock.changePercent) : '-'}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-right text-muted-foreground">
                          {formatCurrency(stock.previousClose, 'TRY')}
                        </TableCell>
                        <TableCell className="text-right text-muted-foreground">
                          {stock.volume?.toLocaleString('tr-TR') || '-'}
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex justify-end gap-2">
                            <WatchlistQuickAddButton symbol={stock.symbol} />
                            <PortfolioQuickTradeCell instrument={stock} selectedPortfolioId={selectedPortfolioId} />
                          </div>
                        </TableCell>
                      </TableRow>
                      )
                    })
                  )}
                </TableBody>
              </Table>

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="flex items-center justify-center gap-2 mt-6">
                  <Button
                    variant="outline"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0 || isFetching}
                  >
                    {t('common.previous')}
                  </Button>
                  <span className="text-sm text-muted-foreground px-4">
                    {t('stocksPage.pagination', { current: page + 1, total: totalPages })}
                  </span>
                  <Button
                    variant="outline"
                    onClick={() => setPage((p) => p + 1)}
                    disabled={page >= totalPages - 1 || isFetching}
                  >
                    {t('common.next')}
                  </Button>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
