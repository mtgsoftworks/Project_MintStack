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
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import {
  selectSidebarCollapsed,
  selectSidebarMobileOpen,
  toggleSidebar,
  setMobileSidebarOpen
} from '@/store/slices/uiSlice'
import { useState } from 'react'

const navigation = [
  {
    name: 'Dashboard',
    href: '/',
    icon: LayoutDashboard
  },
  {
    name: 'Haberler',
    href: '/news',
    icon: Newspaper
  },
  {
    name: 'Piyasalar',
    icon: TrendingUp,
    children: [
      { name: 'Döviz', href: '/market/currencies', icon: DollarSign },
      { name: 'Hisseler', href: '/market/stocks', icon: BarChart3 },
      { name: 'Tahvil/Bono', href: '/market/bonds', icon: Building },
      { name: 'Fonlar', href: '/market/funds', icon: Wallet },
      { name: 'VIOP', href: '/market/viop', icon: LineChart },
    ],
  },
  {
    name: 'Portföy',
    href: '/portfolio',
    icon: PieChart
  },
  {
    href: '/analysis',
    icon: BarChart3
  },
  {
    name: 'Ayarlar',
    href: '/settings',
    icon: Settings
  },
]

function NavItem({ item, collapsed }) {
  const location = useLocation()
  const [open, setOpen] = useState(
    item.children?.some(child => location.pathname.startsWith(child.href))
  )

  if (item.children) {
    return (
      <Collapsible open={open && !collapsed} onOpenChange={setOpen}>
        <CollapsibleTrigger asChild>
          <button
            className={cn(
              "flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
              "text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-foreground"
            )}
          >
            <item.icon className="h-5 w-5 shrink-0" />
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

  const content = (
    <NavLink
      to={item.href}
      className={({ isActive }) =>
        cn(
          "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
          isActive
            ? "bg-primary/10 text-primary border-l-2 border-primary"
            : "text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-foreground"
        )
      }
    >
      <item.icon className="h-5 w-5 shrink-0" />
      {!collapsed && <span>{item.name}</span>}
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
          "fixed left-0 top-0 z-50 flex h-screen flex-col bg-sidebar border-r border-sidebar-border transition-all duration-300",
          collapsed ? "w-[68px]" : "w-64",
          mobileOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0"
        )}
      >
        {/* Logo */}
        <div className="flex h-16 items-center justify-between border-b border-sidebar-border px-4">
          <div className="flex items-center gap-3">
            <div className="flex bg-white h-10 w-10 items-center justify-center rounded-xl overflow-hidden">
              <img src="/logo.png" alt="MintStack" className="h-full w-full object-cover" />
            </div>
            {!collapsed && (
              <div>
                <h1 className="text-lg font-bold text-white">MintStack</h1>
                <p className="text-xs text-sidebar-foreground/50">Finance Portal</p>
              </div>
            )}
          </div>

          {/* Mobile close button */}
          <Button
            variant="ghost"
            size="icon-sm"
            className="lg:hidden text-sidebar-foreground"
            onClick={() => dispatch(setMobileSidebarOpen(false))}
          >
            <X className="h-5 w-5" />
          </Button>
        </div>

        {/* Navigation */}
        <ScrollArea className="flex-1 py-4">
          <nav className="space-y-1 px-3">
            {navigation.map((item) => (
              <NavItem key={item.name} item={item} collapsed={collapsed} />
            ))}
          </nav>
        </ScrollArea>

        {/* Footer */}
        <div className="border-t border-sidebar-border p-4">
          {!collapsed && (
            <p className="text-center text-xs text-sidebar-foreground/50">
              © 2026 MintStack Finance
            </p>
          )}
          {/* Desktop collapse toggle */}
          <div className="mt-2 hidden lg:flex justify-center">
            <Button
              variant="ghost"
              size="icon-sm"
              className="text-sidebar-foreground/70 hover:text-sidebar-foreground"
              onClick={() => dispatch(toggleSidebar())}
            >
              <Menu className="h-5 w-5" />
            </Button>
          </div>
        </div>
      </aside>
    </TooltipProvider>
  )
}

export default Sidebar
