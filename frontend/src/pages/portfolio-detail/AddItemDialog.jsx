import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle
} from '@/components/ui/dialog'
import { useAddPortfolioItemMutation } from '@/store/api/portfolioApi'

export function AddItemDialog({ portfolioId, open, onOpenChange }) {
    const { t } = useTranslation()
    const [instrumentSymbol, setInstrumentSymbol] = useState('')
    const [quantity, setQuantity] = useState('')
    const [purchasePrice, setPurchasePrice] = useState('')
    const [purchaseDate, setPurchaseDate] = useState(() => new Date().toISOString().split('T')[0])
    const [addItem, { isLoading }] = useAddPortfolioItemMutation()

    const resetForm = () => {
        setInstrumentSymbol('')
        setQuantity('')
        setPurchasePrice('')
        setPurchaseDate(new Date().toISOString().split('T')[0])
    }

    const handleSubmit = async (event) => {
        event.preventDefault()
        try {
            await addItem({
                portfolioId,
                instrumentSymbol,
                quantity: parseFloat(quantity),
                purchasePrice: parseFloat(purchasePrice),
                purchaseDate
            }).unwrap()
            toast.success(t('portfolioDetailPage.toast.addSuccess'))
            onOpenChange(false)
            resetForm()
        } catch {
            toast.error(t('portfolioDetailPage.toast.addError'))
        }
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>{t('portfolioDetailPage.addItemDialog.title')}</DialogTitle>
                    <DialogDescription>
                        {t('portfolioDetailPage.addItemDialog.description')}
                    </DialogDescription>
                </DialogHeader>
                <form onSubmit={handleSubmit}>
                    <div className="space-y-4 py-4">
                        <div className="space-y-2">
                            <Label htmlFor="symbol">{t('portfolioDetailPage.addItemDialog.symbolLabel')}</Label>
                            <Input
                                id="symbol"
                                value={instrumentSymbol}
                                onChange={(event) => setInstrumentSymbol(event.target.value.toUpperCase())}
                                placeholder={t('portfolioDetailPage.addItemDialog.symbolPlaceholder')}
                                required
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="quantity">{t('portfolioDetailPage.addItemDialog.quantityLabel')}</Label>
                            <Input
                                id="quantity"
                                type="number"
                                step="0.01"
                                value={quantity}
                                onChange={(event) => setQuantity(event.target.value)}
                                placeholder={t('portfolioDetailPage.addItemDialog.quantityPlaceholder')}
                                required
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="price">{t('portfolioDetailPage.addItemDialog.purchasePriceLabel')}</Label>
                            <Input
                                id="price"
                                type="number"
                                step="0.01"
                                value={purchasePrice}
                                onChange={(event) => setPurchasePrice(event.target.value)}
                                placeholder={t('portfolioDetailPage.addItemDialog.purchasePricePlaceholder')}
                                required
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="purchaseDate">{t('portfolioDetailPage.addItemDialog.purchaseDateLabel')}</Label>
                            <Input
                                id="purchaseDate"
                                type="date"
                                value={purchaseDate}
                                onChange={(event) => setPurchaseDate(event.target.value)}
                                required
                            />
                        </div>
                    </div>
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                            {t('common.cancel')}
                        </Button>
                        <Button type="submit" disabled={isLoading}>
                            {isLoading
                                ? t('portfolioDetailPage.addItemDialog.submitting')
                                : t('portfolioDetailPage.addItemDialog.submit')}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    )
}
