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
file_env 'OPENSEARCH_PASSWORD'
file_env 'SPRING_KAFKA_SASL_PASSWORD'
file_env 'APP_FIELD_ENCRYPTION_KEY'

# Derive Kafka JAAS config from secret value when SASL password is provided.
if [ -n "${SPRING_KAFKA_SASL_PASSWORD:-}" ] && [ -z "${SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG:-}" ]; then
  KAFKA_SASL_USERNAME="${SPRING_KAFKA_SASL_USERNAME:-kafka}"
  export SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG="org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${KAFKA_SASL_USERNAME}\" password=\"${SPRING_KAFKA_SASL_PASSWORD}\";"
fi

echo "[entrypoint] Secrets loaded. Starting application..."

# Execute the main command
exec "$@"
