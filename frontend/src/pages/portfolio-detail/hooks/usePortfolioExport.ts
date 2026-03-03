import { useState } from 'react'
import { toast } from 'sonner'
import { exportPortfolioToExcel, exportPortfolioToPdf } from '@/store/api/portfolioApi'

export function usePortfolioExport({ portfolioId, token, t }) {
    const [isExporting, setIsExporting] = useState(false)

    const runExport = async (exportFn, successKey, errorKey) => {
        if (!token) {
            toast.error(t('portfolioDetailPage.export.authRequired'))
            return
        }

        setIsExporting(true)
        try {
            await exportFn(portfolioId, token)
            toast.success(t(successKey))
        } catch {
            toast.error(t(errorKey))
        } finally {
            setIsExporting(false)
        }
    }

    const handleExportExcel = () => runExport(
        exportPortfolioToExcel,
        'portfolioDetailPage.export.excelSuccess',
        'portfolioDetailPage.export.excelError'
    )

    const handleExportPdf = () => runExport(
        exportPortfolioToPdf,
        'portfolioDetailPage.export.pdfSuccess',
        'portfolioDetailPage.export.pdfError'
    )

    return {
        isExporting,
        handleExportExcel,
        handleExportPdf
    }
}
