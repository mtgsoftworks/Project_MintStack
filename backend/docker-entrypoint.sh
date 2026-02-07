#!/bin/sh
# ===========================================
# Docker Secrets _FILE pattern entrypoint
# ===========================================
# Reads Docker secrets from /run/secrets/ and exposes them
# as environment variables for the Spring Boot application.
# Usage: For any env var FOO_FILE pointing to a secret file,
# this script sets FOO to the file's contents.
# ===========================================

set -e

# Function to process _FILE env vars
file_env() {
  local var="$1"
  local fileVar="${var}_FILE"
  local def="${2:-}"
  
  # Get current values
  eval local val="\${$var:-}"
  eval local fileVal="\${$fileVar:-}"
  
  if [ -n "$val" ] && [ -n "$fileVal" ]; then
    echo "WARNING: Both $var and $fileVar are set. Using $var."
  fi
  
  if [ -n "$fileVal" ]; then
    if [ -f "$fileVal" ]; then
      val="$(cat "$fileVal")"
      export "$var"="$val"
    else
      echo "WARNING: Secret file $fileVal does not exist for $var"
    fi
  fi
  
  if [ -z "$val" ] && [ -n "$def" ]; then
    export "$var"="$def"
  fi
}

# Process known secret environment variables
file_env 'SPRING_DATASOURCE_PASSWORD'
file_env 'SPRING_DATA_REDIS_PASSWORD'
file_env 'ALPHA_VANTAGE_API_KEY'
file_env 'FINNHUB_API_KEY'
file_env 'SMTP_PASSWORD'
file_env 'SMTP_USERNAME'

echo "[entrypoint] Secrets loaded. Starting application..."

# Execute the main command
exec "$@"
