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

function getOrderTypeLabel(orderType) {
    if (orderType === 'LIMIT') return 'limit'
    if (orderType === 'STOP') return 'stop'
    return 'market'
}

function getOrderStatusLabel(orderStatus) {
    if (orderStatus === 'PENDING') return 'pending'
    if (orderStatus === 'PARTIALLY_FILLED') return 'partiallyFilled'
    if (orderStatus === 'FILLED') return 'filled'
    if (orderStatus === 'CANCELED') return 'canceled'
    if (orderStatus === 'REJECTED') return 'rejected'
    return 'unknown'
}

function getStatusVariant(orderStatus) {
    if (orderStatus === 'FILLED') return 'success'
    if (orderStatus === 'PARTIALLY_FILLED') return 'warning'
    if (orderStatus === 'PENDING') return 'secondary'
    if (orderStatus === 'CANCELED') return 'danger'
    if (orderStatus === 'REJECTED') return 'destructive'
    return 'secondary'
}

const ORDER_STATUS_OPTIONS = [
    { value: 'ALL', labelKey: 'portfolioDetailPage.transactions.statusFilters.all' },
    { value: 'PENDING', labelKey: 'portfolioDetailPage.transactions.statusFilters.pending' },
    { value: 'PARTIALLY_FILLED', labelKey: 'portfolioDetailPage.transactions.statusFilters.partiallyFilled' },
    { value: 'FILLED', labelKey: 'portfolioDetailPage.transactions.statusFilters.filled' },
    { value: 'CANCELED', labelKey: 'portfolioDetailPage.transactions.statusFilters.canceled' },
    { value: 'REJECTED', labelKey: 'portfolioDetailPage.transactions.statusFilters.rejected' },
]

export function PortfolioTransactionsCard({
    t,
    transactions,
    totalTransactions,
    transactionsLoading,
    transactionsFetching,
    transactionPages,
    transactionsPage,
    orderStatus,
    onChangeOrderStatus,
    onCancelOrder,
    onPrevPage,
    onNextPage
}) {
    return (
        <Card>
            <CardHeader>
                <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                    <div>
                        <CardTitle>{t('portfolioDetailPage.transactions.title')}</CardTitle>
                        <CardDescription>
                            {t('portfolioDetailPage.transactions.count', { count: totalTransactions })}
                        </CardDescription>
                    </div>
                    <div className="flex flex-wrap gap-2">
                        {ORDER_STATUS_OPTIONS.map((option) => (
                            <Button
                                key={option.value}
                                type="button"
                                size="sm"
                                variant={orderStatus === option.value ? 'default' : 'outline'}
                                onClick={() => onChangeOrderStatus(option.value)}
                            >
                                {t(option.labelKey)}
                            </Button>
                        ))}
                    </div>
                </div>
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
                                    <TableHead>{t('portfolioDetailPage.transactions.headers.order')}</TableHead>
                                    <TableHead>{t('portfolioDetailPage.transactions.headers.status')}</TableHead>
                                    <TableHead>{t('portfolioDetailPage.transactions.headers.symbol')}</TableHead>
                                    <TableHead className="text-right">{t('portfolioDetailPage.transactions.headers.quantity')}</TableHead>
                                    <TableHead className="text-right">{t('portfolioDetailPage.transactions.headers.filled')}</TableHead>
                                    <TableHead className="text-right">{t('portfolioDetailPage.transactions.headers.price')}</TableHead>
                                    <TableHead className="text-right">{t('portfolioDetailPage.transactions.headers.gross')}</TableHead>
                                    <TableHead className="text-right">{t('portfolioDetailPage.transactions.headers.commission')}</TableHead>
                                    <TableHead className="text-right">{t('portfolioDetailPage.transactions.headers.net')}</TableHead>
                                    <TableHead>{t('portfolioDetailPage.transactions.headers.action')}</TableHead>
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
                                                <Badge variant="secondary">
                                                    {t(`portfolioDetailPage.transactions.orderTypes.${getOrderTypeLabel(transaction.orderType)}`)}
                                                </Badge>
                                            </TableCell>
                                            <TableCell>
                                                <Badge variant={getStatusVariant(transaction.orderStatus)}>
                                                    {t(`portfolioDetailPage.transactions.statuses.${getOrderStatusLabel(transaction.orderStatus)}`)}
                                                </Badge>
                                            </TableCell>
                                            <TableCell>
                                                <div>
                                                    <p className="font-semibold">{transaction.instrumentSymbol}</p>
                                                    <p className="text-xs text-muted-foreground">{transaction.instrumentName}</p>
                                                </div>
                                            </TableCell>
                                            <TableCell className="text-right">{formatNumber(transaction.quantity)}</TableCell>
                                            <TableCell className="text-right">{formatNumber(transaction.filledQuantity ?? 0)}</TableCell>
                                            <TableCell className="text-right">{formatCurrency(transaction.averageFillPrice ?? transaction.price, 'TRY')}</TableCell>
                                            <TableCell className="text-right">{formatCurrency(transaction.grossTotal ?? transaction.total, 'TRY')}</TableCell>
                                            <TableCell className="text-right text-muted-foreground">{formatCurrency(transaction.commissionAmount ?? 0, 'TRY')}</TableCell>
                                            <TableCell className="text-right font-semibold">{formatCurrency(transaction.netTotal ?? transaction.total, 'TRY')}</TableCell>
                                            <TableCell>
                                                {(transaction.orderStatus === 'PENDING' || transaction.orderStatus === 'PARTIALLY_FILLED') ? (
                                                    <Button
                                                        type="button"
                                                        size="sm"
                                                        variant="outline"
                                                        onClick={() => onCancelOrder(transaction.id)}
                                                    >
                                                        {t('common.cancel')}
                                                    </Button>
                                                ) : (
                                                    <span className="text-xs text-muted-foreground">-</span>
                                                )}
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
