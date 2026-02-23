import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow
} from '@/components/ui/table'
import { formatCurrency, formatDate, formatNumber } from '@/lib/utils'

export function PortfolioTransactionsCard({
    t,
    transactions,
    totalTransactions,
    transactionsLoading,
    transactionsFetching,
    transactionPages,
    transactionsPage,
    onPrevPage,
    onNextPage
}) {
    return (
        <Card>
            <CardHeader>
                <CardTitle>{t('portfolioDetailPage.transactions.title')}</CardTitle>
                <CardDescription>
                    {t('portfolioDetailPage.transactions.count', { count: totalTransactions })}
                </CardDescription>
            </CardHeader>
            <CardContent>
                {transactionsLoading ? (
                    <div className="space-y-3">
                        {[...Array(5)].map((_, index) => (
                            <Skeleton key={index} className="h-12 w-full" />
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
                                                    {isBuy
                                                        ? t('portfolioDetailPage.transactions.types.buy')
                                                        : t('portfolioDetailPage.transactions.types.sell')}
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
                                    onClick={onPrevPage}
                                    disabled={transactionsPage === 0 || transactionsFetching}
                                >
                                    {t('common.previous')}
                                </Button>
                                <span className="text-sm text-muted-foreground px-4">
                                    {t('portfolioDetailPage.transactions.pagination', {
                                        current: transactionsPage + 1,
                                        total: transactionPages
                                    })}
                                </span>
                                <Button
                                    variant="outline"
                                    onClick={onNextPage}
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
    )
}
