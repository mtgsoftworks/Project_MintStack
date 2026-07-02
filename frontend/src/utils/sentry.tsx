// Sentry Error Tracking Integration
// Install: npm install @sentry/react
/* eslint-disable react-refresh/only-export-components */

import React from 'react'
import type * as SentryTypes from '@sentry/react'

let Sentry: typeof SentryTypes | null = null;

export async function initSentry(): Promise<void> {
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
            maskAllText: true,
            blockAllMedia: true,
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

export function captureException(error: unknown, context: Record<string, unknown> = {}): void {
  console.error('[Error]', error);

  if (Sentry) {
    Sentry.captureException(error, {
      extra: context,
    });
  }
}

export function captureMessage(message: string, level: SentryTypes.SeverityLevel = 'info'): void {
  if (Sentry) {
    Sentry.captureMessage(message, level);
  }
}

export function setUser(user: { id: string; email?: string; username?: string } | null): void {
  if (Sentry && user) {
    Sentry.setUser({
      id: user.id,
      email: user.email,
      username: user.username,
    });
  }
}

export function clearUser(): void {
  if (Sentry) {
    Sentry.setUser(null);
  }
}

export function addBreadcrumb(breadcrumb: SentryTypes.Breadcrumb): void {
  if (Sentry) {
    Sentry.addBreadcrumb(breadcrumb);
  }
}

// React Error Boundary with Sentry
export function SentryErrorBoundary({ children, fallback }: { children: React.ReactNode; fallback: React.ComponentProps<typeof SentryTypes.ErrorBoundary>['fallback'] }) {
  const ErrorBoundary = Sentry?.ErrorBoundary;
  if (ErrorBoundary) {
    return <ErrorBoundary fallback={fallback}>{children}</ErrorBoundary>;
  }
  return <>{children}</>;
}
