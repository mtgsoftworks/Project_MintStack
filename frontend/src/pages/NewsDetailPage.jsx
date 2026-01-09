import { useParams, Link } from 'react-router-dom'
import { ArrowLeft, Calendar, Clock, Eye, ExternalLink, Share2 } from 'lucide-react'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Separator } from '@/components/ui/separator'
import { formatDateTime, formatRelativeTime } from '@/lib/utils'
import { useGetNewsByIdQuery, useIncrementViewCountMutation } from '@/store/api/newsApi'
import { useEffect } from 'react'

export default function NewsDetailPage() {
  const { id } = useParams()
  const { data: news, isLoading, error } = useGetNewsByIdQuery(id)
  const [incrementViewCount] = useIncrementViewCountMutation()

  useEffect(() => {
    if (id) {
      incrementViewCount(id)
    }
  }, [id, incrementViewCount])

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto space-y-6">
        <Skeleton className="h-8 w-48" />
        <Card>
          <Skeleton className="aspect-video" />
          <CardHeader>
            <Skeleton className="h-8 w-full" />
            <Skeleton className="h-8 w-3/4" />
            <div className="flex gap-4 mt-4">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-4 w-24" />
            </div>
          </CardHeader>
          <CardContent>
            <Skeleton className="h-4 w-full mb-2" />
            <Skeleton className="h-4 w-full mb-2" />
            <Skeleton className="h-4 w-3/4" />
          </CardContent>
        </Card>
      </div>
    )
  }

  if (error || !news) {
    return (
      <div className="max-w-4xl mx-auto">
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <p className="text-muted-foreground mb-4">Haber bulunamadı.</p>
            <Button asChild>
              <Link to="/news">
                <ArrowLeft className="mr-2 h-4 w-4" />
                Haberlere Dön
              </Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-in">
      {/* Back Button */}
      <Button variant="ghost" asChild>
        <Link to="/news">
          <ArrowLeft className="mr-2 h-4 w-4" />
          Haberlere Dön
        </Link>
      </Button>

      {/* News Article */}
      <Card>
        {news.imageUrl && (
          <div className="aspect-video overflow-hidden rounded-t-xl">
            <img
              src={news.imageUrl}
              alt={news.title}
              className="h-full w-full object-cover"
            />
          </div>
        )}
        
        <CardHeader className="space-y-4">
          <div className="flex items-center gap-2 flex-wrap">
            {news.category && (
              <Badge variant="secondary">{news.category.name}</Badge>
            )}
            {news.isFeatured && (
              <Badge variant="info">Öne Çıkan</Badge>
            )}
          </div>
          
          <h1 className="text-2xl md:text-3xl font-bold leading-tight">
            {news.title}
          </h1>

          <div className="flex items-center flex-wrap gap-4 text-sm text-muted-foreground">
            <span className="flex items-center gap-1">
              <Calendar className="h-4 w-4" />
              {formatDateTime(news.publishedAt)}
            </span>
            <span className="flex items-center gap-1">
              <Clock className="h-4 w-4" />
              {formatRelativeTime(news.publishedAt)}
            </span>
            <span className="flex items-center gap-1">
              <Eye className="h-4 w-4" />
              {news.viewCount} görüntülenme
            </span>
          </div>

          <div className="flex items-center justify-between pt-2">
            <span className="text-sm font-medium">
              Kaynak: {news.sourceName}
            </span>
            <div className="flex items-center gap-2">
              {news.sourceUrl && (
                <Button variant="outline" size="sm" asChild>
                  <a href={news.sourceUrl} target="_blank" rel="noopener noreferrer">
                    <ExternalLink className="mr-2 h-4 w-4" />
                    Kaynağa Git
                  </a>
                </Button>
              )}
              <Button variant="outline" size="sm">
                <Share2 className="mr-2 h-4 w-4" />
                Paylaş
              </Button>
            </div>
          </div>
        </CardHeader>

        <Separator />

        <CardContent className="pt-6">
          {news.summary && (
            <p className="text-lg text-muted-foreground mb-6 font-medium">
              {news.summary}
            </p>
          )}
          
          {news.content ? (
            <div 
              className="prose prose-neutral dark:prose-invert max-w-none"
              dangerouslySetInnerHTML={{ __html: news.content }}
            />
          ) : (
            <p className="text-muted-foreground">
              İçerik mevcut değil. Daha fazla bilgi için kaynağa göz atın.
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
