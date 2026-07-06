import { ArrowDownRight, Plus, TrendingDown, TrendingUp } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow
} from '@/components/ui/table'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { cn, formatCurrency, formatUserCurrency, formatNumber, formatPercent } from '@/lib/utils'

export function PortfolioItemsTable({ t, items, onOpenAddDialog, onSellItem }) {
    return (
        <Card>
            <CardHeader>
                <CardTitle>{t('portfolioDetailPage.items.title')}</CardTitle>
                <CardDescription>{t('portfolioDetailPage.items.count', { count: items.length })}</CardDescription>
            </CardHeader>
            <CardContent>
                {items.length === 0 ? (
                    <div className="flex flex-col items-center justify-center py-8 text-muted-foreground">
                        <p className="mb-4">{t('portfolioDetailPage.items.empty')}</p>
                        <Button onClick={onOpenAddDialog}>
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
                                <TableHead />
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
                                                    'flex h-10 w-10 items-center justify-center rounded-lg',
                                                    item.profitLossPercent >= 0 ? 'bg-success/10' : 'bg-danger/10'
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
                                        <TableCell className="text-right">{formatUserCurrency(item.purchasePrice)}</TableCell>
                                        <TableCell className="text-right">{formatUserCurrency(item.currentPrice)}</TableCell>
                                        <TableCell className="text-right font-semibold">
                                            {formatUserCurrency(item.currentValue)}
                                        </TableCell>
                                        <TableCell className="text-right">
                                            <div className="flex flex-col items-end">
                                                <span className={cn(
                                                    'font-medium',
                                                    item.profitLoss >= 0 ? 'text-success' : 'text-danger'
                                                )}>
                                                    {formatUserCurrency(item.profitLoss)}
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
                                                onClick={() => onSellItem(item)}
                                            >
                                                <ArrowDownRight className="h-4 w-4 text-danger" />
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
    )
}
