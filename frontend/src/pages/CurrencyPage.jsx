import { useState } from 'react'
import { DollarSign, TrendingUp, TrendingDown, Search, RefreshCw } from 'lucide-react'
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
  const [searchQuery, setSearchQuery] = useState('')
  const { data: currencies, isLoading, isFetching, refetch } = useGetCurrenciesQuery()

  const filteredCurrencies = currencies?.filter((currency) =>
    currency.currencyCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
    currency.currencyName?.toLowerCase().includes(searchQuery.toLowerCase())
  ) || []

  return (
    <div className="space-y-6 animate-in">
      {/* Page Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Döviz Kurları</h1>
          <p className="text-muted-foreground">
            TCMB güncel döviz kurları
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Döviz ara..."
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
                        {currency ? formatCurrency(currency.forexSelling, 'TRY') : '-'}
                      </p>
                      <p className="text-xs text-muted-foreground">{code}/TRY</p>
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
          <CardTitle>Tüm Döviz Kurları</CardTitle>
          <CardDescription>
            Son güncelleme: {currencies?.[0]?.rateDate ? formatDateTime(currencies[0].rateDate) : '-'}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <CurrencyTableSkeleton />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Döviz</TableHead>
                  <TableHead className="text-right">Döviz Alış</TableHead>
                  <TableHead className="text-right">Döviz Satış</TableHead>
                  <TableHead className="text-right">Efektif Alış</TableHead>
                  <TableHead className="text-right">Efektif Satış</TableHead>
                  <TableHead className="text-right">Değişim</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredCurrencies.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                      Sonuç bulunamadı
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
                            <p className="font-medium">{currency.currencyCode}/TRY</p>
                            <p className="text-xs text-muted-foreground">
                              {currency.currencyName}
                            </p>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="text-right font-medium">
                        {formatNumber(currency.forexBuying, 4)}
                      </TableCell>
                      <TableCell className="text-right font-medium">
                        {formatNumber(currency.forexSelling, 4)}
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
