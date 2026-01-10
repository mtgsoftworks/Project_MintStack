import api from './api'

/**
 * Watchlist service for managing user watchlists
 */
const watchlistService = {
    /**
     * Get all user watchlists
     */
    getAll: async () => {
        const response = await api.get('/watchlist')
        return response.data
    },

    /**
     * Get a single watchlist with items
     */
    getById: async (id) => {
        const response = await api.get(`/watchlist/${id}`)
        return response.data
    },

    /**
     * Create a new watchlist
     */
    create: async (data) => {
        const response = await api.post('/watchlist', data)
        return response.data
    },

    /**
     * Update a watchlist
     */
    update: async (id, data) => {
        const response = await api.put(`/watchlist/${id}`, data)
        return response.data
    },

    /**
     * Delete a watchlist
     */
    delete: async (id) => {
        const response = await api.delete(`/watchlist/${id}`)
        return response.data
    },

    /**
     * Add instrument to watchlist
     */
    addInstrument: async (watchlistId, symbol) => {
        const response = await api.post(`/watchlist/${watchlistId}/items/${symbol}`)
        return response.data
    },

    /**
     * Remove instrument from watchlist
     */
    removeInstrument: async (watchlistId, symbol) => {
        const response = await api.delete(`/watchlist/${watchlistId}/items/${symbol}`)
        return response.data
    },
}

export default watchlistService
