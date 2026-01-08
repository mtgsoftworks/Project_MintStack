import { http, HttpResponse } from 'msw'

const API_URL = 'http://localhost:8080/api/v1'

// Mock data
const mockCurrencyRates = [
  {
    id: 1,
    currencyCode: 'USD',
    currencyName: 'US Dollar',
    buyingRate: 32.50,
    sellingRate: 32.80,
    averageRate: 32.65,
    source: 'TCMB',
    fetchedAt: new Date().toISOString(),
  },
  {
    id: 2,
    currencyCode: 'EUR',
    currencyName: 'Euro',
    buyingRate: 35.20,
    sellingRate: 35.50,
    averageRate: 35.35,
    source: 'TCMB',
    fetchedAt: new Date().toISOString(),
  },
]

const mockStocks = [
  {
    id: 1,
    symbol: 'THYAO',
    name: 'Türk Hava Yolları',
    type: 'STOCK',
    exchange: 'BIST',
    currency: 'TRY',
    currentPrice: 280.50,
    previousClose: 275.00,
    change: 5.50,
    changePercent: 2.00,
    isActive: true,
  },
  {
    id: 2,
    symbol: 'GARAN',
    name: 'Garanti Bankası',
    type: 'STOCK',
    exchange: 'BIST',
    currency: 'TRY',
    currentPrice: 50.00,
    previousClose: 48.50,
    change: 1.50,
    changePercent: 3.09,
    isActive: true,
  },
]

const mockPortfolios = [
  {
    id: 1,
    name: 'Ana Portföy',
    description: 'Birincil yatırım portföyüm',
    isDefault: true,
    totalValue: 28050.00,
    totalCost: 25000.00,
    profitLoss: 3050.00,
    profitLossPercent: 12.20,
    itemCount: 1,
  },
]

const mockNews = [
  {
    id: 1,
    title: 'Merkez Bankası faiz kararı açıklandı',
    summary: 'TCMB politika faizi sabit tuttu',
    content: 'Türkiye Cumhuriyet Merkez Bankası bugün yapılan toplantıda...',
    categoryId: 1,
    categoryName: 'Ekonomi',
    categorySlug: 'ekonomi',
    sourceUrl: 'https://example.com/news/1',
    sourceName: 'Ekonomi Haberleri',
    imageUrl: 'https://example.com/images/1.jpg',
    publishedAt: new Date().toISOString(),
  },
  {
    id: 2,
    title: 'BIST 100 endeksi rekor kırdı',
    summary: 'Borsa İstanbul yeni zirveye ulaştı',
    content: 'Borsa İstanbul 100 endeksi bugün tarihi bir zirveye...',
    categoryId: 2,
    categoryName: 'Borsa',
    categorySlug: 'borsa',
    sourceUrl: 'https://example.com/news/2',
    sourceName: 'Borsa Haberleri',
    imageUrl: 'https://example.com/images/2.jpg',
    publishedAt: new Date().toISOString(),
  },
]

const mockUser = {
  id: 1,
  keycloakId: 'mock-user-id',
  email: 'test@example.com',
  firstName: 'Test',
  lastName: 'User',
  phone: '+905551234567',
  notificationsEnabled: true,
}

// API Response wrapper
const apiResponse = (data, success = true) => ({
  success,
  data,
  timestamp: new Date().toISOString(),
})

const paginatedResponse = (content, page = 0, size = 20, totalElements = null) => ({
  success: true,
  data: {
    content,
    pageable: {
      pageNumber: page,
      pageSize: size,
    },
    totalElements: totalElements || content.length,
    totalPages: Math.ceil((totalElements || content.length) / size),
    last: page >= Math.ceil((totalElements || content.length) / size) - 1,
    first: page === 0,
    numberOfElements: content.length,
  },
  timestamp: new Date().toISOString(),
})

export const handlers = [
  // Currency endpoints
  http.get(`${API_URL}/market/currencies`, () => {
    return HttpResponse.json(apiResponse(mockCurrencyRates))
  }),

  http.get(`${API_URL}/market/currencies/:code`, ({ params }) => {
    const rate = mockCurrencyRates.find(r => r.currencyCode === params.code)
    if (!rate) {
      return HttpResponse.json(
        { success: false, message: 'Currency not found' },
        { status: 404 }
      )
    }
    return HttpResponse.json(apiResponse(rate))
  }),

  // Stock endpoints
  http.get(`${API_URL}/market/stocks`, () => {
    return HttpResponse.json(paginatedResponse(mockStocks))
  }),

  http.get(`${API_URL}/market/stocks/:symbol`, ({ params }) => {
    const stock = mockStocks.find(s => s.symbol === params.symbol)
    if (!stock) {
      return HttpResponse.json(
        { success: false, message: 'Stock not found' },
        { status: 404 }
      )
    }
    return HttpResponse.json(apiResponse(stock))
  }),

  http.get(`${API_URL}/market/stocks/:symbol/history`, () => {
    const history = Array.from({ length: 30 }, (_, i) => ({
      date: new Date(Date.now() - i * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      open: 270 + Math.random() * 10,
      high: 280 + Math.random() * 10,
      low: 265 + Math.random() * 10,
      close: 275 + Math.random() * 10,
      volume: 1000000 + Math.floor(Math.random() * 500000),
    }))
    return HttpResponse.json(apiResponse(history))
  }),

  // Portfolio endpoints
  http.get(`${API_URL}/portfolios`, () => {
    return HttpResponse.json(apiResponse(mockPortfolios))
  }),

  http.get(`${API_URL}/portfolios/:id`, ({ params }) => {
    const portfolio = mockPortfolios.find(p => p.id === parseInt(params.id))
    if (!portfolio) {
      return HttpResponse.json(
        { success: false, message: 'Portfolio not found' },
        { status: 404 }
      )
    }
    return HttpResponse.json(apiResponse({
      ...portfolio,
      items: [
        {
          id: 1,
          symbol: 'THYAO',
          name: 'Türk Hava Yolları',
          type: 'STOCK',
          quantity: 100,
          averageCost: 250.00,
          currentPrice: 280.50,
          currentValue: 28050.00,
          profitLoss: 3050.00,
          profitLossPercent: 12.20,
        },
      ],
    }))
  }),

  http.post(`${API_URL}/portfolios`, async ({ request }) => {
    const body = await request.json()
    const newPortfolio = {
      id: mockPortfolios.length + 1,
      ...body,
      isDefault: false,
      totalValue: 0,
      totalCost: 0,
      profitLoss: 0,
      profitLossPercent: 0,
      itemCount: 0,
    }
    return HttpResponse.json(apiResponse(newPortfolio), { status: 201 })
  }),

  http.delete(`${API_URL}/portfolios/:id`, () => {
    return new HttpResponse(null, { status: 204 })
  }),

  // News endpoints
  http.get(`${API_URL}/news`, () => {
    return HttpResponse.json(paginatedResponse(mockNews))
  }),

  http.get(`${API_URL}/news/:id`, ({ params }) => {
    const news = mockNews.find(n => n.id === parseInt(params.id))
    if (!news) {
      return HttpResponse.json(
        { success: false, message: 'News not found' },
        { status: 404 }
      )
    }
    return HttpResponse.json(apiResponse(news))
  }),

  http.get(`${API_URL}/news/category/:slug`, ({ params }) => {
    const filtered = mockNews.filter(n => n.categorySlug === params.slug)
    return HttpResponse.json(paginatedResponse(filtered))
  }),

  http.get(`${API_URL}/news/search`, ({ request }) => {
    const url = new URL(request.url)
    const query = url.searchParams.get('q')?.toLowerCase() || ''
    const filtered = mockNews.filter(n =>
      n.title.toLowerCase().includes(query) ||
      n.summary.toLowerCase().includes(query)
    )
    return HttpResponse.json(paginatedResponse(filtered))
  }),

  http.get(`${API_URL}/news/categories`, () => {
    return HttpResponse.json(apiResponse([
      { id: 1, name: 'Ekonomi', slug: 'ekonomi' },
      { id: 2, name: 'Borsa', slug: 'borsa' },
      { id: 3, name: 'Dünya', slug: 'dunya' },
    ]))
  }),

  // User endpoints
  http.get(`${API_URL}/users/me`, () => {
    return HttpResponse.json(apiResponse(mockUser))
  }),

  http.put(`${API_URL}/users/me`, async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json(apiResponse({ ...mockUser, ...body }))
  }),

  // Analysis endpoints
  http.get(`${API_URL}/analysis/ma/:symbol`, () => {
    const data = Array.from({ length: 30 }, (_, i) => ({
      date: new Date(Date.now() - i * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      price: 275 + Math.random() * 10,
      ma7: 273 + Math.random() * 5,
      ma25: 270 + Math.random() * 5,
      ma99: 265 + Math.random() * 5,
    }))
    return HttpResponse.json(apiResponse({ symbol: 'THYAO', data }))
  }),

  http.get(`${API_URL}/analysis/trend/:symbol`, () => {
    return HttpResponse.json(apiResponse({
      symbol: 'THYAO',
      period: 30,
      startPrice: 250.00,
      endPrice: 280.50,
      changePercent: 12.20,
      trend: 'UPTREND',
      trendStrength: 'MODERATE',
      volatility: 3.45,
      highPrice: 285.00,
      lowPrice: 248.00,
    }))
  }),

  http.post(`${API_URL}/analysis/compare`, async ({ request }) => {
    const body = await request.json()
    const instruments = body.symbols.map(symbol => ({
      symbol,
      name: symbol,
      data: Array.from({ length: 30 }, (_, i) => ({
        date: new Date(Date.now() - i * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
        value: Math.random() * 20 - 10,
        price: 250 + Math.random() * 50,
      })),
    }))
    return HttpResponse.json(apiResponse({
      startDate: body.startDate,
      endDate: body.endDate,
      instruments,
    }))
  }),
]
