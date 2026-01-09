import { configureStore } from '@reduxjs/toolkit'
import { setupListeners } from '@reduxjs/toolkit/query'
import { baseApi } from './api/baseApi'
import authReducer from './slices/authSlice'
import uiReducer from './slices/uiSlice'

export const store = configureStore({
  reducer: {
    [baseApi.reducerPath]: baseApi.reducer,
    auth: authReducer,
    ui: uiReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        // Ignore these action types
        ignoredActions: ['auth/setAuth'],
      },
    }).concat(baseApi.middleware),
  devTools: import.meta.env.DEV,
})

// Enable refetchOnFocus/refetchOnReconnect
setupListeners(store.dispatch)

// Export types for usage with useSelector and useDispatch
export default store
