# ===========================================

# SECRETS DIRECTORY

# ===========================================

# This directory should contain secret files for production deployment

# DO NOT commit actual secret files to version control

# ===========================================

## Required Secret Files

Create the following files in this directory before production deployment:

### 1. postgres_password.txt

```
<your-strong-postgres-password>
```

### 2. redis_password.txt

```
<your-strong-redis-password>
```

### 3. keycloak_admin_password.txt

```
<your-strong-keycloak-admin-password>
```

### 4. alpha_vantage_key.txt

```
<your-alpha-vantage-api-key>
```

## Generating Strong Passwords

Use the following command to generate secure passwords:

```bash
# Linux/macOS
openssl rand -base64 32

# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { [byte](Get-Random -Maximum 256) }))
```

## Security Best Practices

1. **Never commit secret files** - They should be in .gitignore
2. **Use file permissions** - `chmod 600 *.txt` on Linux/macOS
3. **Rotate passwords** - Change periodically (every 90 days recommended)
4. **Use a secrets manager** - Consider HashiCorp Vault for enterprise deployments
5. **Audit access** - Monitor who has access to this directory

## For Docker Swarm

If using Docker Swarm, create secrets directly:

```bash
echo "your_password" | docker secret create postgres_password -
echo "your_password" | docker secret create redis_password -
echo "your_password" | docker secret create keycloak_admin_password -
echo "your_api_key" | docker secret create alpha_vantage_key -
```
