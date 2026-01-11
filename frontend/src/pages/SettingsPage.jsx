import { useState } from 'react'
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle
} from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue
} from '@/components/ui/select'
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow
} from '@/components/ui/table'
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger
} from '@/components/ui/dialog'
import { Switch } from '@/components/ui/switch'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Trash2, Plus, Key, RefreshCw, AlertCircle, Pencil, CheckCircle } from 'lucide-react'
import {
    useGetApiConfigsQuery,
    useAddApiConfigMutation,
    useDeleteApiConfigMutation,
    useTestApiKeyMutation
} from '@/store/api/settingsApi'
import { toast } from 'sonner'

// Provider Capabilities Info
import { useTranslation } from 'react-i18next'

export default function SettingsPage() {
    const { t } = useTranslation()
    const { data: configsData, isLoading, refetch } = useGetApiConfigsQuery()
    const [addConfig, { isLoading: isAdding }] = useAddApiConfigMutation()
    const [deleteConfig, { isLoading: isDeleting }] = useDeleteApiConfigMutation()
    const [testApiKey, { isLoading: isTesting }] = useTestApiKeyMutation()

    const [isDialogOpen, setIsDialogOpen] = useState(false)
    const [editingConfig, setEditingConfig] = useState(null) // null = new, object = editing
    const [isValidated, setIsValidated] = useState(false) // API key validated?
    const [formData, setFormData] = useState({
        provider: 'ALPHA_VANTAGE',
        apiKey: '',
        secretKey: '',
        baseUrl: '',
        isActive: true
    })

    // Provider Capabilities Info
    const PROVIDER_INFO = {
        YAHOO_FINANCE: {
            title: t('settings.providers.info.YAHOO_FINANCE.title'),
            description: t('settings.providers.info.YAHOO_FINANCE.desc'),
            supported: t('settings.providers.info.YAHOO_FINANCE.supported', { returnObjects: true }),
            missing: t('settings.providers.info.YAHOO_FINANCE.missing', { returnObjects: true }),
            color: "bg-blue-50 text-blue-900 border-blue-200"
        },
        ALPHA_VANTAGE: {
            title: t('settings.providers.info.ALPHA_VANTAGE.title'),
            description: t('settings.providers.info.ALPHA_VANTAGE.desc'),
            supported: t('settings.providers.info.ALPHA_VANTAGE.supported', { returnObjects: true }),
            missing: t('settings.providers.info.ALPHA_VANTAGE.missing', { returnObjects: true }),
            color: "bg-yellow-50 text-yellow-900 border-yellow-200"
        },
        FINNHUB: {
            title: t('settings.providers.info.FINNHUB.title'),
            description: t('settings.providers.info.FINNHUB.desc'),
            supported: t('settings.providers.info.FINNHUB.supported', { returnObjects: true }),
            missing: t('settings.providers.info.FINNHUB.missing', { returnObjects: true }),
            color: "bg-orange-50 text-orange-900 border-orange-200"
        },
        TCMB: {
            title: t('settings.providers.info.TCMB.title'),
            description: t('settings.providers.info.TCMB.desc'),
            supported: t('settings.providers.info.TCMB.supported', { returnObjects: true }),
            missing: t('settings.providers.info.TCMB.missing', { returnObjects: true }),
            color: "bg-slate-50 text-slate-900 border-slate-200"
        }
    }

    // Safe access to the list from the response wrapper
    const apiConfigs = configsData?.data || []

    const resetForm = () => {
        setFormData({
            provider: 'ALPHA_VANTAGE',
            apiKey: '',
            secretKey: '',
            baseUrl: '',
            isActive: true
        })
        setEditingConfig(null)
        setIsValidated(false)
    }

    const handleOpenDialog = (config = null) => {
        if (config) {
            // Edit mode
            setEditingConfig(config)
            setFormData({
                provider: config.provider,
                apiKey: '', // Don't show masked key, user must enter new one
                secretKey: '',
                baseUrl: config.baseUrl || '',
                isActive: config.isActive
            })
        } else {
            // New mode
            resetForm()
        }
        setIsDialogOpen(true)
    }

    const handleTestKey = async () => {
        // TCMB validation doesn't need a key
        if (formData.provider !== 'TCMB' && !formData.apiKey.trim()) {
            toast.error(t('settings.apiKeys.validation.required'))
            return
        }

        // Ensure TCMB uses a placeholder if empty
        const dataToTest = {
            ...formData,
            apiKey: (formData.provider === 'TCMB' && !formData.apiKey.trim()) ? 'TCMB_PUBLIC' : formData.apiKey
        }

        try {
            const result = await testApiKey(dataToTest).unwrap()
            if (result.success && result.data?.valid) {
                toast.success(result.data.message || t('settings.apiKeys.valid'))
                setIsValidated(true)
            } else {
                toast.error(result.message || result.data?.message || t('settings.apiKeys.invalid'))
                setIsValidated(false)
            }
        } catch (error) {
            console.error('Test failed:', error)
            toast.error(error.data?.message || t('settings.apiKeys.invalid'))
            setIsValidated(false)
        }
    }

    const handleAddSubmit = async (e) => {
        e.preventDefault()

        if (!isValidated) {
            toast.error(t('settings.apiKeys.validation.testRequired'))
            return
        }

        try {
            await addConfig(formData).unwrap()
            toast.success(editingConfig ? t('settings.apiKeys.update') : t('settings.apiKeys.save'))
            setIsDialogOpen(false)
            resetForm()
        } catch (error) {
            console.error('Failed to add config:', error)
            toast.error(error.data?.message || t('common.error'))
        }
    }

    const handleDelete = async (id) => {
        if (confirm(t('settings.apiKeys.deleteConfirm'))) {
            try {
                await deleteConfig(id).unwrap()
                toast.success(t('settings.apiKeys.delete'))
            } catch (error) {
                toast.error(t('common.error'))
            }
        }
    }

    const getProviderLabel = (provider) => {
        // We can just return the title from PROVIDER_INFO if it matches
        if (PROVIDER_INFO[provider]) {
            return PROVIDER_INFO[provider].title
        }
        return provider
    }

    return (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div>
                <h1 className="text-2xl font-bold">{t('settings.apiKeys.title')}</h1>
                <p className="text-muted-foreground">
                    {t('settings.apiKeys.manage')}
                </p>
            </div>

            <Tabs defaultValue="api-keys" className="space-y-4">
                <TabsList>
                    <TabsTrigger value="general">Genel</TabsTrigger>
                    <TabsTrigger value="api-keys">{t('settings.apiKeys.title')}</TabsTrigger>
                </TabsList>

                <TabsContent value="general">
                    <Card>
                        <CardHeader>
                            <CardTitle>Genel Ayarlar</CardTitle>
                            <CardDescription>Uygulama genel davranışlarını özelleştirin.</CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-6">
                            {/* Theme Settings */}
                            <div className="space-y-4">
                                <h3 className="text-sm font-medium">Görünüm</h3>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>Tema</Label>
                                        <p className="text-sm text-muted-foreground">Uygulama temasını seçin</p>
                                    </div>
                                    <Select defaultValue="dark">
                                        <SelectTrigger className="w-40">
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="light">Açık</SelectItem>
                                            <SelectItem value="dark">Koyu</SelectItem>
                                            <SelectItem value="system">Sistem</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>Dil</Label>
                                        <p className="text-sm text-muted-foreground">Arayüz dilini seçin</p>
                                    </div>
                                    <Select defaultValue="tr">
                                        <SelectTrigger className="w-40">
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="tr">Türkçe</SelectItem>
                                            <SelectItem value="en">English</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                            </div>

                            {/* Data Settings */}
                            <div className="space-y-4">
                                <h3 className="text-sm font-medium">Veri Ayarları</h3>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>Varsayılan Para Birimi</Label>
                                        <p className="text-sm text-muted-foreground">Portföy ve analizlerde kullanılacak para birimi</p>
                                    </div>
                                    <Select defaultValue="TRY">
                                        <SelectTrigger className="w-40">
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="TRY">₺ Türk Lirası</SelectItem>
                                            <SelectItem value="USD">$ Amerikan Doları</SelectItem>
                                            <SelectItem value="EUR">€ Euro</SelectItem>
                                            <SelectItem value="GBP">£ İngiliz Sterlini</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>Zaman Dilimi</Label>
                                        <p className="text-sm text-muted-foreground">Verilerin görüntüleneceği zaman dilimi</p>
                                    </div>
                                    <Select defaultValue="Europe/Istanbul">
                                        <SelectTrigger className="w-40">
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="Europe/Istanbul">İstanbul (UTC+3)</SelectItem>
                                            <SelectItem value="Europe/London">Londra (UTC+0)</SelectItem>
                                            <SelectItem value="America/New_York">New York (UTC-5)</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>

                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>Otomatik Veri Güncelleme</Label>
                                        <p className="text-sm text-muted-foreground">Piyasa verilerini arka planda otomatik güncelle</p>
                                    </div>
                                    <Switch defaultChecked />
                                </div>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>Güncelleme Sıklığı</Label>
                                        <p className="text-sm text-muted-foreground">Veri yenileme aralığı</p>
                                    </div>
                                    <Select defaultValue="60">
                                        <SelectTrigger className="w-40">
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="30">30 saniye</SelectItem>
                                            <SelectItem value="60">1 dakika</SelectItem>
                                            <SelectItem value="300">5 dakika</SelectItem>
                                            <SelectItem value="900">15 dakika</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                            </div>

                            {/* Notification Settings */}
                            <div className="space-y-4">
                                <h3 className="text-sm font-medium">Bildirim Ayarları</h3>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>Fiyat Alarmları</Label>
                                        <p className="text-sm text-muted-foreground">Hedef fiyata ulaşınca bildir</p>
                                    </div>
                                    <Switch defaultChecked />
                                </div>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>Portföy Güncellemeleri</Label>
                                        <p className="text-sm text-muted-foreground">Önemli değer değişikliklerinde bildir</p>
                                    </div>
                                    <Switch defaultChecked />
                                </div>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>Haber Bildirimleri</Label>
                                        <p className="text-sm text-muted-foreground">İlgilendiğiniz konularda haber çıkınca bildir</p>
                                    </div>
                                    <Switch defaultChecked />
                                </div>
                                <div className="flex items-center justify-between py-2 border-b">
                                    <div className="space-y-0.5">
                                        <Label>Sesli Bildirimler</Label>
                                        <p className="text-sm text-muted-foreground">Bildirimler için ses çal</p>
                                    </div>
                                    <Switch />
                                </div>
                            </div>

                            <Button className="w-full mt-4" onClick={() => toast.success(t('success.saved'))}>
                                {t('settings.apiKeys.save')}
                            </Button>
                        </CardContent>
                    </Card>
                </TabsContent>

                <TabsContent value="api-keys">
                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between">
                            <div>
                                <CardTitle>{t('settings.apiKeys.title')}</CardTitle>
                                <CardDescription>
                                    {t('settings.apiKeys.description')}
                                </CardDescription>
                            </div>
                            <Dialog open={isDialogOpen} onOpenChange={(open) => {
                                if (!open) resetForm()
                                setIsDialogOpen(open)
                            }}>
                                <DialogTrigger asChild>
                                    <Button onClick={() => handleOpenDialog(null)}>
                                        <Plus className="h-4 w-4 mr-2" />
                                        {t('settings.apiKeys.add')}
                                    </Button>
                                </DialogTrigger>
                                <DialogContent>
                                    <DialogHeader>
                                        <DialogTitle>
                                            {editingConfig ? t('settings.apiKeys.edit') : t('settings.apiKeys.add')}
                                        </DialogTitle>
                                        <DialogDescription>
                                            {editingConfig
                                                ? t('settings.apiKeys.editDescription')
                                                : t('settings.apiKeys.addDescription')}
                                        </DialogDescription>
                                    </DialogHeader>
                                    <form onSubmit={handleAddSubmit} className="space-y-4 py-4">
                                        <div className="space-y-2">
                                            <Label>{t('settings.apiKeys.provider')}</Label>
                                            <Select
                                                value={formData.provider}
                                                onValueChange={(val) => {
                                                    setFormData({ ...formData, provider: val })
                                                    setIsValidated(false)
                                                }}
                                                disabled={!!editingConfig}
                                            >
                                                <SelectTrigger>
                                                    <SelectValue />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    <SelectItem value="ALPHA_VANTAGE">Alpha Vantage</SelectItem>
                                                    <SelectItem value="YAHOO_FINANCE">Yahoo Finance</SelectItem>
                                                    <SelectItem value="FINNHUB">Finnhub</SelectItem>
                                                    <SelectItem value="TCMB">{t('settings.providers.info.TCMB.title')}</SelectItem>
                                                    <SelectItem value="OTHER">{t('settings.apiKeys.providerOther')}</SelectItem>
                                                </SelectContent>
                                            </Select>

                                            {/* Provider Info Alert */}
                                            {formData.provider && PROVIDER_INFO[formData.provider] && (
                                                <div className={`text-sm p-3 rounded-md border ${PROVIDER_INFO[formData.provider].color}`}>
                                                    <p className="font-semibold mb-1">{PROVIDER_INFO[formData.provider].title}</p>
                                                    <p className="mb-2 text-xs opacity-90">{PROVIDER_INFO[formData.provider].description}</p>

                                                    <div className="grid grid-cols-2 gap-2 text-xs">
                                                        <div>
                                                            <span className="font-semibold block mb-0.5">{t('settings.providers.supported')}</span>
                                                            <ul className="list-disc list-inside opacity-80 space-y-0.5">
                                                                {PROVIDER_INFO[formData.provider].supported.map((item, i) => (
                                                                    <li key={i}>{item}</li>
                                                                ))}
                                                            </ul>
                                                        </div>
                                                        {PROVIDER_INFO[formData.provider].missing.length > 0 && (
                                                            <div>
                                                                <span className="font-semibold block mb-0.5">{t('settings.providers.missing')}</span>
                                                                <ul className="list-disc list-inside opacity-80 space-y-0.5">
                                                                    {PROVIDER_INFO[formData.provider].missing.map((item, i) => (
                                                                        <li key={i}>{item}</li>
                                                                    ))}
                                                                </ul>
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>
                                            )}
                                        </div>

                                        <div className="space-y-2">
                                            <Label>{t('settings.apiKeys.apiKey')}</Label>
                                            <div className="flex gap-2">
                                                <Input
                                                    value={formData.apiKey}
                                                    onChange={(e) => {
                                                        setFormData({ ...formData, apiKey: e.target.value })
                                                        setIsValidated(false)
                                                    }}
                                                    placeholder={formData.provider === 'TCMB' ? t('settings.apiKeys.placeholder.tcmb') : t('settings.apiKeys.placeholder.key')}
                                                    required={formData.provider !== 'TCMB'}
                                                    className={isValidated ? 'border-green-500' : ''}
                                                />
                                                <Button
                                                    type="button"
                                                    variant={isValidated ? 'success' : 'outline'}
                                                    onClick={handleTestKey}
                                                    disabled={isTesting || (formData.provider !== 'TCMB' && !formData.apiKey.trim())}
                                                >
                                                    {isTesting ? (
                                                        <RefreshCw className="h-4 w-4 animate-spin" />
                                                    ) : isValidated ? (
                                                        <><CheckCircle className="h-4 w-4 mr-1" /> {t('settings.apiKeys.valid')}</>
                                                    ) : (
                                                        t('settings.apiKeys.test')
                                                    )}
                                                </Button>
                                            </div>
                                            {!isValidated && (
                                                <p className="text-xs text-muted-foreground">
                                                    {formData.provider === 'TCMB'
                                                        ? t('settings.apiKeys.validation.tcmbInfo')
                                                        : t('settings.apiKeys.validation.testRequired')}
                                                </p>
                                            )}
                                        </div>

                                        <div className="space-y-2">
                                            <Label>{t('settings.apiKeys.secretKey')}</Label>
                                            <Input
                                                type="password"
                                                value={formData.secretKey}
                                                onChange={(e) => setFormData({ ...formData, secretKey: e.target.value })}
                                                placeholder={t('settings.apiKeys.placeholder.secretKey')}
                                            />
                                        </div>

                                        <div className="space-y-2">
                                            <Label>{t('settings.apiKeys.baseUrl')}</Label>
                                            <Input
                                                value={formData.baseUrl}
                                                onChange={(e) => setFormData({ ...formData, baseUrl: e.target.value })}
                                                placeholder={t('settings.apiKeys.placeholder.baseUrl')}
                                            />
                                        </div>

                                        <div className="flex items-center space-x-2 pt-2">
                                            <Switch
                                                checked={formData.isActive}
                                                onCheckedChange={(val) => setFormData({ ...formData, isActive: val })}
                                            />
                                            <Label>{t('settings.apiKeys.active')}</Label>
                                        </div>

                                        <DialogFooter>
                                            <Button
                                                type="submit"
                                                disabled={isAdding || !isValidated}
                                                className={!isValidated ? 'opacity-50' : ''}
                                            >
                                                {isAdding ? t('common.loading') : (editingConfig ? t('settings.apiKeys.update') : t('settings.apiKeys.save'))}
                                            </Button>
                                        </DialogFooter>
                                    </form>
                                </DialogContent>
                            </Dialog>
                        </CardHeader>
                        <CardContent>
                            {isLoading ? (
                                <div className="flex items-center justify-center p-8 text-muted-foreground">
                                    <RefreshCw className="h-6 w-6 animate-spin mr-2" />
                                    {t('common.loading')}
                                </div>
                            ) : apiConfigs.length === 0 ? (
                                <div className="text-center p-8 border-2 border-dashed rounded-lg text-muted-foreground">
                                    <Key className="h-10 w-10 mx-auto mb-3 opacity-50" />
                                    <p>{t('settings.apiKeys.noKeys')}</p>
                                    <Button variant="link" onClick={() => setIsDialogOpen(true)}>
                                        {t('settings.apiKeys.addFirstKey')}
                                    </Button>
                                </div>
                            ) : (
                                <div className="border rounded-md">
                                    <Table>
                                        <TableHeader>
                                            <TableRow>
                                                <TableHead>{t('settings.apiKeys.table.provider')}</TableHead>
                                                <TableHead>{t('settings.apiKeys.table.key')}</TableHead>
                                                <TableHead>{t('settings.apiKeys.table.status')}</TableHead>
                                                <TableHead>{t('settings.apiKeys.table.createdAt')}</TableHead>
                                                <TableHead className="text-right">{t('settings.apiKeys.table.actions')}</TableHead>
                                            </TableRow>
                                        </TableHeader>
                                        <TableBody>
                                            {apiConfigs.map((config) => (
                                                <TableRow key={config.id}>
                                                    <TableCell className="font-medium">
                                                        {getProviderLabel(config.provider)}
                                                    </TableCell>
                                                    <TableCell className="font-mono text-xs">
                                                        {config.apiKey}
                                                    </TableCell>
                                                    <TableCell>
                                                        <Badge variant={config.isActive ? "success" : "secondary"}>
                                                            {config.isActive ? t('settings.apiKeys.active') : t('settings.apiKeys.inactive')}
                                                        </Badge>
                                                    </TableCell>
                                                    <TableCell className="text-muted-foreground text-sm">
                                                        {new Date(config.createdAt).toLocaleDateString()}
                                                    </TableCell>
                                                    <TableCell className="text-right space-x-1">
                                                        <Button
                                                            variant="ghost"
                                                            size="icon"
                                                            className="text-primary hover:text-primary hover:bg-primary/10"
                                                            onClick={() => handleOpenDialog(config)}
                                                            title={t('common.edit')}
                                                        >
                                                            <Pencil className="h-4 w-4" />
                                                        </Button>
                                                        <Button
                                                            variant="ghost"
                                                            size="icon"
                                                            className="text-destructive hover:text-destructive hover:bg-destructive/10"
                                                            onClick={() => handleDelete(config.id)}
                                                            title="Sil"
                                                        >
                                                            <Trash2 className="h-4 w-4" />
                                                        </Button>
                                                    </TableCell>
                                                </TableRow>
                                            ))}
                                        </TableBody>
                                    </Table>
                                </div>
                            )}
                        </CardContent>
                    </Card>
                </TabsContent>
            </Tabs >
        </div >
    )
}
