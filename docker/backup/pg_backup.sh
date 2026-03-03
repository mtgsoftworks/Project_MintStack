#!/usr/bin/env sh
set -eu

# Usage:
#   ./docker/backup/pg_backup.sh [dev|prod] [backup_dir] [retention_days]
# Examples:
#   ./docker/backup/pg_backup.sh dev ./backups 7
#   ./docker/backup/pg_backup.sh prod ./backups 14

MODE="${1:-dev}"
BACKUP_DIR="${2:-./backups}"
RETENTION_DAYS="${3:-7}"

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

TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
BACKUP_FILE="$BACKUP_DIR/${MODE}_${DB_NAME}_${TIMESTAMP}.sql.gz"

echo "Creating backup: $BACKUP_FILE"
docker exec "$CONTAINER_NAME" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_FILE"
echo "Backup completed: $BACKUP_FILE"

echo "Cleaning backups older than ${RETENTION_DAYS} days in $BACKUP_DIR"
find "$BACKUP_DIR" -type f -name "${MODE}_${DB_NAME}_*.sql.gz" -mtime "+$RETENTION_DAYS" -delete
echo "Retention cleanup completed."
