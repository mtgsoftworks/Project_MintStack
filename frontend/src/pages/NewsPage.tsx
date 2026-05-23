import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Clock, Eye, Search } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { formatRelativeTime } from '@/lib/utils'
import { getNewsDisplayTitle, getNewsSourceLabel, getNewsSummary, isSimulationNews } from '@/lib/news'
import NewsImage from '@/components/news/NewsImage'
import {
  useGetNewsByCategoryQuery,
  useGetNewsCategoriesQuery,
  useGetNewsQuery,
  useSearchNewsQuery,
} from '@/store/api/newsApi'

function NewsCard({ news }) {
  const { t } = useTranslation()
  const categoryLabel = news?.category?.name || news?.categoryName
  const simulationNews = isSimulationNews(news)
  const displayTitle = getNewsDisplayTitle(news)
  const summary = getNewsSummary(news)

  return (
    <Link to={`/news/${news.id}`}>
      <Card className={`card-hover group h-full ${simulationNews ? 'border-warning/50 bg-warning/5' : ''}`}>
        <NewsImage imageUrl={news.imageUrl} title={news.title} categorySlug={news?.category?.slug || news?.categorySlug} />
        <CardHeader className="pb-2">
          <div className="mb-2 flex items-center gap-2">
            {categoryLabel && (
              <Badge variant="secondary" className="text-xs">
                {categoryLabel}
              </Badge>
            )}
            {news.isFeatured && (
              <Badge variant="info" className="text-xs">
                {t('newsPage.featured')}
              </Badge>
            )}
            {simulationNews && (
              <Badge variant="warning" className="text-xs">
                {t('newsPage.simulationNews')}
              </Badge>
            )}
          </div>
          <CardTitle className="line-clamp-2 text-base font-semibold transition-colors group-hover:text-primary">
            {displayTitle}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="mb-4 line-clamp-3 text-sm text-muted-foreground">{summary}</p>
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <div className="flex items-center gap-3">
              <span className="flex items-center gap-1">
                <Clock className="h-3 w-3" />
                {formatRelativeTime(news.publishedAt)}
              </span>
              <span className="flex items-center gap-1">
                <Eye className="h-3 w-3" />
                {news.viewCount}
              </span>
            </div>
            <span className={simulationNews ? 'font-semibold text-warning-dark' : ''}>{getNewsSourceLabel(news)}</span>
          </div>
        </CardContent>
      </Card>
    </Link>
  )
}

function NewsCardSkeleton() {
  return (
    <Card>
      <Skeleton className="aspect-video rounded-t-xl" />
      <CardHeader className="pb-2">
        <Skeleton className="mb-2 h-4 w-20" />
        <Skeleton className="h-5 w-full" />
        <Skeleton className="h-5 w-3/4" />
      </CardHeader>
      <CardContent>
        <Skeleton className="mb-2 h-4 w-full" />
        <Skeleton className="mb-4 h-4 w-2/3" />
        <div className="flex justify-between">
          <Skeleton className="h-3 w-24" />
          <Skeleton className="h-3 w-16" />
        </div>
      </CardContent>
    </Card>
  )
}

export default function NewsPage() {
  const { t } = useTranslation()
  const [page, setPage] = useState(0)
  const [category, setCategory] = useState('all')
  const [searchQuery, setSearchQuery] = useState('')

  const { data: categoriesData, isLoading: categoriesLoading } = useGetNewsCategoriesQuery()

  const trimmedSearch = searchQuery.trim()
  const isSearching = trimmedSearch.length > 0
  const isCategoryFiltered = category !== 'all'

  const {
    data: searchData,
    isLoading: searchLoading,
    isFetching: searchFetching,
  } = useSearchNewsQuery({ query: trimmedSearch, page, size: 12 }, { skip: !isSearching })

  const {
    data: categoryData,
    isLoading: categoryLoading,
    isFetching: categoryFetching,
  } = useGetNewsByCategoryQuery({ categorySlug: category, page, size: 12 }, { skip: isSearching || !isCategoryFiltered })

  const {
    data: allNewsData,
    isLoading: allNewsLoading,
    isFetching: allNewsFetching,
  } = useGetNewsQuery(
    {
      page,
      size: 12,
      category: isCategoryFiltered ? category : undefined,
    },
    { skip: isSearching || isCategoryFiltered }
  )

  const data = isSearching ? searchData : isCategoryFiltered ? categoryData : allNewsData
  const isLoading = isSearching ? searchLoading : isCategoryFiltered ? categoryLoading : allNewsLoading
  const isFetching = isSearching ? searchFetching : isCategoryFiltered ? categoryFetching : allNewsFetching

  useEffect(() => {
    setPage(0)
  }, [trimmedSearch])

  const categories = categoriesData || []
  const news = data?.data || []
  const totalPages = data?.totalPages || 0

  return (
    <div className="space-y-6 animate-in">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t('newsPage.title')}</h1>
          <p className="text-muted-foreground">{t('newsPage.subtitle')}</p>
        </div>
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder={t('newsPage.searchPlaceholder')}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-64 pl-9"
            />
          </div>
        </div>
      </div>

      <Tabs
        value={category}
        onValueChange={(value) => {
          setCategory(value)
          setPage(0)
        }}
      >
        <TabsList className="w-full justify-start overflow-x-auto">
          <TabsTrigger value="all">{t('newsPage.all')}</TabsTrigger>
          {categoriesLoading ? (
            <Skeleton className="h-8 w-24" />
          ) : (
            categories.map((cat) => (
              <TabsTrigger key={cat.slug} value={cat.slug}>
                {cat.name}
              </TabsTrigger>
            ))
          )}
        </TabsList>

        <TabsContent value={category} className="mt-6">
          {isLoading ? (
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {[...Array(8)].map((_, i) => (
                <NewsCardSkeleton key={i} />
              ))}
            </div>
          ) : news.length === 0 ? (
            <Card>
              <CardContent className="flex flex-col items-center justify-center py-12">
                <p className="text-muted-foreground">{t('newsPage.emptyCategory')}</p>
              </CardContent>
            </Card>
          ) : (
            <>
              <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                {news.map((item) => (
                  <NewsCard key={item.id} news={item} />
                ))}
              </div>

              {totalPages > 1 && (
                <div className="mt-8 flex items-center justify-center gap-2">
                  <Button
                    variant="outline"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0 || isFetching}
                  >
                    {t('newsPage.previous')}
                  </Button>
                  <span className="px-4 text-sm text-muted-foreground">
                    {t('newsPage.page', { page: page + 1, total: totalPages })}
                  </span>
                  <Button
                    variant="outline"
                    onClick={() => setPage((p) => p + 1)}
                    disabled={page >= totalPages - 1 || isFetching}
                  >
                    {t('newsPage.next')}
                  </Button>
                </div>
              )}
            </>
          )}
        </TabsContent>
      </Tabs>
    </div>
  )
}
