#!/usr/bin/env bash
set -euo pipefail

file_env() {
  local variable="$1"
  local file_variable="${variable}_FILE"
  local value="${!variable:-}"
  local file_value="${!file_variable:-}"

  if [[ -n "$value" && -n "$file_value" ]]; then
    echo "Both ${variable} and ${file_variable} are set; ${variable} takes precedence." >&2
    return
  fi

  if [[ -n "$file_value" ]]; then
    if [[ ! -r "$file_value" ]]; then
      echo "Secret file for ${variable} is not readable: ${file_value}" >&2
      exit 1
    fi
    export "${variable}=$(<"$file_value")"
  fi
}

file_env KC_DB_PASSWORD
file_env KC_BOOTSTRAP_ADMIN_PASSWORD
file_env LDAP_BIND_CREDENTIAL

exec /opt/keycloak/bin/kc.sh "$@"
