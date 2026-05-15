/// <reference types="vite/client" />

export {}

declare global {
  interface Window {
    keycloak?: {
      authenticated?: boolean
      token?: string
      login?: (options?: {
        action?: string
        redirectUri?: string
        prompt?: 'none' | 'login' | 'consent'
      }) => Promise<void>
      logout: (options?: { redirectUri?: string }) => void
      updateToken?: (minValidity: number) => Promise<boolean>
    }
  }
}
