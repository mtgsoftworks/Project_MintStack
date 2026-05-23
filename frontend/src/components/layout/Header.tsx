import { useEffect, useMemo, useState } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import {
  Bell,
  Search,
  User,
  Settings,
  LogOut,
  ChevronDown,
  Sun,
  Moon,
} from 'lucide-react'
import { HamburgerIcon } from '@/components/ui/hamburger'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useTranslation } from 'react-i18next'
import {
  selectSidebarCollapsed,
  setMobileSidebarOpen,
  selectTheme,
  toggleTheme,
} from '@/store/slices/uiSlice'
import { logout, selectUser, selectIsAuthenticated } from '@/store/slices/authSlice'
import { getInitials } from '@/lib/utils'
import { LanguageSwitcher } from '@/components/common/LanguageSwitcher'
import { useGetNotificationsQuery, useMarkNotificationReadMutation } from '@/store/api/userApi'
import websocketService from '@/services/websocketService'
import { baseApi } from '@/store/api/baseApi'

export function Header() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const collapsed = useSelector(selectSidebarCollapsed)
  const user = useSelector(selectUser)
  const isAuthenticated = useSelector(selectIsAuthenticated)
  const theme = useSelector(selectTheme)
  const { t } = useTranslation()
  const [searchQuery, setSearchQuery] = useState('')

  const { data: notificationsData, refetch: refetchNotifications } = useGetNotificationsQuery(
    { page: 0, size: 10 },
    { skip: !isAuthenticated }
  )
  
  const [markAsRead] = useMarkNotificationReadMutation()

  const notifications = useMemo(() => {
    return notificationsData?.data || []
  }, [notificationsData])
  
  const notificationCount = useMemo(() => {
    return notifications.filter(n => !n.isRead).length
  }, [notifications])

  useEffect(() => {
    if (!isAuthenticated) {
      return undefined
    }

    const topic = '/user/queue/notifications'
    const handleNotification = () => {
      dispatch(baseApi.util.invalidateTags([{ type: 'User', id: 'NOTIFICATIONS' }]))
      refetchNotifications()
    }

    websocketService.subscribe(topic, handleNotification)
    return () => websocketService.unsubscribe(topic)
  }, [dispatch, isAuthenticated, refetchNotifications])

  const handleNotificationClick = async (notification) => {
    if (!notification.isRead) {
      try {
        await markAsRead(notification.id).unwrap()
      } catch (error) {
        console.error('Failed to mark notification as read:', error)
      }
    }
    navigate('/notifications')
  }

  const clearLocalSession = () => {
    websocketService.disconnect()
    dispatch(baseApi.util.resetApiState())
    dispatch(logout())
  }

  const handleLogout = async () => {
    const redirectUri = `${window.location.origin}/login`
    clearLocalSession()

    try {
      if (window.keycloak?.logout) {
        await window.keycloak.logout({ redirectUri })
        return
      }
    } catch (error) {
      console.error('Logout failed:', error)
    }

    navigate('/login', { replace: true })
  }

  const handleSearchSubmit = (event) => {
    event.preventDefault()
    const query = searchQuery.trim()
    if (!query) {
      return
    }

    navigate(`/market/search?query=${encodeURIComponent(query)}`)
  }

  return (
    <header
      className={cn(
        "fixed top-0 right-0 z-40 flex h-16 items-center justify-between border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 px-4 lg:px-6 transition-all duration-300",
        collapsed ? "left-[68px]" : "left-64",
        "left-0 lg:left-64",
        collapsed && "lg:left-[68px]"
      )}
    >
      {/* Left side */}
      <div className="flex items-center gap-4">
        {/* Mobile menu button with animation */}
        <div className="lg:hidden">
          <HamburgerIcon
            isOpen={false}
            onClick={() => dispatch(setMobileSidebarOpen(true))}
            className="text-foreground hover:text-primary transition-colors"
          />
        </div>

        {/* Search */}
        <form className="hidden md:flex relative" onSubmit={handleSearchSubmit}>
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            type="search"
            placeholder={t('header.searchPlaceholder')}
            className="w-64 pl-9 bg-muted/50"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </form>
      </div>

      {/* Right side */}
      <div className="flex items-center gap-2">
        {/* Mobile search */}
        <Button
          variant="ghost"
          size="icon"
          className="md:hidden"
          onClick={() => navigate('/market/search')}
          aria-label={t('common.search')}
          title={t('common.search')}
        >
          <Search className="h-5 w-5" />
        </Button>

        {/* Theme Toggle */}
        <Button
          variant="ghost"
          size="icon"
          onClick={() => dispatch(toggleTheme())}
          aria-label={theme === 'dark' ? t('settings.appearance.lightMode') : t('settings.appearance.darkMode')}
          title={theme === 'dark' ? t('settings.appearance.lightMode') : t('settings.appearance.darkMode')}
        >
          {theme === 'dark' ? (
            <Sun className="h-5 w-5 text-yellow-500" />
          ) : (
            <Moon className="h-5 w-5 text-slate-700" />
          )}
        </Button>

        {/* Language Switcher */}
        <div className="hidden sm:block">
          <LanguageSwitcher />
        </div>

        {/* Notifications */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" className="relative">
              <Bell className="h-5 w-5" />
              {notificationCount > 0 && (
                <Badge
                  variant="danger-solid"
                  className="absolute -top-1 -right-1 h-5 w-5 rounded-full p-0 flex items-center justify-center text-[10px]"
                >
                  {notificationCount}
                </Badge>
              )}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-80">
            <DropdownMenuLabel className="flex items-center justify-between">
              <span>{t('header.notifications.title')}</span>
              {notificationCount > 0 && (
                <Badge variant="secondary">
                  {t('header.notifications.new', { count: notificationCount })}
                </Badge>
              )}
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <div className="max-h-64 overflow-y-auto">
              {notifications.length === 0 ? (
                <div className="py-4 text-center text-muted-foreground text-sm">
                  {t('notificationsPage.emptyCategory')}
                </div>
              ) : (
                notifications.slice(0, 5).map((item) => (
                  <DropdownMenuItem
                    key={item.id}
                    className={cn(
                      "flex flex-col items-start gap-1 py-3 cursor-pointer",
                      !item.isRead && "bg-primary/5"
                    )}
                    onClick={() => handleNotificationClick(item)}
                  >
                    <span className="font-medium">{item.title}</span>
                    <span className="text-xs text-muted-foreground">{item.message}</span>
                  </DropdownMenuItem>
                ))
              )}
            </div>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              className="justify-center text-primary cursor-pointer"
              onClick={() => navigate('/notifications')}
            >
              {t('header.notifications.viewAll')}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>

        {/* User Menu */}
        {isAuthenticated && user ? (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" className="flex items-center gap-2 px-2">
                <Avatar className="h-8 w-8">
                  <AvatarImage src={user.avatar} />
                  <AvatarFallback className="bg-primary/10 text-primary text-sm">
                    {getInitials(user.name || user.username)}
                  </AvatarFallback>
                </Avatar>
                <div className="hidden md:flex flex-col items-start">
                  <span className="text-sm font-medium">
                    {user.name || user.username}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {user.email}
                  </span>
                </div>
                <ChevronDown className="h-4 w-4 text-muted-foreground hidden md:block" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>
                <div className="flex flex-col">
                  <span>{user.name || user.username}</span>
                  <span className="text-xs font-normal text-muted-foreground">
                    {user.email}
                  </span>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => navigate('/profile')} className="cursor-pointer">
                <User className="mr-2 h-4 w-4" />
                {t('nav.profile')}
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => navigate('/settings')} className="cursor-pointer">
                <Settings className="mr-2 h-4 w-4" />
                {t('nav.settings')}
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={handleLogout} className="cursor-pointer text-danger">
                <LogOut className="mr-2 h-4 w-4" />
                {t('auth.logout')}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        ) : (
          <Button onClick={() => navigate('/login')}>
            {t('auth.login')}
          </Button>
        )}
      </div>
    </header>
  )
}

export default Header
