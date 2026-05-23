import { useEffect, useState } from 'react'
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
  DialogTitle,
} from '@/components/ui/dialog'
import { useAdjustPortfolioCashMutation } from '@/store/api/portfolioApi'

const THOUSANDS_DOT_PATTERN = /^\d{1,3}(\.\d{3})+$/
const THOUSANDS_COMMA_PATTERN = /^\d{1,3}(,\d{3})+$/

function parseLocalizedAmount(value) {
  if (value == null) {
    return Number.NaN
  }

  const raw = String(value)
    .trim()
    .replace(/\s/g, '')
    .replace(/[^0-9,.-]/g, '')

  if (!raw) {
    return Number.NaN
  }

  const hasComma = raw.includes(',')
  const hasDot = raw.includes('.')

  if (hasComma && hasDot) {
    if (raw.lastIndexOf(',') > raw.lastIndexOf('.')) {
      return Number.parseFloat(raw.replace(/\./g, '').replace(',', '.'))
    }
    return Number.parseFloat(raw.replace(/,/g, ''))
  }

  if (hasComma) {
    if (THOUSANDS_COMMA_PATTERN.test(raw)) {
      return Number.parseFloat(raw.replace(/,/g, ''))
    }
    return Number.parseFloat(raw.replace(',', '.'))
  }

  if (hasDot && THOUSANDS_DOT_PATTERN.test(raw)) {
    return Number.parseFloat(raw.replace(/\./g, ''))
  }

  return Number.parseFloat(raw)
}

function resolveApiErrorMessage(error, fallbackMessage) {
  const responseData = error?.data
  if (!responseData) {
    return fallbackMessage
  }
  if (typeof responseData === 'string') {
    return responseData
  }
  return responseData.message || responseData.error || responseData.details || fallbackMessage
}

export function PortfolioCashDialog({ portfolioId, open, onOpenChange, currentCashBalance = null }) {
  const { t, i18n } = useTranslation()
  const [action, setAction] = useState('DEPOSIT')
  const [amount, setAmount] = useState('')
  const [notes, setNotes] = useState('')
  const [adjustCash, { isLoading }] = useAdjustPortfolioCashMutation()
  const numberLocale = i18n.language === 'en' ? 'en-US' : 'tr-TR'

  useEffect(() => {
    if (open) {
      setAction('DEPOSIT')
      setAmount('')
      setNotes('')
    }
  }, [open])

  const handleSubmit = async (event) => {
    event.preventDefault()

    const parsedAmount = parseLocalizedAmount(amount)
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      toast.error(t('portfolioDetailPage.cashDialog.invalidAmount'))
      return
    }

    if (action === 'WITHDRAW' && Number.isFinite(Number(currentCashBalance))) {
      const availableCash = Number(currentCashBalance)
      if (parsedAmount > availableCash) {
        toast.error(t('portfolioDetailPage.cashDialog.insufficientBalance', { amount: availableCash.toLocaleString(numberLocale) }))
        return
      }
    }

    try {
      await adjustCash({
        portfolioId,
        action,
        amount: parsedAmount,
        notes: notes.trim() || undefined,
      }).unwrap()

      toast.success(t(action === 'DEPOSIT' ? 'portfolioDetailPage.cashDialog.depositSuccess' : 'portfolioDetailPage.cashDialog.withdrawSuccess'))
      onOpenChange(false)
    } catch (error) {
      toast.error(resolveApiErrorMessage(error, t('portfolioDetailPage.cashDialog.failed')))
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('portfolioDetailPage.cashDialog.title')}</DialogTitle>
          <DialogDescription>{t('portfolioDetailPage.cashDialog.description')}</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit}>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label>{t('portfolioDetailPage.cashDialog.actionType')}</Label>
              <div className="flex gap-2">
                <Button
                  type="button"
                  variant={action === 'DEPOSIT' ? 'default' : 'outline'}
                  onClick={() => setAction('DEPOSIT')}
                >
                  {t('portfolioDetailPage.cashDialog.deposit')}
                </Button>
                <Button
                  type="button"
                  variant={action === 'WITHDRAW' ? 'default' : 'outline'}
                  onClick={() => setAction('WITHDRAW')}
                >
                  {t('portfolioDetailPage.cashDialog.withdraw')}
                </Button>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="cash-amount">{t('portfolioDetailPage.cashDialog.amount')}</Label>
              <Input
                id="cash-amount"
                type="text"
                inputMode="decimal"
                value={amount}
                onChange={(event) => setAmount(event.target.value)}
                placeholder="1000 veya 1.000,50"
                required
              />
              <p className="text-xs text-muted-foreground">{t('portfolioDetailPage.cashDialog.amountHelp')}</p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="cash-notes">{t('portfolioDetailPage.cashDialog.note')}</Label>
              <Input
                id="cash-notes"
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
                placeholder={t('portfolioDetailPage.cashDialog.optionalNote')}
              />
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              {t('common.cancel')}
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading
                ? t('profile.saving')
                : action === 'DEPOSIT'
                  ? t('portfolioDetailPage.cashDialog.deposit')
                  : t('portfolioDetailPage.cashDialog.withdraw')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
