import { useMemo } from 'react'
import {
  useGetBondsQuery,
  useGetCurrenciesQuery,
  useGetFundsQuery,
  useGetStocksQuery,
  useGetViopQuery,
} from '@/store/api/marketApi'

function addOption(map, symbol, name, type) {
  const normalizedSymbol = (symbol || '').toUpperCase().trim()
  if (!normalizedSymbol || map.has(normalizedSymbol)) {
    return
  }
  map.set(normalizedSymbol, {
    symbol: normalizedSymbol,
    name: name || normalizedSymbol,
    type: type || '-',
  })
}

export function useInstrumentOptions() {
  const { data: stocksResponse, isFetching: stocksFetching } = useGetStocksQuery({ page: 0, size: 1000, sort: 'symbol,asc' })
  const { data: bondsResponse, isFetching: bondsFetching } = useGetBondsQuery({ page: 0, size: 1000, sort: 'symbol,asc' })
  const { data: fundsResponse, isFetching: fundsFetching } = useGetFundsQuery({ page: 0, size: 5000, sort: 'symbol,asc' })
  const { data: viopResponse, isFetching: viopFetching } = useGetViopQuery({ page: 0, size: 1000, sort: 'symbol,asc' })
  const { data: currencies = [], isFetching: currenciesFetching } = useGetCurrenciesQuery({})

  const instrumentOptions = useMemo(() => {
    const options = new Map()

    for (const item of stocksResponse?.data || []) {
      addOption(options, item.symbol, item.name, item.type || 'STOCK')
    }
    for (const item of bondsResponse?.data || []) {
      addOption(options, item.symbol, item.name, item.type || 'BOND')
    }
    for (const item of fundsResponse?.data || []) {
      addOption(options, item.symbol, item.name, item.type || 'FUND')
    }
    for (const item of viopResponse?.data || []) {
      addOption(options, item.symbol, item.name, item.type || 'VIOP')
    }
    for (const currency of currencies || []) {
      addOption(
        options,
        `${currency.currencyCode}TRY`,
        currency.currencyName ? `${currency.currencyName} / TRY` : `${currency.currencyCode}/TRY`,
        'CURRENCY'
      )
    }

    return [...options.values()].sort((left, right) => left.symbol.localeCompare(right.symbol, 'tr'))
  }, [stocksResponse, bondsResponse, fundsResponse, viopResponse, currencies])

  return {
    instrumentOptions,
    isFetching: stocksFetching || bondsFetching || fundsFetching || viopFetching || currenciesFetching,
  }
}
