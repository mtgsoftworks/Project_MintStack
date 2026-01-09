import { useState } from 'react'
import { Search, RefreshCw, TrendingUp, TrendingDown, LineChart } from 'lucide-react'
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
import { cn, formatCurrency, formatPercent, formatDate } from '@/lib/utils'
import { useGetViopQuery } from '@/store/api/marketApi'

function ViopTableSkeleton() {
  return (
    <div className="space-y-3">
      {[...Array(10)].map((_, i) => (
        <Skeleton key={i} className="h-14 w-full" />
      ))}
    </div>
  )
}

export default function ViopPage() {
  const [searchQuery, setSearchQuery] = useState('')
  const { data, isLoading, isFetching, refetch } = useGetViopQuery({})

  const viop = data?.data || []

  const filteredViop = viop.filter((contract) =>
    contract.symbol.toLowerCase().includes(searchQuery.toLowerCase()) ||
    contract.name?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  return (
    <div className="space-y-6 animate-in">
      {/* Page Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">VIOP</h1>
          <p className="text-muted-foreground">
            Vadeli İşlem ve Opsiyon Piyasası
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Kontrat ara..."
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

      {/* VIOP Table */}
      <Card>
        <CardHeader>
          <CardTitle>Vadeli İşlem Kontratları</CardTitle>
          <CardDescription>
            {viop.length} adet kontrat listeleniyor
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <ViopTableSkeleton />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Kontrat</TableHead>
                  <TableHead>Dayanak Varlık</TableHead>
                  <TableHead className="text-right">Fiyat</TableHead>
                  <TableHead className="text-right">Değişim</TableHead>
                  <TableHead className="text-right">Hacim</TableHead>
                  <TableHead className="text-right">Vade</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredViop.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                      Sonuç bulunamadı
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredViop.map((contract) => (
                    <TableRow key={contract.symbol}>
                      <TableCell>
                        <div className="flex items-center gap-3">
                          <div className={cn(
                            "flex h-10 w-10 items-center justify-center rounded-lg",
                            contract.changePercent >= 0 ? "bg-success/10" : "bg-danger/10"
                          )}>
                            <LineChart className={cn(
                              "h-5 w-5",
                              contract.changePercent >= 0 ? "text-success" : "text-danger"
                            )} />
                          </div>
                          <span className="font-semibold">{contract.symbol}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <span className="text-muted-foreground truncate max-w-[200px] block">
                          {contract.name}
                        </span>
                      </TableCell>
                      <TableCell className="text-right font-semibold">
                        {formatCurrency(contract.currentPrice, 'TRY')}
                      </TableCell>
                      <TableCell className="text-right">
                        <Badge variant={contract.changePercent >= 0 ? 'success' : 'danger'}>
                          {formatPercent(contract.changePercent || 0)}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right text-muted-foreground">
                        {contract.volume?.toLocaleString('tr-TR') || '-'}
                      </TableCell>
                      <TableCell className="text-right text-muted-foreground">
                        {contract.maturityDate ? formatDate(contract.maturityDate) : '-'}
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
