import Keycloak from 'keycloak-js'
import { setKeycloakInstance as setRtkKeycloakInstance } from '@/store/api/baseApi'

const keycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'mintstack-finance',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'finance-frontend',
}

export const keycloak = new Keycloak(keycloakConfig)

setRtkKeycloakInstance(keycloak)
