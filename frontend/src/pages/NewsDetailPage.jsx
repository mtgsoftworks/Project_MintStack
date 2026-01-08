import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeftIcon, ShareIcon, BookmarkIcon } from '@heroicons/react/24/outline'
import { newsService } from '../services/newsService'
import Loading from '../components/common/Loading'
import { format } from 'date-fns'
import { tr } from 'date-fns/locale'

export default function NewsDetailPage() {
  const { id } = useParams()

  const { data: news, isLoading } = useQuery({
    queryKey: ['news', id],
    queryFn: () => newsService.getNewsById(id),
  })

  if (isLoading) {
    return <Loading />
  }

  if (!news) {
    return (
      <div className="text-center py-12">
        <p className="text-dark-400">Haber bulunamadı</p>
        <Link to="/news" className="btn-primary mt-4">
          Haberlere Dön
        </Link>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto animate-in">
      {/* Back Button */}
      <Link
        to="/news"
        className="inline-flex items-center gap-2 text-dark-400 hover:text-white transition-colors mb-6"
      >
        <ArrowLeftIcon className="w-4 h-4" />
        Haberlere Dön
      </Link>

      <article className="card overflow-hidden">
        {/* Image */}
        {news.imageUrl && (
          <div className="aspect-video relative">
            <img
              src={news.imageUrl}
              alt={news.title}
              className="w-full h-full object-cover"
            />
          </div>
        )}

        <div className="p-6 lg:p-8">
          {/* Category & Date */}
          <div className="flex items-center gap-4 mb-4">
            {news.categoryName && (
              <span className="badge-info">{news.categoryName}</span>
            )}
            <span className="text-dark-500 text-sm">
              {news.publishedAt && format(new Date(news.publishedAt), 'd MMMM yyyy, HH:mm', { locale: tr })}
            </span>
          </div>

          {/* Title */}
          <h1 className="text-2xl lg:text-3xl font-bold text-white mb-4">
            {news.title}
          </h1>

          {/* Summary */}
          {news.summary && (
            <p className="text-lg text-dark-300 mb-6 leading-relaxed">
              {news.summary}
            </p>
          )}

          {/* Content */}
          {news.content && (
            <div className="prose prose-invert prose-dark max-w-none">
              <p className="text-dark-200 leading-relaxed whitespace-pre-wrap">
                {news.content}
              </p>
            </div>
          )}

          {/* Footer */}
          <div className="flex items-center justify-between mt-8 pt-6 border-t border-dark-800">
            <div>
              <p className="text-dark-400 text-sm">Kaynak</p>
              <a
                href={news.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary-400 hover:text-primary-300"
              >
                {news.sourceName}
              </a>
            </div>
            <div className="flex items-center gap-3">
              <button className="p-2 rounded-lg bg-dark-800 text-dark-400 hover:text-white transition-colors">
                <BookmarkIcon className="w-5 h-5" />
              </button>
              <button className="p-2 rounded-lg bg-dark-800 text-dark-400 hover:text-white transition-colors">
                <ShareIcon className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Views */}
          {news.viewCount > 0 && (
            <p className="text-dark-500 text-xs mt-4">
              {news.viewCount} görüntülenme
            </p>
          )}
        </div>
      </article>
    </div>
  )
}
