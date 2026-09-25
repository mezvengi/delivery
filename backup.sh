#!/bin/bash
# ==============================================================================
# Database Backup Script for Sour El Ghozlane Delivery System
# Target: PostgreSQL 16 in Docker Container (sour_postgres)
# ==============================================================================

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/sour_delivery_${TIMESTAMP}.sql.gz"
CONTAINER_NAME="sour_postgres"
DB_USER="sour_user"
DB_NAME="sour_delivery"
RETENTION_DAYS=14

mkdir -p "${BACKUP_DIR}"

echo "[$(date)] Starting backup of ${DB_NAME} from container ${CONTAINER_NAME}..."

# Execute pg_dump inside container and compress output with gzip
docker exec -t "${CONTAINER_NAME}" pg_dump -U "${DB_USER}" "${DB_NAME}" | gzip > "${BACKUP_FILE}"

echo "[$(date)] Backup completed successfully: ${BACKUP_FILE} ($(du -h "${BACKUP_FILE}" | cut -f1))"

# Prune old backups older than RETENTION_DAYS
echo "[$(date)] Cleaning up backups older than ${RETENTION_DAYS} days..."
find "${BACKUP_DIR}" -type f -name "sour_delivery_*.sql.gz" -mtime +"${RETENTION_DAYS}" -delete

echo "[$(date)] Backup maintenance routine finished."
