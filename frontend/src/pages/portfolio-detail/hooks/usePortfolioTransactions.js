import { useEffect, useState } from 'react'
import { useGetPortfolioTransactionsQuery } from '@/store/api/portfolioApi'

export function usePortfolioTransactions(portfolioId) {
    const [transactionsPage, setTransactionsPage] = useState(0)

    useEffect(() => {
        setTransactionsPage(0)
    }, [portfolioId])

    const {
        data: transactionsData,
        isLoading: transactionsLoading,
        isFetching: transactionsFetching
    } = useGetPortfolioTransactionsQuery(
        {
            portfolioId,
            page: transactionsPage,
            size: 8
        },
        { skip: !portfolioId }
    )

    return {
        transactionsPage,
        setTransactionsPage,
        transactionsLoading,
        transactionsFetching,
        transactions: transactionsData?.data || [],
        transactionPages: transactionsData?.pagination?.totalPages || 0,
        totalTransactions: transactionsData?.pagination?.totalElements || 0
    }
}
