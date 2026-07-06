import { useMemo, useState } from 'react'

const MS_PER_DAY = 24 * 60 * 60 * 1000

export const MARKET_CHANGE_PERIODS = [
  { value: '1D', days: 1 },
  { value: '1W', days: 7 },
  { value: '1M', days: 30 },
  { value: '3M', days: 90 },
  { value: '6M', days: 180 },
  { value: '1Y', days: 365 },
  { value: 'CUSTOM', days: null },
] as const

export type MarketChangePeriod = (typeof MARKET_CHANGE_PERIODS)[number]['value']

function formatDateInput(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function daysAgo(days: number) {
  return formatDateInput(new Date(Date.now() - days * MS_PER_DAY))
}

export function useMarketChangeRange(defaultPeriod: MarketChangePeriod = '1D') {
  const [period, setPeriod] = useState<MarketChangePeriod>(defaultPeriod)
  const [customStartDate, setCustomStartDate] = useState(() => daysAgo(7))
  const [customEndDate, setCustomEndDate] = useState(() => formatDateInput(new Date()))

  const resolved = useMemo(() => {
    if (period === 'CUSTOM') {
      const start = customStartDate || customEndDate || formatDateInput(new Date())
      const end = customEndDate || start
      return start <= end
        ? { changeStartDate: start, changeEndDate: end }
        : { changeStartDate: end, changeEndDate: start }
    }

    const option = MARKET_CHANGE_PERIODS.find((item) => item.value === period)
    const days = option?.days ?? 1
    return {
      changeStartDate: daysAgo(days),
      changeEndDate: formatDateInput(new Date()),
    }
  }, [customEndDate, customStartDate, period])

  return {
    period: period as MarketChangePeriod,
    setPeriod: setPeriod as (value: MarketChangePeriod) => void,
    customStartDate,
    setCustomStartDate,
    customEndDate,
    setCustomEndDate,
    queryParams: resolved,
  } as const
}

export function getMarketChangeRangeLabel(t: (key: string) => string, range: ReturnType<typeof useMarketChangeRange>) {
  if (range.period === 'CUSTOM') {
    return `${range.queryParams.changeStartDate} / ${range.queryParams.changeEndDate}`
  }
  return t(`marketChangeRange.periods.${range.period}`)
}
