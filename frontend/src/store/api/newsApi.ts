import { baseApi } from './baseApi'

export const newsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    // Get all news with pagination
    getNews: builder.query({
      query: (params: any = {}) => ({
        url: '/news',
        params: {
          page: params.page ?? 0,
          size: params.size ?? 10,
          category: params.category,
        },
      }),
      transformResponse: (response) => response,
      providesTags: (result) =>
        result?.data
          ? [
              ...result.data.map(({ id }) => ({ type: 'News', id })),
              { type: 'News', id: 'LIST' },
            ]
          : [{ type: 'News', id: 'LIST' }],
    }),

    // Get featured news
    getFeaturedNews: builder.query({
      query: () => '/news/featured',
      transformResponse: (response) => response.data,
      providesTags: [{ type: 'News', id: 'FEATURED' }],
    }),

    // Get single news by ID
    getNewsById: builder.query({
      query: (id) => `/news/${id}`,
      transformResponse: (response) => response.data,
      providesTags: (result, error, id) => [{ type: 'News', id }],
    }),

    // Get news by category
    getNewsByCategory: builder.query({
      query: ({ categorySlug, page = 0, size = 10 }) => ({
        url: `/news/category/${categorySlug}`,
        params: { page, size },
      }),
      transformResponse: (response) => response,
      providesTags: (result, error, { categorySlug }) => [
        { type: 'News', id: `CATEGORY_${categorySlug}` },
      ],
    }),

    // Get news categories
    getNewsCategories: builder.query({
      query: () => '/news/categories',
      transformResponse: (response) => response.data,
      providesTags: ['NewsCategories'],
    }),

    // Search news
    searchNews: builder.query({
      query: ({ query, page = 0, size = 10 }) => ({
        url: '/news/search',
        params: { query, page, size },
      }),
      transformResponse: (response) => response,
    }),

    // Refresh news from external feeds
    refreshNews: builder.mutation({
      query: () => ({
        url: '/news/refresh',
        method: 'POST',
      }),
      invalidatesTags: [{ type: 'News', id: 'LIST' }, { type: 'News', id: 'FEATURED' }],
    }),

    // Increment view count
    incrementViewCount: builder.mutation({
      query: (id) => ({
        url: `/news/${id}/view`,
        method: 'POST',
      }),
      invalidatesTags: (result, error, id) => [{ type: 'News', id }],
    }),
  }),
})

export const {
  useGetNewsQuery,
  useGetFeaturedNewsQuery,
  useGetNewsByIdQuery,
  useGetNewsByCategoryQuery,
  useGetNewsCategoriesQuery,
  useSearchNewsQuery,
  useRefreshNewsMutation,
  useIncrementViewCountMutation,
} = newsApi
