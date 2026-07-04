import { useMemo, useState } from 'react'
import { Sparkles, TrendingUp, TrendingDown, Clock, ChevronDown, ChevronUp } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { cn, formatCurrency, formatPercent } from '@/lib/utils'
import { useGetCurrenciesQuery, useGetStocksQuery, useGetBondsQuery, useGetFundsQuery, useGetViopQuery } from '@/store/api/marketApi'

interface PortfolioItem {
  id?: string
  instrumentSymbol?: string
  symbol?: string
  instrumentName?: string
  name?: string
  quantity?: number
  currentPrice?: number
  unitPrice?: number
  currentValue?: number
}

interface PortfolioWhatIfWidgetProps {
  items: PortfolioItem[]
  cashBalance?: number
}

type Period = '1W' | '1M' | '3M' | '1Y'

const PERIOD_DAYS: Record<Period, number> = {
  '1W': 7,
  '1M': 30,
  '3M': 90,
  '1Y': 365,
}

const PERIOD_LABELS: Record<Period, string> = {
  '1W': '1 Hafta Önce',
  '1M': '1 Ay Önce',
  '3M': '3 Ay Önce',
  '1Y': '1 Yıl Önce',
}

function formatDateInput(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function daysAgo(days: number): string {
  return formatDateInput(new Date(Date.now() - days * 24 * 60 * 60 * 1000))
}

export function PortfolioWhatIfWidget({ items = [] }: PortfolioWhatIfWidgetProps) {
  const [period, setPeriod] = useState<Period>('1M')
  const [showDetails, setShowDetails] = useState(false)

  const changeQueryParams = useMemo(() => {
    const days = PERIOD_DAYS[period]
    return {
      changeStartDate: daysAgo(days),
      changeEndDate: formatDateInput(new Date()),
    }
  }, [period])

  // Query market endpoints for range data
  const { data: stocksData } = useGetStocksQuery({ size: 100, ...changeQueryParams })
  const { data: currenciesData } = useGetCurrenciesQuery(changeQueryParams)
  const { data: bondsData } = useGetBondsQuery({ size: 100, ...changeQueryParams })
  const { data: fundsData } = useGetFundsQuery({ size: 100, ...changeQueryParams })
  const { data: viopData } = useGetViopQuery({ size: 100, ...changeQueryParams })

  // Map of symbol -> changeBasePrice (historical price at changeStartDate)
  const priceMap = useMemo(() => {
    const map = new Map<string, { currentPrice: number; historicalPrice: number }>()

    const stocks = stocksData?.data || []
    stocks.forEach((s: { symbol?: string; currentPrice?: number; changeBasePrice?: number }) => {
      if (s.symbol && s.currentPrice != null) {
        map.set(s.symbol.toUpperCase(), {
          currentPrice: Number(s.currentPrice),
          historicalPrice: Number(s.changeBasePrice ?? s.currentPrice),
        })
      }
    })

    const currencies = Array.isArray(currenciesData) ? currenciesData : []
    currencies.forEach((c: { currencyCode?: string; sellingRate?: number; changeBaseRate?: number }) => {
      if (c.currencyCode && c.sellingRate != null) {
        const code = c.currencyCode.toUpperCase()
        const entry = {
          currentPrice: Number(c.sellingRate),
          historicalPrice: Number(c.changeBaseRate ?? c.sellingRate),
        }
        map.set(`${code}TRY`, entry)
        map.set(code, entry)
      }
    })

    const bonds = bondsData?.data || []
    bonds.forEach((b: { symbol?: string; currentPrice?: number; changeBasePrice?: number }) => {
      if (b.symbol && b.currentPrice != null) {
        map.set(b.symbol.toUpperCase(), {
          currentPrice: Number(b.currentPrice),
          historicalPrice: Number(b.changeBasePrice ?? b.currentPrice),
        })
      }
    })

    const funds = fundsData?.data || []
    funds.forEach((f: { symbol?: string; currentPrice?: number; changeBasePrice?: number }) => {
      if (f.symbol && f.currentPrice != null) {
        map.set(f.symbol.toUpperCase(), {
          currentPrice: Number(f.currentPrice),
          historicalPrice: Number(f.changeBasePrice ?? f.currentPrice),
        })
      }
    })

    const viop = viopData?.data || []
    viop.forEach((v: { symbol?: string; currentPrice?: number; changeBasePrice?: number }) => {
      if (v.symbol && v.currentPrice != null) {
        map.set(v.symbol.toUpperCase(), {
          currentPrice: Number(v.currentPrice),
          historicalPrice: Number(v.changeBasePrice ?? v.currentPrice),
        })
      }
    })

    return map
  }, [stocksData, currenciesData, bondsData, fundsData, viopData])

  // Calculate What-If stats
  const calculatedItems = useMemo(() => {
    return items.map((item) => {
      const symbol = (item.instrumentSymbol || item.symbol || '').toUpperCase()
      const name = item.instrumentName || item.name || symbol
      const quantity = Number(item.quantity || 0)
      const currentPrice = Number(item.currentPrice || item.unitPrice || 0)
      const currentValue = Number(item.currentValue || quantity * currentPrice)

      const marketEntry = priceMap.get(symbol)
      const historicalUnitPrice = marketEntry?.historicalPrice && marketEntry.historicalPrice > 0
        ? marketEntry.historicalPrice
        : currentPrice

      const historicalTotalValue = quantity * historicalUnitPrice
      const difference = currentValue - historicalTotalValue
      const differencePercent = historicalTotalValue > 0 ? (difference / historicalTotalValue) * 100 : 0
      const hasHistoricalData = marketEntry != null && marketEntry.historicalPrice > 0 && marketEntry.historicalPrice !== currentPrice

      return {
        symbol,
        name,
        quantity,
        currentPrice,
        currentValue,
        historicalUnitPrice,
        historicalTotalValue,
        difference,
        differencePercent,
        hasHistoricalData,
      }
    })
  }, [items, priceMap])

  const totalNow = useMemo(() => {
    return calculatedItems.reduce((acc, item) => acc + item.currentValue, 0)
  }, [calculatedItems])

  const totalThen = useMemo(() => {
    return calculatedItems.reduce((acc, item) => acc + item.historicalTotalValue, 0)
  }, [calculatedItems])

  const totalDiff = totalNow - totalThen
  const totalDiffPercent = totalThen > 0 ? (totalDiff / totalThen) * 100 : 0
  const isPositive = totalDiff >= 0

  if (items.length === 0) {
    return null
  }

  return (
    <Card className="border-primary/20 bg-gradient-to-br from-background via-background to-primary/5 shadow-md">
      <CardHeader className="pb-3">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <Sparkles className="h-5 w-5" />
            </div>
            <div>
              <CardTitle className="text-lg font-bold flex items-center gap-2">
                Geriye Dönük Performans Simülatörü
                <Badge variant="outline" className="text-xs font-normal">What-If</Badge>
              </CardTitle>
              <CardDescription className="text-xs">
                Bu portföydeki varlıkları geçmiş bir tarihte alsaydınız bugünkü değeriniz ne olurdu?
              </CardDescription>
            </div>
          </div>
          {/* Period Selector Tabs */}
          <div className="flex items-center gap-1 rounded-lg border bg-muted/50 p-1">
            {(['1W', '1M', '3M', '1Y'] as Period[]).map((p) => (
              <Button
                key={p}
                size="sm"
                variant={period === p ? 'default' : 'ghost'}
                className="h-7 px-3 text-xs"
                onClick={() => setPeriod(p)}
              >
                {p}
              </Button>
            ))}
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Main Stat Highlight */}
        <div className="grid gap-4 sm:grid-cols-3 rounded-xl border bg-card/60 p-4 backdrop-blur">
          <div>
            <p className="text-xs text-muted-foreground flex items-center gap-1">
              <Clock className="h-3.5 w-3.5" /> {PERIOD_LABELS[period]} Tahmini Değeri
            </p>
            <p className="text-xl font-bold mt-1">{formatCurrency(totalThen, 'TRY')}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Bugünkü Portföy Değeri</p>
            <p className="text-xl font-bold mt-1">{formatCurrency(totalNow, 'TRY')}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Potansiyel Fark / Kazanç</p>
            <div className="flex items-center gap-2 mt-1">
              <span className={cn('text-xl font-bold', isPositive ? 'text-success' : 'text-danger')}>
                {isPositive ? '+' : ''}{formatCurrency(totalDiff, 'TRY')}
              </span>
              <Badge variant={isPositive ? 'success' : 'danger'} className="flex items-center gap-1">
                {isPositive ? <TrendingUp className="h-3 w-3" /> : <TrendingDown className="h-3 w-3" />}
                {formatPercent(totalDiffPercent)}
              </Badge>
            </div>
          </div>
        </div>

        {/* Insight Callout */}
        <div className={cn(
          'flex items-center justify-between rounded-lg p-3 text-sm font-medium border',
          isPositive
            ? 'border-success/20 bg-success/10 text-success'
            : 'border-danger/20 bg-danger/10 text-danger'
        )}>
          <span>
            💡 Portföyünüzdeki varlıkları <strong>{PERIOD_LABELS[period].toLowerCase()}</strong> almış olsaydınız, bugünkü portföy değeriniz <strong>{formatCurrency(Math.abs(totalDiff), 'TRY')} ({formatPercent(Math.abs(totalDiffPercent))})</strong> {isPositive ? 'daha kârlı' : 'daha zararda'} olacaktı.
          </span>
          <Button
            size="sm"
            variant="ghost"
            className="h-7 text-xs flex items-center gap-1 shrink-0"
            onClick={() => setShowDetails(!showDetails)}
          >
            {showDetails ? 'Gizle' : 'Varlık Detayları'}
            {showDetails ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
          </Button>
        </div>

        {/* Item-by-item breakdown list */}
        {showDetails && (
          <div className="space-y-2 pt-2 border-t animate-in fade-in slide-in-from-top-2">
            <p className="text-xs font-semibold text-muted-foreground mb-2">Varlık Bazlı Karşılaştırma ({PERIOD_LABELS[period]} vs Bugün):</p>
            <div className="divide-y rounded-lg border bg-card">
              {calculatedItems.map((item) => {
                const itemPositive = item.difference >= 0
                return (
                  <div key={item.symbol} className="flex items-center justify-between p-3 text-xs sm:text-sm">
                    <div className="flex items-center gap-2">
                      <span className="font-bold">{item.symbol}</span>
                      <span className="text-muted-foreground hidden sm:inline">({item.quantity} Adet)</span>
                    </div>
                    <div className="flex items-center gap-4 text-right">
                      <div>
                        <p className="text-muted-foreground text-[11px]">{PERIOD_LABELS[period]} Fiyatı</p>
                        <p className="font-medium">{formatCurrency(item.historicalUnitPrice, 'TRY')}</p>
                      </div>
                      <div>
                        <p className="text-muted-foreground text-[11px]">Bugünkü Fiyat</p>
                        <p className="font-semibold">{formatCurrency(item.currentPrice, 'TRY')}</p>
                      </div>
                      <div className="min-w-[90px]">
                        <p className="text-muted-foreground text-[11px]">Fark</p>
                        <span className={cn('font-bold', itemPositive ? 'text-success' : 'text-danger')}>
                          {itemPositive ? '+' : ''}{formatPercent(item.differencePercent)}
                        </span>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
