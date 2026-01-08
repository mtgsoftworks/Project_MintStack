import { NavLink } from 'react-router-dom'
import { 
  HomeIcon, 
  NewspaperIcon, 
  CurrencyDollarIcon,
  ChartBarIcon,
  BriefcaseIcon,
  ChartPieIcon,
  BuildingLibraryIcon,
  BanknotesIcon,
  ArrowTrendingUpIcon,
} from '@heroicons/react/24/outline'

const navigation = [
  { name: 'Dashboard', href: '/', icon: HomeIcon },
  { name: 'Haberler', href: '/news', icon: NewspaperIcon },
  { 
    name: 'Piyasalar',
    icon: ChartBarIcon,
    children: [
      { name: 'Döviz', href: '/market/currencies', icon: CurrencyDollarIcon },
      { name: 'Hisseler', href: '/market/stocks', icon: ChartBarIcon },
      { name: 'Tahvil/Bono', href: '/market/bonds', icon: BuildingLibraryIcon },
      { name: 'Fonlar', href: '/market/funds', icon: BanknotesIcon },
      { name: 'VIOP', href: '/market/viop', icon: ArrowTrendingUpIcon },
    ],
  },
  { name: 'Portföy', href: '/portfolio', icon: BriefcaseIcon },
  { name: 'Analiz', href: '/analysis', icon: ChartPieIcon },
]

export default function Sidebar() {
  return (
    <aside className="fixed left-0 top-0 h-screen w-64 bg-dark-900 border-r border-dark-800 flex flex-col z-50">
      {/* Logo */}
      <div className="h-16 flex items-center px-6 border-b border-dark-800">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center">
            <span className="text-white font-bold text-xl">M</span>
          </div>
          <div>
            <h1 className="text-lg font-bold text-white">MintStack</h1>
            <p className="text-xs text-dark-500">Finance Portal</p>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 py-6 px-3 overflow-y-auto">
        <ul className="space-y-1">
          {navigation.map((item) => (
            <li key={item.name}>
              {item.children ? (
                <div className="mb-2">
                  <div className="flex items-center gap-3 px-3 py-2 text-dark-400 text-sm font-medium">
                    <item.icon className="w-5 h-5" />
                    {item.name}
                  </div>
                  <ul className="ml-8 space-y-1">
                    {item.children.map((child) => (
                      <li key={child.name}>
                        <NavLink
                          to={child.href}
                          className={({ isActive }) =>
                            `flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-all ${
                              isActive
                                ? 'bg-primary-500/10 text-primary-400'
                                : 'text-dark-400 hover:text-white hover:bg-dark-800'
                            }`
                          }
                        >
                          <child.icon className="w-4 h-4" />
                          {child.name}
                        </NavLink>
                      </li>
                    ))}
                  </ul>
                </div>
              ) : (
                <NavLink
                  to={item.href}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all ${
                      isActive
                        ? 'bg-primary-500/10 text-primary-400 border-l-2 border-primary-500'
                        : 'text-dark-400 hover:text-white hover:bg-dark-800'
                    }`
                  }
                >
                  <item.icon className="w-5 h-5" />
                  {item.name}
                </NavLink>
              )}
            </li>
          ))}
        </ul>
      </nav>

      {/* Footer */}
      <div className="p-4 border-t border-dark-800">
        <div className="card p-4 bg-gradient-to-br from-primary-500/10 to-accent-500/10">
          <p className="text-sm font-medium text-white mb-1">Pro'ya Yükseltin</p>
          <p className="text-xs text-dark-400 mb-3">
            Daha fazla özellik ve analiz araçlarına erişin
          </p>
          <button className="w-full btn-accent text-sm">
            Yükselt
          </button>
        </div>
      </div>
    </aside>
  )
}
