import { useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { AlertTriangle, ArrowLeft, Calendar, Clock, Eye, ExternalLink, Share2 } from 'lucide-react'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Separator } from '@/components/ui/separator'
import { formatDateTime, formatRelativeTime } from '@/lib/utils'
import { getNewsDisplayTitle, getNewsSourceLabel, getNewsSummary, isSimulationNews } from '@/lib/news'
import NewsImage from '@/components/news/NewsImage'
import { useGetNewsByIdQuery, useIncrementViewCountMutation } from '@/store/api/newsApi'

export default function NewsDetailPage() {
  const { id } = useParams()
  const { data: news, isLoading, error } = useGetNewsByIdQuery(id)
  const [incrementViewCount] = useIncrementViewCountMutation()

  useEffect(() => {
    if (id) {
      incrementViewCount(id)
    }
  }, [id, incrementViewCount])

  const categoryLabel = news?.category?.name || news?.categoryName
  const simulationNews = isSimulationNews(news)
  const displayTitle = getNewsDisplayTitle(news)
  const summary = getNewsSummary(news, 420)

  if (isLoading) {
    return (
      <div className="mx-auto max-w-4xl space-y-6">
        <Skeleton className="h-8 w-48" />
        <Card>
          <Skeleton className="aspect-video" />
          <CardHeader>
            <Skeleton className="h-8 w-full" />
            <Skeleton className="h-8 w-3/4" />
            <div className="mt-4 flex gap-4">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-4 w-24" />
            </div>
          </CardHeader>
          <CardContent>
            <Skeleton className="mb-2 h-4 w-full" />
            <Skeleton className="mb-2 h-4 w-full" />
            <Skeleton className="h-4 w-3/4" />
          </CardContent>
        </Card>
      </div>
    )
  }

  if (error || !news) {
    return (
      <div className="mx-auto max-w-4xl">
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <p className="mb-4 text-muted-foreground">Haber bulunamadi.</p>
            <Button asChild>
              <Link to="/news">
                <ArrowLeft className="mr-2 h-4 w-4" />
                Haberlere Don
              </Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6 animate-in">
      <Button variant="ghost" asChild>
        <Link to="/news">
          <ArrowLeft className="mr-2 h-4 w-4" />
          Haberlere Don
        </Link>
      </Button>

      <Card className={simulationNews ? 'border-warning/50 bg-warning/5' : ''}>
        <NewsImage imageUrl={news.imageUrl} title={news.title} categorySlug={news?.category?.slug || news?.categorySlug} />

        <CardHeader className="space-y-4">
          <div className="flex flex-wrap items-center gap-2">
            {categoryLabel && <Badge variant="secondary">{categoryLabel}</Badge>}
            {news.isFeatured && <Badge variant="info">One Cikan</Badge>}
            {simulationNews && <Badge variant="warning">Simulasyon Haberi</Badge>}
          </div>

          <h1 className="text-2xl font-bold leading-tight md:text-3xl">{displayTitle}</h1>

          <div className="flex flex-wrap items-center gap-4 text-sm text-muted-foreground">
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
              {news.viewCount} goruntulenme
            </span>
          </div>

          {simulationNews && (
            <div className="flex items-start gap-2 rounded-md border border-warning/40 bg-warning/10 p-3 text-sm text-warning-dark">
              <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
              <div>
                <p className="font-semibold">Simulasyon haberi</p>
                <p>Bu icerik simulasyon ortami tarafindan uretilmistir. Gercek haber kaynagi degildir.</p>
              </div>
            </div>
          )}

          <div className="flex items-center justify-between pt-2">
            <span className={`text-sm font-medium ${simulationNews ? 'text-warning-dark' : ''}`}>
              Kaynak: {getNewsSourceLabel(news)}
            </span>
            <div className="flex items-center gap-2">
              {news.sourceUrl && !simulationNews && (
                <Button variant="outline" size="sm" asChild>
                  <a href={news.sourceUrl} target="_blank" rel="noopener noreferrer">
                    <ExternalLink className="mr-2 h-4 w-4" />
                    Kaynaga Git
                  </a>
                </Button>
              )}
              {simulationNews && <span className="text-xs text-warning-dark">Gercek dis kaynak linki yoktur.</span>}
              <Button variant="outline" size="sm">
                <Share2 className="mr-2 h-4 w-4" />
                Paylas
              </Button>
            </div>
          </div>
        </CardHeader>

        <Separator />

        <CardContent className="pt-6">
          {summary && <p className="mb-6 text-lg font-medium text-muted-foreground">{summary}</p>}

          {news.content ? (
            <div className="prose prose-neutral max-w-none dark:prose-invert" dangerouslySetInnerHTML={{ __html: news.content }} />
          ) : (
            <p className="text-muted-foreground">Icerik mevcut degil. Daha fazla bilgi icin kaynaga goz atin.</p>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
