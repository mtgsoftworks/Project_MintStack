import { Outlet } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { cn } from '@/lib/utils'
import { Sidebar } from './Sidebar'
import { Header } from './Header'
import { Toaster } from '@/components/ui/sonner'
import { selectSidebarCollapsed } from '@/store/slices/uiSlice'

export function Layout() {
  const collapsed = useSelector(selectSidebarCollapsed)

  return (
    <div className="min-h-screen bg-background">
      {/* Sidebar */}
      <Sidebar />

      {/* Header */}
      <Header />

      {/* Main Content */}
      <main
        className={cn(
          "min-h-screen pt-16 transition-all duration-300",
          collapsed ? "lg:pl-[68px]" : "lg:pl-64"
        )}
      >
        <div className="container mx-auto p-4 lg:p-6">
          <Outlet />
        </div>
      </main>

      {/* Toast Notifications */}
      <Toaster position="top-right" />
    </div>
  )
}

export default Layout
