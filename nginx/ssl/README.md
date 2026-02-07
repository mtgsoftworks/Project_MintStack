# SSL Certificates

This directory should contain SSL certificates for production deployment.

## Required Files

- `fullchain.pem` - Full certificate chain (certificate + intermediate CA)
- `privkey.pem` - Private key

## Generating Self-Signed Certificates (Development/Testing)

```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout privkey.pem \
  -out fullchain.pem \
  -subj "/C=TR/ST=Sakarya/L=Sakarya/O=MintStack/CN=localhost"
```

## Using Let's Encrypt (Production)

```bash
# Install certbot
sudo apt install certbot

# Generate certificates
sudo certbot certonly --standalone -d yourdomain.com

# Copy certificates
sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem ./fullchain.pem
sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem ./privkey.pem
```

## Security

- **NEVER** commit actual certificate files to version control
- Set file permissions: `chmod 600 privkey.pem`
- Renew certificates before expiry (Let's Encrypt: every 90 days)
