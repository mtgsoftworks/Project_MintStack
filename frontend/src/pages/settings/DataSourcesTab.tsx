import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle
} from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue
} from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import RefreshButton from '@/components/common/RefreshButton'
import { Database, History, Key, Loader2, Zap } from 'lucide-react'
import { DATA_SOURCE_TYPES } from './providerInfo'

const BACKFILL_TYPES = [
    { value: 'STOCK', label: 'Hisse' },
    { value: 'FUND', label: 'Fon' },
    { value: 'CURRENCY', label: 'Doviz' },
    { value: 'INDEX', label: 'Endeks' },
    { value: 'BOND', label: 'Tahvil/Bono' },
    { value: 'VIOP', label: 'VIOP' },
]

export function DataSourcesTab({
    t,
    apiConfigs,
    preferencesData,
    isRefreshing,
    isAdmin,
    backfillForm,
    isBackfillingMarketData,
    getProviderLabel,
    onSelectDataPreference,
    onRefreshData,
    onOpenApiKeysTab,
    onBackfillFormChange,
    onToggleBackfillType,
    onBackfillMarketData
}) {
    const hasApiConfigs = Boolean(apiConfigs?.length)
    const renderBackfillPanel = () => {
        if (!isAdmin) {
            return null
        }

        return (
            <div
                className="mt-6 rounded-xl border border-dashed bg-muted/20 p-4"
                aria-busy={isBackfillingMarketData}
            >
                <div className="mb-4 flex items-start gap-3">
                    <div className="rounded-lg bg-primary/10 p-2 text-primary">
                        <History className="h-5 w-5" />
                    </div>
                    <div>
                        <h3 className="font-semibold">Gecmis Veri Backfill</h3>
                        <p className="text-sm text-muted-foreground">
                            Yahoo hisse/endeks, TEFAS fon ve TCMB doviz gecmisini price_history tablosuna yazar. Tahvil/VIOP icin kaynak yoksa sentetik seri kullanilir.
                        </p>
                    </div>
                </div>

                <div className="grid gap-4 md:grid-cols-3">
                    <div className="space-y-2">
                        <Label>Donem</Label>
                        <Select
                            value={backfillForm.days}
                            onValueChange={(value) => onBackfillFormChange('days', value)}
                            disabled={isBackfillingMarketData}
                        >
                            <SelectTrigger>
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="7">7 Gun</SelectItem>
                                <SelectItem value="30">1 Ay</SelectItem>
                                <SelectItem value="90">3 Ay</SelectItem>
                                <SelectItem value="365">1 Yil</SelectItem>
                            </SelectContent>
                        </Select>
                    </div>
                    <div className="space-y-2">
                        <Label>Enstruman Limiti</Label>
                        <Input
                            type="number"
                            min="1"
                            max="500"
                            value={backfillForm.maxInstruments}
                            onChange={(event) => onBackfillFormChange('maxInstruments', event.target.value)}
                            disabled={isBackfillingMarketData}
                        />
                    </div>
                    <div className="space-y-2">
                        <Label>Opsiyonel Semboller</Label>
                        <Input
                            value={backfillForm.symbols}
                            onChange={(event) => onBackfillFormChange('symbols', event.target.value)}
                            placeholder="THYAO,ASELS,USDTRY"
                            disabled={isBackfillingMarketData}
                        />
                    </div>
                </div>

                <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                    {BACKFILL_TYPES.map((type) => (
                        <label key={type.value} className="flex items-center gap-2 rounded-lg border bg-background px-3 py-2 text-sm">
                            <Checkbox
                                checked={backfillForm.instrumentTypes.includes(type.value)}
                                onCheckedChange={(checked) => onToggleBackfillType(type.value, Boolean(checked))}
                                disabled={isBackfillingMarketData}
                            />
                            {type.label}
                        </label>
                    ))}
                </div>

                <label className="mt-4 flex items-center gap-2 text-sm">
                    <Checkbox
                        checked={backfillForm.includeSyntheticFallback}
                        onCheckedChange={(checked) => onBackfillFormChange('includeSyntheticFallback', Boolean(checked))}
                        disabled={isBackfillingMarketData}
                    />
                    Kaynak eksikse sentetik fallback ile grafik/analiz bos kalmasin
                </label>

                {isBackfillingMarketData && (
                    <div className="mt-4 flex items-center gap-3 rounded-lg border border-primary/20 bg-primary/5 px-4 py-3 text-sm text-primary">
                        <Loader2 className="h-4 w-4 animate-spin" />
                        <div>
                            <p className="font-medium">Gecmis veri dolduruluyor</p>
                            <p className="text-xs text-muted-foreground">
                                Secilen kaynaklardan fiyat gecmisi cekiliyor; bu islem bir kac dakika surebilir.
                            </p>
                        </div>
                    </div>
                )}

                <div className="mt-4 flex justify-end">
                    <Button
                        onClick={onBackfillMarketData}
                        disabled={isBackfillingMarketData}
                    >
                        {isBackfillingMarketData ? (
                            <>
                                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                                Dolduruluyor...
                            </>
                        ) : (
                            'Gecmis Veriyi Doldur'
                        )}
                    </Button>
                </div>
            </div>
        )
    }

    return (
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2">
                    <Database className="h-5 w-5" />
                    {t('settings.dataSources.title', { defaultValue: 'Veri Kaynaklari' })}
                </CardTitle>
                <CardDescription>
                    {t('settings.dataSources.description', { defaultValue: 'Her veri turu icin kaynak secin' })}
                </CardDescription>
            </CardHeader>
            <CardContent>
                {!hasApiConfigs ? (
                    <div className="text-center p-8 border-2 border-dashed rounded-lg text-muted-foreground">
                        <Key className="h-10 w-10 mx-auto mb-3 opacity-50" />
                        <p className="mb-2">{t('settings.dataSources.noApiKeys', { defaultValue: 'Once API anahtari ekleyin' })}</p>
                        <Button variant="link" onClick={onOpenApiKeysTab}>
                            {t('settings.apiKeys.add')}
                        </Button>
                    </div>
                ) : (
                    <div className="space-y-6">
                        {DATA_SOURCE_TYPES.map((dataType) => {
                            const currentPreference = preferencesData?.data?.find((item) => item.dataType === dataType.type)
                            const availableProviders = apiConfigs.filter((config) =>
                                config.isActive && dataType.providers.includes(config.provider)
                            )

                            return (
                                <div key={dataType.type} className="flex items-center justify-between py-4 border-b last:border-0">
                                    <div className="flex items-center gap-3">
                                        <div>
                                            <Label className="text-base font-medium">{dataType.label}</Label>
                                            <p className="text-sm text-muted-foreground">
                                                {availableProviders.length > 0
                                                    ? `${availableProviders.length} kaynak mevcut`
                                                    : 'Kaynak yok - API anahtari ekleyin'}
                                            </p>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-2">
                                        {availableProviders.length > 0 ? (
                                            <Select
                                                value={currentPreference?.provider || ''}
                                                onValueChange={(provider) => onSelectDataPreference(dataType, provider)}
                                            >
                                                <SelectTrigger className="w-48">
                                                    <SelectValue placeholder={t('settings.dataSources.selectSource')} />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    {availableProviders.map((config) => (
                                                        <SelectItem key={`${dataType.type}-${config.provider}`} value={config.provider}>
                                                            {getProviderLabel(config.provider)}
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                        ) : (
                                            <Badge variant="secondary">
                                                {t('settings.dataSources.noProvider', { defaultValue: 'Kaynak yok' })}
                                            </Badge>
                                        )}
                                        {currentPreference && (
                                            <Badge variant="success" className="ml-2">
                                                <Zap className="h-3 w-3 mr-1" />
                                                {t('settings.dataSources.active')}
                                            </Badge>
                                        )}
                                    </div>
                                </div>
                            )
                        })}

                        <div className="pt-4 border-t">
                            <RefreshButton
                                variant="outline"
                                onRefresh={onRefreshData}
                                isLoading={isRefreshing}
                                disabled={!apiConfigs.some((config) => config.isActive)}
                                className="group"
                            >
                                {t('settings.dataSources.refreshNow', { defaultValue: 'Verileri Simdi Guncelle' })}
                            </RefreshButton>
                        </div>
                    </div>
                )}
                {renderBackfillPanel()}
            </CardContent>
        </Card>
    )
}
