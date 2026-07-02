import { useState, useEffect, type ChangeEvent } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { useTranslation } from 'react-i18next'
import { Key, Shield } from 'lucide-react'
import { toast } from 'sonner'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Separator } from '@/components/ui/separator'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { selectUser, selectRoles, selectIsAdmin } from '@/store/slices/authSlice'
import { selectTheme, setTheme } from '@/store/slices/uiSlice'
import { useGetProfileQuery, useUpdateProfileMutation } from '@/store/api/userApi'
import { getInitials } from '@/lib/utils'

export default function ProfilePage() {
  const { t } = useTranslation()
  const user = useSelector(selectUser)
  const roles = useSelector(selectRoles)
  const isAdmin = useSelector(selectIsAdmin)
  const theme = useSelector(selectTheme)
  const dispatch = useDispatch()
  const { data: profile } = useGetProfileQuery(undefined, { skip: false })
  const [updateProfile, { isLoading: updating }] = useUpdateProfileMutation()

  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [bio, setBio] = useState('')
  const [location, setLocation] = useState('')

  const [emailNotifications, setEmailNotifications] = useState(true)
  const [pushNotifications, setPushNotifications] = useState(true)
  const [priceAlerts, setPriceAlerts] = useState(true)
  const [portfolioUpdates, setPortfolioUpdates] = useState(true)
  const [compactView, setCompactView] = useState(false)

  useEffect(() => {
    if (profile) {
      setFirstName(profile.firstName || '')
      setLastName(profile.lastName || '')
      setPhoneNumber(profile.phoneNumber || '')
      setBio(profile.bio || '')
      setLocation(profile.location || '')

      setEmailNotifications(profile.emailNotifications ?? true)
      setPushNotifications(profile.pushNotifications ?? true)
      setPriceAlerts(profile.priceAlerts ?? true)
      setPortfolioUpdates(profile.portfolioUpdates ?? true)
      setCompactView(profile.compactView ?? false)
    }
  }, [profile])

  const handleUpdateProfile = async () => {
    try {
      await updateProfile({
        firstName,
        lastName,
        phoneNumber,
        bio,
        location
      }).unwrap()
      toast.success(t('profile.updated'))
    } catch (_error) {
      toast.error(t('profile.updateFailed'))
    }
  }

  const handlePreferenceChange = async (key: string, value: boolean) => {
    const previousState = {
      emailNotifications,
      pushNotifications,
      priceAlerts,
      portfolioUpdates,
      compactView,
    }

    try {
      if (key === 'emailNotifications') setEmailNotifications(value)
      if (key === 'pushNotifications') setPushNotifications(value)
      if (key === 'priceAlerts') setPriceAlerts(value)
      if (key === 'portfolioUpdates') setPortfolioUpdates(value)
      if (key === 'compactView') setCompactView(value)

      await updateProfile({ [key]: value }).unwrap()

      toast.success(t('profile.settingsUpdated'))
    } catch {
      setEmailNotifications(previousState.emailNotifications)
      setPushNotifications(previousState.pushNotifications)
      setPriceAlerts(previousState.priceAlerts)
      setPortfolioUpdates(previousState.portfolioUpdates)
      setCompactView(previousState.compactView)
      toast.error(t('profile.settingsUpdateFailed'))
    }
  }

  const runKeycloakAction = async (action: 'UPDATE_PASSWORD' | 'CONFIGURE_TOTP') => {
    const redirectUri = `${window.location.origin}/profile`
    const client = window.keycloak as { login?: (opts: { action: string; redirectUri: string; prompt: string }) => Promise<void>; updateToken?: (minValidity: number) => Promise<boolean> } | undefined

    if (!client?.login) {
      toast.error(t('profile.keycloakSessionFailed'))
      return
    }

    try {
      await client.updateToken?.(30)
      await client.login({
        action,
        redirectUri,
        prompt: 'login',
      })
    } catch (error) {
      console.error(`Keycloak action failed: ${action}`, error)
      toast.error(t('profile.keycloakSecurityFailed'))
    }
  }

  return (
    <div className="space-y-6 animate-in">
      <div>
        <h1 className="text-2xl font-bold">{t('profile.title')}</h1>
        <p className="text-muted-foreground">{t('profile.subtitle')}</p>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-1">
          <CardContent className="pt-6">
            <div className="flex flex-col items-center text-center">
              <Avatar className="h-24 w-24 mb-4">
                <AvatarImage src={user?.avatar} />
                <AvatarFallback className="text-2xl bg-primary/10 text-primary">
                  {getInitials(profile?.fullName || user?.name || user?.username)}
                </AvatarFallback>
              </Avatar>
              <h2 className="text-xl font-semibold">{profile?.fullName || user?.name || user?.username}</h2>
              <p className="text-sm text-muted-foreground mb-4">{user?.email}</p>
              <div className="flex flex-wrap justify-center gap-2">
                {roles.map((role: string) => (
                  <Badge key={role} variant="secondary">
                    {role}
                  </Badge>
                ))}
              </div>
            </div>

            <Separator className="my-6" />

            <div className="space-y-4">
              {isAdmin ? (
                <>
                  <Button
                    variant="outline"
                    className="w-full justify-start"
                    onClick={() => runKeycloakAction('UPDATE_PASSWORD')}
                  >
                    <Key className="mr-2 h-4 w-4" />
                    {t('profile.changePassword')}
                  </Button>
                  <Button
                    variant="outline"
                    className="w-full justify-start"
                    onClick={() => runKeycloakAction('CONFIGURE_TOTP')}
                  >
                    <Shield className="mr-2 h-4 w-4" />
                    {t('profile.securitySettings')}
                  </Button>
                </>
              ) : (
                <p className="text-sm text-muted-foreground">
                  {t('profile.securityAdminOnly')}
                </p>
              )}
            </div>
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <Tabs defaultValue="general" className="w-full">
            <CardHeader>
              <TabsList className="grid w-full grid-cols-3">
                <TabsTrigger value="general">{t('profile.tabs.general')}</TabsTrigger>
                <TabsTrigger value="notifications">{t('profile.tabs.notifications')}</TabsTrigger>
                <TabsTrigger value="preferences">{t('profile.tabs.preferences')}</TabsTrigger>
              </TabsList>
            </CardHeader>

            <CardContent>
              <TabsContent value="general" className="space-y-6">
                <div className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="username">{t('profile.username')}</Label>
                    <Input
                      id="username"
                      value={user?.username || ''}
                      disabled
                      className="bg-muted"
                    />
                    <p className="text-xs text-muted-foreground">
                      {t('profile.usernameReadonly')}
                    </p>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="firstName">{t('profile.firstName')}</Label>
                      <Input
                        id="firstName"
                        value={firstName}
                        onChange={(e: ChangeEvent<HTMLInputElement>) => setFirstName(e.target.value)}
                        placeholder={t('profile.firstNamePlaceholder')}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="lastName">{t('profile.lastName')}</Label>
                      <Input
                        id="lastName"
                        value={lastName}
                        onChange={(e: ChangeEvent<HTMLInputElement>) => setLastName(e.target.value)}
                        placeholder={t('profile.lastNamePlaceholder')}
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="email">{t('profile.email')}</Label>
                    <Input
                      id="email"
                      type="email"
                      value={user?.email || ''}
                      disabled
                      className="bg-muted"
                    />
                    <p className="text-xs text-muted-foreground">
                      {t('profile.emailKeycloakHint')}
                    </p>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="phoneNumber">{t('profile.phone')}</Label>
                      <Input
                        id="phoneNumber"
                        value={phoneNumber}
                        onChange={(e: ChangeEvent<HTMLInputElement>) => setPhoneNumber(e.target.value)}
                        placeholder="+90 5XX XXX XX XX"
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="location">{t('profile.location')}</Label>
                      <Input
                        id="location"
                        value={location}
                        onChange={(e: ChangeEvent<HTMLInputElement>) => setLocation(e.target.value)}
                        placeholder={t('profile.locationPlaceholder')}
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="bio">{t('profile.bio')}</Label>
                    <textarea
                      id="bio"
                      className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                      value={bio}
                      onChange={(e: ChangeEvent<HTMLTextAreaElement>) => setBio(e.target.value)}
                      placeholder={t('profile.bioPlaceholder')}
                    />
                  </div>

                  <Button
                    onClick={handleUpdateProfile}
                    disabled={updating}
                  >
                    {updating ? t('profile.saving') : t('profile.saveChanges')}
                  </Button>
                </div>
              </TabsContent>

              <TabsContent value="notifications" className="space-y-6">
                <div className="space-y-4">
                  <div className="flex items-center justify-between py-2">
                    <div className="space-y-0.5">
                      <Label>{t('profile.emailNotifications')}</Label>
                      <p className="text-sm text-muted-foreground">
                        {t('profile.emailNotificationsDesc')}
                      </p>
                    </div>
                    <Switch
                      checked={emailNotifications}
                      onCheckedChange={(val: boolean) => handlePreferenceChange('emailNotifications', val)}
                    />
                  </div>

                  <Separator />

                  <div className="flex items-center justify-between py-2">
                    <div className="space-y-0.5">
                      <Label>{t('profile.pushNotifications')}</Label>
                      <p className="text-sm text-muted-foreground">
                        {t('profile.pushNotificationsDesc')}
                      </p>
                    </div>
                    <Switch
                      checked={pushNotifications}
                      onCheckedChange={(val: boolean) => handlePreferenceChange('pushNotifications', val)}
                    />
                  </div>

                  <Separator />

                  <div className="flex items-center justify-between py-2">
                    <div className="space-y-0.5">
                      <Label>{t('profile.priceAlerts')}</Label>
                      <p className="text-sm text-muted-foreground">
                        {t('profile.priceAlertsDesc')}
                      </p>
                    </div>
                    <Switch
                      checked={priceAlerts}
                      onCheckedChange={(val: boolean) => handlePreferenceChange('priceAlerts', val)}
                    />
                  </div>

                  <Separator />

                  <div className="flex items-center justify-between py-2">
                    <div className="space-y-0.5">
                      <Label>{t('profile.portfolioUpdates')}</Label>
                      <p className="text-sm text-muted-foreground">
                        {t('profile.portfolioUpdatesDesc')}
                      </p>
                    </div>
                    <Switch
                      checked={portfolioUpdates}
                      onCheckedChange={(val: boolean) => handlePreferenceChange('portfolioUpdates', val)}
                    />
                  </div>
                </div>
              </TabsContent>

              <TabsContent value="preferences" className="space-y-6">
                <div className="space-y-4">
                  <div className="flex items-center justify-between py-2">
                    <div className="space-y-0.5">
                      <Label>{t('profile.darkMode')}</Label>
                      <p className="text-sm text-muted-foreground">
                        {t('profile.darkModeDesc')}
                      </p>
                    </div>
                    <Switch
                      checked={theme === 'dark'}
                      onCheckedChange={(checked: boolean) => dispatch(setTheme(checked ? 'dark' : 'light'))}
                    />
                  </div>

                  <Separator />

                  <div className="flex items-center justify-between py-2">
                    <div className="space-y-0.5">
                      <Label>{t('profile.compactView')}</Label>
                      <p className="text-sm text-muted-foreground">
                        {t('profile.compactViewDesc')}
                      </p>
                    </div>
                    <Switch
                      checked={compactView}
                      onCheckedChange={(val: boolean) => handlePreferenceChange('compactView', val)}
                    />
                  </div>

                  <Separator />

                  <div className="flex items-center justify-between py-2">
                    <div className="space-y-0.5">
                      <Label>{t('profile.defaultCurrency')}</Label>
                      <p className="text-sm text-muted-foreground">
                        {t('profile.defaultCurrencyDesc')}
                      </p>
                    </div>
                    <Badge>TRY</Badge>
                  </div>
                </div>
              </TabsContent>
            </CardContent>
          </Tabs>
        </Card>
      </div>
    </div>
  )
}
