import React from 'react'
import ReactDOM from 'react-dom/client'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'
import { store } from '@/store'
import { ErrorBoundary } from '@/components/layout'
import { registerServiceWorker } from '@/utils/serviceWorker'
import { initSentry } from '@/utils/sentry'
import App from './App'
import './i18n' // i18n initialization
import './index.css'

// Initialize Sentry error tracking in production
initSentry()

// Register PWA service worker in production
if (import.meta.env.PROD) {
  registerServiceWorker()
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <Provider store={store}>
      <BrowserRouter>
        <ErrorBoundary>
          <App />
        </ErrorBoundary>
      </BrowserRouter>
    </Provider>
  </React.StrictMode>
)
