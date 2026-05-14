import { baseApi } from './baseApi'

export const glossaryApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getGlossaryTerms: builder.query({
      query: ({ query = '', category = '', locale = 'tr', page = 0, size = 100 } = {}) => ({
        url: '/glossary',
        params: { query, category, locale, page, size },
      }),
      transformResponse: (response) => response.data,
      providesTags: ['Glossary'],
    }),
    getGlossaryTerm: builder.query({
      query: ({ slug, locale = 'tr' }) => ({
        url: `/glossary/${slug}`,
        params: { locale },
      }),
      transformResponse: (response) => response.data,
      providesTags: (result, error, { slug }) => [{ type: 'Glossary', id: slug }],
    }),
  }),
})

export const {
  useGetGlossaryTermsQuery,
  useGetGlossaryTermQuery,
} = glossaryApi
