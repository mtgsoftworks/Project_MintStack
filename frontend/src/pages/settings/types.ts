// Shared types for settings pages

// API Config types
export interface ApiKeyConfig {
    id: number | string
    provider: string
    apiKey: string
    secretKey?: string
    modelName?: string
    baseUrl?: string
    isActive: boolean
    createdAt: string
}

export interface ApiConfigFormData {
    provider: string
    apiKey: string
    secretKey: string
    modelName: string
    baseUrl: string
    isActive: boolean
}

// Provider capabilities
export interface ProviderCapability {
    enabled: boolean
    policies?: string[]
}

export type ProviderCapabilities = Record<string, ProviderCapability>

// Provider info
export interface ProviderInfoItem {
    title: string
    description: string
    supported: string[]
    missing: string[]
    color: string
}

export type ProviderInfo = Record<string, ProviderInfoItem>

// Data source preferences
export interface DataPreference {
    dataType: string
    provider: string
    isEnabled: boolean
}

// Backfill form
export interface BackfillFormData {
    days: string
    maxInstruments: string
    instrumentTypes: string[]
    symbols: string
    includeSyntheticFallback: boolean
}

// Notification settings
export interface NotificationSettings {
    priceAlerts: boolean
    portfolioUpdates: boolean
    emailNotifications: boolean
    pushNotifications: boolean
}

// Data source type from providerInfo.ts
export interface DataSourceType {
    type: string
    labelKey: string
    providers: string[]
    unavailableReason?: string
}

// Callback types
export type FormFieldChangeHandler = (field: string, value: string | boolean, resetValidation?: boolean) => void
export type BackfillFormChangeHandler = (field: string, value: string) => void
export type ToggleBackfillTypeHandler = (type: string, checked: boolean) => void
export type SelectDataPreferenceHandler = (dataType: DataSourceType, provider: string) => void
export type NotificationToggleHandler = (key: keyof NotificationSettings, value: boolean) => void
