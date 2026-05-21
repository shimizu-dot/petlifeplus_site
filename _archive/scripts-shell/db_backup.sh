#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKUP_DIR="${ROOT_DIR}/backups"
DATE="$(date +%Y%m%d_%H%M%S)"
OUT_FILE="${BACKUP_DIR}/${DATE}_backup.sql.gz"
KEEP_GENERATIONS=7

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-petlifeplus}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-hs0512}"

mkdir -p "${BACKUP_DIR}"

# Remove partial file on failure
trap 'rm -f "${OUT_FILE}"' ERR

PGPASSWORD="${DB_PASSWORD}" pg_dump \
    -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" \
    -F p | gzip > "${OUT_FILE}"

echo "Backup created: ${OUT_FILE}"

# Retain only the latest N backups
ls -1t "${BACKUP_DIR}"/*_backup.sql.gz 2>/dev/null | tail -n +$((KEEP_GENERATIONS + 1)) | xargs -r rm -f
echo "Retention: kept latest ${KEEP_GENERATIONS} backups"
