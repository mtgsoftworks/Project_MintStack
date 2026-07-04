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
import type { ChangeEvent } from 'react'
import { Badge } from '@/components/ui/badge'
import RefreshButton from '@/components/common/RefreshButton'
import { Database, History, Key, Loader2, Zap, AlertTriangle } from 'lucide-react'
import { cn } from '@/lib/utils'
import { DATA_SOURCE_TYPES } from './providerInfo'
import type { ApiKeyConfig, BackfillFormData, DataPreference, DataSourceType } from './types'

export interface DataSourcesTabProps {
    t: (key: string, options?: Record<string, unknown>) => string
    apiConfigs: ApiKeyConfig[]
    preferencesData?: { data: DataPreference[] }
    isRefreshing: boolean
    isAdmin: boolean
    backfillForm: BackfillFormData
    isBackfillingMarketData: boolean
    getProviderLabel: (provider: string) => string
    onSelectDataPreference: (dataType: DataSourceType, provider: string) => void
    onRefreshData: () => void
    onOpenApiKeysTab: () => void
    onBackfillFormChange: (field: string, value: string) => void
    onToggleBackfillType: (type: string, checked: boolean) => void
    onBackfillMarketData: () => void
}

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
}: DataSourcesTabProps) {
    const hasApiConfigs = Boolean(apiConfigs?.length)
    const TYPE_PROVIDERS_MAP: Record<string, { labelKey: string; defaultLabel: string; providers: string[]; hint: string }> = {
        STOCK: { labelKey: 'settings.dataSources.backfill.types.stock', defaultLabel: 'Hisse Senetleri', providers: ['ALPHA_VANTAGE', 'YAHOO_FINANCE', 'FINNHUB', 'BIST_DATASTORE', 'FINTABLES'], hint: 'Alpha Vantage / Yahoo / Finnhub' },
        FUND: { labelKey: 'settings.dataSources.backfill.types.fund', defaultLabel: 'Yatırım Fonları', providers: ['TEFAS', 'YAHOO_FINANCE'], hint: 'TEFAS / Yahoo Finance' },
        CURRENCY: { labelKey: 'settings.dataSources.backfill.types.currency', defaultLabel: 'Döviz Kurları', providers: ['TCMB', 'YAHOO_FINANCE', 'ALPHA_VANTAGE'], hint: 'TCMB / Yahoo / Alpha Vantage' },
        INDEX: { labelKey: 'settings.dataSources.backfill.types.index', defaultLabel: 'Endeksler', providers: ['YAHOO_FINANCE', 'ALPHA_VANTAGE', 'BIST_DATASTORE'], hint: 'Yahoo / Alpha Vantage' },
        BOND: { labelKey: 'settings.dataSources.backfill.types.bond', defaultLabel: 'Tahvil & Bono', providers: ['TCMB', 'YAHOO_FINANCE'], hint: 'TCMB / Yahoo Finance' },
        VIOP: { labelKey: 'settings.dataSources.backfill.types.viop', defaultLabel: 'VİOP', providers: ['BIST_DATASTORE', 'YAHOO_FINANCE'], hint: 'BIST Datastore / Yahoo' },
    }

    const BACKFILL_TYPES = [
        { value: 'STOCK', i18nKey: 'settings.dataSources.backfill.types.stock' },
        { value: 'FUND', i18nKey: 'settings.dataSources.backfill.types.fund' },
        { value: 'CURRENCY', i18nKey: 'settings.dataSources.backfill.types.currency' },
        { value: 'INDEX', i18nKey: 'settings.dataSources.backfill.types.index' },
        { value: 'BOND', i18nKey: 'settings.dataSources.backfill.types.bond' },
        { value: 'VIOP', i18nKey: 'settings.dataSources.backfill.types.viop' },
    ]

    const activeProviders = new Set(
        (apiConfigs || [])
            .filter((c) => c.isActive)
            .map((c) => c.provider)
    )

    const missingTypesForSelected = backfillForm.instrumentTypes.filter((type) => {
        const info = TYPE_PROVIDERS_MAP[type]
        if (!info) return false
        return !info.providers.some((p) => activeProviders.has(p))
    })

    const isBackfillDisabled =
        isBackfillingMarketData ||
        backfillForm.instrumentTypes.length === 0 ||
        missingTypesForSelected.length > 0

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
                        <h3 className="font-semibold">{t('settings.dataSources.backfill.title')}</h3>
                        <p className="text-sm text-muted-foreground">
                            {t('settings.dataSources.backfill.description')}
                        </p>
                    </div>
                </div>

                <div className="grid gap-4 md:grid-cols-3">
                    <div className="space-y-2">
                        <Label>{t('settings.dataSources.backfill.period')}</Label>
                        <Select
                            value={backfillForm.days}
                            onValueChange={(value) => onBackfillFormChange('days', value)}
                            disabled={isBackfillingMarketData}
                        >
                            <SelectTrigger>
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="7">{t('settings.dataSources.backfill.periodOptions.7')}</SelectItem>
                                <SelectItem value="30">{t('settings.dataSources.backfill.periodOptions.30')}</SelectItem>
                                <SelectItem value="90">{t('settings.dataSources.backfill.periodOptions.90')}</SelectItem>
                                <SelectItem value="365">{t('settings.dataSources.backfill.periodOptions.365')}</SelectItem>
                            </SelectContent>
                        </Select>
                    </div>
                    <div className="space-y-2">
                        <Label>{t('settings.dataSources.backfill.instrumentLimit')}</Label>
                        <Input
                            type="number"
                            min="1"
                            max="5000"
                            value={backfillForm.maxInstruments}
                            onChange={(event: ChangeEvent<HTMLInputElement>) => onBackfillFormChange('maxInstruments', event.target.value)}
                            disabled={isBackfillingMarketData}
                        />
                    </div>
                    <div className="space-y-2">
                        <Label>{t('settings.dataSources.backfill.optionalSymbols')}</Label>
                        <Input
                            value={backfillForm.symbols}
                            onChange={(event: ChangeEvent<HTMLInputElement>) => onBackfillFormChange('symbols', event.target.value)}
                            placeholder={t('settings.dataSources.backfill.symbolsPlaceholder')}
                            disabled={isBackfillingMarketData}
                        />
                    </div>
                </div>

                <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                    {BACKFILL_TYPES.map((type) => {
                        const isSelected = backfillForm.instrumentTypes.includes(type.value)
                        const info = TYPE_PROVIDERS_MAP[type.value]
                        const hasActiveApi = info ? info.providers.some((p) => activeProviders.has(p)) : true

                        return (
                            <label
                                key={type.value}
                                className={cn(
                                    "flex items-center justify-between rounded-lg border bg-background px-3 py-2 text-sm transition-colors cursor-pointer",
                                    isSelected && !hasActiveApi && "border-destructive/60 bg-destructive/10 text-destructive font-medium",
                                    !hasActiveApi && !isSelected && "border-border/60 opacity-70"
                                )}
                            >
                                <div className="flex items-center gap-2">
                                    <Checkbox
                                        checked={isSelected}
                                        onCheckedChange={(checked: boolean) => onToggleBackfillType(type.value, Boolean(checked))}
                                        disabled={isBackfillingMarketData}
                                    />
                                    <span>{t(type.i18nKey)}</span>
                                </div>
                                {!hasActiveApi && (
                                    <Badge variant="destructive" className="text-[10px] px-1.5 py-0">
                                        API Kapalı
                                    </Badge>
                                )}
                            </label>
                        )
                    })}
                </div>

                {missingTypesForSelected.length > 0 && (
                    <div className="mt-4 flex flex-col gap-2 rounded-lg border border-destructive/40 bg-destructive/10 p-3.5 text-sm text-destructive">
                        <div className="flex items-center gap-2 font-semibold">
                            <AlertTriangle className="h-4 w-4 text-destructive shrink-0" />
                            <span>Geçmiş Veri İndirme Engellendi: Aktif API Kaynağı Eksik</span>
                        </div>
                        <p className="text-xs text-destructive/90">
                            Seçtiğiniz enstrüman türü için gerekli API kaynağı eklenmemiş veya pasif durumda. İndirme işleminin başlayabilmesi için aşağıdaki API'lerin aktif edilmesi gerekir:
                        </p>
                        <ul className="list-disc list-inside text-xs space-y-1 ml-1">
                            {missingTypesForSelected.map((type) => {
                                const info = TYPE_PROVIDERS_MAP[type]
                                return (
                                    <li key={type} className="font-medium">
                                        <span className="font-semibold">{t(info?.labelKey || type)}:</span> Gerekli API ({info?.hint}) aktif edilmemiş!
                                    </li>
                                )
                            })}
                        </ul>
                        <div className="mt-1 flex justify-start">
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                className="h-8 border-destructive/40 text-destructive hover:bg-destructive/20 text-xs gap-1.5"
                                onClick={onOpenApiKeysTab}
                            >
                                <Key className="h-3.5 w-3.5" />
                                API Anahtarları Sekmesinden Aktif Et
                            </Button>
                        </div>
                    </div>
                )}

                {backfillForm.instrumentTypes.length === 0 && (
                    <div className="mt-4 flex items-center gap-2 rounded-lg border border-amber-500/40 bg-amber-500/10 p-3 text-xs text-amber-600 dark:text-amber-400">
                        <AlertTriangle className="h-4 w-4 shrink-0" />
                        <span>Lütfen geçmiş verisini indirmek istediğiniz en az bir enstrüman türü (Hisse, Fon, Döviz vb.) seçin.</span>
                    </div>
                )}

                {isBackfillingMarketData && (
                    <div className="mt-4 flex items-center gap-3 rounded-lg border border-primary/20 bg-primary/5 px-4 py-3 text-sm text-primary">
                        <Loader2 className="h-4 w-4 animate-spin" />
                        <div>
                            <p className="font-medium">{t('settings.dataSources.backfill.loadingTitle')}</p>
                            <p className="text-xs text-muted-foreground">
                                {t('settings.dataSources.backfill.loadingDescription')}
                            </p>
                        </div>
                    </div>
                )}

                <div className="mt-4 flex justify-end">
                    <Button
                        onClick={onBackfillMarketData}
                        disabled={isBackfillDisabled}
                        title={missingTypesForSelected.length > 0 ? "İlgili API kaynağı kapalı olduğu için buton pasiftir." : undefined}
                    >
                        {isBackfillingMarketData ? (
                            <>
                                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                                {t('settings.dataSources.backfill.buttonLoading')}
                            </>
                        ) : (
                            t('settings.dataSources.backfill.button')
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
                    {t('settings.dataSources.title')}
                </CardTitle>
                <CardDescription>
                    {t('settings.dataSources.description')}
                </CardDescription>
            </CardHeader>
            <CardContent>
                {!hasApiConfigs ? (
                    <div className="text-center p-8 border-2 border-dashed rounded-lg text-muted-foreground">
                        <Key className="h-10 w-10 mx-auto mb-3 opacity-50" />
                        <p className="mb-2">
                            {isAdmin
                                ? t('settings.dataSources.noApiKeys')
                                : t('settings.dataSources.adminManaged')}
                        </p>
                        {isAdmin && (
                            <Button variant="link" onClick={onOpenApiKeysTab}>
                                {t('settings.apiKeys.add')}
                            </Button>
                        )}
                    </div>
                ) : (
                    <div className="space-y-6">
                        {DATA_SOURCE_TYPES.map((dataType: DataSourceType) => {
                            const currentPreference = preferencesData?.data?.find((item) => item.dataType === dataType.type)
                            const availableProviders = apiConfigs.filter((config) =>
                                config.isActive && dataType.providers.includes(config.provider)
                            )
                            const unavailableReason = dataType.unavailableReason
                            const isUnavailable = Boolean(unavailableReason)

                            return (
                                <div key={dataType.type} className="flex items-center justify-between py-4 border-b last:border-0">
                                    <div className="flex items-center gap-3">
                                        <div>
                                            <Label className="text-base font-medium">{t(dataType.labelKey)}</Label>
                                            <p className="text-sm text-muted-foreground">
                                                {isUnavailable
                                                    ? unavailableReason
                                                    : availableProviders.length > 0
                                                    ? t('settings.dataSources.availableCount', { count: availableProviders.length })
                                                    : t('settings.dataSources.noSourceAddKey')}
                                            </p>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-2">
                                        {isUnavailable ? (
                                            <Badge variant="secondary">
                                                {t('settings.dataSources.verifiedSourceNone')}
                                            </Badge>
                                        ) : availableProviders.length > 0 ? (
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
                                                {t('settings.dataSources.noProvider')}
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
                                {t('settings.dataSources.refreshNow')}
                            </RefreshButton>
                        </div>
                    </div>
                )}
                {renderBackfillPanel()}
            </CardContent>
        </Card>
    )
}
