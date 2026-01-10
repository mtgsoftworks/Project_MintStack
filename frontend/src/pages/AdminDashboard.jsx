import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { Users, BarChart3, Bell, Wallet, RefreshCw, UserCheck, UserX, Search } from 'lucide-react'
import adminService from '@/services/adminService'

export default function AdminDashboard() {
    const { t } = useTranslation()
    const [dashboard, setDashboard] = useState(null)
    const [users, setUsers] = useState([])
    const [loading, setLoading] = useState(true)
    const [searchQuery, setSearchQuery] = useState('')
    const [currentPage, setCurrentPage] = useState(0)

    useEffect(() => {
        loadData()
    }, [currentPage])

    const loadData = async () => {
        try {
            setLoading(true)
            const [dashboardRes, usersRes] = await Promise.all([
                adminService.getDashboard(),
                searchQuery
                    ? adminService.searchUsers(searchQuery, currentPage)
                    : adminService.getUsers(currentPage)
            ])
            setDashboard(dashboardRes.data)
            setUsers(usersRes.data?.content || [])
        } catch (error) {
            console.error('Error loading admin data:', error)
        } finally {
            setLoading(false)
        }
    }

    const handleSearch = async () => {
        setCurrentPage(0)
        loadData()
    }

    const handleActivateUser = async (userId) => {
        try {
            await adminService.activateUser(userId)
            loadData()
        } catch (error) {
            console.error('Error activating user:', error)
        }
    }

    const handleDeactivateUser = async (userId) => {
        if (!confirm(t('common.confirm'))) return
        try {
            await adminService.deactivateUser(userId)
            loadData()
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
        { label: 'Portföyler', value: dashboard?.totalPortfolios || 0, icon: Wallet, color: 'bg-purple-500' },
        { label: 'Aktif Alarmlar', value: dashboard?.activeAlerts || 0, icon: Bell, color: 'bg-orange-500' },
        { label: 'Enstrümanlar', value: dashboard?.totalInstruments || 0, icon: BarChart3, color: 'bg-cyan-500' },
        { label: 'Watchlistler', value: dashboard?.totalWatchlists || 0, icon: RefreshCw, color: 'bg-pink-500' },
    ]

    return (
        <div className="p-6">
            <div className="flex items-center justify-between mb-6">
                <h1 className="text-2xl font-bold text-gray-900">{t('admin.dashboard')}</h1>
                <button
                    onClick={loadData}
                    className="flex items-center gap-2 px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg"
                >
                    <RefreshCw className="w-4 h-4" />
                    {t('common.refresh')}
                </button>
            </div>

            {/* Stats Grid */}
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
                {stats.map((stat, index) => (
                    <div key={index} className="bg-white rounded-xl p-4 shadow-sm">
                        <div className={`w-10 h-10 ${stat.color} rounded-lg flex items-center justify-center mb-3`}>
                            <stat.icon className="w-5 h-5 text-white" />
                        </div>
                        <div className="text-2xl font-bold text-gray-900">{stat.value}</div>
                        <div className="text-sm text-gray-500">{stat.label}</div>
                    </div>
                ))}
            </div>

            {/* Users Section */}
            <div className="bg-white rounded-xl shadow-sm">
                <div className="p-4 border-b flex items-center justify-between">
                    <h2 className="font-semibold text-gray-900">{t('admin.users')}</h2>
                    <div className="flex items-center gap-2">
                        <div className="relative">
                            <input
                                type="text"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                                placeholder={t('common.search')}
                                className="pl-10 pr-4 py-2 border rounded-lg w-64"
                            />
                            <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
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
                        <thead className="bg-gray-50">
                            <tr>
                                <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">Kullanıcı</th>
                                <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">Email</th>
                                <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">Portföy</th>
                                <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">Durum</th>
                                <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">Kayıt</th>
                                <th className="px-4 py-3 text-right text-sm font-medium text-gray-600"></th>
                            </tr>
                        </thead>
                        <tbody className="divide-y">
                            {users.map((user) => (
                                <tr key={user.id} className="hover:bg-gray-50">
                                    <td className="px-4 py-3">
                                        <div>
                                            <div className="font-medium text-gray-900">
                                                {user.firstName} {user.lastName}
                                            </div>
                                            <div className="text-sm text-gray-500">@{user.username}</div>
                                        </div>
                                    </td>
                                    <td className="px-4 py-3 text-gray-600">{user.email}</td>
                                    <td className="px-4 py-3">{user.portfolioCount}</td>
                                    <td className="px-4 py-3">
                                        {user.isActive ? (
                                            <span className="inline-flex items-center gap-1 px-2 py-1 bg-green-100 text-green-700 rounded-full text-sm">
                                                <UserCheck className="w-3 h-3" />
                                                Aktif
                                            </span>
                                        ) : (
                                            <span className="inline-flex items-center gap-1 px-2 py-1 bg-red-100 text-red-700 rounded-full text-sm">
                                                <UserX className="w-3 h-3" />
                                                Pasif
                                            </span>
                                        )}
                                    </td>
                                    <td className="px-4 py-3 text-sm text-gray-500">
                                        {user.createdAt && new Date(user.createdAt).toLocaleDateString('tr-TR')}
                                    </td>
                                    <td className="px-4 py-3 text-right">
                                        {user.isActive ? (
                                            <button
                                                onClick={() => handleDeactivateUser(user.id)}
                                                className="text-red-500 hover:text-red-600 text-sm"
                                            >
                                                {t('admin.deactivate')}
                                            </button>
                                        ) : (
                                            <button
                                                onClick={() => handleActivateUser(user.id)}
                                                className="text-emerald-500 hover:text-emerald-600 text-sm"
                                            >
                                                {t('admin.activate')}
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))}
                            {users.length === 0 && (
                                <tr>
                                    <td colSpan={6} className="px-4 py-8 text-center text-gray-500">
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
