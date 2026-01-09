import { useState } from 'react'
import { Search, RefreshCw, TrendingUp, TrendingDown } from 'lucide-react'
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
  const [searchQuery, setSearchQuery] = useState('')
  const { data, isLoading, isFetching, refetch } = useGetBondsQuery({})

  const bonds = data?.data || []

  const filteredBonds = bonds.filter((bond) =>
    bond.symbol.toLowerCase().includes(searchQuery.toLowerCase()) ||
    bond.name?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  return (
    <div className="space-y-6 animate-in">
      {/* Page Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Tahvil & Bono</h1>
          <p className="text-muted-foreground">
            Devlet tahvilleri ve hazine bonoları
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Tahvil/Bono ara..."
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
          <CardTitle>Tahvil & Bono Listesi</CardTitle>
          <CardDescription>
            {bonds.length} adet tahvil/bono listeleniyor
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <BondTableSkeleton />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Sembol</TableHead>
                  <TableHead>İsim</TableHead>
                  <TableHead className="text-right">Fiyat</TableHead>
                  <TableHead className="text-right">Getiri</TableHead>
                  <TableHead className="text-right">Değişim</TableHead>
                  <TableHead className="text-right">Vade</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredBonds.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                      Sonuç bulunamadı
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
                          <span className="font-semibold">{bond.symbol}</span>
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
                        {bond.yield ? `%${bond.yield.toFixed(2)}` : '-'}
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
