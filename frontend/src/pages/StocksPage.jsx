import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Search, RefreshCw, TrendingUp, TrendingDown, ArrowUpDown } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { cn, formatCurrency, formatPercent } from '@/lib/utils'
import { useGetStocksQuery } from '@/store/api/marketApi'

function StockTableSkeleton() {
  return (
    <div className="space-y-3">
      {[...Array(10)].map((_, i) => (
        <Skeleton key={i} className="h-14 w-full" />
      ))}
    </div>
  )
}

export default function StocksPage() {
  const [page, setPage] = useState(0)
  const [searchQuery, setSearchQuery] = useState('')
  const [sortBy, setSortBy] = useState('symbol')
  const [sortOrder, setSortOrder] = useState('asc')

  const { data, isLoading, isFetching, refetch } = useGetStocksQuery({
    page,
    size: 20,
    sort: `${sortBy},${sortOrder}`,
  })

  const stocks = data?.data || []
  const totalPages = data?.pagination?.totalPages || 0

  const filteredStocks = stocks.filter((stock) =>
    stock.symbol.toLowerCase().includes(searchQuery.toLowerCase()) ||
    stock.name?.toLowerCase().includes(searchQuery.toLowerCase())
  )

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
          <h1 className="text-2xl font-bold">Hisse Senetleri</h1>
          <p className="text-muted-foreground">
            BIST hisse senetleri listesi
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Hisse ara..."
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

      {/* Stocks Table */}
      <Card>
        <CardHeader>
          <CardTitle>Hisse Listesi</CardTitle>
          <CardDescription>
            {data?.pagination?.totalElements || 0} hisse senedi listeleniyor
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <StockTableSkeleton />
          ) : (
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
                        Sembol
                        <ArrowUpDown className="ml-2 h-4 w-4" />
                      </Button>
                    </TableHead>
                    <TableHead>Şirket</TableHead>
                    <TableHead className="text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        className="-mr-3 h-8"
                        onClick={() => handleSort('currentPrice')}
                      >
                        Fiyat
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
                        Değişim
                        <ArrowUpDown className="ml-2 h-4 w-4" />
                      </Button>
                    </TableHead>
                    <TableHead className="text-right">Önceki Kapanış</TableHead>
                    <TableHead className="text-right">Hacim</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredStocks.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                        Sonuç bulunamadı
                      </TableCell>
                    </TableRow>
                  ) : (
                    filteredStocks.map((stock) => (
                      <TableRow key={stock.symbol}>
                        <TableCell>
                          <Link
                            to={`/market/stocks/${stock.symbol}`}
                            className="flex items-center gap-3 hover:text-primary transition-colors"
                          >
                            <div className={cn(
                              "flex h-10 w-10 items-center justify-center rounded-lg",
                              stock.changePercent >= 0 ? "bg-success/10" : "bg-danger/10"
                            )}>
                              {stock.changePercent >= 0 ? (
                                <TrendingUp className="h-5 w-5 text-success" />
                              ) : (
                                <TrendingDown className="h-5 w-5 text-danger" />
                              )}
                            </div>
                            <span className="font-semibold">{stock.symbol}</span>
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
                          <Badge variant={stock.changePercent >= 0 ? 'success' : 'danger'}>
                            {formatPercent(stock.changePercent || 0)}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-right text-muted-foreground">
                          {formatCurrency(stock.previousClose, 'TRY')}
                        </TableCell>
                        <TableCell className="text-right text-muted-foreground">
                          {stock.volume?.toLocaleString('tr-TR') || '-'}
                        </TableCell>
                      </TableRow>
                    ))
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
                    Önceki
                  </Button>
                  <span className="text-sm text-muted-foreground px-4">
                    Sayfa {page + 1} / {totalPages}
                  </span>
                  <Button
                    variant="outline"
                    onClick={() => setPage((p) => p + 1)}
                    disabled={page >= totalPages - 1 || isFetching}
                  >
                    Sonraki
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
