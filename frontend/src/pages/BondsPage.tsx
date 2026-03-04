import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Search, RefreshCw, TrendingUp, TrendingDown } from 'lucide-react'
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
import { cn, formatCurrency, formatPercent, formatDate } from '@/lib/utils'
import { useGetBondsQuery } from '@/store/api/marketApi'

function BondTableSkeleton() {
  return (
    <div className="space-y-3">
      {[...Array(10)].map((_, i) => (
        <Skeleton key={i} className="h-14 w-full" />
      ))}
    </div>
  )
}

export default function BondsPage() {
  const { t } = useTranslation()
  const [searchQuery, setSearchQuery] = useState('')
  const { data, isLoading, isFetching, refetch } = useGetBondsQuery({})

  const bonds = data?.data || []

  const filteredBonds = bonds.filter((bond) =>
    bond.symbol.toLowerCase().includes(searchQuery.toLowerCase()) ||
    bond.name?.toLowerCase().includes(searchQuery.toLowerCase())
  ).sort((left, right) => left.symbol.localeCompare(right.symbol))

  return (
    <div className="space-y-6 animate-in">
      {/* Page Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t('bondsPage.title')}</h1>
          <p className="text-muted-foreground">
            {t('bondsPage.subtitle')}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder={t('bondsPage.searchPlaceholder')}
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

      {/* Bonds Table */}
      <Card>
        <CardHeader>
          <CardTitle>{t('bondsPage.listTitle')}</CardTitle>
          <CardDescription>
            {t('bondsPage.listDescription', { count: bonds.length })}
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
                  <TableHead className="text-right">{t('bondsPage.headers.change')}</TableHead>
                  <TableHead className="text-right">{t('bondsPage.headers.maturity')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredBonds.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                      {t('bondsPage.empty')}
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredBonds.map((bond) => (
                    <TableRow key={bond.symbol}>
                      <TableCell>
                        <div className="flex items-center gap-3">
                          <div className={cn(
                            "flex h-10 w-10 items-center justify-center rounded-lg",
                            bond.changePercent >= 0 ? "bg-success/10" : "bg-danger/10"
                          )}>
                            {bond.changePercent >= 0 ? (
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
                        <Badge variant={bond.changePercent >= 0 ? 'success' : 'danger'}>
                          {formatPercent(bond.changePercent || 0)}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right text-muted-foreground">
                        {bond.maturityDate ? formatDate(bond.maturityDate) : '-'}
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
