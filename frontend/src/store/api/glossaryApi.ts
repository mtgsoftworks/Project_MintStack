import { baseApi } from './baseApi'

interface GlossaryQueryParams {
  query?: string
  category?: string
  locale?: string
  page?: number
  size?: number
  slug?: string
}

export const glossaryApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getGlossaryTerms: builder.query({
      query: (params: GlossaryQueryParams = {}) => ({
        url: '/glossary',
        params: {
          query: params.query ?? '',
          category: params.category ?? '',
          locale: params.locale ?? 'tr',
          page: params.page ?? 0,
          size: params.size ?? 100,
        },
      }),
      transformResponse: (response: any) => response.data,
      providesTags: ['Glossary'],
    }),
    getGlossaryTerm: builder.query({
      query: (params: GlossaryQueryParams) => ({
        url: `/glossary/${params.slug}`,
        params: { locale: params.locale ?? 'tr' },
      }),
      transformResponse: (response: any) => response.data,
      providesTags: (result: any, error: any, args: GlossaryQueryParams) => [{ type: 'Glossary', id: args.slug ?? '' }],
    }),
  }),
})

export const {
  useGetGlossaryTermsQuery,
  useGetGlossaryTermQuery,
} = glossaryApi
