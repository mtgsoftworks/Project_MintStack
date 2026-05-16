export type NewsLike = {
  title?: string | null
  summary?: string | null
  content?: string | null
  llmSummary?: string | null
  categorySlug?: string | null
  sourceName?: string | null
  sourceUrl?: string | null
  isSimulated?: boolean | null
}

const SIMULATION_SOURCE_KEYWORD = 'simulasyon'
const SIMULATION_SOURCE_URL = 'mintstack.local/simulation-news'
const SIMULATION_TITLE_PREFIX = 'SIMULASYON'
const SUMMARY_PREFIX_PATTERN = /^(source|kaynak|read more|devami|detaylar)[:\-\s]+/i
const HTML_TAG_PATTERN = /<[^>]*>/g
const MULTI_SPACE_PATTERN = /\s+/g
const DEFAULT_FALLBACK_IMAGE = '/images/news/fallback-market.svg'
const CATEGORY_FALLBACK_IMAGES: Record<string, string> = {
  ekonomi: '/images/news/fallback-economy.svg',
  sirket: '/images/news/fallback-company.svg',
  piyasa: '/images/news/fallback-market.svg',
}

export const isSimulationNews = (news: NewsLike | null | undefined): boolean => {
  if (!news) {
    return false
  }

  if (typeof news.isSimulated === 'boolean') {
    return news.isSimulated
  }

  const sourceName = (news.sourceName || '').toLowerCase()
  const sourceUrl = (news.sourceUrl || '').toLowerCase()

  return sourceName.startsWith(SIMULATION_SOURCE_KEYWORD) || sourceUrl.includes(SIMULATION_SOURCE_URL)
}

export const getNewsSourceLabel = (news: NewsLike | null | undefined): string => {
  if (!news?.sourceName) {
    return 'Bilinmeyen Kaynak'
  }

  if (isSimulationNews(news)) {
    return news.sourceName
  }

  return news.sourceName
}

export const getNewsDisplayTitle = (news: NewsLike | null | undefined): string => {
  const rawTitle = news?.title?.trim() || ''
  if (!rawTitle) {
    return SIMULATION_TITLE_PREFIX
  }

  if (!isSimulationNews(news)) {
    return rawTitle
  }

  const upper = rawTitle.toUpperCase()
  if (
    upper.startsWith(`${SIMULATION_TITLE_PREFIX} `) ||
    upper.startsWith(`${SIMULATION_TITLE_PREFIX}:`) ||
    upper.startsWith(`[${SIMULATION_TITLE_PREFIX}]`)
  ) {
    return rawTitle
  }

  return `[${SIMULATION_TITLE_PREFIX}] ${rawTitle}`
}

const sanitizeText = (value: string | null | undefined): string => {
  return (value || '')
    .replace(HTML_TAG_PATTERN, ' ')
    .replace(MULTI_SPACE_PATTERN, ' ')
    .trim()
}

export const getNewsSummary = (news: NewsLike | null | undefined, maxLength = 220): string => {
  const llmSummary = sanitizeText(news?.llmSummary)
  const summary = sanitizeText(news?.summary)
  const content = sanitizeText(news?.content)

  let selected = llmSummary || summary || content
  if (!selected) {
    selected = getNewsDisplayTitle(news)
  }

  selected = selected.replace(SUMMARY_PREFIX_PATTERN, '').trim()
  if (selected.length <= maxLength) {
    return selected
  }

  return `${selected.slice(0, Math.max(20, maxLength - 3)).trim()}...`
}

export const getNewsImageFallback = (news: NewsLike | null | undefined): string => {
  const slug = (news?.categorySlug || '').toLowerCase()
  return CATEGORY_FALLBACK_IMAGES[slug] || DEFAULT_FALLBACK_IMAGE
}
