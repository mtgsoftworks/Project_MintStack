import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { MagnifyingGlassIcon } from '@heroicons/react/24/outline'
import { newsService } from '../services/newsService'
import Loading from '../components/common/Loading'
import { format } from 'date-fns'
import { tr } from 'date-fns/locale'

export default function NewsPage() {
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('')
  const [page, setPage] = useState(0)

  const { data: categories } = useQuery({
    queryKey: ['newsCategories'],
    queryFn: newsService.getCategories,
  })

  const { data, isLoading } = useQuery({
    queryKey: ['news', search, category, page],
    queryFn: () => newsService.getNews({ search, category, page, size: 12 }),
  })

  const news = data?.data || []
  const pagination = data?.pagination

  return (
    <div className="space-y-6 animate-in">
      <div>
        <h1 className="text-2xl font-bold text-white">Haberler</h1>
        <p className="text-dark-400">Finansal piyasa haberleri</p>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-4">
        <div className="relative flex-1 max-w-md">
          <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-dark-500" />
          <input
            type="text"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value)
              setPage(0)
            }}
            placeholder="Haber ara..."
            className="input pl-10"
          />
        </div>
        
        <select
          value={category}
          onChange={(e) => {
            setCategory(e.target.value)
            setPage(0)
          }}
          className="input max-w-xs"
        >
          <option value="">Tüm Kategoriler</option>
          {categories?.map((cat) => (
            <option key={cat.id} value={cat.slug}>{cat.name}</option>
          ))}
        </select>
      </div>

      {/* News Grid */}
      {isLoading ? (
        <Loading />
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {news.map((item, index) => (
              <Link
                key={item.id}
                to={`/news/${item.id}`}
                className={`card-hover overflow-hidden group animate-in stagger-${Math.min(index + 1, 5)}`}
                style={{ opacity: 0 }}
              >
                {item.imageUrl && (
                  <div className="aspect-video relative overflow-hidden">
                    <img
                      src={item.imageUrl}
                      alt={item.title}
                      className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                    />
                    {item.categoryName && (
                      <span className="absolute top-3 left-3 badge-info">
                        {item.categoryName}
                      </span>
                    )}
                  </div>
                )}
                <div className="p-5">
                  <h3 className="text-white font-semibold line-clamp-2 mb-2 group-hover:text-primary-400 transition-colors">
                    {item.title}
                  </h3>
                  {item.summary && (
                    <p className="text-dark-400 text-sm line-clamp-2 mb-4">
                      {item.summary}
                    </p>
                  )}
                  <div className="flex items-center justify-between text-xs text-dark-500">
                    <span>{item.sourceName}</span>
                    <span>
                      {item.publishedAt && format(new Date(item.publishedAt), 'd MMM yyyy', { locale: tr })}
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>

          {news.length === 0 && (
            <div className="text-center py-12">
              <p className="text-dark-400">Haber bulunamadı</p>
            </div>
          )}

          {/* Pagination */}
          {pagination && pagination.totalPages > 1 && (
            <div className="flex items-center justify-center gap-4">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={pagination.first}
                className="btn-secondary"
              >
                Önceki
              </button>
              <span className="text-dark-400">
                Sayfa {pagination.page + 1} / {pagination.totalPages}
              </span>
              <button
                onClick={() => setPage(p => p + 1)}
                disabled={pagination.last}
                className="btn-secondary"
              >
                Sonraki
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
