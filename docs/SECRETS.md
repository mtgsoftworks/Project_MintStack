# Secrets Management Guide

## 🔐 Overview

This document describes how to securely manage secrets for MintStack Finance Portal across different environments.

---

## Environment-Based Configuration

### Development (Local)

For local development, copy `.env.example` to `.env`:

```bash
cp env.local .env
# Edit .env and replace placeholder values
```

> [!WARNING]
> Never commit `.env` file to version control!

### Production (Docker)

Production uses Docker Secrets for secure secret management:

```bash
# 1. Create secrets directory
mkdir -p secrets

# 2. Generate and store passwords
openssl rand -base64 32 > secrets/postgres_password.txt
openssl rand -base64 32 > secrets/redis_password.txt
openssl rand -base64 32 > secrets/keycloak_admin_password.txt
echo "your-api-key" > secrets/alpha_vantage_key.txt

# 3. Set proper permissions
chmod 600 secrets/*.txt

# 4. Deploy with production compose
docker-compose -f docker-compose.prod.yml up -d
```

---

## Secret Files Reference

| File | Description | Used By |
|------|-------------|---------|
| `postgres_password.txt` | PostgreSQL admin password | postgres, backend, keycloak |
| `redis_password.txt` | Redis authentication password | redis, backend |
| `keycloak_admin_password.txt` | Keycloak admin console password | keycloak |
| `alpha_vantage_key.txt` | Alpha Vantage API key | backend |

---

## Docker Secrets (Swarm Mode)

For Docker Swarm deployments:

```bash
# Create secrets
echo "your_password" | docker secret create postgres_password -
echo "your_password" | docker secret create redis_password -
echo "your_password" | docker secret create keycloak_admin_password -
echo "your_api_key" | docker secret create alpha_vantage_key -

# List secrets
docker secret ls

# Remove a secret
docker secret rm secret_name
```

---

## Kubernetes Secrets

For Kubernetes deployments:

```yaml
# secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: mintstack-secrets
type: Opaque
stringData:
  POSTGRES_PASSWORD: "<your-password>"
  REDIS_PASSWORD: "<your-password>"
  KEYCLOAK_ADMIN_PASSWORD: "<your-password>"
  ALPHA_VANTAGE_API_KEY: "<your-api-key>"
```

Apply with:

```bash
kubectl apply -f secrets.yaml
```

---

## Best Practices

### Password Generation

```bash
# Linux/macOS
openssl rand -base64 32

# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { [byte](Get-Random -Maximum 256) }))

# Using /dev/urandom
cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1
```

### Security Checklist

- [ ] All secrets are excluded from version control (check `.gitignore`)
- [ ] Secret files have restricted permissions (`chmod 600`)
- [ ] Secrets are rotated every 90 days
- [ ] No secrets in application logs
- [ ] Secrets are encrypted at rest
- [ ] Access to secrets is audited

### What NOT to Do

❌ Never commit `.env` files with real secrets  
❌ Never log secrets in application output  
❌ Never share secrets via email or chat  
❌ Never use default passwords in production  
❌ Never store secrets in source code  

---

## Enterprise Solutions

For larger deployments, consider:

| Solution | Description |
|----------|-------------|
| **HashiCorp Vault** | Secrets management with dynamic credentials |
| **AWS Secrets Manager** | AWS-native secrets management |
| **Azure Key Vault** | Azure-native key management |
| **GCP Secret Manager** | Google Cloud secrets service |

---

## Troubleshooting

### Secret Not Found

```bash
# Check if secret file exists
ls -la secrets/

# Check file permissions
stat secrets/postgres_password.txt
```

### Docker Secret Issues

```bash
# Inspect secret usage
docker service inspect service_name --pretty

# Check if container has access
docker exec container_name cat /run/secrets/postgres_password
```

---

## Contact

For security-related issues, contact the DevOps team.
