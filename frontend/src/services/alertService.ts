import api from './api'

/**
 * Alert service for managing price alerts
 */
const alertService = {
    /**
     * Get all user alerts
     */
    getAll: async () => {
        const response = await api.get('/alerts')
        return response.data
    },

    /**
     * Get active alerts only
     */
    getActive: async () => {
        const response = await api.get('/alerts/active')
        return response.data
    },

    /**
     * Create a new price alert
     */
    create: async (data) => {
        const response = await api.post('/alerts', data)
        return response.data
    },

    /**
     * Delete an alert
     */
    delete: async (id) => {
        const response = await api.delete(`/alerts/${id}`)
        return response.data
    },

    /**
     * Deactivate an alert
     */
    deactivate: async (id) => {
        const response = await api.put(`/alerts/${id}/deactivate`)
        return response.data
    },
}

export default alertService
