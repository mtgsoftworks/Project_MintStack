import { useSelector, useDispatch } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import {
  Menu,
  Bell,
  Search,
  User,
  Settings,
  LogOut,
  ChevronDown,
} from 'lucide-react'
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
} from '@/store/slices/uiSlice'
import { selectUser, selectIsAuthenticated } from '@/store/slices/authSlice'
import { getInitials } from '@/lib/utils'
import { LanguageSwitcher } from '@/components/common/LanguageSwitcher'

export function Header() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const collapsed = useSelector(selectSidebarCollapsed)
  const user = useSelector(selectUser)
  const isAuthenticated = useSelector(selectIsAuthenticated)
  const { t } = useTranslation()

  const notifications = [
    {
      id: 'portfolio',
      title: t('header.notifications.items.portfolioUpdated.title'),
      description: t('header.notifications.items.portfolioUpdated.description', {
        symbol: 'THYAO',
        change: '2.5',
      }),
      time: t('header.notifications.items.portfolioUpdated.time'),
    },
    {
      id: 'currency',
      title: t('header.notifications.items.currencyAlert.title'),
      description: t('header.notifications.items.currencyAlert.description', {
        pair: 'USD/TRY',
      }),
      time: t('header.notifications.items.currencyAlert.time'),
    },
    {
      id: 'news',
      title: t('header.notifications.items.news.title'),
      description: t('header.notifications.items.news.description'),
      time: t('header.notifications.items.news.time'),
    },
  ]
  const notificationCount = notifications.length

  const handleLogout = () => {
    // Keycloak logout
    if (window.keycloak) {
      window.keycloak.logout({ redirectUri: window.location.origin })
    }
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
        {/* Mobile menu button */}
        <Button
          variant="ghost"
          size="icon"
          className="lg:hidden"
          onClick={() => dispatch(setMobileSidebarOpen(true))}
        >
          <Menu className="h-5 w-5" />
        </Button>

        {/* Search */}
        <div className="hidden md:flex relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            type="search"
            placeholder={t('header.searchPlaceholder')}
            className="w-64 pl-9 bg-muted/50"
          />
        </div>
      </div>

      {/* Right side */}
      <div className="flex items-center gap-2">
        {/* Mobile search */}
        <Button variant="ghost" size="icon" className="md:hidden">
          <Search className="h-5 w-5" />
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
              <Badge
                variant="danger-solid"
                className="absolute -top-1 -right-1 h-5 w-5 rounded-full p-0 flex items-center justify-center text-[10px]"
              >
                {notificationCount}
              </Badge>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-80">
            <DropdownMenuLabel className="flex items-center justify-between">
              <span>{t('header.notifications.title')}</span>
              <Badge variant="secondary">
                {t('header.notifications.new', { count: notificationCount })}
              </Badge>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <div className="max-h-64 overflow-y-auto">
              {notifications.map((item) => (
                <DropdownMenuItem key={item.id} className="flex flex-col items-start gap-1 py-3 cursor-pointer">
                  <span className="font-medium">{item.title}</span>
                  <span className="text-xs text-muted-foreground">{item.description}</span>
                  <span className="text-xs text-muted-foreground">{item.time}</span>
                </DropdownMenuItem>
              ))}
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
