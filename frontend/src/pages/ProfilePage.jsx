import { useSelector, useDispatch } from 'react-redux'
import { User, Mail, Phone, Settings, Key, Bell, Shield } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Separator } from '@/components/ui/separator'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { selectUser, selectRoles } from '@/store/slices/authSlice'
import { selectTheme, setTheme } from '@/store/slices/uiSlice'
import { useGetProfileQuery, useUpdateProfileMutation } from '@/store/api/userApi'
import { getInitials } from '@/lib/utils'
import { toast } from 'sonner'
import { useState } from 'react'

export default function ProfilePage() {
  const user = useSelector(selectUser)
  const roles = useSelector(selectRoles)
  const theme = useSelector(selectTheme)
  const dispatch = useDispatch()
  const { data: profile, isLoading } = useGetProfileQuery()
  const [updateProfile, { isLoading: updating }] = useUpdateProfileMutation()

  const [displayName, setDisplayName] = useState(user?.name || '')
  const [emailNotifications, setEmailNotifications] = useState(true)
  const [pushNotifications, setPushNotifications] = useState(true)

  const handleUpdateProfile = async () => {
    try {
      await updateProfile({ displayName }).unwrap()
      toast.success('Profil güncellendi')
    } catch (error) {
      toast.error('Profil güncellenemedi')
    }
  }

  const handleKeycloakSettings = () => {
    if (window.keycloak) {
      window.keycloak.accountManagement()
    }
  }

  return (
    <div className="space-y-6 animate-in">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold">Profil</h1>
        <p className="text-muted-foreground">
          Hesap ayarlarınızı yönetin
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Profile Card */}
        <Card className="lg:col-span-1">
          <CardContent className="pt-6">
            <div className="flex flex-col items-center text-center">
              <Avatar className="h-24 w-24 mb-4">
                <AvatarImage src={user?.avatar} />
                <AvatarFallback className="text-2xl bg-primary/10 text-primary">
                  {getInitials(user?.name || user?.username)}
                </AvatarFallback>
              </Avatar>
              <h2 className="text-xl font-semibold">{user?.name || user?.username}</h2>
              <p className="text-sm text-muted-foreground mb-4">{user?.email}</p>
              <div className="flex flex-wrap justify-center gap-2">
                {roles.map((role) => (
                  <Badge key={role} variant="secondary">
                    {role}
                  </Badge>
                ))}
              </div>
            </div>

            <Separator className="my-6" />

            <div className="space-y-4">
              <Button
                variant="outline"
                className="w-full justify-start"
                onClick={handleKeycloakSettings}
              >
                <Key className="mr-2 h-4 w-4" />
                Şifre Değiştir
              </Button>
              <Button
                variant="outline"
                className="w-full justify-start"
                onClick={handleKeycloakSettings}
              >
                <Shield className="mr-2 h-4 w-4" />
                Güvenlik Ayarları
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* Settings Tabs */}
        <Card className="lg:col-span-2">
          <Tabs defaultValue="general" className="w-full">
            <CardHeader>
              <TabsList className="grid w-full grid-cols-3">
                <TabsTrigger value="general">Genel</TabsTrigger>
                <TabsTrigger value="notifications">Bildirimler</TabsTrigger>
                <TabsTrigger value="preferences">Tercihler</TabsTrigger>
              </TabsList>
            </CardHeader>

            <CardContent>
              {/* General Tab */}
              <TabsContent value="general" className="space-y-6">
                <div className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="username">Kullanıcı Adı</Label>
                    <Input
                      id="username"
                      value={user?.username || ''}
                      disabled
                      className="bg-muted"
                    />
                    <p className="text-xs text-muted-foreground">
                      Kullanıcı adı değiştirilemez
                    </p>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="displayName">Görünen Ad</Label>
                    <Input
                      id="displayName"
                      value={displayName}
                      onChange={(e) => setDisplayName(e.target.value)}
                      placeholder="Adınız Soyadınız"
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="email">E-posta</Label>
                    <Input
                      id="email"
                      type="email"
                      value={user?.email || ''}
                      disabled
                      className="bg-muted"
                    />
                    <p className="text-xs text-muted-foreground">
                      E-posta değişikliği için Keycloak panelini kullanın
                    </p>
                  </div>

                  <Button
                    onClick={handleUpdateProfile}
                    disabled={updating}
                  >
                    {updating ? 'Kaydediliyor...' : 'Değişiklikleri Kaydet'}
                  </Button>
                </div>
              </TabsContent>

              {/* Notifications Tab */}
              <TabsContent value="notifications" className="space-y-6">
                <div className="space-y-4">
                  <div className="flex items-center justify-between py-2">
                    <div className="space-y-0.5">
                      <Label>E-posta Bildirimleri</Label>
                      <p className="text-sm text-muted-foreground">
                        Önemli güncellemeler için e-posta alın
                      </p>
                    </div>
                    <Switch
                      checked={emailNotifications}
                      onCheckedChange={setEmailNotifications}
                    />
                  </div>

                  <Separator />

                  <div className="flex items-center justify-between py-2">
                    <div className="space-y-0.5">
                      <Label>Push Bildirimleri</Label>
                      <p className="text-sm text-muted-foreground">
                        Tarayıcı bildirimleri alın
                      </p>
                    </div>
                    <Switch
                      checked={pushNotifications}
                      onCheckedChange={setPushNotifications}
                    />
                  </div>

                  <Separator />

                  <div className="flex items-center justify-between py-2">
                    <div className="space-y-0.5">
                      <Label>Fiyat Alarmları</Label>
                      <p className="text-sm text-muted-foreground">
                        Hedef fiyatlara ulaşıldığında bildirim alın
                      </p>
                    </div>
                    <Switch defaultChecked />
                  </div>

                  <Separator />

                  <div className="flex items-center justify-between py-2">
                    <div className="space-y-0.5">
                      <Label>Portföy Güncellemeleri</Label>
                      <p className="text-sm text-muted-foreground">
                        Portföyünüzdeki önemli değişiklikler
                      </p>
                    </div>
                    <Switch defaultChecked />
                  </div>
                </div>
              </TabsContent>

              {/* Preferences Tab */}
              <TabsContent value="preferences" className="space-y-6">
                <div className="space-y-4">
                  <div className="flex items-center justify-between py-2">
                    <div className="space-y-0.5">
                      <Label>Karanlık Mod</Label>
                      <p className="text-sm text-muted-foreground">
                        Karanlık tema kullan
                      </p>
                    </div>
                    <Switch
                      checked={theme === 'dark'}
                      onCheckedChange={(checked) => dispatch(setTheme(checked ? 'dark' : 'light'))}
                    />
                  </div>

                  <Separator />

                  <div className="flex items-center justify-between py-2">
                    <div className="space-y-0.5">
                      <Label>Kompakt Görünüm</Label>
                      <p className="text-sm text-muted-foreground">
                        Daha fazla veri göster
                      </p>
                    </div>
                    <Switch />
                  </div>

                  <Separator />

                  <div className="flex items-center justify-between py-2">
                    <div className="space-y-0.5">
                      <Label>Varsayılan Para Birimi</Label>
                      <p className="text-sm text-muted-foreground">
                        Fiyatlar için varsayılan para birimi
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
