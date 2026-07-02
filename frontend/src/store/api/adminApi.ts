import { baseApi } from './baseApi'

interface AdminQueryParams {
  page?: number
  size?: number
  query?: string
}

export const adminApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAdminDashboard: builder.query({
      query: () => '/admin/dashboard',
      transformResponse: (response: any) => response.data,
      providesTags: ['AdminDashboard'],
    }),
    getAdminUsers: builder.query({
      query: (params: AdminQueryParams = {}) => ({
        url: '/admin/users',
        params: { page: params.page ?? 0, size: params.size ?? 20 },
      }),
      transformResponse: (response: any) => response.data,
      providesTags: (result: any) =>
        result?.content
          ? [
              ...result.content.map(({ id }: { id: string }) => ({ type: 'AdminUsers', id })),
              { type: 'AdminUsers', id: 'LIST' },
            ]
          : [{ type: 'AdminUsers', id: 'LIST' }],
    }),
    searchAdminUsers: builder.query({
      query: (params: AdminQueryParams) => ({
        url: '/admin/users/search',
        params: { query: params.query, page: params.page ?? 0, size: params.size ?? 20 },
      }),
      transformResponse: (response: any) => response.data,
      providesTags: [{ type: 'AdminUsers', id: 'LIST' }],
    }),
    activateAdminUser: builder.mutation({
      query: (id: string) => ({
        url: `/admin/users/${id}/activate`,
        method: 'PUT',
      }),
      invalidatesTags: ['AdminDashboard', { type: 'AdminUsers', id: 'LIST' }],
    }),
    deactivateAdminUser: builder.mutation({
      query: (id: string) => ({
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
