import { Fragment } from 'react'
import { Menu, Transition } from '@headlessui/react'
import { 
  BellIcon, 
  UserCircleIcon,
  MagnifyingGlassIcon,
  ArrowRightOnRectangleIcon,
  Cog6ToothIcon,
} from '@heroicons/react/24/outline'
import { useAuth } from '../../context/AuthContext'
import { useNavigate } from 'react-router-dom'

export default function Header() {
  const { user, logout, isAuthenticated, login } = useAuth()
  const navigate = useNavigate()

  return (
    <header className="h-16 bg-dark-900/80 backdrop-blur-sm border-b border-dark-800 px-6 flex items-center justify-between sticky top-0 z-40">
      {/* Search */}
      <div className="flex-1 max-w-xl">
        <div className="relative">
          <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-dark-500" />
          <input
            type="text"
            placeholder="Enstrüman, haber ara..."
            className="w-full pl-10 pr-4 py-2 bg-dark-800 border border-dark-700 rounded-lg 
                     text-dark-100 placeholder-dark-500
                     focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500
                     transition-all"
          />
        </div>
      </div>

      {/* Right side */}
      <div className="flex items-center gap-4">
        {/* Notifications */}
        <button className="relative p-2 text-dark-400 hover:text-white transition-colors">
          <BellIcon className="w-6 h-6" />
          <span className="absolute top-1 right-1 w-2 h-2 bg-primary-500 rounded-full" />
        </button>

        {/* User Menu */}
        {isAuthenticated ? (
          <Menu as="div" className="relative">
            <Menu.Button className="flex items-center gap-3 p-2 rounded-lg hover:bg-dark-800 transition-colors">
              <div className="text-right hidden sm:block">
                <p className="text-sm font-medium text-white">{user?.fullName || user?.email}</p>
                <p className="text-xs text-dark-400">
                  {user?.roles?.includes('admin') ? 'Admin' : 'Kullanıcı'}
                </p>
              </div>
              <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center">
                <span className="text-white font-semibold">
                  {user?.firstName?.[0] || user?.email?.[0]?.toUpperCase()}
                </span>
              </div>
            </Menu.Button>

            <Transition
              as={Fragment}
              enter="transition ease-out duration-100"
              enterFrom="transform opacity-0 scale-95"
              enterTo="transform opacity-100 scale-100"
              leave="transition ease-in duration-75"
              leaveFrom="transform opacity-100 scale-100"
              leaveTo="transform opacity-0 scale-95"
            >
              <Menu.Items className="absolute right-0 mt-2 w-56 origin-top-right bg-dark-800 border border-dark-700 rounded-xl shadow-xl overflow-hidden">
                <div className="p-4 border-b border-dark-700">
                  <p className="text-sm font-medium text-white">{user?.fullName}</p>
                  <p className="text-xs text-dark-400">{user?.email}</p>
                </div>
                
                <div className="p-2">
                  <Menu.Item>
                    {({ active }) => (
                      <button
                        onClick={() => navigate('/profile')}
                        className={`${
                          active ? 'bg-dark-700' : ''
                        } flex items-center gap-3 w-full px-3 py-2 rounded-lg text-sm text-dark-200 transition-colors`}
                      >
                        <UserCircleIcon className="w-5 h-5" />
                        Profil
                      </button>
                    )}
                  </Menu.Item>
                  <Menu.Item>
                    {({ active }) => (
                      <button
                        className={`${
                          active ? 'bg-dark-700' : ''
                        } flex items-center gap-3 w-full px-3 py-2 rounded-lg text-sm text-dark-200 transition-colors`}
                      >
                        <Cog6ToothIcon className="w-5 h-5" />
                        Ayarlar
                      </button>
                    )}
                  </Menu.Item>
                </div>

                <div className="p-2 border-t border-dark-700">
                  <Menu.Item>
                    {({ active }) => (
                      <button
                        onClick={logout}
                        className={`${
                          active ? 'bg-red-500/10' : ''
                        } flex items-center gap-3 w-full px-3 py-2 rounded-lg text-sm text-red-400 transition-colors`}
                      >
                        <ArrowRightOnRectangleIcon className="w-5 h-5" />
                        Çıkış Yap
                      </button>
                    )}
                  </Menu.Item>
                </div>
              </Menu.Items>
            </Transition>
          </Menu>
        ) : (
          <button
            onClick={login}
            className="btn-primary"
          >
            Giriş Yap
          </button>
        )}
      </div>
    </header>
  )
}
