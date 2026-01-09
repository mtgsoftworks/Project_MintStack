import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Plus, Wallet, TrendingUp, TrendingDown, MoreVertical, Trash2, Edit } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { cn, formatCurrency, formatPercent } from '@/lib/utils'
import {
  useGetPortfoliosQuery,
  useGetPortfolioSummaryQuery,
  useCreatePortfolioMutation,
  useDeletePortfolioMutation,
} from '@/store/api/portfolioApi'
import { toast } from 'sonner'
import PieChart from '@/components/charts/PieChart'

function PortfolioCardSkeleton() {
  return (
    <Card>
      <CardHeader>
        <Skeleton className="h-6 w-32" />
        <Skeleton className="h-4 w-48" />
      </CardHeader>
      <CardContent>
        <Skeleton className="h-10 w-24 mb-2" />
        <Skeleton className="h-4 w-16" />
      </CardContent>
    </Card>
  )
}

function CreatePortfolioDialog({ open, onOpenChange }) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [createPortfolio, { isLoading }] = useCreatePortfolioMutation()

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      await createPortfolio({ name, description }).unwrap()
      toast.success('Portföy oluşturuldu')
      onOpenChange(false)
      setName('')
      setDescription('')
    } catch (error) {
      toast.error('Portföy oluşturulamadı')
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Yeni Portföy</DialogTitle>
          <DialogDescription>
            Yeni bir portföy oluşturun ve varlıklarınızı takip edin.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit}>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="name">Portföy Adı</Label>
              <Input
                id="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Ana Portföy"
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">Açıklama (Opsiyonel)</Label>
              <Input
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Uzun vadeli yatırımlar"
              />
            </div>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              İptal
            </Button>
            <Button type="submit" disabled={isLoading || !name}>
              {isLoading ? 'Oluşturuluyor...' : 'Oluştur'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export default function PortfolioPage() {
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const { data: portfolios, isLoading } = useGetPortfoliosQuery()
  const { data: summary } = useGetPortfolioSummaryQuery()
  const [deletePortfolio] = useDeletePortfolioMutation()

  const handleDelete = async (id) => {
    if (window.confirm('Bu portföyü silmek istediğinizden emin misiniz?')) {
      try {
        await deletePortfolio(id).unwrap()
        toast.success('Portföy silindi')
      } catch (error) {
        toast.error('Portföy silinemedi')
      }
    }
  }

  return (
    <div className="space-y-6 animate-in">
      {/* Page Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Portföyler</h1>
          <p className="text-muted-foreground">
            Yatırım portföylerinizi yönetin
          </p>
        </div>
        <Button onClick={() => setCreateDialogOpen(true)}>
          <Plus className="mr-2 h-4 w-4" />
          Yeni Portföy
        </Button>
      </div>

      {/* Summary Cards */}
      {summary && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Card className="card-hover">
            <CardContent className="p-6">
              <div className="flex items-center justify-between mb-4">
                <span className="text-sm font-medium text-muted-foreground">Toplam Değer</span>
                <div className="p-2 rounded-lg bg-primary/10">
                  <Wallet className="h-5 w-5 text-primary" />
                </div>
              </div>
              <div className="stat-value">{formatCurrency(summary.totalValue, 'TRY')}</div>
            </CardContent>
          </Card>
          <Card className="card-hover">
            <CardContent className="p-6">
              <div className="flex items-center justify-between mb-4">
                <span className="text-sm font-medium text-muted-foreground">Toplam Maliyet</span>
                <div className="p-2 rounded-lg bg-muted">
                  <Wallet className="h-5 w-5 text-muted-foreground" />
                </div>
              </div>
              <div className="stat-value">{formatCurrency(summary.totalCost, 'TRY')}</div>
            </CardContent>
          </Card>
          <Card className="card-hover">
            <CardContent className="p-6">
              <div className="flex items-center justify-between mb-4">
                <span className="text-sm font-medium text-muted-foreground">Toplam K/Z</span>
                <div className={cn(
                  "p-2 rounded-lg",
                  summary.totalProfitLoss >= 0 ? "bg-success/10" : "bg-danger/10"
                )}>
                  {summary.totalProfitLoss >= 0 ? (
                    <TrendingUp className="h-5 w-5 text-success" />
                  ) : (
                    <TrendingDown className="h-5 w-5 text-danger" />
                  )}
                </div>
              </div>
              <div className={cn(
                "stat-value",
                summary.totalProfitLoss >= 0 ? "text-success" : "text-danger"
              )}>
                {formatCurrency(summary.totalProfitLoss, 'TRY')}
              </div>
              <Badge variant={summary.totalProfitLossPercent >= 0 ? 'success' : 'danger'} className="mt-2">
                {formatPercent(summary.totalProfitLossPercent)}
              </Badge>
            </CardContent>
          </Card>
          <Card className="card-hover">
            <CardContent className="p-6">
              <div className="flex items-center justify-between mb-4">
                <span className="text-sm font-medium text-muted-foreground">Portföy Sayısı</span>
                <div className="p-2 rounded-lg bg-info/10">
                  <Wallet className="h-5 w-5 text-info" />
                </div>
              </div>
              <div className="stat-value">{portfolios?.length || 0}</div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Portfolio Grid */}
      {isLoading ? (
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <PortfolioCardSkeleton key={i} />
          ))}
        </div>
      ) : portfolios?.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Wallet className="h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-medium mb-2">Henüz portföyünüz yok</h3>
            <p className="text-muted-foreground text-center mb-4">
              Yatırımlarınızı takip etmek için ilk portföyünüzü oluşturun.
            </p>
            <Button onClick={() => setCreateDialogOpen(true)}>
              <Plus className="mr-2 h-4 w-4" />
              Portföy Oluştur
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {portfolios?.map((portfolio) => (
            <Card key={portfolio.id} className="card-hover">
              <CardHeader className="pb-2">
                <div className="flex items-start justify-between">
                  <div>
                    <CardTitle className="text-lg">{portfolio.name}</CardTitle>
                    {portfolio.description && (
                      <CardDescription>{portfolio.description}</CardDescription>
                    )}
                  </div>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon-sm">
                        <MoreVertical className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem asChild>
                        <Link to={`/portfolio/${portfolio.id}`}>
                          <Edit className="mr-2 h-4 w-4" />
                          Düzenle
                        </Link>
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        className="text-danger"
                        onClick={() => handleDelete(portfolio.id)}
                      >
                        <Trash2 className="mr-2 h-4 w-4" />
                        Sil
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </CardHeader>
              <CardContent>
                <Link to={`/portfolio/${portfolio.id}`} className="block">
                  <div className="text-2xl font-bold mb-1">
                    {formatCurrency(portfolio.totalValue || 0, 'TRY')}
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={portfolio.profitLossPercent >= 0 ? 'success' : 'danger'}>
                      {formatPercent(portfolio.profitLossPercent || 0)}
                    </Badge>
                    <span className={cn(
                      "text-sm font-medium",
                      portfolio.profitLoss >= 0 ? "text-success" : "text-danger"
                    )}>
                      {formatCurrency(portfolio.profitLoss || 0, 'TRY')}
                    </span>
                  </div>
                  <p className="text-xs text-muted-foreground mt-2">
                    {portfolio.itemCount || 0} varlık
                  </p>
                </Link>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Create Portfolio Dialog */}
      <CreatePortfolioDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
      />
    </div>
  )
}
