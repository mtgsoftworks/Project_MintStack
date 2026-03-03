#!/usr/bin/env sh
set -eu

# Usage:
#   ./docker/backup/pg_restore.sh [dev|prod] <backup_file.sql.gz>
# Example:
#   ./docker/backup/pg_restore.sh dev ./backups/dev_mintstack_finance_20260303_120000.sql.gz

MODE="${1:-dev}"
BACKUP_FILE="${2:-}"

if [ -z "$BACKUP_FILE" ]; then
  echo "Backup file path is required."
  echo "Usage: ./docker/backup/pg_restore.sh [dev|prod] <backup_file.sql.gz>"
  exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
  echo "Backup file not found: $BACKUP_FILE"
  exit 1
fi

case "$MODE" in
  dev)
    CONTAINER_NAME="mintstack-postgres"
    DB_NAME="${POSTGRES_DB:-mintstack_finance}"
    DB_USER="${POSTGRES_USER:-mintstack}"
    ;;
  prod)
    CONTAINER_NAME="mintstack-postgres-prod"
    DB_NAME="${POSTGRES_DB:-mintstack_finance}"
    DB_USER="${POSTGRES_USER:-mintstack}"
    ;;
  *)
    echo "Invalid mode: $MODE. Use dev or prod."
    exit 1
    ;;
esac

echo "Restoring backup file: $BACKUP_FILE"
echo "Target: container=$CONTAINER_NAME db=$DB_NAME user=$DB_USER"

if echo "$BACKUP_FILE" | grep -q '\.gz$'; then
  gunzip -c "$BACKUP_FILE" | docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME"
else
  cat "$BACKUP_FILE" | docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME"
fi

echo "Restore completed."
