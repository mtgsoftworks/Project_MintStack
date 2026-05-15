import { useEffect, useState } from 'react'
import { useGetPortfoliosQuery } from '@/store/api/portfolioApi'

export function useDefaultPortfolioSelection() {
  const [selectedPortfolioId, setSelectedPortfolioId] = useState('')
  const { data: portfolios = [] } = useGetPortfoliosQuery()

  useEffect(() => {
    if (!selectedPortfolioId && portfolios.length > 0) {
      const defaultPortfolio = portfolios.find((portfolio) => portfolio.isDefault) || portfolios[0]
      setSelectedPortfolioId(defaultPortfolio?.id || '')
    }
  }, [portfolios, selectedPortfolioId])

  return {
    portfolios,
    selectedPortfolioId,
    setSelectedPortfolioId,
  }
}
