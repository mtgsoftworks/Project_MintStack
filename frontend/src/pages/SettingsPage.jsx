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
import { Trash2, Plus, Key, RefreshCw, AlertCircle } from 'lucide-react'
import {
    useGetApiConfigsQuery,
    useAddApiConfigMutation,
    useDeleteApiConfigMutation
} from '@/store/api/settingsApi'
import { toast } from 'sonner'

export default function SettingsPage() {
    const { data: configsData, isLoading, refetch } = useGetApiConfigsQuery()
    const [addConfig, { isLoading: isAdding }] = useAddApiConfigMutation()
    const [deleteConfig, { isLoading: isDeleting }] = useDeleteApiConfigMutation()

    const [isDialogOpen, setIsDialogOpen] = useState(false)
    const [formData, setFormData] = useState({
        provider: 'YAHOO_FINANCE',
        apiKey: '',
        secretKey: '',
        baseUrl: '',
        isActive: true
    })

    // Safe access to the list from the response wrapper
    const apiConfigs = configsData?.data || []

    const handleAddSubmit = async (e) => {
        e.preventDefault()
        try {
            await addConfig(formData).unwrap()
            toast.success('API anahtarı başarıyla eklendi')
            setIsDialogOpen(false)
            setFormData({
                provider: 'YAHOO_FINANCE',
                apiKey: '',
                secretKey: '',
                baseUrl: '',
                isActive: true
            })
        } catch (error) {
            console.error('Failed to add config:', error)
            toast.error('API anahtarı eklenirken hata oluştu')
        }
    }

    const handleDelete = async (id) => {
        if (confirm('Bu API anahtarını silmek istediğinize emin misiniz?')) {
            try {
                await deleteConfig(id).unwrap()
                toast.success('API anahtarı silindi')
            } catch (error) {
                toast.error('Silme işlemi başarısız oldu')
            }
        }
    }

    const getProviderLabel = (provider) => {
        switch (provider) {
            case 'YAHOO_FINANCE': return 'Yahoo Finance'
            case 'ALPHA_VANTAGE': return 'Alpha Vantage'
            case 'FINNHUB': return 'Finnhub'
            case 'OTHER': return 'Diğer'
            default: return provider
        }
    }

    return (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div>
                <h1 className="text-2xl font-bold">Ayarlar</h1>
                <p className="text-muted-foreground">
                    Uygulama yapılandırması ve dış servis bağlantıları
                </p>
            </div>

            <Tabs defaultValue="api-keys" className="space-y-4">
                <TabsList>
                    <TabsTrigger value="general">Genel</TabsTrigger>
                    <TabsTrigger value="api-keys">API Anahtarları</TabsTrigger>
                </TabsList>

                <TabsContent value="general">
                    <Card>
                        <CardHeader>
                            <CardTitle>Genel Ayarlar</CardTitle>
                            <CardDescription>Uygulama genel davranışlarını özelleştirin.</CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            <div className="flex items-center justify-between py-2 border-b">
                                <div className="space-y-0.5">
                                    <Label>Otomatik Veri Güncelleme</Label>
                                    <p className="text-sm text-muted-foreground">Piyasa verilerini arka planda otomatik güncelle</p>
                                </div>
                                <Switch defaultChecked disabled />
                            </div>
                            <div className="p-4 bg-muted/50 rounded-lg flex items-center gap-3 text-muted-foreground text-sm">
                                <AlertCircle className="h-5 w-5" />
                                <span>Daha fazla genel ayar yakında eklenecek.</span>
                            </div>
                        </CardContent>
                    </Card>
                </TabsContent>

                <TabsContent value="api-keys">
                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between">
                            <div>
                                <CardTitle>Harici API Yapılandırması</CardTitle>
                                <CardDescription>
                                    Piyasa verilerini çekmek için kullanılan servislerin anahtarlarını yönetin.
                                </CardDescription>
                            </div>
                            <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
                                <DialogTrigger asChild>
                                    <Button>
                                        <Plus className="h-4 w-4 mr-2" />
                                        Yeni Ekle
                                    </Button>
                                </DialogTrigger>
                                <DialogContent>
                                    <DialogHeader>
                                        <DialogTitle>Yeni API Anahtarı Ekle</DialogTitle>
                                        <DialogDescription>
                                            Sağlayıcı seçin ve anahtar bilgilerinizi girin.
                                        </DialogDescription>
                                    </DialogHeader>
                                    <form onSubmit={handleAddSubmit} className="space-y-4 py-4">
                                        <div className="space-y-2">
                                            <Label>Sağlayıcı</Label>
                                            <Select
                                                value={formData.provider}
                                                onValueChange={(val) => setFormData({ ...formData, provider: val })}
                                            >
                                                <SelectTrigger>
                                                    <SelectValue />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    <SelectItem value="YAHOO_FINANCE">Yahoo Finance</SelectItem>
                                                    <SelectItem value="ALPHA_VANTAGE">Alpha Vantage</SelectItem>
                                                    <SelectItem value="FINNHUB">Finnhub</SelectItem>
                                                    <SelectItem value="OTHER">Diğer</SelectItem>
                                                </SelectContent>
                                            </Select>
                                        </div>

                                        <div className="space-y-2">
                                            <Label>API Key</Label>
                                            <Input
                                                value={formData.apiKey}
                                                onChange={(e) => setFormData({ ...formData, apiKey: e.target.value })}
                                                placeholder="Anahtarınızı buraya yapıştırın"
                                                required
                                            />
                                        </div>

                                        <div className="space-y-2">
                                            <Label>Secret Key (Opsiyonel)</Label>
                                            <Input
                                                type="password"
                                                value={formData.secretKey}
                                                onChange={(e) => setFormData({ ...formData, secretKey: e.target.value })}
                                                placeholder="Varsa gizli anahtar"
                                            />
                                        </div>

                                        <div className="space-y-2">
                                            <Label>Base URL (Opsiyonel)</Label>
                                            <Input
                                                value={formData.baseUrl}
                                                onChange={(e) => setFormData({ ...formData, baseUrl: e.target.value })}
                                                placeholder="https://api.example.com"
                                            />
                                        </div>

                                        <div className="flex items-center space-x-2 pt-2">
                                            <Switch
                                                checked={formData.isActive}
                                                onCheckedChange={(val) => setFormData({ ...formData, isActive: val })}
                                            />
                                            <Label>Aktif</Label>
                                        </div>

                                        <DialogFooter>
                                            <Button type="submit" disabled={isAdding}>
                                                {isAdding ? 'Kaydediliyor...' : 'Kaydet'}
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
                                    Yükleniyor...
                                </div>
                            ) : apiConfigs.length === 0 ? (
                                <div className="text-center p-8 border-2 border-dashed rounded-lg text-muted-foreground">
                                    <Key className="h-10 w-10 mx-auto mb-3 opacity-50" />
                                    <p>Henüz eklenmiş bir API anahtarı yok.</p>
                                    <Button variant="link" onClick={() => setIsDialogOpen(true)}>
                                        İlk anahtarınızı ekleyin
                                    </Button>
                                </div>
                            ) : (
                                <div className="border rounded-md">
                                    <Table>
                                        <TableHeader>
                                            <TableRow>
                                                <TableHead>Sağlayıcı</TableHead>
                                                <TableHead>Anahtar</TableHead>
                                                <TableHead>Durum</TableHead>
                                                <TableHead>Oluşturulma</TableHead>
                                                <TableHead className="text-right">İşlemler</TableHead>
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
                                                            {config.isActive ? "Aktif" : "Pasif"}
                                                        </Badge>
                                                    </TableCell>
                                                    <TableCell className="text-muted-foreground text-sm">
                                                        {new Date(config.createdAt).toLocaleDateString()}
                                                    </TableCell>
                                                    <TableCell className="text-right">
                                                        <Button
                                                            variant="ghost"
                                                            size="icon"
                                                            className="text-destructive hover:text-destructive hover:bg-destructive/10"
                                                            onClick={() => handleDelete(config.id)}
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
            </Tabs>
        </div>
    )
}
