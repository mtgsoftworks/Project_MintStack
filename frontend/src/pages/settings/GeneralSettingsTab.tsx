import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle
} from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue
} from '@/components/ui/select'
import {
    DialogClose,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger
} from '@/components/ui/dialog'
import { Switch } from '@/components/ui/switch'
import { AlertCircle, RefreshCw, Trash2 } from 'lucide-react'
import type { NotificationSettings } from './types'

export interface GeneralSettingsTabProps {
    t: (key: string, options?: Record<string, unknown>) => string
    i18n: { language?: string; changeLanguage: (lang: string) => void }
    theme: string
    currency: string
    timezone: string
    autoUpdate: boolean
    enableNews: boolean
    refreshRate: number
    notificationSettings: NotificationSettings
    isClearingCache: boolean
    isSavingSettings: boolean
    onThemeChange: (value: string) => void
    onLanguageChange: (value: string) => void
    onCurrencyChange: (value: string) => void
    onTimezoneChange: (value: string) => void
    onAutoUpdateChange: (value: boolean) => void
    onEnableNewsChange: (value: boolean) => void
    onRefreshRateChange: (value: number) => void
    onNotificationToggle: (key: keyof NotificationSettings, value: boolean) => void
    isAdmin: boolean
    onFullReset: () => void
    onClearCache: () => void
    onSaveSettings: () => void
}

export function GeneralSettingsTab({
    t,
    i18n,
    theme,
    currency,
    timezone,
    autoUpdate,
    enableNews,
    refreshRate,
    notificationSettings,
    isClearingCache,
    isSavingSettings,
    onThemeChange,
    onLanguageChange,
    onCurrencyChange,
    onTimezoneChange,
    onAutoUpdateChange,
    onEnableNewsChange,
    onRefreshRateChange,
    onNotificationToggle,
    isAdmin,
    onFullReset,
    onClearCache,
    onSaveSettings
}: GeneralSettingsTabProps) {
    return (
        <>
            <Card>
                <CardHeader>
                    <CardTitle>{t('settingsPage.general.title')}</CardTitle>
                    <CardDescription>{t('settingsPage.general.description')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                    <div className="space-y-4">
                        <h3 className="text-sm font-medium">{t('settingsPage.sections.appearance')}</h3>
                        <div className="flex items-center justify-between py-2 border-b">
                            <div className="space-y-0.5">
                                <Label>{t('settings.theme')}</Label>
                                <p className="text-sm text-muted-foreground">{t('settingsPage.appearance.themeDescription')}</p>
                            </div>
                            <Select value={theme} onValueChange={onThemeChange}>
                                <SelectTrigger className="w-40">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="light">{t('settingsPage.appearance.themeOptions.light')}</SelectItem>
                                    <SelectItem value="dark">{t('settingsPage.appearance.themeOptions.dark')}</SelectItem>
                                    <SelectItem value="system">{t('settingsPage.appearance.themeOptions.system')}</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                        <div className="flex items-center justify-between py-2 border-b">
                            <div className="space-y-0.5">
                                <Label>{t('settings.language')}</Label>
                                <p className="text-sm text-muted-foreground">{t('settingsPage.appearance.languageDescription')}</p>
                            </div>
                            <Select value={i18n.language?.split('-')[0] || 'tr'} onValueChange={onLanguageChange}>
                                <SelectTrigger className="w-40">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="tr">{t('settingsPage.appearance.languageOptions.tr')}</SelectItem>
                                    <SelectItem value="en">{t('settingsPage.appearance.languageOptions.en')}</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                    </div>

                    <div className="space-y-4">
                        <h3 className="text-sm font-medium">{t('settingsPage.sections.data')}</h3>
                        <div className="flex items-center justify-between py-2 border-b">
                            <div className="space-y-0.5">
                                <Label>{t('settingsPage.data.currency.label')}</Label>
                                <p className="text-sm text-muted-foreground">{t('settingsPage.data.currency.description')}</p>
                            </div>
                            <Select value={currency} onValueChange={onCurrencyChange}>
                                <SelectTrigger className="w-40">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="TRY">{t('settingsPage.data.currency.options.try')}</SelectItem>
                                    <SelectItem value="USD">{t('settingsPage.data.currency.options.usd')}</SelectItem>
                                    <SelectItem value="EUR">{t('settingsPage.data.currency.options.eur')}</SelectItem>
                                    <SelectItem value="GBP">{t('settingsPage.data.currency.options.gbp')}</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                        <div className="flex items-center justify-between py-2 border-b">
                            <div className="space-y-0.5">
                                <Label>{t('settingsPage.data.timezone.label')}</Label>
                                <p className="text-sm text-muted-foreground">{t('settingsPage.data.timezone.description')}</p>
                            </div>
                            <Select value={timezone} onValueChange={onTimezoneChange}>
                                <SelectTrigger className="w-40">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="Europe/Istanbul">{t('settingsPage.data.timezone.options.istanbul')}</SelectItem>
                                    <SelectItem value="Europe/London">{t('settingsPage.data.timezone.options.london')}</SelectItem>
                                    <SelectItem value="America/New_York">{t('settingsPage.data.timezone.options.newYork')}</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                        <div className="flex items-center justify-between py-2 border-b">
                            <div className="space-y-0.5">
                                <Label>{t('settingsPage.data.autoUpdate.label')}</Label>
                                <p className="text-sm text-muted-foreground">{t('settingsPage.data.autoUpdate.description')}</p>
                            </div>
                            <Switch checked={autoUpdate} onCheckedChange={onAutoUpdateChange} />
                        </div>
                        <div className="flex items-center justify-between py-2 border-b">
                            <div className="space-y-0.5">
                                <Label>{t('settingsPage.data.enableNews.label')}</Label>
                                <p className="text-sm text-muted-foreground">{t('settingsPage.data.enableNews.description')}</p>
                            </div>
                            <Switch checked={enableNews} onCheckedChange={onEnableNewsChange} />
                        </div>
                        <div className="flex items-center justify-between py-2 border-b">
                            <div className="space-y-0.5">
                                <Label>{t('settingsPage.data.refreshRate.label')}</Label>
                                <p className="text-sm text-muted-foreground">{t('settingsPage.data.refreshRate.description')}</p>
                            </div>
                            <Select value={String(refreshRate)} onValueChange={(value) => onRefreshRateChange(parseInt(value, 10))}>
                                <SelectTrigger className="w-40">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="30">{t('settingsPage.data.refreshRate.options.30')}</SelectItem>
                                    <SelectItem value="60">{t('settingsPage.data.refreshRate.options.60')}</SelectItem>
                                    <SelectItem value="300">{t('settingsPage.data.refreshRate.options.300')}</SelectItem>
                                    <SelectItem value="900">{t('settingsPage.data.refreshRate.options.900')}</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                    </div>

                    <div className="space-y-4">
                        <h3 className="text-sm font-medium">{t('settingsPage.sections.notifications')}</h3>
                        <div className="flex items-center justify-between py-2 border-b">
                            <div className="space-y-0.5">
                                <Label>{t('settingsPage.notifications.priceAlerts.label')}</Label>
                                <p className="text-sm text-muted-foreground">{t('settingsPage.notifications.priceAlerts.description')}</p>
                            </div>
                            <Switch
                                checked={notificationSettings.priceAlerts}
                                onCheckedChange={(value: boolean) => onNotificationToggle('priceAlerts', value)}
                            />
                        </div>
                        <div className="flex items-center justify-between py-2 border-b">
                            <div className="space-y-0.5">
                                <Label>{t('settingsPage.notifications.portfolioUpdates.label')}</Label>
                                <p className="text-sm text-muted-foreground">{t('settingsPage.notifications.portfolioUpdates.description')}</p>
                            </div>
                            <Switch
                                checked={notificationSettings.portfolioUpdates}
                                onCheckedChange={(value: boolean) => onNotificationToggle('portfolioUpdates', value)}
                            />
                        </div>
                        <div className="flex items-center justify-between py-2 border-b">
                            <div className="space-y-0.5">
                                <Label>{t('settingsPage.notifications.news.label')}</Label>
                                <p className="text-sm text-muted-foreground">{t('settingsPage.notifications.news.description')}</p>
                            </div>
                            <Switch
                                checked={notificationSettings.emailNotifications}
                                onCheckedChange={(value: boolean) => onNotificationToggle('emailNotifications', value)}
                            />
                        </div>
                        <div className="flex items-center justify-between py-2 border-b">
                            <div className="space-y-0.5">
                                <Label>{t('settingsPage.notifications.sound.label')}</Label>
                                <p className="text-sm text-muted-foreground">{t('settingsPage.notifications.sound.description')}</p>
                            </div>
                            <Switch
                                checked={notificationSettings.pushNotifications}
                                onCheckedChange={(value: boolean) => onNotificationToggle('pushNotifications', value)}
                            />
                        </div>
                    </div>

                    <Button className="w-full mt-4" onClick={onSaveSettings} disabled={isSavingSettings}>
                        {isSavingSettings ? t('common.loading') : t('common.save')}
                    </Button>
                </CardContent>
            </Card>

            {isAdmin && (
                <Card className="border-destructive/50 mt-6">
                    <CardHeader>
                        <CardTitle className="text-destructive">{t('settingsPage.dangerZone.title')}</CardTitle>
                        <CardDescription>{t('settingsPage.dangerZone.description')}</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="flex items-center justify-between py-2">
                            <div className="space-y-0.5">
                                <Label className="text-destructive font-medium">{t('settingsPage.dangerZone.reset.title')}</Label>
                                <p className="text-sm text-muted-foreground">{t('settingsPage.dangerZone.reset.description')}</p>
                            </div>
                            <Dialog>
                                <DialogTrigger asChild>
                                    <Button variant="destructive" size="sm">
                                        <Trash2 className="h-4 w-4 mr-2" />
                                        {t('settingsPage.dangerZone.reset.button')}
                                    </Button>
                                </DialogTrigger>
                                <DialogContent>
                                    <DialogHeader>
                                        <DialogTitle className="text-destructive flex items-center gap-2">
                                            <AlertCircle className="h-5 w-5" />
                                            {t('settingsPage.dangerZone.reset.dialog.title')}
                                        </DialogTitle>
                                        <DialogDescription>{t('settingsPage.dangerZone.reset.dialog.description')}</DialogDescription>
                                    </DialogHeader>
                                    <div className="py-4">
                                        <p className="text-sm text-muted-foreground mb-4">{t('settingsPage.dangerZone.reset.dialog.continue')}</p>
                                        <div className="p-3 bg-destructive/10 border border-destructive/20 rounded-md">
                                            <p className="text-sm font-medium text-destructive">{t('settingsPage.dangerZone.reset.dialog.warningTitle')}</p>
                                            <ul className="text-sm text-muted-foreground mt-2 space-y-1 list-disc list-inside">
                                                {(t('settingsPage.dangerZone.reset.dialog.items', { returnObjects: true }) as unknown as string[]).map((item: string) => (
                                                    <li key={item}>{item}</li>
                                                ))}
                                            </ul>
                                        </div>
                                    </div>
                                    <DialogFooter className="gap-2">
                                        <DialogClose asChild>
                                            <Button variant="outline">{t('common.cancel')}</Button>
                                        </DialogClose>
                                        <Button variant="destructive" onClick={onFullReset}>
                                            <Trash2 className="h-4 w-4 mr-2" />
                                            {t('settingsPage.dangerZone.reset.confirmButton')}
                                        </Button>
                                    </DialogFooter>
                                </DialogContent>
                            </Dialog>
                        </div>

                        <div className="flex items-center justify-between py-2 border-t">
                            <div className="space-y-0.5">
                                <Label className="font-medium">{t('settingsPage.cache.title')}</Label>
                                <p className="text-sm text-muted-foreground">{t('settingsPage.cache.description')}</p>
                            </div>
                            <Button
                                variant="outline"
                                size="sm"
                                disabled={isClearingCache}
                                onClick={onClearCache}
                            >
                                {isClearingCache ? (
                                    <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                                ) : (
                                    <RefreshCw className="h-4 w-4 mr-2" />
                                )}
                                {t('settingsPage.cache.button')}
                            </Button>
                        </div>
                    </CardContent>
                </Card>
            )}
        </>
    )
}
