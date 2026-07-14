#!/usr/bin/env bash

set -Eeuo pipefail

export LC_ALL=C
export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

readonly APP_JAR="/opt/secp/smart-elderly-care-backend-0.0.1-SNAPSHOT.jar"
readonly APP_DIR="/opt/secp"
readonly APP_BASENAME="$(basename -- "$APP_JAR")"
readonly INCOMING_DIR="/opt/secp/incoming"
readonly BACKUP_DIR="/opt/secp/backups"
readonly APP_ENV="/opt/secp/app.env"
readonly PROD_CONFIG="/opt/secp/application-prod.yml"
readonly SERVICE_NAME="secp-backend.service"
readonly HEALTH_URL="http://127.0.0.1:8080/api/actuator/health"
readonly HEALTH_TIMEOUT_SECONDS=90
readonly HEALTH_INTERVAL_SECONDS=3
readonly BACKUP_KEEP=5
readonly LOCK_FILE="/run/lock/deploy-secp-backend.lock"

ACTIVATION_TMP=""
ROLLBACK_TMP=""

log() {
  printf '[deploy-secp-backend] %s\n' "$*"
}

fail() {
  printf '[deploy-secp-backend] ERROR: %s\n' "$*" >&2
  exit 1
}

cleanup() {
  if [[ -n "$ACTIVATION_TMP" && -e "$ACTIVATION_TMP" ]]; then
    rm -f -- "$ACTIVATION_TMP"
  fi
  if [[ -n "$ROLLBACK_TMP" && -e "$ROLLBACK_TMP" ]]; then
    rm -f -- "$ROLLBACK_TMP"
  fi
  return 0
}

trap cleanup EXIT

health_check() {
  local curl_timeout="$1"
  local response

  systemctl is-active --quiet "$SERVICE_NAME" || return 1
  response="$(curl --fail --silent --show-error --max-time "$curl_timeout" "$HEALTH_URL")" || return 1
  grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' <<<"$response"
}

wait_for_health() {
  local deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))
  local remaining
  local curl_timeout
  local sleep_seconds

  while (( SECONDS < deadline )); do
    remaining=$((deadline - SECONDS))
    curl_timeout=5
    if (( remaining < curl_timeout )); then
      curl_timeout=$remaining
    fi

    if (( curl_timeout > 0 )) && health_check "$curl_timeout"; then
      return 0
    fi

    remaining=$((deadline - SECONDS))
    if (( remaining <= 0 )); then
      break
    fi

    sleep_seconds=$HEALTH_INTERVAL_SECONDS
    if (( remaining < sleep_seconds )); then
      sleep_seconds=$remaining
    fi
    sleep "$sleep_seconds"
  done

  return 1
}

restore_backup() {
  local backup_path="$1"
  local expected_sha256="$2"
  local restored_sha256

  ROLLBACK_TMP="${APP_DIR}/.${APP_BASENAME}.rollback.$$"

  if ! cp -- "$backup_path" "$ROLLBACK_TMP"; then
    log "Failed to copy the rollback JAR"
    return 1
  fi
  if ! chown root:root "$ROLLBACK_TMP" || ! chmod 0644 "$ROLLBACK_TMP"; then
    log "Failed to set rollback JAR ownership or permissions"
    return 1
  fi

  read -r restored_sha256 _ < <(sha256sum -- "$ROLLBACK_TMP")
  if [[ "$restored_sha256" != "$expected_sha256" ]]; then
    log "Rollback temporary JAR checksum does not match the backup"
    return 1
  fi

  if ! mv -fT -- "$ROLLBACK_TMP" "$APP_JAR"; then
    log "Failed to restore the previous JAR atomically"
    return 1
  fi
  ROLLBACK_TMP=""

  read -r restored_sha256 _ < <(sha256sum -- "$APP_JAR")
  if [[ "$restored_sha256" != "$expected_sha256" ]]; then
    log "Restored JAR checksum does not match the backup"
    return 1
  fi

  if ! systemctl restart "$SERVICE_NAME"; then
    log "Failed to restart $SERVICE_NAME after rollback"
    return 1
  fi
  if ! wait_for_health; then
    log "The previous version did not recover within ${HEALTH_TIMEOUT_SECONDS} seconds"
    return 1
  fi

  log "Previous version restored successfully"
  return 0
}

prune_backups() {
  local -a backups=()
  local index

  mapfile -d '' -t backups < <(
    find "$BACKUP_DIR" -maxdepth 1 -type f -name "${APP_BASENAME}.*.bak" -printf '%T@ %p\0' \
      | sort -z -nr \
      | cut -z -d ' ' -f 2-
  )

  if (( ${#backups[@]} <= BACKUP_KEEP )); then
    return 0
  fi

  for (( index=BACKUP_KEEP; index<${#backups[@]}; index++ )); do
    rm -f -- "${backups[$index]}"
  done
}

if (( $# != 3 )); then
  fail "Usage: $0 <absolute-new-jar-path> <git-commit-sha> <expected-sha256>"
fi

if (( EUID != 0 )); then
  fail "This script must run as root (use sudo)"
fi

for required_command in jar curl systemctl sha256sum realpath install find sort cut grep flock; do
  command -v "$required_command" >/dev/null 2>&1 \
    || fail "Required command not found: $required_command"
done

exec 9>"$LOCK_FILE"
flock -n 9 || fail "Another backend deployment is already running"

readonly REQUESTED_NEW_JAR="$1"
GIT_SHA="${2,,}"
EXPECTED_SHA256="${3,,}"

[[ "$REQUESTED_NEW_JAR" == /* ]] || fail "The new JAR path must be absolute"
[[ "$GIT_SHA" =~ ^[0-9a-f]{40}$ ]] \
  || fail "The Git commit SHA must contain exactly 40 hexadecimal characters"
[[ "$EXPECTED_SHA256" =~ ^[0-9a-f]{64}$ ]] \
  || fail "The expected SHA-256 must contain exactly 64 hexadecimal characters"
[[ -d "$INCOMING_DIR" ]] || fail "Incoming directory does not exist: $INCOMING_DIR"
[[ ! -L "$INCOMING_DIR" ]] || fail "Incoming directory must not be a symbolic link"
[[ ! -L "$REQUESTED_NEW_JAR" ]] || fail "The new JAR must not be a symbolic link"
[[ -f "$REQUESTED_NEW_JAR" ]] || fail "The new JAR must be a regular file"
[[ -s "$REQUESTED_NEW_JAR" ]] || fail "The new JAR must not be empty"

NEW_JAR="$(realpath -e -- "$REQUESTED_NEW_JAR")"
CANONICAL_INCOMING_DIR="$(realpath -e -- "$INCOMING_DIR")"
[[ "$(dirname -- "$NEW_JAR")" == "$CANONICAL_INCOMING_DIR" ]] \
  || fail "The new JAR must be located directly in $INCOMING_DIR"
[[ "$(basename -- "$NEW_JAR")" == "backend-${GIT_SHA}.jar" ]] \
  || fail "The uploaded JAR name must be backend-${GIT_SHA}.jar"

jar tf "$NEW_JAR" >/dev/null || fail "The uploaded file is not a valid JAR"
[[ -f "$APP_JAR" && -s "$APP_JAR" ]] || fail "Current application JAR is missing or empty"
[[ ! -L "$APP_JAR" ]] || fail "Current application JAR must not be a symbolic link"
[[ -f "$APP_ENV" && -r "$APP_ENV" ]] || fail "Production environment file is missing or unreadable: $APP_ENV"
[[ -f "$PROD_CONFIG" && -r "$PROD_CONFIG" ]] || fail "Production configuration file is missing or unreadable: $PROD_CONFIG"

read -r NEW_SHA256 _ < <(sha256sum -- "$NEW_JAR")
[[ "$NEW_SHA256" == "$EXPECTED_SHA256" ]] || fail "Uploaded JAR SHA-256 does not match the expected SHA-256"

install -d -o root -g root -m 0700 "$BACKUP_DIR"
[[ ! -L "$BACKUP_DIR" ]] || fail "Backup directory must not be a symbolic link"

read -r ORIGINAL_SHA256 _ < <(sha256sum -- "$APP_JAR")

TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
BACKUP_PATH="${BACKUP_DIR}/${APP_BASENAME}.${TIMESTAMP}.${GIT_SHA}.${ORIGINAL_SHA256:0:12}.bak"
[[ ! -e "$BACKUP_PATH" ]] || fail "Backup path already exists: $BACKUP_PATH"

log "Backing up the current application JAR"
cp --preserve=mode,ownership -- "$APP_JAR" "$BACKUP_PATH"

read -r BACKUP_SHA256 _ < <(sha256sum -- "$BACKUP_PATH")
[[ "$BACKUP_SHA256" == "$ORIGINAL_SHA256" ]] || fail "Backup checksum verification failed"

ACTIVATION_TMP="${APP_DIR}/.${APP_BASENAME}.${GIT_SHA}.new.$$"
log "Preparing the new application JAR"
cp -- "$NEW_JAR" "$ACTIVATION_TMP"
chown root:root "$ACTIVATION_TMP"
chmod 0644 "$ACTIVATION_TMP"

read -r COPIED_SHA256 _ < <(sha256sum -- "$ACTIVATION_TMP")
[[ "$COPIED_SHA256" == "$NEW_SHA256" ]] || fail "Prepared JAR checksum verification failed"

log "Activating the new application JAR"
mv -fT -- "$ACTIVATION_TMP" "$APP_JAR"
ACTIVATION_TMP=""

deployment_failed=false
if ! systemctl restart "$SERVICE_NAME"; then
  log "Failed to restart $SERVICE_NAME with the new JAR"
  deployment_failed=true
elif ! wait_for_health; then
  log "New version did not become healthy within ${HEALTH_TIMEOUT_SECONDS} seconds"
  deployment_failed=true
fi

if [[ "$deployment_failed" == true ]]; then
  log "Deployment failed; restoring the previous JAR"
  if ! restore_backup "$BACKUP_PATH" "$ORIGINAL_SHA256"; then
    log "Automatic rollback failed; manual recovery is required"
  fi
  exit 1
fi

rm -f -- "$NEW_JAR"
prune_backups

log "Deployment succeeded: ${GIT_SHA}"
