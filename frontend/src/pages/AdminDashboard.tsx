import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Users, BarChart3, Bell, Wallet, RefreshCw, UserCheck, UserX, Search } from 'lucide-react'
import RefreshButton from '@/components/common/RefreshButton'
import {
    useActivateAdminUserMutation,
    useDeactivateAdminUserMutation,
    useGetAdminDashboardQuery,
    useGetAdminUsersQuery,
    useSearchAdminUsersQuery
} from '@/store/api/adminApi'

export default function AdminDashboard() {
    const { t } = useTranslation()
    const [searchInput, setSearchInput] = useState('')
    const [searchQuery, setSearchQuery] = useState('')
    const [currentPage, setCurrentPage] = useState(0)

    const { data: dashboard, isLoading: dashboardLoading, refetch: refetchDashboard } = useGetAdminDashboardQuery()
    const { data: usersPage, isFetching: usersFetching, refetch: refetchUsers } = useGetAdminUsersQuery(
        { page: currentPage, size: 20 },
        { skip: Boolean(searchQuery) }
    )
    const { data: searchedUsersPage, isFetching: searchFetching, refetch: refetchSearch } = useSearchAdminUsersQuery(
        { query: searchQuery, page: currentPage, size: 20 },
        { skip: !searchQuery }
    )

    const [activateUser, { isLoading: activating }] = useActivateAdminUserMutation()
    const [deactivateUser, { isLoading: deactivating }] = useDeactivateAdminUserMutation()

    const usersData = searchQuery ? searchedUsersPage : usersPage
    const users = usersData?.content || []
    const loading = dashboardLoading || usersFetching || searchFetching
    const mutating = activating || deactivating

    const handleSearch = () => {
        setCurrentPage(0)
        setSearchQuery(searchInput.trim())
    }

    const handleRefresh = () => {
        refetchDashboard()
        if (searchQuery) {
            refetchSearch()
            return
        }
        refetchUsers()
    }

    const handleActivateUser = async (userId) => {
        try {
            await activateUser(userId).unwrap()
        } catch (error) {
            console.error('Error activating user:', error)
        }
    }

    const handleDeactivateUser = async (userId) => {
        if (!window.confirm(t('common.confirm'))) return
        try {
            await deactivateUser(userId).unwrap()
        } catch (error) {
            console.error('Error deactivating user:', error)
        }
    }

    if (loading && !dashboard) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-500" />
            </div>
        )
    }

    const stats = [
        { label: t('admin.totalUsers'), value: dashboard?.totalUsers || 0, icon: Users, color: 'bg-blue-500' },
        { label: t('admin.activeUsers'), value: dashboard?.activeUsers || 0, icon: UserCheck, color: 'bg-emerald-500' },
        { label: t('admin.stats.portfolios'), value: dashboard?.totalPortfolios || 0, icon: Wallet, color: 'bg-purple-500' },
        { label: t('admin.stats.activeAlerts'), value: dashboard?.activeAlerts || 0, icon: Bell, color: 'bg-orange-500' },
        { label: t('admin.stats.instruments'), value: dashboard?.totalInstruments || 0, icon: BarChart3, color: 'bg-cyan-500' },
        { label: t('admin.stats.watchlists'), value: dashboard?.totalWatchlists || 0, icon: RefreshCw, color: 'bg-pink-500' },
    ]

    return (
        <div className="p-6">
            <div className="flex items-center justify-between mb-6">
                <h1 className="text-2xl font-bold text-foreground">{t('admin.dashboard')}</h1>
                <RefreshButton
                    variant="ghost"
                    className="px-4 py-2 text-muted-foreground hover:bg-muted"
                    onRefresh={handleRefresh}
                    isLoading={loading || mutating}
                    disabled={mutating}
                >
                    {t('common.refresh')}
                </RefreshButton>
            </div>

            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
                {stats.map((stat, index) => (
                    <div key={index} className="rounded-xl border border-border bg-card p-4 shadow-sm">
                        <div className={`w-10 h-10 ${stat.color} rounded-lg flex items-center justify-center mb-3`}>
                            <stat.icon className="w-5 h-5 text-white" />
                        </div>
                        <div className="text-2xl font-bold text-foreground">{stat.value}</div>
                        <div className="text-sm text-muted-foreground">{stat.label}</div>
                    </div>
                ))}
            </div>

            <div className="rounded-xl border border-border bg-card shadow-sm">
                <div className="p-4 border-b flex items-center justify-between">
                    <h2 className="font-semibold text-foreground">{t('admin.users')}</h2>
                    <div className="flex items-center gap-2">
                        <div className="relative">
                            <input
                                type="text"
                                value={searchInput}
                                onChange={(event) => setSearchInput(event.target.value)}
                                onKeyPress={(event) => event.key === 'Enter' && handleSearch()}
                                placeholder={t('common.search')}
                                className="w-64 rounded-lg border border-input bg-background py-2 pl-10 pr-4 text-foreground placeholder:text-muted-foreground"
                            />
                            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                        </div>
                        <button
                            onClick={handleSearch}
                            className="px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600"
                        >
                            {t('common.search')}
                        </button>
                    </div>
                </div>

                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead className="bg-muted/50">
                            <tr>
                                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('admin.table.user')}</th>
                                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('admin.table.email')}</th>
                                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('admin.table.portfolios')}</th>
                                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('admin.table.status')}</th>
                                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('admin.table.registered')}</th>
                                <th className="px-4 py-3 text-right text-sm font-medium text-muted-foreground"></th>
                            </tr>
                        </thead>
                        <tbody className="divide-y">
                            {users.map((user) => (
                                <tr key={user.id} className="hover:bg-muted/40">
                                    <td className="px-4 py-3">
                                        <div>
                                            <div className="font-medium text-foreground">
                                                {user.firstName} {user.lastName}
                                            </div>
                                            <div className="text-sm text-muted-foreground">@{user.username}</div>
                                        </div>
                                    </td>
                                    <td className="px-4 py-3 text-muted-foreground">{user.email}</td>
                                    <td className="px-4 py-3">{user.portfolioCount}</td>
                                    <td className="px-4 py-3">
                                        {user.isActive ? (
                                            <span className="inline-flex items-center gap-1 rounded-full bg-success/15 px-2 py-1 text-sm text-success">
                                                <UserCheck className="w-3 h-3" />
                                                {t('admin.status.active')}
                                            </span>
                                        ) : (
                                            <span className="inline-flex items-center gap-1 rounded-full bg-danger/15 px-2 py-1 text-sm text-danger">
                                                <UserX className="w-3 h-3" />
                                                {t('admin.status.inactive')}
                                            </span>
                                        )}
                                    </td>
                                    <td className="px-4 py-3 text-sm text-muted-foreground">
                                        {user.createdAt && new Date(user.createdAt).toLocaleDateString('tr-TR')}
                                    </td>
                                    <td className="px-4 py-3 text-right">
                                        {user.isActive ? (
                                            <button
                                                onClick={() => handleDeactivateUser(user.id)}
                                                className="text-red-500 hover:text-red-600 text-sm disabled:opacity-50"
                                                disabled={mutating}
                                            >
                                                {t('admin.deactivate')}
                                            </button>
                                        ) : (
                                            <button
                                                onClick={() => handleActivateUser(user.id)}
                                                className="text-emerald-500 hover:text-emerald-600 text-sm disabled:opacity-50"
                                                disabled={mutating}
                                            >
                                                {t('admin.activate')}
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))}
                            {users.length === 0 && (
                                <tr>
                                    <td colSpan={6} className="px-4 py-8 text-center text-muted-foreground">
                                        {t('common.noData')}
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    )
}
