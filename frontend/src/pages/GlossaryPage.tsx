import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { BookOpen, Search } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { useGetGlossaryTermsQuery } from '@/store/api/glossaryApi'

export default function GlossaryPage() {
  const { t } = useTranslation()
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('')
  const { data: terms = [], isLoading } = useGetGlossaryTermsQuery({ query, category, size: 200 })

  const categories = useMemo<string[]>(() => {
    return Array.from(new Set((terms as any[] || []).map((term) => String(term.category || '')).filter(Boolean))).sort()
  }, [terms])

  return (
    <div className="space-y-6 animate-in">
      <div className="relative overflow-hidden rounded-2xl border bg-gradient-to-br from-emerald-950 via-slate-950 to-cyan-950 p-6 text-white shadow-xl">
        <div className="absolute -right-16 -top-16 h-48 w-48 rounded-full bg-emerald-400/20 blur-3xl" />
        <div className="absolute -bottom-20 left-16 h-48 w-48 rounded-full bg-cyan-400/20 blur-3xl" />
        <div className="relative flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-white/10">
              <BookOpen className="h-6 w-6" />
            </div>
            <h1 className="text-3xl font-bold tracking-tight">{t('glossary.title')}</h1>
            <p className="mt-2 max-w-2xl text-sm text-slate-200">
              {t('glossary.subtitle')}
            </p>
          </div>
          <Badge className="w-fit bg-white/15 text-white hover:bg-white/20">
            {t('glossary.badge')}
          </Badge>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Terimleri ara</CardTitle>
          <CardDescription>Terim, kategori veya alias uzerinden filtreleyin.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-3 md:grid-cols-[1fr_220px]">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                className="pl-9"
                  placeholder={t('glossary.searchPlaceholder')}
                value={query}
                onChange={(event) => setQuery(event.target.value)}
              />
            </div>
            <select
              className="h-10 rounded-md border border-input bg-background px-3 text-sm"
              value={category}
              onChange={(event) => setCategory(event.target.value)}
            >
              <option value="">Tum kategoriler</option>
              {categories.map((item) => (
                <option key={item} value={item}>{item}</option>
              ))}
            </select>
          </div>
        </CardContent>
      </Card>

      {isLoading ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {[...Array(6)].map((_, index) => (
            <Skeleton key={index} className="h-40 rounded-xl" />
          ))}
        </div>
      ) : terms.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            Eslesen kavram bulunamadi.
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {terms.map((term) => (
            <Card key={term.id || term.slug} className="overflow-hidden border-slate-200/70">
              <CardHeader className="space-y-2">
                <div className="flex items-start justify-between gap-3">
                  <CardTitle className="text-xl">{term.term}</CardTitle>
                  <Badge variant="outline">{term.category}</Badge>
                </div>
                {term.aliases?.length > 0 && (
                  <div className="flex flex-wrap gap-1">
                    {term.aliases.slice(0, 4).map((alias) => (
                      <Badge key={alias} variant="secondary" className="text-xs">{alias}</Badge>
                    ))}
                  </div>
                )}
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="text-sm leading-6 text-muted-foreground">{term.definition}</p>
                {(term.sourceName || term.sourceUrl) && (
                  <div className="border-t pt-3 text-xs text-muted-foreground">
                    Kaynak:{' '}
                    {term.sourceUrl ? (
                      <a className="font-medium text-primary hover:underline" href={term.sourceUrl} target="_blank" rel="noreferrer">
                        {term.sourceName || term.sourceUrl}
                      </a>
                    ) : (
                      <span className="font-medium">{term.sourceName}</span>
                    )}
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
