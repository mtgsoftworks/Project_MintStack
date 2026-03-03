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
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger
} from '@/components/ui/dialog'
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow
} from '@/components/ui/table'
import { Switch } from '@/components/ui/switch'
import { Badge } from '@/components/ui/badge'
import { CheckCircle, Key, Pencil, Plus, RefreshCw, Trash2 } from 'lucide-react'

export function ApiKeysTab({
    t,
    apiConfigs,
    isLoading,
    isDialogOpen,
    editingConfig,
    formData,
    isValidated,
    isAdding,
    isTesting,
    providerInfo,
    getProviderLabel,
    onOpenDialog,
    onDialogOpenChange,
    onFormFieldChange,
    onSubmit,
    onTestKey,
    onDelete
}) {
    const requiresValidation = formData.provider !== 'TCMB' && !(formData.provider === 'YAHOO_FINANCE' && !formData.apiKey.trim())
    const canSubmit = isValidated || editingConfig || !requiresValidation
    const apiKeyRequired = !editingConfig && formData.provider !== 'TCMB' && formData.provider !== 'YAHOO_FINANCE'

    return (
        <Card>
            <CardHeader className="flex flex-row items-center justify-between">
                <div>
                    <CardTitle>{t('settings.apiKeys.title')}</CardTitle>
                    <CardDescription>{t('settings.apiKeys.description')}</CardDescription>
                </div>
                <Dialog open={isDialogOpen} onOpenChange={onDialogOpenChange}>
                    <DialogTrigger asChild>
                        <Button onClick={() => onOpenDialog(null)}>
                            <Plus className="h-4 w-4 mr-2" />
                            {t('settings.apiKeys.add')}
                        </Button>
                    </DialogTrigger>
                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>{editingConfig ? t('settings.apiKeys.edit') : t('settings.apiKeys.add')}</DialogTitle>
                            <DialogDescription>
                                {editingConfig ? t('settings.apiKeys.editDescription') : t('settings.apiKeys.addDescription')}
                            </DialogDescription>
                        </DialogHeader>

                        <form onSubmit={onSubmit} className="space-y-4 py-4">
                            <div className="space-y-2">
                                <Label>{t('settings.apiKeys.provider')}</Label>
                                <Select
                                    value={formData.provider}
                                    onValueChange={(value) => onFormFieldChange('provider', value, true)}
                                    disabled={Boolean(editingConfig)}
                                >
                                    <SelectTrigger>
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="ALPHA_VANTAGE">{t('settings.apiKeys.providers.alphaVantage')}</SelectItem>
                                        <SelectItem value="YAHOO_FINANCE">{t('settings.apiKeys.providers.yahooFinance')}</SelectItem>
                                        <SelectItem value="FINNHUB">{t('settings.apiKeys.providers.finnhub')}</SelectItem>
                                        <SelectItem value="TCMB">{t('settings.providers.info.TCMB.title')}</SelectItem>
                                        <SelectItem value="OTHER">{t('settings.apiKeys.providerOther')}</SelectItem>
                                    </SelectContent>
                                </Select>

                                {formData.provider && providerInfo[formData.provider] && (
                                    <div className={`text-sm p-3 rounded-md border ${providerInfo[formData.provider].color}`}>
                                        <p className="font-semibold mb-1">{providerInfo[formData.provider].title}</p>
                                        <p className="mb-2 text-xs opacity-90">{providerInfo[formData.provider].description}</p>

                                        <div className="grid grid-cols-2 gap-2 text-xs">
                                            <div>
                                                <span className="font-semibold block mb-0.5">{t('settings.providers.supported')}</span>
                                                <ul className="list-disc list-inside opacity-80 space-y-0.5">
                                                    {providerInfo[formData.provider].supported.map((item, index) => (
                                                        <li key={`${formData.provider}-supported-${index}`}>{item}</li>
                                                    ))}
                                                </ul>
                                            </div>
                                            {providerInfo[formData.provider].missing.length > 0 && (
                                                <div>
                                                    <span className="font-semibold block mb-0.5">{t('settings.providers.missing')}</span>
                                                    <ul className="list-disc list-inside opacity-80 space-y-0.5">
                                                        {providerInfo[formData.provider].missing.map((item, index) => (
                                                            <li key={`${formData.provider}-missing-${index}`}>{item}</li>
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
                                        onChange={(event) => onFormFieldChange('apiKey', event.target.value, true)}
                                        placeholder={editingConfig
                                            ? t('settings.apiKeys.placeholder.unchanged')
                                            : (formData.provider === 'TCMB' || formData.provider === 'YAHOO_FINANCE'
                                                ? t('settings.apiKeys.placeholder.optional')
                                                : t('settings.apiKeys.placeholder.key'))}
                                        required={apiKeyRequired}
                                        className={isValidated ? 'border-green-500' : ''}
                                    />
                                    <Button
                                        type="button"
                                        variant={isValidated ? 'success' : 'outline'}
                                        onClick={onTestKey}
                                        disabled={isTesting || (requiresValidation && !formData.apiKey.trim())}
                                    >
                                        {isTesting ? (
                                            <RefreshCw className="h-4 w-4 animate-spin" />
                                        ) : isValidated ? (
                                            <>
                                                <CheckCircle className="h-4 w-4 mr-1" />
                                                {t('settings.apiKeys.valid')}
                                            </>
                                        ) : (
                                            t('settings.apiKeys.test')
                                        )}
                                    </Button>
                                </div>
                                {!isValidated && !editingConfig && (
                                    <p className="text-xs text-muted-foreground">
                                        {formData.provider === 'TCMB' || formData.provider === 'YAHOO_FINANCE'
                                            ? t('settings.apiKeys.validation.optionalInfo')
                                            : t('settings.apiKeys.validation.testRequired')}
                                    </p>
                                )}
                            </div>

                            <div className="space-y-2">
                                <Label>{t('settings.apiKeys.secretKey')}</Label>
                                <Input
                                    type="password"
                                    value={formData.secretKey}
                                    onChange={(event) => onFormFieldChange('secretKey', event.target.value)}
                                    placeholder={t('settings.apiKeys.placeholder.secretKey')}
                                />
                            </div>

                            <div className="space-y-2">
                                <Label>{t('settings.apiKeys.baseUrl')}</Label>
                                <Input
                                    value={formData.baseUrl}
                                    onChange={(event) => onFormFieldChange('baseUrl', event.target.value)}
                                    placeholder={t('settings.apiKeys.placeholder.baseUrl')}
                                />
                            </div>

                            <div className="flex items-center space-x-2 pt-2">
                                <Switch
                                    checked={formData.isActive}
                                    onCheckedChange={(value) => onFormFieldChange('isActive', value)}
                                />
                                <Label>{t('settings.apiKeys.active')}</Label>
                            </div>

                            <DialogFooter>
                                <Button
                                    type="submit"
                                    disabled={isAdding || !canSubmit}
                                    className={!canSubmit ? 'opacity-50' : ''}
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
                        <Button variant="link" onClick={() => onOpenDialog(null)}>
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
                                        <TableCell className="font-medium">{getProviderLabel(config.provider)}</TableCell>
                                        <TableCell className="font-mono text-xs">{config.apiKey}</TableCell>
                                        <TableCell>
                                            <Badge variant={config.isActive ? 'success' : 'secondary'}>
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
                                                onClick={() => onOpenDialog(config)}
                                                title={t('common.edit')}
                                            >
                                                <Pencil className="h-4 w-4" />
                                            </Button>
                                            <Button
                                                variant="ghost"
                                                size="icon"
                                                className="text-destructive hover:text-destructive hover:bg-destructive/10"
                                                onClick={() => onDelete(config.id)}
                                                title={t('common.delete')}
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
    )
}
