import { baseApi } from './baseApi'

export const userApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    // Get current user profile
    getProfile: builder.query({
      query: () => '/users/me',
      transformResponse: (response) => response.data,
      providesTags: ['User'],
    }),

    // Update user profile
    updateProfile: builder.mutation({
      query: (data) => ({
        url: '/users/me',
        method: 'PUT',
        body: data,
      }),
      invalidatesTags: ['User'],
    }),

    // Get user preferences
    getPreferences: builder.query({
      query: () => '/users/me/preferences',
      transformResponse: (response) => response.data,
      providesTags: [{ type: 'User', id: 'PREFERENCES' }],
    }),

    // Update user preferences
    updatePreferences: builder.mutation({
      query: (data) => ({
        url: '/users/me/preferences',
        method: 'PUT',
        body: data,
      }),
      invalidatesTags: [{ type: 'User', id: 'PREFERENCES' }],
    }),

    // Get user notifications
    getNotifications: builder.query({
      query: ({ page = 0, size = 20 } = {}) => ({
        url: '/users/me/notifications',
        params: { page, size },
      }),
      transformResponse: (response) => response,
      providesTags: [{ type: 'User', id: 'NOTIFICATIONS' }],
    }),

    // Mark notification as read
    markNotificationRead: builder.mutation({
      query: (id) => ({
        url: `/users/me/notifications/${id}/read`,
        method: 'POST',
      }),
      invalidatesTags: [{ type: 'User', id: 'NOTIFICATIONS' }],
    }),

    // Mark all notifications as read
    markAllNotificationsRead: builder.mutation({
      query: () => ({
        url: '/users/me/notifications/read-all',
        method: 'POST',
      }),
      invalidatesTags: [{ type: 'User', id: 'NOTIFICATIONS' }],
    }),
  }),
})

export const {
  useGetProfileQuery,
  useUpdateProfileMutation,
  useGetPreferencesQuery,
  useUpdatePreferencesMutation,
  useGetNotificationsQuery,
  useMarkNotificationReadMutation,
  useMarkAllNotificationsReadMutation,
} = userApi
