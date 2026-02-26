# MintStack Finance Portal - Deployment Guide

## 1. Requirements

- Linux host (Ubuntu 22.04+ recommended)
- Docker 24+
- Docker Compose 2.20+
- Git
- Minimum: 4 CPU, 8 GB RAM, 50 GB SSD

## 2. Development Deployment

```bash
git clone https://github.com/YOUR_USERNAME/MintStack-Finance.git
cd MintStack-Finance
cp .env.example .env
docker compose up -d
```

Common operations:

```bash
docker compose logs -f
docker compose restart
docker compose down -v
```

## 3. Production Deployment

1. Configure `.env` with strong non-default values.
2. Create required Docker secret files under `./secrets`.
3. Provide TLS certificates under `./nginx/ssl`.
4. Start production stack:

```bash
docker compose -f docker-compose.prod.yml up -d
```

## 4. Backups

### PostgreSQL

Development:

```bash
docker exec mintstack-postgres pg_dump -U mintstack mintstack_finance > backup.sql
```

Production:

```bash
docker exec mintstack-postgres-prod pg_dump -U mintstack mintstack_finance > backup-prod.sql
```

### Keycloak Realm Export

Development:

```bash
docker exec mintstack-keycloak /opt/keycloak/bin/kc.sh export --realm mintstack-finance --file /tmp/realm-export.json
docker cp mintstack-keycloak:/tmp/realm-export.json .
```

Production:

```bash
docker exec mintstack-keycloak-prod /opt/keycloak/bin/kc.sh export --realm mintstack-finance --file /tmp/realm-export.json
docker cp mintstack-keycloak-prod:/tmp/realm-export.json ./realm-export-prod.json
```

## 5. CI/CD Notes

- CI pipeline validates backend/frontend build and tests.
- Deploy workflow signs image artifacts and deploys via SSH using repository secrets.
- Required staging secrets: `STAGING_HOST`, `STAGING_USER`, `STAGING_SSH_KEY`
- Required production secrets: `PROD_HOST`, `PROD_USER`, `PROD_SSH_KEY`
