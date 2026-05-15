/// <reference types="vite/client" />

export {}

declare global {
  interface Window {
    keycloak?: {
      authenticated?: boolean
      token?: string
      logout: (options?: { redirectUri?: string }) => void
      updateToken?: (minValidity: number) => Promise<boolean>
      accountManagement?: () => Promise<void> | void
      createAccountUrl?: (options?: { redirectUri?: string }) => string
    }
  }
}
