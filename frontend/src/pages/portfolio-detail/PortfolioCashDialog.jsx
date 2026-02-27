import { useEffect, useState } from 'react'
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

export function PortfolioCashDialog({ portfolioId, open, onOpenChange }) {
  const [action, setAction] = useState('DEPOSIT')
  const [amount, setAmount] = useState('')
  const [notes, setNotes] = useState('')
  const [adjustCash, { isLoading }] = useAdjustPortfolioCashMutation()

  useEffect(() => {
    if (open) {
      setAction('DEPOSIT')
      setAmount('')
      setNotes('')
    }
  }, [open])

  const handleSubmit = async (event) => {
    event.preventDefault()

    const parsedAmount = parseFloat(amount)
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      toast.error('Gecerli bir tutar girin')
      return
    }

    try {
      await adjustCash({
        portfolioId,
        action,
        amount: parsedAmount,
        notes: notes.trim() || undefined,
      }).unwrap()

      toast.success(action === 'DEPOSIT' ? 'Nakit eklendi' : 'Nakit cekildi')
      onOpenChange(false)
    } catch {
      toast.error('Nakit islemi basarisiz oldu')
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Nakit Islem</DialogTitle>
          <DialogDescription>Portfoy nakit bakiyesini artirabilir veya azaltabilirsiniz.</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit}>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label>Islem Turu</Label>
              <div className="flex gap-2">
                <Button
                  type="button"
                  variant={action === 'DEPOSIT' ? 'default' : 'outline'}
                  onClick={() => setAction('DEPOSIT')}
                >
                  Yatir
                </Button>
                <Button
                  type="button"
                  variant={action === 'WITHDRAW' ? 'default' : 'outline'}
                  onClick={() => setAction('WITHDRAW')}
                >
                  Cek
                </Button>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="cash-amount">Tutar</Label>
              <Input
                id="cash-amount"
                type="number"
                min="0.000001"
                step="0.01"
                value={amount}
                onChange={(event) => setAmount(event.target.value)}
                placeholder="1000"
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="cash-notes">Not</Label>
              <Input
                id="cash-notes"
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
                placeholder="Opsiyonel not"
              />
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Vazgec
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? 'Kaydediliyor...' : action === 'DEPOSIT' ? 'Yatir' : 'Cek'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
