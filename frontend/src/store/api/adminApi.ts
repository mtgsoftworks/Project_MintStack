import { baseApi } from './baseApi'

export const adminApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAdminDashboard: builder.query({
      query: () => '/admin/dashboard',
      transformResponse: (response) => response.data,
      providesTags: ['AdminDashboard'],
    }),
    getAdminUsers: builder.query({
      query: ({ page = 0, size = 20 } = {}) => ({
        url: '/admin/users',
        params: { page, size },
      }),
      transformResponse: (response) => response.data,
      providesTags: (result) =>
        result?.content
          ? [
              ...result.content.map(({ id }) => ({ type: 'AdminUsers', id })),
              { type: 'AdminUsers', id: 'LIST' },
            ]
          : [{ type: 'AdminUsers', id: 'LIST' }],
    }),
    searchAdminUsers: builder.query({
      query: ({ query, page = 0, size = 20 }) => ({
        url: '/admin/users/search',
        params: { query, page, size },
      }),
      transformResponse: (response) => response.data,
      providesTags: [{ type: 'AdminUsers', id: 'LIST' }],
    }),
    activateAdminUser: builder.mutation({
      query: (id) => ({
        url: `/admin/users/${id}/activate`,
        method: 'PUT',
      }),
      invalidatesTags: ['AdminDashboard', { type: 'AdminUsers', id: 'LIST' }],
    }),
    deactivateAdminUser: builder.mutation({
      query: (id) => ({
        url: `/admin/users/${id}/deactivate`,
        method: 'PUT',
      }),
      invalidatesTags: ['AdminDashboard', { type: 'AdminUsers', id: 'LIST' }],
    }),
  }),
})

export const {
  useGetAdminDashboardQuery,
  useGetAdminUsersQuery,
  useSearchAdminUsersQuery,
  useActivateAdminUserMutation,
  useDeactivateAdminUserMutation,
} = adminApi
