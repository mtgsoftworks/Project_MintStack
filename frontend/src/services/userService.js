import api from './api'

export const userService = {
  // Get profile
  getProfile: async () => {
    const response = await api.get('/users/profile')
    return response.data.data
  },

  // Update profile
  updateProfile: async (data) => {
    const response = await api.put('/users/profile', data)
    return response.data.data
  },
}
