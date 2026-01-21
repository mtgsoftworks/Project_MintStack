import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
    Bell,
    BellOff,
    Check,
    CheckCheck,
    TrendingUp,
    TrendingDown,
    AlertCircle,
    Newspaper,
    DollarSign,
    RefreshCw,
    Trash2
} from 'lucide-react'
import { cn } from '@/lib/utils'

// Mock notification data - will be replaced with API call
const mockNotifications = [
    {
        id: 1,
        type: 'price_alert',
        titleKey: 'notificationsPage.mockNotifications.priceAlert.title',
        messageKey: 'notificationsPage.mockNotifications.priceAlert.message',
        messageValues: { symbol: 'THYAO', price: '₺185.00' },
        timestampKey: 'notificationsPage.mockNotifications.priceAlert.timestamp',
        icon: TrendingUp,
        iconColor: 'text-success',
        read: false,
    },
    {
        id: 2,
        type: 'portfolio',
        titleKey: 'notificationsPage.mockNotifications.portfolio.title',
        messageKey: 'notificationsPage.mockNotifications.portfolio.message',
        messageValues: { symbol: 'GARAN', change: '2.5' },
        timestampKey: 'notificationsPage.mockNotifications.portfolio.timestamp',
        icon: DollarSign,
        iconColor: 'text-primary',
        read: false,
    },
    {
        id: 3,
        type: 'news',
        titleKey: 'notificationsPage.mockNotifications.news.title',
        messageKey: 'notificationsPage.mockNotifications.news.message',
        timestampKey: 'notificationsPage.mockNotifications.news.timestamp',
        icon: Newspaper,
        iconColor: 'text-warning',
        read: false,
    },
    {
        id: 4,
        type: 'price_alert',
        titleKey: 'notificationsPage.mockNotifications.priceDrop.title',
        messageKey: 'notificationsPage.mockNotifications.priceDrop.message',
        messageValues: { pair: 'EUR/TRY' },
        timestampKey: 'notificationsPage.mockNotifications.priceDrop.timestamp',
        icon: TrendingDown,
        iconColor: 'text-danger',
        read: true,
    },
    {
        id: 5,
        type: 'system',
        titleKey: 'notificationsPage.mockNotifications.system.title',
        messageKey: 'notificationsPage.mockNotifications.system.message',
        timestampKey: 'notificationsPage.mockNotifications.system.timestamp',
        icon: AlertCircle,
        iconColor: 'text-muted-foreground',
        read: true,
    }
]

export default function NotificationsPage() {
    const { t } = useTranslation()
    const [notifications, setNotifications] = useState(mockNotifications)
    const [activeTab, setActiveTab] = useState('all')

    const unreadCount = notifications.filter(n => !n.read).length

    const filteredNotifications = notifications.filter(n => {
        if (activeTab === 'all') return true
        if (activeTab === 'unread') return !n.read
        if (activeTab === 'alerts') return n.type === 'price_alert'
        if (activeTab === 'news') return n.type === 'news'
        return true
    })

    const handleMarkAsRead = (id) => {
        setNotifications(prev =>
            prev.map(n => n.id === id ? { ...n, read: true } : n)
        )
    }

    const handleMarkAllAsRead = () => {
        setNotifications(prev => prev.map(n => ({ ...n, read: true })))
    }

    const handleDelete = (id) => {
        setNotifications(prev => prev.filter(n => n.id !== id))
    }

    const handleClearAll = () => {
        if (confirm(t('notificationsPage.confirmClearAll'))) {
            setNotifications([])
        }
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
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={handleClearAll}
                        disabled={notifications.length === 0}
                        className="text-destructive hover:text-destructive"
                    >
                        <Trash2 className="h-4 w-4 mr-2" />
                        {t('notificationsPage.actions.clearAll')}
                    </Button>
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
                                        const IconComponent = notification.icon
                                        return (
                                            <div
                                                key={notification.id}
                                                className={cn(
                                                    "flex items-start gap-4 p-4 rounded-lg border transition-colors",
                                                    !notification.read && "bg-primary/5 border-primary/20",
                                                    notification.read && "bg-muted/30"
                                                )}
                                            >
                                                <div className={cn(
                                                    "flex-shrink-0 p-2 rounded-full",
                                                    !notification.read ? "bg-primary/10" : "bg-muted"
                                                )}>
                                                    <IconComponent className={cn("h-5 w-5", notification.iconColor)} />
                                                </div>
                                                <div className="flex-1 min-w-0">
                                                    <div className="flex items-start justify-between gap-2">
                                                        <div>
                                                            <p className={cn(
                                                                "font-medium",
                                                                !notification.read && "text-foreground",
                                                                notification.read && "text-muted-foreground"
                                                            )}>
                                                                {t(notification.titleKey)}
                                                            </p>
                                                            <p className="text-sm text-muted-foreground mt-1">
                                                                {t(notification.messageKey, notification.messageValues)}
                                                            </p>
                                                            <p className="text-xs text-muted-foreground mt-2">
                                                                {t(notification.timestampKey)}
                                                            </p>
                                                        </div>
                                                        <div className="flex items-center gap-1">
                                                            {!notification.read && (
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
                                                            <Button
                                                                variant="ghost"
                                                                size="icon"
                                                                className="h-8 w-8 text-destructive hover:text-destructive"
                                                                onClick={() => handleDelete(notification.id)}
                                                                title={t('notificationsPage.actions.delete')}
                                                            >
                                                                <Trash2 className="h-4 w-4" />
                                                            </Button>
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
