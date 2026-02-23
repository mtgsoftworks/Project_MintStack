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
import { Switch } from '@/components/ui/switch'
import { AlertCircle, FlaskConical, Minus, RotateCcw, TrendingDown, TrendingUp } from 'lucide-react'

export function SimulationTab({
    t,
    simConfig,
    simStatus,
    onToggleSimulation,
    onUpdateSimulationConfig,
    onResetSimulation
}) {
    const updateConfig = (partialConfig) => onUpdateSimulationConfig(partialConfig)

    return (
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2">
                    <FlaskConical className="h-5 w-5" />
                    {t('settings.simulation.title', { defaultValue: 'Simulasyon Modu' })}
                </CardTitle>
                <CardDescription>
                    {t('settings.simulation.description', { defaultValue: 'Gercek API olmadan gercekci piyasa verileri ile test edin' })}
                </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
                <div className="flex items-center justify-between p-4 border rounded-lg bg-gradient-to-r from-purple-50 to-blue-50 dark:from-purple-950/20 dark:to-blue-950/20">
                    <div className="space-y-1">
                        <Label className="text-base font-semibold flex items-center gap-2">
                            <FlaskConical className="h-4 w-4" />
                            {t('settings.simulation.enable', { defaultValue: 'Simulasyon Modunu Aktif Et' })}
                        </Label>
                        <p className="text-sm text-muted-foreground">
                            {t('settings.simulation.enableDesc', { defaultValue: 'Aktif oldugunda gercek API yerine simule veriler kullanilir' })}
                        </p>
                    </div>
                    <div className="flex items-center gap-2">
                        {simConfig?.enabled && (
                            <span className="text-xs px-2 py-1 rounded-full bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-100">
                                {t('settings.simulation.active', { defaultValue: 'Aktif' })}
                            </span>
                        )}
                        <Switch checked={simConfig?.enabled || false} onCheckedChange={onToggleSimulation} />
                    </div>
                </div>

                {simConfig?.enabled && simStatus && (
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                        <div className="p-3 border rounded-lg text-center">
                            <p className="text-2xl font-bold text-blue-600">{simStatus.stockCount || 0}</p>
                            <p className="text-xs text-muted-foreground">{t('settings.simulation.stocks', { defaultValue: 'Hisse Senedi' })}</p>
                        </div>
                        <div className="p-3 border rounded-lg text-center">
                            <p className="text-2xl font-bold text-green-600">{simStatus.currencyCount || 0}</p>
                            <p className="text-xs text-muted-foreground">{t('settings.simulation.currencies', { defaultValue: 'Doviz Kuru' })}</p>
                        </div>
                        <div className="p-3 border rounded-lg text-center">
                            <p className="text-2xl font-bold text-purple-600">{simStatus.indexCount || 0}</p>
                            <p className="text-xs text-muted-foreground">{t('settings.simulation.indices', { defaultValue: 'Endeks' })}</p>
                        </div>
                        <div className="p-3 border rounded-lg text-center">
                            <p className="text-2xl font-bold text-orange-600">{simStatus.tickCount || 0}</p>
                            <p className="text-xs text-muted-foreground">{t('settings.simulation.ticks', { defaultValue: 'Guncelleme' })}</p>
                        </div>
                    </div>
                )}

                {simConfig?.enabled && (
                    <div className="space-y-4 p-4 border rounded-lg">
                        <h4 className="font-medium">{t('settings.simulation.config', { defaultValue: 'Simulasyon Ayarlari' })}</h4>

                        <div className="space-y-2">
                            <Label>{t('settings.simulation.volatility', { defaultValue: 'Volatilite Seviyesi' })}</Label>
                            <Select value={simConfig?.volatilityLevel || 'MEDIUM'} onValueChange={(value) => updateConfig({ volatilityLevel: value })}>
                                <SelectTrigger>
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="LOW">
                                        <span className="flex items-center gap-2">
                                            <Minus className="h-4 w-4 text-green-500" />
                                            {t('settings.simulation.volatility.low', { defaultValue: 'Dusuk (Sakin Piyasa)' })}
                                        </span>
                                    </SelectItem>
                                    <SelectItem value="MEDIUM">
                                        <span className="flex items-center gap-2">
                                            <TrendingUp className="h-4 w-4 text-blue-500" />
                                            {t('settings.simulation.volatility.medium', { defaultValue: 'Orta (Normal)' })}
                                        </span>
                                    </SelectItem>
                                    <SelectItem value="HIGH">
                                        <span className="flex items-center gap-2">
                                            <TrendingUp className="h-4 w-4 text-orange-500" />
                                            {t('settings.simulation.volatility.high', { defaultValue: 'Yuksek (Hareketli)' })}
                                        </span>
                                    </SelectItem>
                                    <SelectItem value="EXTREME">
                                        <span className="flex items-center gap-2">
                                            <AlertCircle className="h-4 w-4 text-red-500" />
                                            {t('settings.simulation.volatility.extreme', { defaultValue: 'Ekstrem (Kriz Modu)' })}
                                        </span>
                                    </SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="space-y-2">
                            <Label>{t('settings.simulation.trend', { defaultValue: 'Piyasa Trendi' })}</Label>
                            <Select value={simConfig?.marketTrend || 'NEUTRAL'} onValueChange={(value) => updateConfig({ marketTrend: value })}>
                                <SelectTrigger>
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="BULLISH">
                                        <span className="flex items-center gap-2">
                                            <TrendingUp className="h-4 w-4 text-green-500" />
                                            {t('settings.simulation.trend.bullish', { defaultValue: 'Boga (Yukselis)' })}
                                        </span>
                                    </SelectItem>
                                    <SelectItem value="NEUTRAL">
                                        <span className="flex items-center gap-2">
                                            <Minus className="h-4 w-4 text-gray-500" />
                                            {t('settings.simulation.trend.neutral', { defaultValue: 'Notr' })}
                                        </span>
                                    </SelectItem>
                                    <SelectItem value="BEARISH">
                                        <span className="flex items-center gap-2">
                                            <TrendingDown className="h-4 w-4 text-red-500" />
                                            {t('settings.simulation.trend.bearish', { defaultValue: 'Ayi (Dusus)' })}
                                        </span>
                                    </SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="space-y-2">
                            <Label>{t('settings.simulation.interval', { defaultValue: 'Guncelleme Araligi' })}</Label>
                            <Select value={String(simConfig?.updateIntervalSeconds || 5)} onValueChange={(value) => updateConfig({ updateIntervalSeconds: parseInt(value, 10) })}>
                                <SelectTrigger>
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="1">1 {t('settings.simulation.seconds', { defaultValue: 'saniye' })}</SelectItem>
                                    <SelectItem value="3">3 {t('settings.simulation.seconds', { defaultValue: 'saniye' })}</SelectItem>
                                    <SelectItem value="5">5 {t('settings.simulation.seconds', { defaultValue: 'saniye' })}</SelectItem>
                                    <SelectItem value="10">10 {t('settings.simulation.seconds', { defaultValue: 'saniye' })}</SelectItem>
                                    <SelectItem value="30">30 {t('settings.simulation.seconds', { defaultValue: 'saniye' })}</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="flex items-center justify-between">
                            <div className="space-y-0.5">
                                <Label>{t('settings.simulation.randomEvents', { defaultValue: 'Rastgele Piyasa Olaylari' })}</Label>
                                <p className="text-sm text-muted-foreground">
                                    {t('settings.simulation.randomEventsDesc', { defaultValue: 'Beklenmedik fiyat hareketleri simule et' })}
                                </p>
                            </div>
                            <Switch
                                checked={simConfig?.enableRandomEvents || false}
                                onCheckedChange={(value) => updateConfig({ enableRandomEvents: value })}
                            />
                        </div>

                        <div className="flex items-center justify-between">
                            <div className="space-y-0.5">
                                <Label>{t('settings.simulation.marketHours', { defaultValue: 'Piyasa Saatlerini Kullan (10:00-18:00)' })}</Label>
                                <p className="text-sm text-muted-foreground">
                                    {t('settings.simulation.marketHoursDesc', { defaultValue: 'Kapaliyken 7/24 calisir' })}
                                </p>
                            </div>
                            <Switch
                                checked={simConfig?.enableMarketHours || false}
                                onCheckedChange={(value) => updateConfig({ enableMarketHours: value })}
                            />
                        </div>
                    </div>
                )}

                {simConfig?.enabled && (
                    <div className="flex justify-end">
                        <Button variant="outline" onClick={onResetSimulation}>
                            <RotateCcw className="h-4 w-4 mr-2" />
                            {t('settings.simulation.resetButton', { defaultValue: 'Simulasyonu Sifirla' })}
                        </Button>
                    </div>
                )}

                <div className="p-4 bg-blue-50 dark:bg-blue-950/20 border border-blue-200 dark:border-blue-800 rounded-lg">
                    <h4 className="font-medium text-blue-900 dark:text-blue-100 mb-2">
                        {t('settings.simulation.infoTitle', { defaultValue: 'Simulasyon Hakkinda' })}
                    </h4>
                    <ul className="text-sm text-blue-800 dark:text-blue-200 space-y-1 list-disc list-inside">
                        <li>{t('settings.simulation.info1', { defaultValue: 'BIST 30 hisseleri icin gercekci fiyat hareketleri' })}</li>
                        <li>{t('settings.simulation.info2', { defaultValue: 'TCMB doviz kurlari simulasyonu' })}</li>
                        <li>{t('settings.simulation.info3', { defaultValue: 'Geometric Brownian Motion ve Mean Reversion algoritmalari' })}</li>
                        <li>{t('settings.simulation.info4', { defaultValue: 'WebSocket ile gercek zamanli guncellemeler' })}</li>
                    </ul>
                </div>
            </CardContent>
        </Card>
    )
}
