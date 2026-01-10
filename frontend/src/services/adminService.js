import api from './api'

/**
 * Admin service for admin panel operations
 */
const adminService = {
    /**
     * Get dashboard statistics
     */
    getDashboard: async () => {
        const response = await api.get('/admin/dashboard')
        return response.data
    },

    /**
     * Get all users with pagination
     */
    getUsers: async (page = 0, size = 20) => {
        const response = await api.get('/admin/users', { params: { page, size } })
        return response.data
    },

    /**
     * Get user by ID
     */
    getUser: async (id) => {
        const response = await api.get(`/admin/users/${id}`)
        return response.data
    },

    /**
     * Search users
     */
    searchUsers: async (query, page = 0, size = 20) => {
        const response = await api.get('/admin/users/search', { params: { query, page, size } })
        return response.data
    },

    /**
     * Activate user
     */
    activateUser: async (id) => {
        const response = await api.put(`/admin/users/${id}/activate`)
        return response.data
    },

    /**
     * Deactivate user
     */
    deactivateUser: async (id) => {
        const response = await api.put(`/admin/users/${id}/deactivate`)
        return response.data
    },
}

export default adminService
