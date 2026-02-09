import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useParams, Link } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { ArrowLeft, Plus, Trash2, Edit, TrendingUp, TrendingDown, FileSpreadsheet, FileText, Download } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { cn, formatCurrency, formatPercent, formatNumber, formatDate } from '@/lib/utils'
import { 
  useGetPortfolioQuery,
  useAddPortfolioItemMutation,
  useRemovePortfolioItemMutation,
  useGetPortfolioTransactionsQuery,
  exportPortfolioToExcel,
  exportPortfolioToPdf,
} from '@/store/api/portfolioApi'
import { selectToken } from '@/store/slices/authSlice'
import { toast } from 'sonner'
import PieChart from '@/components/charts/PieChart'

function AddItemDialog({ portfolioId, open, onOpenChange }) {
  const { t } = useTranslation()
  const [instrumentSymbol, setInstrumentSymbol] = useState('')
  const [quantity, setQuantity] = useState('')
  const [purchasePrice, setPurchasePrice] = useState('')
  const [purchaseDate, setPurchaseDate] = useState(() => new Date().toISOString().split('T')[0])
  const [addItem, { isLoading }] = useAddPortfolioItemMutation()

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      await addItem({
        portfolioId,
        instrumentSymbol,
        quantity: parseFloat(quantity),
        purchasePrice: parseFloat(purchasePrice),
        purchaseDate,
      }).unwrap()
      toast.success(t('portfolioDetailPage.toast.addSuccess'))
      onOpenChange(false)
      setInstrumentSymbol('')
      setQuantity('')
      setPurchasePrice('')
      setPurchaseDate(new Date().toISOString().split('T')[0])
    } catch (error) {
      toast.error(t('portfolioDetailPage.toast.addError'))
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('portfolioDetailPage.addItemDialog.title')}</DialogTitle>
          <DialogDescription>
            {t('portfolioDetailPage.addItemDialog.description')}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit}>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="symbol">{t('portfolioDetailPage.addItemDialog.symbolLabel')}</Label>
              <Input
                id="symbol"
                value={instrumentSymbol}
                onChange={(e) => setInstrumentSymbol(e.target.value.toUpperCase())}
                placeholder={t('portfolioDetailPage.addItemDialog.symbolPlaceholder')}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="quantity">{t('portfolioDetailPage.addItemDialog.quantityLabel')}</Label>
              <Input
                id="quantity"
                type="number"
                step="0.01"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                placeholder={t('portfolioDetailPage.addItemDialog.quantityPlaceholder')}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="price">{t('portfolioDetailPage.addItemDialog.purchasePriceLabel')}</Label>
              <Input
                id="price"
                type="number"
                step="0.01"
                value={purchasePrice}
                onChange={(e) => setPurchasePrice(e.target.value)}
                placeholder={t('portfolioDetailPage.addItemDialog.purchasePricePlaceholder')}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="purchaseDate">{t('portfolioDetailPage.addItemDialog.purchaseDateLabel')}</Label>
              <Input
                id="purchaseDate"
                type="date"
                value={purchaseDate}
                onChange={(e) => setPurchaseDate(e.target.value)}
                required
              />
            </div>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              {t('common.cancel')}
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? t('portfolioDetailPage.addItemDialog.submitting') : t('portfolioDetailPage.addItemDialog.submit')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export default function PortfolioDetailPage() {
  const { t } = useTranslation()
  const { id } = useParams()
  const token = useSelector(selectToken)
  const [addDialogOpen, setAddDialogOpen] = useState(false)
  const [transactionsPage, setTransactionsPage] = useState(0)
  const [isExporting, setIsExporting] = useState(false)
  const { data: portfolio, isLoading, error } = useGetPortfolioQuery(id)
  const {
    data: transactionsData,
    isLoading: transactionsLoading,
    isFetching: transactionsFetching,
  } = useGetPortfolioTransactionsQuery({
    portfolioId: id,
    page: transactionsPage,
    size: 8,
  }, { skip: !id })
  const [removeItem] = useRemovePortfolioItemMutation()

  useEffect(() => {
    setTransactionsPage(0)
  }, [id])

  const handleRemoveItem = async (itemId) => {
    if (window.confirm(t('portfolioDetailPage.confirmDelete'))) {
      try {
        await removeItem({ portfolioId: id, itemId }).unwrap()
        toast.success(t('portfolioDetailPage.toast.deleteSuccess'))
      } catch (error) {
        toast.error(t('portfolioDetailPage.toast.deleteError'))
      }
    }
  }

  const handleExportExcel = async () => {
    if (!token) {
      toast.error(t('portfolioDetailPage.export.authRequired'))
      return
    }
    setIsExporting(true)
    try {
      await exportPortfolioToExcel(id, token)
      toast.success(t('portfolioDetailPage.export.excelSuccess'))
    } catch (error) {
      toast.error(t('portfolioDetailPage.export.excelError'))
    } finally {
      setIsExporting(false)
    }
  }

  const handleExportPdf = async () => {
    if (!token) {
      toast.error(t('portfolioDetailPage.export.authRequired'))
      return
    }
    setIsExporting(true)
    try {
      await exportPortfolioToPdf(id, token)
      toast.success(t('portfolioDetailPage.export.pdfSuccess'))
    } catch (error) {
      toast.error(t('portfolioDetailPage.export.pdfError'))
    } finally {
      setIsExporting(false)
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2">
            <Skeleton className="h-96" />
          </div>
          <Skeleton className="h-96" />
        </div>
      </div>
    )
  }

  if (error || !portfolio) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-12">
          <p className="text-muted-foreground mb-4">{t('portfolioDetailPage.notFound')}</p>
          <Button asChild>
            <Link to="/portfolio">
              <ArrowLeft className="mr-2 h-4 w-4" />
              {t('portfolioDetailPage.backToPortfolios')}
            </Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const items = portfolio.items || []
  const chartData = items.map((item) => ({
    name: item.instrumentSymbol || item.symbol || item.instrumentName || item.name || '-',
    value: item.currentValue || 0,
  }))
  const transactions = transactionsData?.data || []
  const transactionPages = transactionsData?.pagination?.totalPages || 0

  return (
    <div className="space-y-6 animate-in">
      {/* Back Button */}
      <Button variant="ghost" asChild>
        <Link to="/portfolio">
          <ArrowLeft className="mr-2 h-4 w-4" />
          {t('portfolioDetailPage.backToPortfolios')}
        </Link>
      </Button>

      {/* Portfolio Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">{portfolio.name}</h1>
          {portfolio.description && (
            <p className="text-muted-foreground">{portfolio.description}</p>
          )}
        </div>
        <div className="flex items-center gap-2">
          {/* Export Dropdown */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" disabled={isExporting}>
                <Download className="mr-2 h-4 w-4" />
                {isExporting ? t('portfolioDetailPage.export.exporting') : t('portfolioDetailPage.export.title')}
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={handleExportExcel}>
                <FileSpreadsheet className="mr-2 h-4 w-4 text-green-600" />
                {t('portfolioDetailPage.export.excel')}
              </DropdownMenuItem>
              <DropdownMenuItem onClick={handleExportPdf}>
                <FileText className="mr-2 h-4 w-4 text-red-600" />
                {t('portfolioDetailPage.export.pdf')}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          <Button onClick={() => setAddDialogOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            {t('portfolioDetailPage.addItem')}
          </Button>
        </div>
      </div>

      {/* Summary and Chart */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Chart */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>{t('portfolioDetailPage.chart.title')}</CardTitle>
          </CardHeader>
          <CardContent>
            {items.length > 0 ? (
              <PieChart data={chartData} />
            ) : (
              <div className="flex items-center justify-center h-64 text-muted-foreground">
                {t('portfolioDetailPage.chart.empty')}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Summary */}
        <Card>
          <CardHeader>
            <CardTitle>{t('portfolioDetailPage.summary.title')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">{t('portfolioDetailPage.summary.totalValue')}</span>
              <span className="font-semibold">{formatCurrency(portfolio.totalValue || 0, 'TRY')}</span>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">{t('portfolioDetailPage.summary.totalCost')}</span>
              <span className="font-medium">{formatCurrency(portfolio.totalCost || 0, 'TRY')}</span>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">{t('portfolioDetailPage.summary.profitLoss')}</span>
              <span className={cn(
                "font-semibold",
                portfolio.profitLoss >= 0 ? "text-success" : "text-danger"
              )}>
                {formatCurrency(portfolio.profitLoss || 0, 'TRY')}
              </span>
            </div>
            <div className="flex justify-between py-2">
              <span className="text-muted-foreground">{t('portfolioDetailPage.summary.profitLossRatio')}</span>
              <Badge variant={portfolio.profitLossPercent >= 0 ? 'success' : 'danger'}>
                {formatPercent(portfolio.profitLossPercent || 0)}
              </Badge>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Items Table */}
      <Card>
        <CardHeader>
          <CardTitle>{t('portfolioDetailPage.items.title')}</CardTitle>
          <CardDescription>{t('portfolioDetailPage.items.count', { count: items.length })}</CardDescription>
        </CardHeader>
        <CardContent>
          {items.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-8 text-muted-foreground">
              <p className="mb-4">{t('portfolioDetailPage.items.empty')}</p>
              <Button onClick={() => setAddDialogOpen(true)}>
                <Plus className="mr-2 h-4 w-4" />
                {t('portfolioDetailPage.addItem')}
              </Button>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('portfolioDetailPage.items.headers.symbol')}</TableHead>
                  <TableHead className="text-right">{t('portfolioDetailPage.items.headers.quantity')}</TableHead>
                  <TableHead className="text-right">{t('portfolioDetailPage.items.headers.purchasePrice')}</TableHead>
                  <TableHead className="text-right">{t('portfolioDetailPage.items.headers.currentPrice')}</TableHead>
                  <TableHead className="text-right">{t('portfolioDetailPage.items.headers.value')}</TableHead>
                  <TableHead className="text-right">{t('portfolioDetailPage.items.headers.profitLoss')}</TableHead>
                  <TableHead></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.map((item) => {
                  const displaySymbol = item.instrumentSymbol || item.symbol || '-'
                  const displayName = item.instrumentName || item.name || '-'

                  return (
                    <TableRow key={item.id}>
                    <TableCell>
                      <div className="flex items-center gap-3">
                        <div className={cn(
                          "flex h-10 w-10 items-center justify-center rounded-lg",
                          item.profitLossPercent >= 0 ? "bg-success/10" : "bg-danger/10"
                        )}>
                          {item.profitLossPercent >= 0 ? (
                            <TrendingUp className="h-5 w-5 text-success" />
                          ) : (
                            <TrendingDown className="h-5 w-5 text-danger" />
                          )}
                        </div>
                        <div>
                          <p className="font-semibold">{displaySymbol}</p>
                          <p className="text-xs text-muted-foreground">{displayName}</p>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell className="text-right">{formatNumber(item.quantity)}</TableCell>
                    <TableCell className="text-right">{formatCurrency(item.purchasePrice, 'TRY')}</TableCell>
                    <TableCell className="text-right">{formatCurrency(item.currentPrice, 'TRY')}</TableCell>
                    <TableCell className="text-right font-semibold">
                      {formatCurrency(item.currentValue, 'TRY')}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex flex-col items-end">
                        <span className={cn(
                          "font-medium",
                          item.profitLoss >= 0 ? "text-success" : "text-danger"
                        )}>
                          {formatCurrency(item.profitLoss, 'TRY')}
                        </span>
                        <Badge variant={item.profitLossPercent >= 0 ? 'success' : 'danger'} className="mt-1">
                          {formatPercent(item.profitLossPercent)}
                        </Badge>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => handleRemoveItem(item.id)}
                      >
                        <Trash2 className="h-4 w-4 text-danger" />
                      </Button>
                    </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Transaction History */}
      <Card>
        <CardHeader>
          <CardTitle>{t('portfolioDetailPage.transactions.title')}</CardTitle>
          <CardDescription>
            {t('portfolioDetailPage.transactions.count', { count: transactionsData?.pagination?.totalElements || 0 })}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {transactionsLoading ? (
            <div className="space-y-3">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : transactions.length === 0 ? (
            <div className="flex items-center justify-center py-8 text-muted-foreground">
              {t('portfolioDetailPage.transactions.empty')}
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>{t('portfolioDetailPage.transactions.headers.date')}</TableHead>
                    <TableHead>{t('portfolioDetailPage.transactions.headers.type')}</TableHead>
                    <TableHead>{t('portfolioDetailPage.transactions.headers.symbol')}</TableHead>
                    <TableHead className="text-right">{t('portfolioDetailPage.transactions.headers.quantity')}</TableHead>
                    <TableHead className="text-right">{t('portfolioDetailPage.transactions.headers.price')}</TableHead>
                    <TableHead className="text-right">{t('portfolioDetailPage.transactions.headers.total')}</TableHead>
                    <TableHead>{t('portfolioDetailPage.transactions.headers.note')}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {transactions.map((transaction) => {
                    const isBuy = transaction.transactionType === 'BUY'
                    return (
                      <TableRow key={transaction.id}>
                        <TableCell>{formatDate(transaction.transactionDate)}</TableCell>
                        <TableCell>
                          <Badge variant={isBuy ? 'success' : 'danger'}>
                            {isBuy ? t('portfolioDetailPage.transactions.types.buy') : t('portfolioDetailPage.transactions.types.sell')}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div>
                            <p className="font-semibold">{transaction.instrumentSymbol}</p>
                            <p className="text-xs text-muted-foreground">{transaction.instrumentName}</p>
                          </div>
                        </TableCell>
                        <TableCell className="text-right">
                          {formatNumber(transaction.quantity)}
                        </TableCell>
                        <TableCell className="text-right">
                          {formatCurrency(transaction.price, 'TRY')}
                        </TableCell>
                        <TableCell className="text-right font-semibold">
                          {formatCurrency(transaction.total, 'TRY')}
                        </TableCell>
                        <TableCell className="text-muted-foreground">
                          {transaction.notes || '-'}
                        </TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>

              {transactionPages > 1 && (
                <div className="flex items-center justify-center gap-2 mt-6">
                  <Button
                    variant="outline"
                    onClick={() => setTransactionsPage((p) => Math.max(0, p - 1))}
                    disabled={transactionsPage === 0 || transactionsFetching}
                  >
                    {t('common.previous')}
                  </Button>
                  <span className="text-sm text-muted-foreground px-4">
                    {t('portfolioDetailPage.transactions.pagination', { current: transactionsPage + 1, total: transactionPages })}
                  </span>
                  <Button
                    variant="outline"
                    onClick={() => setTransactionsPage((p) => p + 1)}
                    disabled={transactionsPage >= transactionPages - 1 || transactionsFetching}
                  >
                    {t('common.next')}
                  </Button>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>

      {/* Add Item Dialog */}
      <AddItemDialog
        portfolioId={id}
        open={addDialogOpen}
        onOpenChange={setAddDialogOpen}
      />
    </div>
  )
}
