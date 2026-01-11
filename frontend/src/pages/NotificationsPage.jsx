import { useState } from 'react'
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
        title: 'Fiyat Alarmı Tetiklendi',
        message: 'THYAO hissesi hedef fiyat olan ₺185.00\'a ulaştı',
        icon: TrendingUp,
        iconColor: 'text-success',
        read: false,
        timestamp: '5 dakika önce'
    },
    {
        id: 2,
        type: 'portfolio',
        title: 'Portföy Güncellendi',
        message: 'Portföyünüzdeki GARAN hissesi bugün %2.5 değer kazandı',
        icon: DollarSign,
        iconColor: 'text-primary',
        read: false,
        timestamp: '1 saat önce'
    },
    {
        id: 3,
        type: 'news',
        title: 'Önemli Haber',
        message: 'Merkez Bankası faiz kararını açıkladı',
        icon: Newspaper,
        iconColor: 'text-warning',
        read: false,
        timestamp: '2 saat önce'
    },
    {
        id: 4,
        type: 'price_alert',
        title: 'Düşüş Alarmı',
        message: 'EUR/TRY kuru belirlediğiniz alt limite yaklaştı',
        icon: TrendingDown,
        iconColor: 'text-danger',
        read: true,
        timestamp: '5 saat önce'
    },
    {
        id: 5,
        type: 'system',
        title: 'Sistem Bildirimi',
        message: 'API anahtarınız başarıyla doğrulandı ve kaydedildi',
        icon: AlertCircle,
        iconColor: 'text-muted-foreground',
        read: true,
        timestamp: 'Dün'
    }
]

export default function NotificationsPage() {
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
        if (confirm('Tüm bildirimleri silmek istediğinize emin misiniz?')) {
            setNotifications([])
        }
    }

    return (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold flex items-center gap-2">
                        <Bell className="h-6 w-6" />
                        Bildirimler
                    </h1>
                    <p className="text-muted-foreground">
                        Alarm ve sistem bildirimlerinizi yönetin
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
                        Tümünü Okundu İşaretle
                    </Button>
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={handleClearAll}
                        disabled={notifications.length === 0}
                        className="text-destructive hover:text-destructive"
                    >
                        <Trash2 className="h-4 w-4 mr-2" />
                        Tümünü Sil
                    </Button>
                </div>
            </div>

            <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-4">
                <TabsList>
                    <TabsTrigger value="all" className="flex gap-2">
                        Tümü
                        {notifications.length > 0 && (
                            <Badge variant="secondary" className="ml-1">{notifications.length}</Badge>
                        )}
                    </TabsTrigger>
                    <TabsTrigger value="unread" className="flex gap-2">
                        Okunmamış
                        {unreadCount > 0 && (
                            <Badge variant="danger-solid" className="ml-1">{unreadCount}</Badge>
                        )}
                    </TabsTrigger>
                    <TabsTrigger value="alerts">Alarmlar</TabsTrigger>
                    <TabsTrigger value="news">Haberler</TabsTrigger>
                </TabsList>

                <TabsContent value={activeTab}>
                    <Card>
                        <CardHeader>
                            <CardTitle>
                                {activeTab === 'all' && 'Tüm Bildirimler'}
                                {activeTab === 'unread' && 'Okunmamış Bildirimler'}
                                {activeTab === 'alerts' && 'Fiyat Alarmları'}
                                {activeTab === 'news' && 'Haber Bildirimleri'}
                            </CardTitle>
                            <CardDescription>
                                {filteredNotifications.length === 0
                                    ? 'Henüz bildirim yok'
                                    : `${filteredNotifications.length} bildirim`
                                }
                            </CardDescription>
                        </CardHeader>
                        <CardContent>
                            {filteredNotifications.length === 0 ? (
                                <div className="text-center py-12 text-muted-foreground">
                                    <BellOff className="h-12 w-12 mx-auto mb-4 opacity-50" />
                                    <p>Bu kategoride bildirim bulunmuyor</p>
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
                                                                {notification.title}
                                                            </p>
                                                            <p className="text-sm text-muted-foreground mt-1">
                                                                {notification.message}
                                                            </p>
                                                            <p className="text-xs text-muted-foreground mt-2">
                                                                {notification.timestamp}
                                                            </p>
                                                        </div>
                                                        <div className="flex items-center gap-1">
                                                            {!notification.read && (
                                                                <Button
                                                                    variant="ghost"
                                                                    size="icon"
                                                                    className="h-8 w-8"
                                                                    onClick={() => handleMarkAsRead(notification.id)}
                                                                    title="Okundu işaretle"
                                                                >
                                                                    <Check className="h-4 w-4" />
                                                                </Button>
                                                            )}
                                                            <Button
                                                                variant="ghost"
                                                                size="icon"
                                                                className="h-8 w-8 text-destructive hover:text-destructive"
                                                                onClick={() => handleDelete(notification.id)}
                                                                title="Sil"
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
