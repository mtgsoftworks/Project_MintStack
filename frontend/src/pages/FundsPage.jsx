import { useState } from 'react'
import { Search, RefreshCw, TrendingUp, TrendingDown, Wallet } from 'lucide-react'
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
import { useGetFundsQuery } from '@/store/api/marketApi'

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
  const [searchQuery, setSearchQuery] = useState('')
  const { data, isLoading, isFetching, refetch } = useGetFundsQuery({})

  const funds = data?.data || []

  const filteredFunds = funds.filter((fund) =>
    fund.symbol.toLowerCase().includes(searchQuery.toLowerCase()) ||
    fund.name?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  return (
    <div className="space-y-6 animate-in">
      {/* Page Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Yatırım Fonları</h1>
          <p className="text-muted-foreground">
            Yatırım fonları ve borsa yatırım fonları
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Fon ara..."
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

      {/* Funds Table */}
      <Card>
        <CardHeader>
          <CardTitle>Fon Listesi</CardTitle>
          <CardDescription>
            {funds.length} adet yatırım fonu listeleniyor
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <FundTableSkeleton />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Kod</TableHead>
                  <TableHead>Fon Adı</TableHead>
                  <TableHead className="text-right">Fiyat</TableHead>
                  <TableHead className="text-right">Değişim</TableHead>
                  <TableHead className="text-right">Toplam Değer</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredFunds.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} className="text-center py-8 text-muted-foreground">
                      Sonuç bulunamadı
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
                          <span className="font-semibold">{fund.symbol}</span>
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
                        {fund.totalValue ? formatCurrency(fund.totalValue, 'TRY') : '-'}
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
