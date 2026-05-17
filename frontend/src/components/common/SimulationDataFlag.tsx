import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

export function SimulationDataFlag({ className = '' }) {
  const { t } = useTranslation()

  return (
    <Badge
      variant="outline"
      className={cn(
        'h-5 border-info/30 bg-info/5 px-1.5 text-[10px] font-medium uppercase tracking-[0.04em] text-info',
        className
      )}
    >
      {t('common.simulatedShort')}
    </Badge>
  )
}

export default SimulationDataFlag
