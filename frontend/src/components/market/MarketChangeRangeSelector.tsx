import { ChangeEvent } from 'react'
import { CalendarDays } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { MARKET_CHANGE_PERIODS } from '@/hooks/useMarketChangeRange'
import { cn } from '@/lib/utils'

type PeriodValue = (typeof MARKET_CHANGE_PERIODS)[number]['value']

export interface MarketChangeRangeSelectorProps {
  period: PeriodValue
  setPeriod: (value: PeriodValue) => void
  customStartDate: string
  setCustomStartDate: (value: string) => void
  customEndDate: string
  setCustomEndDate: (value: string) => void
  className?: string
}

export default function MarketChangeRangeSelector({
  period,
  setPeriod,
  customStartDate,
  setCustomStartDate,
  customEndDate,
  setCustomEndDate,
  className = undefined,
}: MarketChangeRangeSelectorProps) {
  const { t } = useTranslation()

  const handleStartDateChange = (event: ChangeEvent<HTMLInputElement>) => {
    setCustomStartDate(event.target.value)
  }

  const handleEndDateChange = (event: ChangeEvent<HTMLInputElement>) => {
    setCustomEndDate(event.target.value)
  }

  return (
    <div className={cn('flex flex-wrap items-center gap-2', className)}>
      <div className="flex items-center gap-2 rounded-md border border-input bg-background px-3">
        <CalendarDays className="h-4 w-4 text-muted-foreground" />
        <Select value={period} onValueChange={setPeriod}>
          <SelectTrigger className="h-9 w-32 border-0 px-0 shadow-none focus:ring-0">
            <SelectValue aria-label={t('marketChangeRange.label')} />
          </SelectTrigger>
          <SelectContent>
            {MARKET_CHANGE_PERIODS.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {t(`marketChangeRange.periods.${option.value}`)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      {period === 'CUSTOM' && (
        <>
          <Input
            type="date"
            value={customStartDate}
            onChange={handleStartDateChange}
            aria-label={t('marketChangeRange.startDate')}
            className="h-10 w-36"
          />
          <Input
            type="date"
            value={customEndDate}
            onChange={handleEndDateChange}
            aria-label={t('marketChangeRange.endDate')}
            className="h-10 w-36"
          />
        </>
      )}
    </div>
  )
}
