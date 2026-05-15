import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Search, Wallet } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import SimulationDataFlag from '@/components/common/SimulationDataFlag'
import RefreshButton from '@/components/common/RefreshButton'
import PortfolioQuickTradeCell from '@/components/market/PortfolioQuickTradeCell'
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
import { formatCurrency, formatPercent } from '@/lib/utils'
import { useGetFundsQuery } from '@/store/api/marketApi'
import { useDefaultPortfolioSelection } from '@/hooks/useDefaultPortfolioSelection'

function FundTableSkeleton() {
  return (
    <div className="space-y-3">
      {[...Array(10)].map((_, i) => (
        <Skeleton key={i} className="h-14 w-full" />
      ))}
    </div>
  )
}

export default function FundsPage() {
  const { t } = useTranslation()
  const [searchQuery, setSearchQuery] = useState('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(50)
  const { portfolios, selectedPortfolioId, setSelectedPortfolioId } = useDefaultPortfolioSelection()
  const { data, isLoading, isFetching, refetch } = useGetFundsQuery({
    page,
    size: pageSize,
    search: searchQuery.trim() || undefined,
  })

  const funds = data?.data || []
  const totalPages = data?.pagination?.totalPages || 0
  const totalElements = data?.pagination?.totalElements || 0

  useEffect(() => {
    setPage(0)
  }, [searchQuery, pageSize])

  const filteredFunds = funds.filter((fund) =>
    fund.symbol.toLowerCase().includes(searchQuery.toLowerCase()) ||
    fund.name?.toLowerCase().includes(searchQuery.toLowerCase())
  ).sort((left, right) => left.symbol.localeCompare(right.symbol))

  return (
    <div className="space-y-6 animate-in">
      {/* Page Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t('fundsPage.title')}</h1>
          <p className="text-muted-foreground">
            {t('fundsPage.subtitle')}
          </p>
        </div>
        <div className="flex items-center gap-2">
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
              placeholder={t('fundsPage.searchPlaceholder')}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 w-64"
            />
          </div>
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
            isLoading={isFetching}
            onRefresh={refetch}
          />
        </div>
      </div>

      {/* Funds Table */}
      <Card>
        <CardHeader>
          <CardTitle>{t('fundsPage.listTitle')}</CardTitle>
          <CardDescription>
            {t('fundsPage.listDescription', { count: totalElements })}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <FundTableSkeleton />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('fundsPage.headers.code')}</TableHead>
                  <TableHead>{t('fundsPage.headers.name')}</TableHead>
                  <TableHead className="text-right">{t('fundsPage.headers.price')}</TableHead>
                  <TableHead className="text-right">{t('fundsPage.headers.change')}</TableHead>
                  <TableHead className="text-right">{t('fundsPage.headers.totalValue')}</TableHead>
                  <TableHead className="text-right">Islem</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredFunds.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                      {t('fundsPage.empty')}
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredFunds.map((fund) => (
                    <TableRow key={fund.symbol}>
                      <TableCell>
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-info/10">
                            <Wallet className="h-5 w-5 text-info" />
                          </div>
                          <div className="flex items-center gap-2">
                            <span className="font-semibold">{fund.symbol}</span>
                            {isSimulatedMarketData(fund) && <SimulationDataFlag className="h-4 px-1 text-[9px]" />}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <span className="text-muted-foreground truncate max-w-[250px] block">
                          {fund.name}
                        </span>
                      </TableCell>
                      <TableCell className="text-right font-semibold">
                        {formatCurrency(fund.currentPrice, 'TRY')}
                      </TableCell>
                      <TableCell className="text-right">
                        <Badge variant={fund.changePercent >= 0 ? 'success' : 'danger'}>
                          {formatPercent(fund.changePercent || 0)}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right text-muted-foreground">
                        {fund.totalValue != null ? formatCurrency(fund.totalValue, 'TRY') : '-'}
                      </TableCell>
                      <TableCell className="text-right">
                        <PortfolioQuickTradeCell instrument={fund} selectedPortfolioId={selectedPortfolioId} />
                      </TableCell>
                    </TableRow>
                  ))
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
