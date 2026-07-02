import { NavLink, useLocation } from 'react-router-dom'
import { useSelector, useDispatch } from 'react-redux'
import {
  LayoutDashboard,
  Newspaper,
  TrendingUp,
  DollarSign,
  BarChart3,
  Building,
  Wallet,
  LineChart,
  PieChart,
  ChevronDown,
  ChevronRight,
  Menu,
  X,
  Settings,
  Eye,
  BellRing,
  Bell,
  type LucideIcon,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import {
  selectSidebarCollapsed,
  selectSidebarMobileOpen,
  toggleSidebar,
  setMobileSidebarOpen
} from '@/store/slices/uiSlice'
import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

interface NavChild {
  name: string
  href?: string
  icon: LucideIcon
}

interface NavItemProps {
  item: NavChild & {
    children?: NavChild[]
  }
  collapsed: boolean
}

function NavItem({ item, collapsed }: NavItemProps) {
  const location = useLocation()
  const hasActiveChild = Boolean(item.children?.some(child => location.pathname.startsWith(child.href)))
  const [open, setOpen] = useState(hasActiveChild)

  useEffect(() => {
    if (hasActiveChild) {
      setOpen(true)
    }
  }, [hasActiveChild])

  if (item.children) {
    return (
      <Collapsible open={open && !collapsed} onOpenChange={setOpen}>
        <CollapsibleTrigger asChild>
          <button
            className={cn(
              "flex w-full items-center rounded-lg py-2.5 text-sm font-medium transition-colors",
              collapsed ? "justify-center px-0" : "gap-3 px-3",
              hasActiveChild
                ? "bg-white/10 text-blue-300"
                : "text-slate-300 hover:text-white hover:bg-white/5"
            )}
          >
            <item.icon className={cn("shrink-0", collapsed ? "h-6 w-6" : "h-5 w-5")} />
            {!collapsed && (
              <>
                <span className="flex-1 text-left">{item.name}</span>
                {open ? (
                  <ChevronDown className="h-4 w-4" />
                ) : (
                  <ChevronRight className="h-4 w-4" />
                )}
              </>
            )}
          </button>
        </CollapsibleTrigger>
        <CollapsibleContent className="pl-4">
          <div className="mt-1 space-y-1">
            {item.children.map((child) => (
              <NavLink
                key={child.name}
                to={child.href}
                className={({ isActive }) =>
                  cn(
                    "flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors",
                    isActive
                      ? "bg-primary/10 text-primary font-medium"
                      : "text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-foreground"
                  )
                }
              >
                <child.icon className="h-4 w-4 shrink-0" />
                <span>{child.name}</span>
              </NavLink>
            ))}
          </div>
        </CollapsibleContent>
      </Collapsible>
    )
  }

  const isActive = location.pathname === item.href ||
    (item.href !== '/' && location.pathname.startsWith(item.href))

  const content = (
    <NavLink
      to={item.href}
      className={cn(
        "group relative flex items-center rounded-lg transition-all",
        collapsed ? "w-10 h-10 justify-center" : "w-full gap-3 px-3 py-2.5 justify-start",
        isActive
          ? "bg-white/10 text-blue-300"
          : "text-slate-400 hover:text-white hover:bg-white/5"
      )}
    >
      {collapsed && isActive && (
        <div className="absolute left-0 top-1/2 -translate-y-1/2 -ml-5 w-1 h-6 bg-blue-400 rounded-r-full shadow-[0_0_10px_rgba(59,130,246,0.5)]" />
      )}
      <item.icon className={cn("shrink-0", collapsed ? "h-6 w-6" : "h-5 w-5")} />
      {!collapsed && <span className="text-sm font-medium">{item.name}</span>}
    </NavLink>
  )

  if (collapsed) {
    return (
      <Tooltip>
        <TooltipTrigger asChild>{content}</TooltipTrigger>
        <TooltipContent side="right">{item.name}</TooltipContent>
      </Tooltip>
    )
  }

  return content
}

export function Sidebar() {
  const dispatch = useDispatch()
  const collapsed = useSelector(selectSidebarCollapsed)
  const mobileOpen = useSelector(selectSidebarMobileOpen)
  const { t } = useTranslation()

  const navigation = [
    {
      name: t('nav.home'),
      href: '/',
      icon: LayoutDashboard
    },
    {
      name: t('nav.news'),
      href: '/news',
      icon: Newspaper
    },
    {
      name: t('nav.market'),
      icon: TrendingUp,
      children: [
        { name: t('market.currencies'), href: '/market/currencies', icon: DollarSign },
        { name: t('market.stocks'), href: '/market/stocks', icon: BarChart3 },
        { name: t('market.bonds'), href: '/market/bonds', icon: Building },
        { name: t('market.funds'), href: '/market/funds', icon: Wallet },
        { name: t('market.viop'), href: '/market/viop', icon: LineChart },
      ],
    },
    {
      name: t('nav.portfolio'),
      href: '/portfolio',
      icon: PieChart
    },
    {
      name: t('nav.analysis'),
      href: '/analysis',
      icon: BarChart3
    },
    {
      name: t('nav.watchlist'),
      href: '/watchlist',
      icon: Eye
    },
    {
      name: t('nav.alerts'),
      href: '/alerts',
      icon: BellRing
    },
    {
      name: t('nav.notifications'),
      href: '/notifications',
      icon: Bell
    },
    {
      name: t('nav.settings'),
      href: '/settings',
      icon: Settings
    },
  ]

  return (
    <TooltipProvider delayDuration={0}>
      {/* Mobile overlay */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 lg:hidden"
          onClick={() => dispatch(setMobileSidebarOpen(false))}
        />
      )}

      {/* Sidebar */}
      <aside
        className={cn(
          "fixed left-0 top-0 z-50 flex h-screen flex-col items-center py-6 transition-all duration-300",
          "bg-gradient-to-b from-blue-900 via-slate-900 to-slate-950 border-r border-white/10 shadow-2xl",
          collapsed ? "w-20" : "w-64 items-stretch",
          mobileOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0"
        )}
      >
        {/* Logo */}
        <div className={cn(
          "flex items-center transition-all duration-300",
          collapsed ? "justify-center mb-10" : "justify-between h-16 px-4 border-b border-white/10"
        )}>
          <div className="flex items-center gap-3">
            <div className={cn(
              "flex items-center justify-center rounded-xl overflow-hidden cursor-pointer hover:scale-105 transition-transform",
              "bg-gradient-to-br from-white to-blue-100 shadow-lg shadow-blue-900/50",
              collapsed ? "w-10 h-10" : "w-10 h-10"
            )}>
              <img src="/logo.png" alt="MintStack" className="h-full w-full object-cover" />
            </div>
            {!collapsed && (
              <div>
                <h1 className="text-lg font-bold text-white">MintStack</h1>
                <p className="text-xs text-slate-400">{t('app.tagline')}</p>
              </div>
            )}
          </div>

          {/* Mobile close button */}
          {!collapsed && (
            <Button
              variant="ghost"
              size="icon-sm"
              className="lg:hidden text-slate-400"
              onClick={() => dispatch(setMobileSidebarOpen(false))}
            >
              <X className="h-5 w-5" />
            </Button>
          )}
        </div>

        {/* Navigation */}
        <nav className={cn(
          "flex-1 flex flex-col w-full",
          collapsed ? "gap-6 items-center" : "gap-1 px-3 py-4"
        )}>
          {navigation.map((item) => (
            <NavItem key={item.name || item.href} item={item} collapsed={collapsed} />
          ))}
        </nav>

        {/* Footer */}
        <div className="mt-auto flex flex-col items-center">
          {/* Desktop collapse toggle */}
          <button
            className="w-10 h-10 flex items-center justify-center rounded-lg text-slate-400 hover:text-white hover:bg-white/5 transition-colors"
            onClick={() => dispatch(toggleSidebar())}
          >
            <Menu className="h-6 w-6" />
          </button>
          {!collapsed && (
            <p className="text-center text-xs text-slate-500 mt-4 mb-2">
              {t('footer.copyright', { year: 2026 })}
            </p>
          )}
        </div>
      </aside>
    </TooltipProvider>
  )
}

export default Sidebar
