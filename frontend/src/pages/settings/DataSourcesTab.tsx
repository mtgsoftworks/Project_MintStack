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
import { Badge } from '@/components/ui/badge'
import RefreshButton from '@/components/common/RefreshButton'
import { Database, Key, Zap } from 'lucide-react'
import { DATA_SOURCE_TYPES } from './providerInfo'

export function DataSourcesTab({
    t,
    apiConfigs,
    preferencesData,
    isRefreshing,
    getProviderLabel,
    onSelectDataPreference,
    onRefreshData,
    onOpenApiKeysTab
}) {
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
                {!apiConfigs || apiConfigs.length === 0 ? (
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
            </CardContent>
        </Card>
    )
}
