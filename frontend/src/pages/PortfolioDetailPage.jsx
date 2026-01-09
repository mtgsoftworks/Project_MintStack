import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { ArrowLeft, Plus, Trash2, Edit, TrendingUp, TrendingDown } from 'lucide-react'
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
import { cn, formatCurrency, formatPercent, formatNumber } from '@/lib/utils'
import { 
  useGetPortfolioQuery,
  useAddPortfolioItemMutation,
  useRemovePortfolioItemMutation,
} from '@/store/api/portfolioApi'
import { toast } from 'sonner'
import PieChart from '@/components/charts/PieChart'

function AddItemDialog({ portfolioId, open, onOpenChange }) {
  const [instrumentId, setInstrumentId] = useState('')
  const [quantity, setQuantity] = useState('')
  const [purchasePrice, setPurchasePrice] = useState('')
  const [addItem, { isLoading }] = useAddPortfolioItemMutation()

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      await addItem({
        portfolioId,
        instrumentId,
        quantity: parseFloat(quantity),
        purchasePrice: parseFloat(purchasePrice),
      }).unwrap()
      toast.success('Varlık eklendi')
      onOpenChange(false)
      setInstrumentId('')
      setQuantity('')
      setPurchasePrice('')
    } catch (error) {
      toast.error('Varlık eklenemedi')
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Varlık Ekle</DialogTitle>
          <DialogDescription>
            Portföyünüze yeni bir varlık ekleyin.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit}>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="symbol">Sembol</Label>
              <Input
                id="symbol"
                value={instrumentId}
                onChange={(e) => setInstrumentId(e.target.value)}
                placeholder="THYAO"
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="quantity">Miktar</Label>
              <Input
                id="quantity"
                type="number"
                step="0.01"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                placeholder="100"
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="price">Alış Fiyatı (TL)</Label>
              <Input
                id="price"
                type="number"
                step="0.01"
                value={purchasePrice}
                onChange={(e) => setPurchasePrice(e.target.value)}
                placeholder="150.00"
                required
              />
            </div>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              İptal
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? 'Ekleniyor...' : 'Ekle'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export default function PortfolioDetailPage() {
  const { id } = useParams()
  const [addDialogOpen, setAddDialogOpen] = useState(false)
  const { data: portfolio, isLoading, error } = useGetPortfolioQuery(id)
  const [removeItem] = useRemovePortfolioItemMutation()

  const handleRemoveItem = async (itemId) => {
    if (window.confirm('Bu varlığı silmek istediğinizden emin misiniz?')) {
      try {
        await removeItem({ portfolioId: id, itemId }).unwrap()
        toast.success('Varlık silindi')
      } catch (error) {
        toast.error('Varlık silinemedi')
      }
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
          <p className="text-muted-foreground mb-4">Portföy bulunamadı.</p>
          <Button asChild>
            <Link to="/portfolio">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Portföylere Dön
            </Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const items = portfolio.items || []
  const chartData = items.map(item => ({
    name: item.symbol,
    value: item.currentValue || 0,
  }))

  return (
    <div className="space-y-6 animate-in">
      {/* Back Button */}
      <Button variant="ghost" asChild>
        <Link to="/portfolio">
          <ArrowLeft className="mr-2 h-4 w-4" />
          Portföylere Dön
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
        <Button onClick={() => setAddDialogOpen(true)}>
          <Plus className="mr-2 h-4 w-4" />
          Varlık Ekle
        </Button>
      </div>

      {/* Summary and Chart */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Chart */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Portföy Dağılımı</CardTitle>
          </CardHeader>
          <CardContent>
            {items.length > 0 ? (
              <PieChart data={chartData} />
            ) : (
              <div className="flex items-center justify-center h-64 text-muted-foreground">
                Henüz varlık yok
              </div>
            )}
          </CardContent>
        </Card>

        {/* Summary */}
        <Card>
          <CardHeader>
            <CardTitle>Özet</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">Toplam Değer</span>
              <span className="font-semibold">{formatCurrency(portfolio.totalValue || 0, 'TRY')}</span>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">Toplam Maliyet</span>
              <span className="font-medium">{formatCurrency(portfolio.totalCost || 0, 'TRY')}</span>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">Kar/Zarar</span>
              <span className={cn(
                "font-semibold",
                portfolio.profitLoss >= 0 ? "text-success" : "text-danger"
              )}>
                {formatCurrency(portfolio.profitLoss || 0, 'TRY')}
              </span>
            </div>
            <div className="flex justify-between py-2">
              <span className="text-muted-foreground">K/Z Oranı</span>
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
          <CardTitle>Varlıklar</CardTitle>
          <CardDescription>{items.length} adet varlık</CardDescription>
        </CardHeader>
        <CardContent>
          {items.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-8 text-muted-foreground">
              <p className="mb-4">Bu portföyde henüz varlık yok.</p>
              <Button onClick={() => setAddDialogOpen(true)}>
                <Plus className="mr-2 h-4 w-4" />
                Varlık Ekle
              </Button>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Sembol</TableHead>
                  <TableHead className="text-right">Miktar</TableHead>
                  <TableHead className="text-right">Alış Fiyatı</TableHead>
                  <TableHead className="text-right">Güncel Fiyat</TableHead>
                  <TableHead className="text-right">Değer</TableHead>
                  <TableHead className="text-right">K/Z</TableHead>
                  <TableHead></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.map((item) => (
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
                          <p className="font-semibold">{item.symbol}</p>
                          <p className="text-xs text-muted-foreground">{item.name}</p>
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
                ))}
              </TableBody>
            </Table>
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
