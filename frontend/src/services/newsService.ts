import axios from 'axios'

// Public API client (no auth required for news)
const publicApi = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

export const newsService = {
  // Get all news with pagination (public)
  getNews: async (params = {}) => {
    const response = await publicApi.get('/news', { params })
    return response.data
  },

  // Get latest news (public)
  getLatestNews: async () => {
    const response = await publicApi.get('/news/latest')
    return response.data.data
  },

  // Get featured news (public)
  getFeaturedNews: async () => {
    const response = await publicApi.get('/news/featured')
    return response.data.data
  },

  // Get single news (public)
  getNewsById: async (id) => {
    const response = await publicApi.get(`/news/${id}`)
    return response.data.data
  },

  // Get categories (public)
  getCategories: async () => {
    const response = await publicApi.get('/news/categories')
    return response.data.data
  },

  // Search news (public)
  searchNews: async (query, params = {}) => {
    const response = await publicApi.get('/news', { params: { search: query, ...params } })
    return response.data
  },
}
