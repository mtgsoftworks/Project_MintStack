import api from './api'

export const newsService = {
  // Get all news with pagination
  getNews: async (params = {}) => {
    const response = await api.get('/news', { params })
    return response.data
  },

  // Get latest news
  getLatestNews: async () => {
    const response = await api.get('/news/latest')
    return response.data.data
  },

  // Get featured news
  getFeaturedNews: async () => {
    const response = await api.get('/news/featured')
    return response.data.data
  },

  // Get single news
  getNewsById: async (id) => {
    const response = await api.get(`/news/${id}`)
    return response.data.data
  },

  // Get categories
  getCategories: async () => {
    const response = await api.get('/news/categories')
    return response.data.data
  },

  // Search news
  searchNews: async (query, params = {}) => {
    const response = await api.get('/news', { params: { search: query, ...params } })
    return response.data
  },
}
