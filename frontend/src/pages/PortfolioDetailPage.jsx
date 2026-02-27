import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { ArrowLeft } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import {
    useCancelPortfolioOrderMutation,
    useGetPortfolioQuery,
    useProcessPortfolioOrdersMutation
} from '@/store/api/portfolioApi'
import { selectToken } from '@/store/slices/authSlice'
import { PortfolioTradeDialog } from '@/pages/portfolio-detail/PortfolioTradeDialog'
import { PortfolioCashDialog } from '@/pages/portfolio-detail/PortfolioCashDialog'
import { PortfolioHeaderActions } from '@/pages/portfolio-detail/PortfolioHeaderActions'
import { PortfolioSummarySection } from '@/pages/portfolio-detail/PortfolioSummarySection'
import { PortfolioItemsTable } from '@/pages/portfolio-detail/PortfolioItemsTable'
import { PortfolioTransactionsCard } from '@/pages/portfolio-detail/PortfolioTransactionsCard'
import { usePortfolioTransactions } from '@/pages/portfolio-detail/hooks/usePortfolioTransactions'
import { usePortfolioExport } from '@/pages/portfolio-detail/hooks/usePortfolioExport'
import { toast } from 'sonner'

export default function PortfolioDetailPage() {
    const { t } = useTranslation()
    const { id } = useParams()
    const token = useSelector(selectToken)
    const [buyDialogOpen, setBuyDialogOpen] = useState(false)
    const [sellDialogOpen, setSellDialogOpen] = useState(false)
    const [cashDialogOpen, setCashDialogOpen] = useState(false)
    const [sellSymbol, setSellSymbol] = useState('')
    const [sellInstrumentId, setSellInstrumentId] = useState(null)
    const [sellMaxQuantity, setSellMaxQuantity] = useState(null)
    const [processOrders, { isLoading: isProcessingOrders }] = useProcessPortfolioOrdersMutation()
    const [cancelOrder] = useCancelPortfolioOrderMutation()

    const { data: portfolio, isLoading, error } = useGetPortfolioQuery(id)

    const {
        transactionsPage,
        setTransactionsPage,
        orderStatus,
        setOrderStatus,
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

    const handleOpenSellDialog = (item) => {
        const quantity = Number.parseFloat(item.quantity)
        setSellSymbol(item.instrumentSymbol || item.symbol || '')
        setSellInstrumentId(item.instrumentId || null)
        setSellMaxQuantity(Number.isFinite(quantity) ? quantity : null)
        setSellDialogOpen(true)
    }

    const handleProcessOrders = async () => {
        try {
            await processOrders({ portfolioId: id }).unwrap()
            toast.success('Bekleyen emirler islendi')
        } catch {
            toast.error('Bekleyen emirler islenemedi')
        }
    }

    const handleCancelOrder = async (orderId) => {
        try {
            await cancelOrder({ portfolioId: id, orderId }).unwrap()
            toast.success('Emir iptal edildi')
        } catch {
            toast.error('Emir iptal edilemedi')
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
    const chartDataBySymbol = items.reduce((accumulator, item) => {
        const symbol = item.instrumentSymbol || item.symbol || item.instrumentName || item.name || '-'
        const numericValue = Number(item.currentValue ?? 0)
        const value = Number.isFinite(numericValue) ? numericValue : 0
        accumulator.set(symbol, (accumulator.get(symbol) || 0) + value)
        return accumulator
    }, new Map())
    const chartData = Array.from(chartDataBySymbol.entries()).map(([name, value]) => ({
        name,
        value,
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
                    isProcessingOrders={isProcessingOrders}
                    onProcessOrders={handleProcessOrders}
                    onOpenCashDialog={() => setCashDialogOpen(true)}
                    onExportExcel={handleExportExcel}
                    onExportPdf={handleExportPdf}
                    onOpenAddDialog={() => setBuyDialogOpen(true)}
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
                onOpenAddDialog={() => setBuyDialogOpen(true)}
                onSellItem={handleOpenSellDialog}
            />

            <PortfolioTransactionsCard
                t={t}
                transactions={transactions}
                totalTransactions={totalTransactions}
                transactionsLoading={transactionsLoading}
                transactionsFetching={transactionsFetching}
                transactionPages={transactionPages}
                transactionsPage={transactionsPage}
                orderStatus={orderStatus}
                onChangeOrderStatus={setOrderStatus}
                onCancelOrder={handleCancelOrder}
                onPrevPage={() => setTransactionsPage((page) => Math.max(0, page - 1))}
                onNextPage={() => setTransactionsPage((page) => page + 1)}
            />

            <PortfolioTradeDialog
                portfolioId={id}
                mode="BUY"
                open={buyDialogOpen}
                onOpenChange={setBuyDialogOpen}
            />

            <PortfolioTradeDialog
                portfolioId={id}
                mode="SELL"
                open={sellDialogOpen}
                onOpenChange={setSellDialogOpen}
                defaultSymbol={sellSymbol}
                defaultInstrumentId={sellInstrumentId}
                maxQuantity={sellMaxQuantity}
            />

            <PortfolioCashDialog
                portfolioId={id}
                open={cashDialogOpen}
                onOpenChange={setCashDialogOpen}
                currentCashBalance={portfolio.cashBalance}
            />
        </div>
    )
}
