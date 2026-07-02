import React, { useState } from 'react'
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
} from '@/components/ui/dialog'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { cn, formatCurrency, formatPercent } from '@/lib/utils'
import { useTranslation } from 'react-i18next'
import {
  useGetPortfoliosQuery,
  useGetPortfolioSummaryQuery,
  useCreatePortfolioMutation,
  useDeletePortfolioMutation,
} from '@/store/api/portfolioApi'
import { toast } from 'sonner'

interface Portfolio {
  id: string | number
  name: string
  description?: string
  cashBalance?: number
  netAssetValue?: number
  profitLoss?: number
  profitLossPercent?: number
  itemCount?: number
}

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

interface CreatePortfolioDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

function CreatePortfolioDialog({ open, onOpenChange }: CreatePortfolioDialogProps) {
  const { t } = useTranslation()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [initialCashBalance, setInitialCashBalance] = useState('100000')
  const [commissionRate, setCommissionRate] = useState('0.001')
  const [createPortfolio, { isLoading }] = useCreatePortfolioMutation()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const parsedInitialCash = Number.parseFloat(initialCashBalance)
    const parsedCommissionRate = Number.parseFloat(commissionRate)

    if (!Number.isFinite(parsedInitialCash) || parsedInitialCash < 0) {
      toast.error('Baslangic nakit bakiyesi gecersiz')
      return
    }

    if (!Number.isFinite(parsedCommissionRate) || parsedCommissionRate < 0 || parsedCommissionRate > 0.1) {
      toast.error('Komisyon orani 0 ile 0.1 arasinda olmali')
      return
    }

    try {
      await createPortfolio({
        name,
        description,
        initialCashBalance: parsedInitialCash,
        commissionRate: parsedCommissionRate,
      }).unwrap()
      toast.success(t('portfolioPage.toast.createSuccess'))
      onOpenChange(false)
      setName('')
      setDescription('')
      setInitialCashBalance('100000')
      setCommissionRate('0.001')
    } catch (_error) {
      toast.error(t('portfolioPage.toast.createError'))
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('portfolioPage.dialog.title')}</DialogTitle>
          <DialogDescription>
            {t('portfolioPage.dialog.description')}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit}>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="name">{t('portfolioPage.dialog.nameLabel')}</Label>
              <Input
                id="name"
                value={name}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setName(e.target.value)}
                placeholder={t('portfolioPage.dialog.namePlaceholder')}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">{t('portfolioPage.dialog.descriptionLabel')}</Label>
              <Input
                id="description"
                value={description}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setDescription(e.target.value)}
                placeholder={t('portfolioPage.dialog.descriptionPlaceholder')}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="initial-cash">Baslangic Nakit (TRY)</Label>
              <Input
                id="initial-cash"
                type="number"
                min="0"
                step="0.01"
                value={initialCashBalance}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setInitialCashBalance(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="commission-rate">Komisyon Orani (ondalik)</Label>
              <Input
                id="commission-rate"
                type="number"
                min="0"
                max="0.1"
                step="0.0001"
                value={commissionRate}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setCommissionRate(e.target.value)}
                required
              />
              <p className="text-xs text-muted-foreground">Ornek: 0.001 = %0.1</p>
            </div>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              {t('common.cancel')}
            </Button>
            <Button type="submit" disabled={isLoading || !name}>
              {isLoading ? t('portfolioPage.dialog.creating') : t('portfolioPage.dialog.create')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export default function PortfolioPage() {
  const { t } = useTranslation()
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const { data: portfolios = [], isLoading } = useGetPortfoliosQuery(undefined)
  const { data: summary } = useGetPortfolioSummaryQuery(undefined)
  const [deletePortfolio] = useDeletePortfolioMutation()

  const handleDelete = async (id: string | number) => {
    if (window.confirm(t('portfolioPage.toast.deleteConfirm'))) {
      try {
        await deletePortfolio(id).unwrap()
        toast.success(t('portfolioPage.toast.deleteSuccess'))
      } catch (_error) {
        toast.error(t('portfolioPage.toast.deleteError'))
      }
    }
  }

  return (
    <div className="space-y-6 animate-in">
      {/* Page Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t('portfolioPage.title')}</h1>
          <p className="text-muted-foreground">
            {t('portfolioPage.subtitle')}
          </p>
        </div>
        <Button onClick={() => setCreateDialogOpen(true)}>
          <Plus className="mr-2 h-4 w-4" />
          {t('portfolioPage.new')}
        </Button>
      </div>

      {/* Summary Cards */}
      {summary && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <Card className="card-hover">
            <CardContent className="p-6">
              <div className="flex items-center justify-between mb-4">
                <span className="text-sm font-medium text-muted-foreground">{t('portfolioPage.summary.totalValue')}</span>
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
                <span className="text-sm font-medium text-muted-foreground">Toplam Nakit</span>
                <div className="p-2 rounded-lg bg-info/10">
                  <Wallet className="h-5 w-5 text-info" />
                </div>
              </div>
              <div className="stat-value">{formatCurrency(summary.totalCashBalance || 0, 'TRY')}</div>
            </CardContent>
          </Card>
          <Card className="card-hover">
            <CardContent className="p-6">
              <div className="flex items-center justify-between mb-4">
                <span className="text-sm font-medium text-muted-foreground">{t('portfolioPage.summary.netAsset')}</span>
                <div className="p-2 rounded-lg bg-muted">
                  <Wallet className="h-5 w-5 text-muted-foreground" />
                </div>
              </div>
              <div className="stat-value">{formatCurrency(summary.totalNetAssetValue || 0, 'TRY')}</div>
            </CardContent>
          </Card>
          <Card className="card-hover">
            <CardContent className="p-6">
              <div className="flex items-center justify-between mb-4">
                <span className="text-sm font-medium text-muted-foreground">{t('portfolioPage.summary.totalProfitLoss')}</span>
                <div className={cn(
                  'p-2 rounded-lg',
                  summary.totalProfitLoss >= 0 ? 'bg-success/10' : 'bg-danger/10'
                )}>
                  {summary.totalProfitLoss >= 0 ? (
                    <TrendingUp className="h-5 w-5 text-success" />
                  ) : (
                    <TrendingDown className="h-5 w-5 text-danger" />
                  )}
                </div>
              </div>
              <div className={cn(
                'stat-value',
                summary.totalProfitLoss >= 0 ? 'text-success' : 'text-danger'
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
                <span className="text-sm font-medium text-muted-foreground">{t('portfolioPage.summary.portfolioCount')}</span>
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
            <h3 className="text-lg font-medium mb-2">{t('portfolioPage.empty.title')}</h3>
            <p className="text-muted-foreground text-center mb-4">
              {t('portfolioPage.empty.description')}
            </p>
            <Button onClick={() => setCreateDialogOpen(true)}>
              <Plus className="mr-2 h-4 w-4" />
              {t('portfolioPage.empty.cta')}
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {portfolios.map((portfolio: Portfolio) => (
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
                          {t('portfolioPage.actions.edit')}
                        </Link>
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        className="text-danger"
                        onClick={() => handleDelete(portfolio.id)}
                      >
                        <Trash2 className="mr-2 h-4 w-4" />
                        {t('portfolioPage.actions.delete')}
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </CardHeader>
              <CardContent>
                <Link to={`/portfolio/${portfolio.id}`} className="block">
                  <div className="text-2xl font-bold mb-1">
                    {formatCurrency(portfolio.netAssetValue || 0, 'TRY')}
                  </div>
                  <div className="text-xs text-muted-foreground mb-2">
                    Nakit: {formatCurrency(portfolio.cashBalance || 0, 'TRY')}
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={(portfolio.profitLossPercent ?? 0) >= 0 ? 'success' : 'danger'}>
                      {formatPercent(portfolio.profitLossPercent ?? 0)}
                    </Badge>
                    <span className={cn(
                      'text-sm font-medium',
                      (portfolio.profitLoss ?? 0) >= 0 ? 'text-success' : 'text-danger'
                    )}>
                      {formatCurrency(portfolio.profitLoss ?? 0, 'TRY')}
                    </span>
                  </div>
                  <p className="text-xs text-muted-foreground mt-2">
                    {t('portfolioPage.card.items', { count: portfolio.itemCount || 0 })}
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
