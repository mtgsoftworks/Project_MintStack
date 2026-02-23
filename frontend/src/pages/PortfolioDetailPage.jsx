import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { ArrowLeft } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import {
    useGetPortfolioQuery,
    useRemovePortfolioItemMutation
} from '@/store/api/portfolioApi'
import { selectToken } from '@/store/slices/authSlice'
import { toast } from 'sonner'
import { AddItemDialog } from '@/pages/portfolio-detail/AddItemDialog'
import { PortfolioHeaderActions } from '@/pages/portfolio-detail/PortfolioHeaderActions'
import { PortfolioSummarySection } from '@/pages/portfolio-detail/PortfolioSummarySection'
import { PortfolioItemsTable } from '@/pages/portfolio-detail/PortfolioItemsTable'
import { PortfolioTransactionsCard } from '@/pages/portfolio-detail/PortfolioTransactionsCard'
import { usePortfolioTransactions } from '@/pages/portfolio-detail/hooks/usePortfolioTransactions'
import { usePortfolioExport } from '@/pages/portfolio-detail/hooks/usePortfolioExport'

export default function PortfolioDetailPage() {
    const { t } = useTranslation()
    const { id } = useParams()
    const token = useSelector(selectToken)
    const [addDialogOpen, setAddDialogOpen] = useState(false)

    const { data: portfolio, isLoading, error } = useGetPortfolioQuery(id)
    const [removeItem] = useRemovePortfolioItemMutation()

    const {
        transactionsPage,
        setTransactionsPage,
        transactionsLoading,
        transactionsFetching,
        transactions,
        transactionPages,
        totalTransactions
    } = usePortfolioTransactions(id)

    const {
        isExporting,
        handleExportExcel,
        handleExportPdf
    } = usePortfolioExport({
        portfolioId: id,
        token,
        t
    })

    const handleRemoveItem = async (itemId) => {
        if (!window.confirm(t('portfolioDetailPage.confirmDelete'))) {
            return
        }

        try {
            await removeItem({ portfolioId: id, itemId }).unwrap()
            toast.success(t('portfolioDetailPage.toast.deleteSuccess'))
        } catch {
            toast.error(t('portfolioDetailPage.toast.deleteError'))
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
        value: item.currentValue || 0
    }))

    return (
        <div className="space-y-6 animate-in">
            <Button variant="ghost" asChild>
                <Link to="/portfolio">
                    <ArrowLeft className="mr-2 h-4 w-4" />
                    {t('portfolioDetailPage.backToPortfolios')}
                </Link>
            </Button>

            <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                <div>
                    <h1 className="text-2xl font-bold">{portfolio.name}</h1>
                    {portfolio.description && (
                        <p className="text-muted-foreground">{portfolio.description}</p>
                    )}
                </div>
                <PortfolioHeaderActions
                    t={t}
                    isExporting={isExporting}
                    onExportExcel={handleExportExcel}
                    onExportPdf={handleExportPdf}
                    onOpenAddDialog={() => setAddDialogOpen(true)}
                />
            </div>

            <PortfolioSummarySection
                t={t}
                portfolio={portfolio}
                items={items}
                chartData={chartData}
            />

            <PortfolioItemsTable
                t={t}
                items={items}
                onOpenAddDialog={() => setAddDialogOpen(true)}
                onRemoveItem={handleRemoveItem}
            />

            <PortfolioTransactionsCard
                t={t}
                transactions={transactions}
                totalTransactions={totalTransactions}
                transactionsLoading={transactionsLoading}
                transactionsFetching={transactionsFetching}
                transactionPages={transactionPages}
                transactionsPage={transactionsPage}
                onPrevPage={() => setTransactionsPage((page) => Math.max(0, page - 1))}
                onNextPage={() => setTransactionsPage((page) => page + 1)}
            />

            <AddItemDialog
                portfolioId={id}
                open={addDialogOpen}
                onOpenChange={setAddDialogOpen}
            />
        </div>
    )
}
