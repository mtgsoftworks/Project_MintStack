import { FormEvent, useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Search, ArrowRight } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
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
import { formatCurrency } from '@/lib/utils'
import { useSearchMarketQuery } from '@/store/api/marketApi'

function SearchSkeleton() {
  return (
    <div className="space-y-3">
      {[...Array(8)].map((_, index) => (
        <Skeleton key={index} className="h-12 w-full" />
      ))}
    </div>
  )
}

function labelForType(type: string | undefined) {
  if (!type) {
    return 'Bilinmiyor'
  }
  switch (type) {
    case 'STOCK':
      return 'Hisse'
    case 'BOND':
      return 'Tahvil/Bono'
    case 'FUND':
      return 'Yatirim Fonu'
    case 'VIOP':
      return 'VIOP'
    case 'CURRENCY':
      return 'Doviz'
    case 'INDEX':
      return 'Endeks'
    case 'CRYPTO':
      return 'Kripto'
    default:
      return type
  }
}

function routeForInstrument(symbol: string, type: string | undefined) {
  const encoded = encodeURIComponent(symbol)
  switch (type) {
    case 'STOCK':
      return `/market/stocks/${encoded}`
    case 'BOND':
      return `/market/bonds?search=${encoded}`
    case 'FUND':
      return `/market/funds?search=${encoded}`
    case 'VIOP':
      return `/market/viop?search=${encoded}`
    case 'CURRENCY':
      return `/market/currencies?search=${encoded}`
    case 'INDEX':
      return `/analysis?symbol=${encoded}`
    default:
      return `/market/stocks?search=${encoded}`
  }
}

export default function MarketSearchPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const initialQuery = searchParams.get('query')?.trim() || ''
  const [searchQuery, setSearchQuery] = useState(initialQuery)

  useEffect(() => {
    setSearchQuery(initialQuery)
  }, [initialQuery])

  const query = initialQuery
  const shouldSearch = query.length > 0

  const { data: results = [], isFetching, isLoading } = useSearchMarketQuery(query, {
    skip: !shouldSearch,
    refetchOnFocus: true,
    refetchOnReconnect: true,
  })

  const groupedCounts = useMemo(() => {
    const counts: Record<string, number> = {}
    for (const item of results) {
      const key = labelForType(item.type)
      counts[key] = (counts[key] || 0) + 1
    }
    return counts
  }, [results])

  const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const trimmed = searchQuery.trim()
    if (!trimmed) {
      setSearchParams({})
      return
    }
    setSearchParams({ query: trimmed })
  }

  return (
    <div className="space-y-6 animate-in">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold">{t('common.search')}</h1>
        <p className="text-muted-foreground">
          Tum piyasalarda sembol ve isim bazli arama
        </p>
      </div>

      <Card>
        <CardContent className="pt-6">
          <form className="flex flex-col gap-3 md:flex-row" onSubmit={handleSearchSubmit}>
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                className="pl-9"
                placeholder={t('header.searchPlaceholder')}
              />
            </div>
            <Button type="submit">{t('common.search')}</Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Arama Sonuclari</CardTitle>
          <CardDescription>
            {shouldSearch
              ? `"${query}" icin ${results.length} sonuc bulundu`
              : 'Aramak istediginiz kelimeyi yazin'}
          </CardDescription>
          {shouldSearch && results.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {Object.entries(groupedCounts).map(([label, count]) => (
                <Badge key={label} variant="outline">
                  {label}: {count}
                </Badge>
              ))}
            </div>
          )}
        </CardHeader>
        <CardContent>
          {!shouldSearch ? (
            <div className="py-10 text-center text-sm text-muted-foreground">
              Ornek: THYAO, USD, XU100, FROTO, TUREX
            </div>
          ) : isLoading || isFetching ? (
            <SearchSkeleton />
          ) : results.length === 0 ? (
            <div className="py-10 text-center text-sm text-muted-foreground">
              Sonuc bulunamadi.
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Sembol</TableHead>
                  <TableHead>Ad</TableHead>
                  <TableHead>Tur</TableHead>
                  <TableHead className="text-right">Fiyat</TableHead>
                  <TableHead className="text-right">Aksiyon</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {results.map((item) => (
                  <TableRow key={`${item.type}-${item.symbol}`}>
                    <TableCell className="font-semibold">{item.symbol}</TableCell>
                    <TableCell className="text-muted-foreground">{item.name || '-'}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">{labelForType(item.type)}</Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      {item.currentPrice != null ? formatCurrency(item.currentPrice, item.currency || 'TRY') : '-'}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate(routeForInstrument(item.symbol, item.type))}
                      >
                        Ac
                        <ArrowRight className="ml-1 h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
