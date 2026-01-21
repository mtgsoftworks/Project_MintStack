// Sentry Error Tracking Integration
// Install: npm install @sentry/react

let Sentry = null;

export async function initSentry() {
  if (import.meta.env.PROD && import.meta.env.VITE_SENTRY_DSN) {
    try {
      const SentryModule = await import('@sentry/react');
      Sentry = SentryModule;
      
      Sentry.init({
        dsn: import.meta.env.VITE_SENTRY_DSN,
        environment: import.meta.env.MODE,
        integrations: [
          Sentry.browserTracingIntegration(),
          Sentry.replayIntegration({
            maskAllText: false,
            blockAllMedia: false,
          }),
        ],
        tracesSampleRate: 0.1,
        replaysSessionSampleRate: 0.1,
        replaysOnErrorSampleRate: 1.0,
      });
      
      console.log('[Sentry] Initialized successfully');
    } catch (error) {
      console.warn('[Sentry] Failed to initialize:', error);
    }
  }
}

export function captureException(error, context = {}) {
  console.error('[Error]', error);
  
  if (Sentry) {
    Sentry.captureException(error, {
      extra: context,
    });
  }
}

export function captureMessage(message, level = 'info') {
  if (Sentry) {
    Sentry.captureMessage(message, level);
  }
}

export function setUser(user) {
  if (Sentry && user) {
    Sentry.setUser({
      id: user.id,
      email: user.email,
      username: user.username,
    });
  }
}

export function clearUser() {
  if (Sentry) {
    Sentry.setUser(null);
  }
}

export function addBreadcrumb(breadcrumb) {
  if (Sentry) {
    Sentry.addBreadcrumb(breadcrumb);
  }
}

// React Error Boundary with Sentry
export function SentryErrorBoundary({ children, fallback }) {
  if (Sentry) {
    return Sentry.ErrorBoundary({ fallback, children });
  }
  return children;
}
