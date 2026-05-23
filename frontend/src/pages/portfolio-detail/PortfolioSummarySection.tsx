import PieChart from '@/components/charts/PieChart'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { cn, formatCurrency, formatPercent } from '@/lib/utils'

export function PortfolioSummarySection({ t, portfolio, items, chartData }) {
    return (
        <div className="grid gap-6 lg:grid-cols-3">
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
                        <span className="text-muted-foreground">Nakit Bakiye</span>
                        <span className="font-semibold">{formatCurrency(portfolio.cashBalance || 0, 'TRY')}</span>
                    </div>
                    <div className="flex justify-between py-2 border-b">
                        <span className="text-muted-foreground">{t('portfolioDetailPage.summary.netAssetValue')}</span>
                        <span className="font-semibold">{formatCurrency(portfolio.netAssetValue || 0, 'TRY')}</span>
                    </div>
                    <div className="flex justify-between py-2 border-b">
                        <span className="text-muted-foreground">{t('portfolioDetailPage.summary.totalCost')}</span>
                        <span className="font-medium">{formatCurrency(portfolio.totalCost || 0, 'TRY')}</span>
                    </div>
                    <div className="flex justify-between py-2 border-b">
                        <span className="text-muted-foreground">{t('portfolioDetailPage.summary.profitLoss')}</span>
                        <span className={cn(
                            'font-semibold',
                            portfolio.profitLoss >= 0 ? 'text-success' : 'text-danger'
                        )}>
                            {formatCurrency(portfolio.profitLoss || 0, 'TRY')}
                        </span>
                    </div>
                    <div className="flex justify-between py-2 border-b">
                        <span className="text-muted-foreground">Gerceklesmis Kar/Zarar</span>
                        <span className={cn(
                            'font-semibold',
                            (portfolio.realizedProfitLoss || 0) >= 0 ? 'text-success' : 'text-danger'
                        )}>
                            {formatCurrency(portfolio.realizedProfitLoss || 0, 'TRY')}
                        </span>
                    </div>
                    <div className="flex justify-between py-2 border-b">
                        <span className="text-muted-foreground">Gerceklesmemis Kar/Zarar</span>
                        <span className={cn(
                            'font-semibold',
                            (portfolio.unrealizedProfitLoss || 0) >= 0 ? 'text-success' : 'text-danger'
                        )}>
                            {formatCurrency(portfolio.unrealizedProfitLoss || 0, 'TRY')}
                        </span>
                    </div>
                    <div className="flex justify-between py-2 border-b">
                        <span className="text-muted-foreground">Komisyon Orani</span>
                        <span className="font-medium">%{(((portfolio.commissionRate || 0) * 100)).toFixed(2)}</span>
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
    )
}
