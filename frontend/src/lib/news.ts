export type NewsLike = {
  title?: string | null
  sourceName?: string | null
  sourceUrl?: string | null
  isSimulated?: boolean | null
}

const SIMULATION_SOURCE_KEYWORD = 'simulasyon'
const SIMULATION_SOURCE_URL = 'mintstack.local/simulation-news'
const SIMULATION_TITLE_PREFIX = 'SIMULASYON'

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
