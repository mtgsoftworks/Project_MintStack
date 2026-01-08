import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { UserCircleIcon, KeyIcon, ShieldCheckIcon } from '@heroicons/react/24/outline'
import { userService } from '../services/userService'
import { useAuth } from '../context/AuthContext'
import Loading from '../components/common/Loading'
import toast from 'react-hot-toast'

export default function ProfilePage() {
  const { user: authUser, keycloak } = useAuth()
  const queryClient = useQueryClient()
  const [isEditing, setIsEditing] = useState(false)
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
  })

  const { data: profile, isLoading } = useQuery({
    queryKey: ['profile'],
    queryFn: userService.getProfile,
    onSuccess: (data) => {
      setFormData({
        firstName: data.firstName || '',
        lastName: data.lastName || '',
      })
    },
  })

  const updateMutation = useMutation({
    mutationFn: userService.updateProfile,
    onSuccess: () => {
      queryClient.invalidateQueries(['profile'])
      setIsEditing(false)
      toast.success('Profil güncellendi')
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Bir hata oluştu')
    },
  })

  const handleSubmit = (e) => {
    e.preventDefault()
    updateMutation.mutate(formData)
  }

  const handleManageAccount = () => {
    keycloak?.accountManagement()
  }

  if (isLoading) {
    return <Loading />
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-in">
      <div>
        <h1 className="text-2xl font-bold text-white">Profil</h1>
        <p className="text-dark-400">Hesap bilgilerinizi yönetin</p>
      </div>

      {/* Profile Card */}
      <div className="card p-6">
        <div className="flex items-start gap-6">
          <div className="w-24 h-24 rounded-full bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center flex-shrink-0">
            <span className="text-3xl font-bold text-white">
              {profile?.firstName?.[0] || profile?.email?.[0]?.toUpperCase() || '?'}
            </span>
          </div>
          
          <div className="flex-1">
            {isEditing ? (
              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label">Ad</label>
                    <input
                      type="text"
                      value={formData.firstName}
                      onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                      className="input"
                    />
                  </div>
                  <div>
                    <label className="label">Soyad</label>
                    <input
                      type="text"
                      value={formData.lastName}
                      onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                      className="input"
                    />
                  </div>
                </div>
                <div className="flex gap-3">
                  <button
                    type="button"
                    onClick={() => setIsEditing(false)}
                    className="btn-secondary"
                  >
                    İptal
                  </button>
                  <button
                    type="submit"
                    disabled={updateMutation.isPending}
                    className="btn-primary"
                  >
                    {updateMutation.isPending ? 'Kaydediliyor...' : 'Kaydet'}
                  </button>
                </div>
              </form>
            ) : (
              <>
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h2 className="text-xl font-semibold text-white">
                      {profile?.fullName || 'İsim belirtilmemiş'}
                    </h2>
                    <p className="text-dark-400">{profile?.email}</p>
                  </div>
                  <button
                    onClick={() => setIsEditing(true)}
                    className="btn-secondary text-sm"
                  >
                    Düzenle
                  </button>
                </div>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <p className="text-dark-500">Kayıt Tarihi</p>
                    <p className="text-dark-200">
                      {profile?.createdAt && new Date(profile.createdAt).toLocaleDateString('tr-TR')}
                    </p>
                  </div>
                  <div>
                    <p className="text-dark-500">Portföy Sayısı</p>
                    <p className="text-dark-200">{profile?.portfolioCount || 0}</p>
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
      </div>

      {/* Account Settings */}
      <div className="card overflow-hidden">
        <div className="p-4 border-b border-dark-800">
          <h2 className="text-lg font-semibold text-white">Hesap Ayarları</h2>
        </div>
        <div className="divide-y divide-dark-800">
          <button
            onClick={handleManageAccount}
            className="w-full flex items-center gap-4 p-4 hover:bg-dark-800/50 transition-colors text-left"
          >
            <div className="w-10 h-10 rounded-lg bg-dark-800 flex items-center justify-center">
              <UserCircleIcon className="w-5 h-5 text-dark-400" />
            </div>
            <div className="flex-1">
              <p className="text-white font-medium">Hesap Yönetimi</p>
              <p className="text-dark-400 text-sm">Keycloak hesap ayarları</p>
            </div>
            <span className="text-dark-500">→</span>
          </button>
          
          <button
            onClick={handleManageAccount}
            className="w-full flex items-center gap-4 p-4 hover:bg-dark-800/50 transition-colors text-left"
          >
            <div className="w-10 h-10 rounded-lg bg-dark-800 flex items-center justify-center">
              <KeyIcon className="w-5 h-5 text-dark-400" />
            </div>
            <div className="flex-1">
              <p className="text-white font-medium">Şifre Değiştir</p>
              <p className="text-dark-400 text-sm">Hesap şifrenizi güncelleyin</p>
            </div>
            <span className="text-dark-500">→</span>
          </button>
          
          <button
            onClick={handleManageAccount}
            className="w-full flex items-center gap-4 p-4 hover:bg-dark-800/50 transition-colors text-left"
          >
            <div className="w-10 h-10 rounded-lg bg-dark-800 flex items-center justify-center">
              <ShieldCheckIcon className="w-5 h-5 text-dark-400" />
            </div>
            <div className="flex-1">
              <p className="text-white font-medium">İki Faktörlü Doğrulama</p>
              <p className="text-dark-400 text-sm">2FA ile hesabınızı güvence altına alın</p>
            </div>
            <span className="text-dark-500">→</span>
          </button>
        </div>
      </div>

      {/* Roles */}
      <div className="card p-6">
        <h2 className="text-lg font-semibold text-white mb-4">Roller</h2>
        <div className="flex flex-wrap gap-2">
          {authUser?.roles?.map((role) => (
            <span key={role} className="badge-info">
              {role}
            </span>
          ))}
          {(!authUser?.roles || authUser.roles.length === 0) && (
            <p className="text-dark-400">Rol atanmamış</p>
          )}
        </div>
      </div>
    </div>
  )
}
