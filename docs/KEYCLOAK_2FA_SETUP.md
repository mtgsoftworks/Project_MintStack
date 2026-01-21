# Keycloak 2FA (Two-Factor Authentication) Setup Guide

## Overview

MintStack Finance Portal uses Keycloak for authentication. This guide explains how to enable 2FA (Two-Factor Authentication) and "Remember Me" functionality.

## Prerequisites

- Keycloak server running (v23.0+)
- Admin access to Keycloak Admin Console
- `mintstack-finance` realm created

## Enable 2FA (OTP - One Time Password)

### Step 1: Configure OTP Policy

1. Login to Keycloak Admin Console
2. Select `mintstack-finance` realm
3. Go to **Authentication** → **Policies** → **OTP Policy**
4. Configure settings:
   ```
   OTP Type: totp (Time-based)
   OTP Hash Algorithm: SHA1
   Number of Digits: 6
   Look Ahead Window: 1
   OTP Token Period: 30 seconds
   ```
5. Click **Save**

### Step 2: Create Required Action

1. Go to **Authentication** → **Required Actions**
2. Find **Configure OTP** and enable:
   - ✅ Enabled
   - ✅ Default Action (if you want all users to set up 2FA)
3. Click **Save**

### Step 3: Configure Authentication Flow

1. Go to **Authentication** → **Flows**
2. Select **Browser** flow
3. Add **OTP Form** execution after **Username Password Form**:
   ```
   Browser
   ├── Cookie (ALTERNATIVE)
   ├── Kerberos (DISABLED)
   └── Forms (ALTERNATIVE)
       ├── Username Password Form (REQUIRED)
       └── OTP Form (REQUIRED)  ← Add this
   ```
4. Set OTP Form requirement to **REQUIRED** or **CONDITIONAL**

### Step 4: For Conditional 2FA (Optional)

If you want 2FA only for specific users/roles:

1. Create a new flow based on Browser
2. Add **Conditional OTP Form** instead of OTP Form
3. Configure conditions (e.g., user has role "require-2fa")

## Enable "Remember Me" Feature

### Step 1: Realm Settings

1. Go to **Realm Settings** → **Sessions**
2. Configure:
   ```
   SSO Session Idle: 30 days (or your preference)
   SSO Session Max: 90 days
   Remember Me: ON ✅
   ```

### Step 2: Client Settings

1. Go to **Clients** → `finance-frontend`
2. In **Settings** tab, ensure:
   ```
   Standard Flow Enabled: ON
   Direct Access Grants Enabled: ON (if needed)
   ```

### Step 3: Login Theme (Optional)

1. Go to **Realm Settings** → **Themes**
2. Set **Login Theme** to include "Remember Me" checkbox
3. Default Keycloak theme already includes this

## Frontend Configuration

The frontend is already configured to work with Keycloak. No changes needed unless customizing:

```javascript
// frontend/src/App.jsx
keycloak.init({
  onLoad: 'login-required',
  silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
  pkceMethod: 'S256',
})
```

## Testing 2FA

1. Login to the application
2. You'll be prompted to set up OTP
3. Use an authenticator app (Google Authenticator, Authy, etc.)
4. Scan QR code and enter the 6-digit code
5. Future logins will require OTP

## Troubleshooting

### "Remember Me" not working
- Check realm session settings
- Verify client has correct settings
- Clear browser cookies and try again

### 2FA not prompting
- Verify OTP Form is in the authentication flow
- Check user has "Configure OTP" required action
- Ensure OTP Policy is configured correctly

### Users locked out
- Admin can remove OTP credential:
  1. Go to **Users** → Select user
  2. **Credentials** tab
  3. Delete OTP credential
  4. User will be prompted to set up again

## Security Recommendations

1. **Require 2FA for admin users** - Always
2. **Session timeout** - Set reasonable values (not too long)
3. **Brute force protection** - Enable in Realm Settings → Security Defenses
4. **Password policy** - Configure strong password requirements

## API Endpoints (Backend)

The backend doesn't need changes for 2FA - Keycloak handles it. However, ensure:

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}
```

## References

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Keycloak OTP Guide](https://www.keycloak.org/docs/latest/server_admin/#otp-policies)
- [Keycloak Session Management](https://www.keycloak.org/docs/latest/server_admin/#session-and-token-timeouts)
