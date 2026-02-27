import { useEffect, useState } from 'react'
import { useGetPortfolioTransactionsQuery } from '@/store/api/portfolioApi'

export function usePortfolioTransactions(portfolioId) {
    const [transactionsPage, setTransactionsPage] = useState(0)
    const [orderStatus, setOrderStatus] = useState('ALL')

    useEffect(() => {
        setTransactionsPage(0)
    }, [portfolioId])

    useEffect(() => {
        setTransactionsPage(0)
    }, [orderStatus])

    const {
        data: transactionsData,
        isLoading: transactionsLoading,
        isFetching: transactionsFetching
    } = useGetPortfolioTransactionsQuery(
        {
            portfolioId,
            page: transactionsPage,
            size: 8,
            orderStatus: orderStatus === 'ALL' ? undefined : orderStatus,
        },
        { skip: !portfolioId }
    )

    return {
        transactionsPage,
        setTransactionsPage,
        orderStatus,
        setOrderStatus,
        transactionsLoading,
        transactionsFetching,
        transactions: transactionsData?.data || [],
        transactionPages: transactionsData?.pagination?.totalPages || 0,
        totalTransactions: transactionsData?.pagination?.totalElements || 0
    }
}
