import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Database, FlaskConical } from 'lucide-react'
import { useSelector } from 'react-redux'
import { useTranslation } from 'react-i18next'
import { ApiKeysTab } from '@/pages/settings/ApiKeysTab'
import { DataSourcesTab } from '@/pages/settings/DataSourcesTab'
import { GeneralSettingsTab } from '@/pages/settings/GeneralSettingsTab'
import { SimulationTab } from '@/pages/settings/SimulationTab'
import { selectIsAdmin } from '@/store/slices/authSlice'
import { getProviderInfo } from '@/pages/settings/providerInfo'
import { useGeneralSettings } from '@/pages/settings/hooks/useGeneralSettings'
import { useApiDataSourceSettings } from '@/pages/settings/hooks/useApiDataSourceSettings'
import { useSimulationSettings } from '@/pages/settings/hooks/useSimulationSettings'

export default function SettingsPage() {
    const { t, i18n } = useTranslation()
    const isAdmin = useSelector(selectIsAdmin)

    const generalSettings = useGeneralSettings({ t, i18n })
    const apiDataSourceSettings = useApiDataSourceSettings({ t })
    const simulationSettings = useSimulationSettings({ t })

    const providerInfo = getProviderInfo(t)
    const getProviderLabel = (provider) => providerInfo[provider]?.title || provider

    return (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div>
                <h1 className="text-2xl font-bold">{t('settings.apiKeys.title')}</h1>
                <p className="text-muted-foreground">{t('settings.apiKeys.manage')}</p>
            </div>

            <Tabs defaultValue="api-keys" className="space-y-4">
                <TabsList>
                    <TabsTrigger value="general">{t('settingsPage.tabs.general')}</TabsTrigger>
                    <TabsTrigger value="api-keys">{t('settings.apiKeys.title')}</TabsTrigger>
                    <TabsTrigger value="data-sources">
                        <Database className="h-4 w-4 mr-2" />
                        {t('settings.dataSources.title', { defaultValue: 'Veri Kaynaklari' })}
                    </TabsTrigger>
                    {isAdmin && (
                        <TabsTrigger value="simulation">
                            <FlaskConical className="h-4 w-4 mr-2" />
                            {t('settings.simulation.title', { defaultValue: 'Simulasyon' })}
                        </TabsTrigger>
                    )}
                </TabsList>

                <TabsContent value="general">
                    <GeneralSettingsTab
                        t={t}
                        i18n={i18n}
                        theme={generalSettings.theme}
                        currency={generalSettings.currency}
                        timezone={generalSettings.timezone}
                        autoUpdate={generalSettings.autoUpdate}
                        refreshRate={generalSettings.refreshRate}
                        notificationSettings={generalSettings.notificationSettings}
                        isClearingCache={generalSettings.isClearingCache}
                        onThemeChange={generalSettings.handleThemeChange}
                        onLanguageChange={generalSettings.handleLanguageChange}
                        onCurrencyChange={generalSettings.handleCurrencyChange}
                        onTimezoneChange={generalSettings.handleTimezoneChange}
                        onAutoUpdateChange={generalSettings.handleAutoUpdateChange}
                        onRefreshRateChange={generalSettings.handleRefreshRateChange}
                        onNotificationToggle={generalSettings.handleNotificationToggle}
                        onFullReset={generalSettings.handleFullReset}
                        onClearCache={generalSettings.handleClearCache}
                        onSaveSettings={generalSettings.handleSaveSettings}
                    />
                </TabsContent>

                <TabsContent value="api-keys">
                    <ApiKeysTab
                        t={t}
                        apiConfigs={apiDataSourceSettings.apiConfigs}
                        isLoading={apiDataSourceSettings.isLoading}
                        isDialogOpen={apiDataSourceSettings.isDialogOpen}
                        editingConfig={apiDataSourceSettings.editingConfig}
                        formData={apiDataSourceSettings.formData}
                        isValidated={apiDataSourceSettings.isValidated}
                        isAdding={apiDataSourceSettings.isAdding}
                        isTesting={apiDataSourceSettings.isTesting}
                        providerInfo={providerInfo}
                        getProviderLabel={getProviderLabel}
                        onOpenDialog={apiDataSourceSettings.handleOpenDialog}
                        onDialogOpenChange={apiDataSourceSettings.handleDialogOpenChange}
                        onFormFieldChange={apiDataSourceSettings.handleFormFieldChange}
                        onSubmit={apiDataSourceSettings.handleAddSubmit}
                        onTestKey={apiDataSourceSettings.handleTestKey}
                        onDelete={apiDataSourceSettings.handleDelete}
                    />
                </TabsContent>

                <TabsContent value="data-sources">
                    <DataSourcesTab
                        t={t}
                        apiConfigs={apiDataSourceSettings.apiConfigs}
                        preferencesData={apiDataSourceSettings.preferencesData}
                        getProviderLabel={getProviderLabel}
                        onSelectDataPreference={apiDataSourceSettings.handleSelectDataPreference}
                        onRefreshData={apiDataSourceSettings.handleRefreshDataSources}
                        onOpenApiKeysTab={() => apiDataSourceSettings.handleOpenDialog(null)}
                    />
                </TabsContent>

                {isAdmin && (
                    <TabsContent value="simulation">
                        <SimulationTab
                            t={t}
                            simConfig={simulationSettings.simConfig}
                            simStatus={simulationSettings.simStatus}
                            onToggleSimulation={simulationSettings.handleToggleSimulation}
                            onUpdateSimulationConfig={simulationSettings.handleUpdateSimulationConfig}
                            onResetSimulation={simulationSettings.handleResetSimulation}
                        />
                    </TabsContent>
                )}
            </Tabs>

            {isAdmin && simulationSettings.simConfig?.enabled && (
                <Badge variant="success" className="w-fit">
                    {t('settings.simulation.active', { defaultValue: 'Simulasyon Aktif' })}
                </Badge>
            )}
        </div>
    )
}
