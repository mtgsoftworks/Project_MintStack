import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Calendar, Clock, Eye, Search, Filter } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { cn, formatRelativeTime, truncate } from '@/lib/utils'
import { useGetNewsQuery, useGetNewsCategoriesQuery } from '@/store/api/newsApi'

function NewsCard({ news }) {
  return (
    <Link to={`/news/${news.id}`}>
      <Card className="card-hover h-full">
        {news.imageUrl && (
          <div className="aspect-video overflow-hidden rounded-t-xl">
            <img
              src={news.imageUrl}
              alt={news.title}
              className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
            />
          </div>
        )}
        <CardHeader className="pb-2">
          <div className="flex items-center gap-2 mb-2">
            {news.category && (
              <Badge variant="secondary" className="text-xs">
                {news.category.name}
              </Badge>
            )}
            {news.isFeatured && (
              <Badge variant="info" className="text-xs">
                Öne Çıkan
              </Badge>
            )}
          </div>
          <CardTitle className="text-base font-semibold line-clamp-2 group-hover:text-primary transition-colors">
            {news.title}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground line-clamp-2 mb-4">
            {news.summary}
          </p>
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
            <span>{news.sourceName}</span>
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
        <Skeleton className="h-4 w-20 mb-2" />
        <Skeleton className="h-5 w-full" />
        <Skeleton className="h-5 w-3/4" />
      </CardHeader>
      <CardContent>
        <Skeleton className="h-4 w-full mb-2" />
        <Skeleton className="h-4 w-2/3 mb-4" />
        <div className="flex justify-between">
          <Skeleton className="h-3 w-24" />
          <Skeleton className="h-3 w-16" />
        </div>
      </CardContent>
    </Card>
  )
}

export default function NewsPage() {
  const [page, setPage] = useState(0)
  const [category, setCategory] = useState('all')
  const [searchQuery, setSearchQuery] = useState('')

  const { data: categoriesData, isLoading: categoriesLoading } = useGetNewsCategoriesQuery()
  const { data, isLoading, isFetching } = useGetNewsQuery({
    page,
    size: 12,
    category: category !== 'all' ? category : undefined,
  })

  const categories = categoriesData || []
  const news = data?.data || []
  const totalPages = data?.totalPages || 0

  return (
    <div className="space-y-6 animate-in">
      {/* Page Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Haberler</h1>
          <p className="text-muted-foreground">
            Güncel finans ve ekonomi haberleri
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Haberlerde ara..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 w-64"
            />
          </div>
        </div>
      </div>

      {/* Category Tabs */}
      <Tabs value={category} onValueChange={(value) => { setCategory(value); setPage(0); }}>
        <TabsList className="w-full justify-start overflow-x-auto">
          <TabsTrigger value="all">Tümü</TabsTrigger>
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
                <p className="text-muted-foreground">Bu kategoride haber bulunamadı.</p>
              </CardContent>
            </Card>
          ) : (
            <>
              <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                {news.map((item) => (
                  <NewsCard key={item.id} news={item} />
                ))}
              </div>

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="flex items-center justify-center gap-2 mt-8">
                  <Button
                    variant="outline"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0 || isFetching}
                  >
                    Önceki
                  </Button>
                  <span className="text-sm text-muted-foreground px-4">
                    Sayfa {page + 1} / {totalPages}
                  </span>
                  <Button
                    variant="outline"
                    onClick={() => setPage((p) => p + 1)}
                    disabled={page >= totalPages - 1 || isFetching}
                  >
                    Sonraki
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
