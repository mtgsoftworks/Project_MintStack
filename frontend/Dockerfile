# Build stage
FROM node:20.19-alpine AS build
WORKDIR /app

# Copy package files
COPY package*.json ./
RUN npm ci

# Declare build arguments for Vite (embedded at build time)
ARG VITE_API_URL=/api/v1
ARG VITE_KEYCLOAK_URL
ARG VITE_KEYCLOAK_REALM=mintstack-finance
ARG VITE_KEYCLOAK_CLIENT_ID=finance-frontend
ARG VITE_WS_URL

# Make build args available as env vars for Vite
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL
ENV VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM
ENV VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID
ENV VITE_WS_URL=$VITE_WS_URL

# Copy source and build
COPY . .
RUN npm run build

# Production stage with non-root nginx
FROM nginxinc/nginx-unprivileged:1.27-alpine
WORKDIR /usr/share/nginx/html

ENV PORT=3000
ENV BACKEND_UPSTREAM=http://backend:8080
ENV NGINX_RESOLVER=127.0.0.11

# Copy built assets from build stage
COPY --chown=101:101 --from=build /app/dist .

# Copy nginx configuration template. The official nginx entrypoint renders
# /etc/nginx/templates/*.template with environment variables before startup.
COPY --chown=101:101 nginx.conf /etc/nginx/templates/default.conf.template

# Expose port
EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD sh -c 'wget -q --spider "http://127.0.0.1:${PORT:-3000}/health" || exit 1'

CMD ["nginx", "-g", "daemon off;"]
