import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { 
  ArrowTrendingUpIcon, 
  ArrowTrendingDownIcon,
  CurrencyDollarIcon,
  BriefcaseIcon,
  NewspaperIcon,
  ChartBarIcon,
} from '@heroicons/react/24/outline'
import { marketService } from '../services/marketService'
import { portfolioService } from '../services/portfolioService'
import { newsService } from '../services/newsService'
import Loading from '../components/common/Loading'
import PriceChart from '../components/charts/PriceChart'
import PieChart from '../components/charts/PieChart'

export default function DashboardPage() {
  const { data: currencies, isLoading: currenciesLoading } = useQuery({
    queryKey: ['currencies'],
    queryFn: marketService.getCurrencies,
  })

  const { data: portfolios, isLoading: portfoliosLoading } = useQuery({
    queryKey: ['portfolios'],
    queryFn: portfolioService.getPortfolios,
  })

  const { data: news, isLoading: newsLoading } = useQuery({
    queryKey: ['latestNews'],
    queryFn: newsService.getLatestNews,
  })

  // Mock data for demo chart
  const chartData = Array.from({ length: 30 }, (_, i) => ({
    date: new Date(Date.now() - (29 - i) * 24 * 60 * 60 * 1000).toISOString(),
    close: 34.5 + Math.random() * 2,
  }))

  const totalPortfolioValue = portfolios?.reduce((sum, p) => sum + (p.totalValue || 0), 0) || 0
  const totalProfitLoss = portfolios?.reduce((sum, p) => sum + (p.profitLoss || 0), 0) || 0

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('tr-TR', {
      style: 'currency',
      currency: 'TRY',
    }).format(value)
  }

  return (
    <div className="space-y-6 animate-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Dashboard</h1>
          <p className="text-dark-400">Finansal piyasalara genel bakış</p>
        </div>
        <div className="text-sm text-dark-500">
          Son güncelleme: {new Date().toLocaleTimeString('tr-TR')}
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="stat-card">
          <div className="flex items-center justify-between">
            <div className="w-12 h-12 rounded-lg bg-primary-500/10 flex items-center justify-center">
              <BriefcaseIcon className="w-6 h-6 text-primary-400" />
            </div>
            {totalProfitLoss >= 0 ? (
              <span className="badge-success">
                <ArrowTrendingUpIcon className="w-4 h-4 mr-1" />
                Kar
              </span>
            ) : (
              <span className="badge-danger">
                <ArrowTrendingDownIcon className="w-4 h-4 mr-1" />
                Zarar
              </span>
            )}
          </div>
          <div className="mt-4">
            <p className="stat-value">{formatCurrency(totalPortfolioValue)}</p>
            <p className="stat-label">Toplam Portföy Değeri</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="flex items-center justify-between">
            <div className="w-12 h-12 rounded-lg bg-accent-500/10 flex items-center justify-center">
              <CurrencyDollarIcon className="w-6 h-6 text-accent-400" />
            </div>
          </div>
          <div className="mt-4">
            <p className="stat-value">
              {currencies?.[0]?.sellingRate?.toFixed(4) || '—'}
            </p>
            <p className="stat-label">USD/TRY Satış</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="flex items-center justify-between">
            <div className="w-12 h-12 rounded-lg bg-blue-500/10 flex items-center justify-center">
              <ChartBarIcon className="w-6 h-6 text-blue-400" />
            </div>
          </div>
          <div className="mt-4">
            <p className="stat-value">{portfolios?.length || 0}</p>
            <p className="stat-label">Aktif Portföy</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="flex items-center justify-between">
            <div className="w-12 h-12 rounded-lg bg-purple-500/10 flex items-center justify-center">
              <NewspaperIcon className="w-6 h-6 text-purple-400" />
            </div>
          </div>
          <div className="mt-4">
            <p className="stat-value">{news?.length || 0}</p>
            <p className="stat-label">Güncel Haber</p>
          </div>
        </div>
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Chart */}
        <div className="lg:col-span-2 card p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-lg font-semibold text-white">USD/TRY</h2>
              <p className="text-dark-400 text-sm">Son 30 gün</p>
            </div>
            <Link to="/market/currencies" className="text-primary-400 text-sm hover:text-primary-300">
              Tümünü Gör →
            </Link>
          </div>
          <PriceChart data={chartData} height={250} />
        </div>

        {/* Portfolio Distribution */}
        <div className="card p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-lg font-semibold text-white">Portföy Dağılımı</h2>
          </div>
          {portfoliosLoading ? (
            <Loading />
          ) : portfolios?.length > 0 ? (
            <PieChart
              data={portfolios.map(p => ({
                name: p.name,
                value: p.totalValue || 0,
                percent: ((p.totalValue || 0) / totalPortfolioValue * 100).toFixed(1),
              }))}
              height={200}
            />
          ) : (
            <div className="text-center py-8">
              <p className="text-dark-400 text-sm mb-4">Henüz portföy bulunmuyor</p>
              <Link to="/portfolio" className="btn-primary text-sm">
                Portföy Oluştur
              </Link>
            </div>
          )}
        </div>
      </div>

      {/* Bottom Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Currency Rates */}
        <div className="card p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-lg font-semibold text-white">Döviz Kurları</h2>
            <Link to="/market/currencies" className="text-primary-400 text-sm hover:text-primary-300">
              Tümünü Gör →
            </Link>
          </div>
          {currenciesLoading ? (
            <Loading />
          ) : (
            <div className="space-y-3">
              {currencies?.slice(0, 5).map((currency) => (
                <div
                  key={currency.currencyCode}
                  className="flex items-center justify-between py-2 border-b border-dark-800 last:border-0"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-dark-800 flex items-center justify-center">
                      <span className="text-xs font-medium text-dark-300">
                        {currency.currencyCode}
                      </span>
                    </div>
                    <span className="text-dark-200">{currency.currencyName}</span>
                  </div>
                  <div className="text-right">
                    <p className="text-white font-medium">
                      {currency.sellingRate?.toFixed(4)}
                    </p>
                    <p className="text-dark-500 text-xs">
                      Alış: {currency.buyingRate?.toFixed(4)}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Latest News */}
        <div className="card p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-lg font-semibold text-white">Son Haberler</h2>
            <Link to="/news" className="text-primary-400 text-sm hover:text-primary-300">
              Tümünü Gör →
            </Link>
          </div>
          {newsLoading ? (
            <Loading />
          ) : (
            <div className="space-y-4">
              {news?.slice(0, 4).map((item) => (
                <Link
                  key={item.id}
                  to={`/news/${item.id}`}
                  className="block group"
                >
                  <div className="flex gap-3">
                    {item.imageUrl && (
                      <img
                        src={item.imageUrl}
                        alt=""
                        className="w-16 h-16 rounded-lg object-cover flex-shrink-0"
                      />
                    )}
                    <div className="flex-1 min-w-0">
                      <h3 className="text-dark-200 text-sm font-medium line-clamp-2 group-hover:text-white transition-colors">
                        {item.title}
                      </h3>
                      <p className="text-dark-500 text-xs mt-1">
                        {item.sourceName} • {new Date(item.publishedAt).toLocaleDateString('tr-TR')}
                      </p>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
