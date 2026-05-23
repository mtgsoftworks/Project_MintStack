import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import RefreshButton from '@/components/common/RefreshButton'
import {
    Bell,
    BellOff,
    Check,
    CheckCheck,
    TrendingUp,
    AlertCircle,
    Newspaper,
    DollarSign,
    Loader2
} from 'lucide-react'
import { cn, formatRelativeTime } from '@/lib/utils'
import { 
    useGetNotificationsQuery, 
    useMarkNotificationReadMutation,
    useMarkAllNotificationsReadMutation 
} from '@/store/api/userApi'

const getNotificationIcon = (type) => {
    switch (type?.toLowerCase()) {
        case 'alert':
        case 'price_alert':
            return { icon: TrendingUp, color: 'text-success' }
        case 'portfolio':
            return { icon: DollarSign, color: 'text-primary' }
        case 'news':
            return { icon: Newspaper, color: 'text-warning' }
        case 'system':
        default:
            return { icon: AlertCircle, color: 'text-muted-foreground' }
    }
}

export default function NotificationsPage() {
    const { t } = useTranslation()
    const [activeTab, setActiveTab] = useState('all')
    
    const { data: notificationsData, isLoading, isFetching, refetch } = useGetNotificationsQuery({ page: 0, size: 50 })
    const [markAsRead] = useMarkNotificationReadMutation()
    const [markAllAsRead] = useMarkAllNotificationsReadMutation()
    
    const notifications = useMemo(() => {
        return notificationsData?.data || []
    }, [notificationsData])

    const unreadCount = notifications.filter(n => !n.isRead).length

    const filteredNotifications = notifications.filter(n => {
        if (activeTab === 'all') return true
        if (activeTab === 'unread') return !n.isRead
        if (activeTab === 'alerts') return n.type === 'ALERT' || n.type === 'price_alert'
        if (activeTab === 'news') return n.type === 'NEWS' || n.type === 'news'
        return true
    })

    const handleMarkAsRead = async (id) => {
        try {
            await markAsRead(id).unwrap()
            refetch()
        } catch (error) {
            console.error('Failed to mark notification as read:', error)
        }
    }

    const handleMarkAllAsRead = async () => {
        try {
            await markAllAsRead().unwrap()
            refetch()
        } catch (error) {
            console.error('Failed to mark all notifications as read:', error)
        }
    }

    if (isLoading) {
        return (
            <div className="flex items-center justify-center h-64">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
        )
    }

    return (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold flex items-center gap-2">
                        <Bell className="h-6 w-6" />
                        {t('notificationsPage.title')}
                    </h1>
                    <p className="text-muted-foreground">
                        {t('notificationsPage.subtitle')}
                    </p>
                </div>
                <div className="flex gap-2">
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={handleMarkAllAsRead}
                        disabled={unreadCount === 0}
                    >
                        <CheckCheck className="h-4 w-4 mr-2" />
                        {t('notificationsPage.actions.markAllRead')}
                    </Button>
                    <RefreshButton
                        variant="outline"
                        size="sm"
                        onRefresh={refetch}
                        isLoading={isFetching}
                    >
                        {t('common.refresh')}
                    </RefreshButton>
                </div>
            </div>

            <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-4">
                <TabsList>
                    <TabsTrigger value="all" className="flex gap-2">
                        {t('notificationsPage.tabs.all')}
                        {notifications.length > 0 && (
                            <Badge variant="secondary" className="ml-1">{notifications.length}</Badge>
                        )}
                    </TabsTrigger>
                    <TabsTrigger value="unread" className="flex gap-2">
                        {t('notificationsPage.tabs.unread')}
                        {unreadCount > 0 && (
                            <Badge variant="danger-solid" className="ml-1">{unreadCount}</Badge>
                        )}
                    </TabsTrigger>
                    <TabsTrigger value="alerts">{t('notificationsPage.tabs.alerts')}</TabsTrigger>
                    <TabsTrigger value="news">{t('notificationsPage.tabs.news')}</TabsTrigger>
                </TabsList>

                <TabsContent value={activeTab}>
                    <Card>
                        <CardHeader>
                            <CardTitle>
                                {t(`notificationsPage.cardTitles.${activeTab}`)}
                            </CardTitle>
                            <CardDescription>
                                {filteredNotifications.length === 0
                                    ? t('notificationsPage.cardDescription.empty')
                                    : t('notificationsPage.cardDescription.count', { count: filteredNotifications.length })
                                }
                            </CardDescription>
                        </CardHeader>
                        <CardContent>
                            {filteredNotifications.length === 0 ? (
                                <div className="text-center py-12 text-muted-foreground">
                                    <BellOff className="h-12 w-12 mx-auto mb-4 opacity-50" />
                                    <p>{t('notificationsPage.emptyCategory')}</p>
                                </div>
                            ) : (
                                <div className="space-y-2">
                                    {filteredNotifications.map((notification) => {
                                        const { icon: IconComponent, color: iconColor } = getNotificationIcon(notification.type)
                                        return (
                                            <div
                                                key={notification.id}
                                                className={cn(
                                                    "flex items-start gap-4 p-4 rounded-lg border transition-colors",
                                                    !notification.isRead && "bg-primary/5 border-primary/20",
                                                    notification.isRead && "bg-muted/30"
                                                )}
                                            >
                                                <div className={cn(
                                                    "flex-shrink-0 p-2 rounded-full",
                                                    !notification.isRead ? "bg-primary/10" : "bg-muted"
                                                )}>
                                                    <IconComponent className={cn("h-5 w-5", iconColor)} />
                                                </div>
                                                <div className="flex-1 min-w-0">
                                                    <div className="flex items-start justify-between gap-2">
                                                        <div>
                                                            <p className={cn(
                                                                "font-medium",
                                                                !notification.isRead && "text-foreground",
                                                                notification.isRead && "text-muted-foreground"
                                                            )}>
                                                                {notification.title}
                                                            </p>
                                                            <p className="text-sm text-muted-foreground mt-1">
                                                                {notification.message}
                                                            </p>
                                                            <p className="text-xs text-muted-foreground mt-2">
                                                                {formatRelativeTime(notification.createdAt)}
                                                            </p>
                                                        </div>
                                                        <div className="flex items-center gap-1">
                                                            {!notification.isRead && (
                                                                <Button
                                                                    variant="ghost"
                                                                    size="icon"
                                                                    className="h-8 w-8"
                                                                    onClick={() => handleMarkAsRead(notification.id)}
                                                                    title={t('notificationsPage.actions.markRead')}
                                                                >
                                                                    <Check className="h-4 w-4" />
                                                                </Button>
                                                            )}
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        )
                                    })}
                                </div>
                            )}
                        </CardContent>
                    </Card>
                </TabsContent>
            </Tabs>
        </div>
    )
}
