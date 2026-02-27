import { Download, FileSpreadsheet, FileText, Plus, Wallet, PlayCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger
} from '@/components/ui/dropdown-menu'

export function PortfolioHeaderActions({
    t,
    isExporting,
    isProcessingOrders,
    onProcessOrders,
    onOpenCashDialog,
    onExportExcel,
    onExportPdf,
    onOpenAddDialog
}) {
    return (
        <div className="flex items-center gap-2">
            <Button variant="outline" onClick={onOpenCashDialog}>
                <Wallet className="mr-2 h-4 w-4" />
                Nakit
            </Button>

            <Button variant="outline" onClick={onProcessOrders} disabled={isProcessingOrders}>
                <PlayCircle className="mr-2 h-4 w-4" />
                {isProcessingOrders ? 'Isleniyor...' : 'Bekleyen Emirleri Isle'}
            </Button>

            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button variant="outline" disabled={isExporting}>
                        <Download className="mr-2 h-4 w-4" />
                        {isExporting
                            ? t('portfolioDetailPage.export.exporting')
                            : t('portfolioDetailPage.export.title')}
                    </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={onExportExcel}>
                        <FileSpreadsheet className="mr-2 h-4 w-4 text-green-600" />
                        {t('portfolioDetailPage.export.excel')}
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={onExportPdf}>
                        <FileText className="mr-2 h-4 w-4 text-red-600" />
                        {t('portfolioDetailPage.export.pdf')}
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            <Button onClick={onOpenAddDialog}>
                <Plus className="mr-2 h-4 w-4" />
                {t('portfolioDetailPage.addItem')}
            </Button>
        </div>
    )
}
