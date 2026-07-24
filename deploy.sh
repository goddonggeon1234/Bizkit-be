#!/usr/bin/env bash
set -euo pipefail

BASE="/home/ubuntu"

SERVICE_UNIT="bizkit-backend.service"
ENV_FILE="${BASE}/.env-be"

ARTIFACT_DIR="${BASE}/artifact/be"
RELEASES_DIR="${ARTIFACT_DIR}/releases"
CURRENT_LINK="${ARTIFACT_DIR}/current"
BACKUP_DIR="${BASE}/backup/be"

CURRENT_FILE="${ARTIFACT_DIR}/.current_version"

RELEASE_ID="${1:-}"
DOWNLOADED_JAR="${ARTIFACT_DIR}/bizkit-be-${RELEASE_ID}.jar"

HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8080/actuator/health}"

KEEP_RELEASES="${KEEP_RELEASES:-2}"
KEEP_BACKUPS="${KEEP_BACKUPS:-2}"

echo "[deploy-be] cleanup: keep releases=${KEEP_RELEASES}, backups=${KEEP_BACKUPS}"

cleanup_releases() {
  local keep="$1"
  local releases_dir="$2"
  local current_id="$3"
  local prev_id="$4"

  [[ -d "$releases_dir" ]] || return 0

  mapfile -t all < <(ls -1 "$releases_dir" 2>/dev/null | sort -V)

  local total="${#all[@]}"
  if (( total <= keep )); then
    echo "[deploy-be] cleanup releases: nothing to delete (total=${total})"
    return 0
  fi

  local delete_count=$(( total - keep ))
  local deleted=0

  for v in "${all[@]}"; do
    if [[ "$v" == "$current_id" || "$v" == "$prev_id" ]]; then
      continue
    fi

    echo "[deploy-be] cleanup releases: rm -rf ${releases_dir}/${v}"
    sudo rm -rf "${releases_dir}/${v}" || true
    deleted=$((deleted + 1))

    if (( deleted >= delete_count )); then
      break
    fi
  done
}

cleanup_backups() {
  local keep="$1"
  local backup_dir="$2"

  [[ -d "$backup_dir" ]] || return 0

  mapfile -t files < <(ls -1t "$backup_dir" 2>/dev/null || true)
  local total="${#files[@]}"

  if (( total <= keep )); then
    echo "[deploy-be] cleanup backups: nothing to delete (total=${total})"
    return 0
  fi

  for ((i=keep; i<total; i++)); do
    echo "[deploy-be] cleanup backups: rm -f ${backup_dir}/${files[$i]}"
    sudo rm -f "${backup_dir}/${files[$i]}" || true
  done
}



if [[ -z "${RELEASE_ID}" ]]; then
  echo "[deploy-be] missing RELEASE_ID. usage: deploy-be.sh <version>" >&2
  exit 2
fi

if [[ ! -f "${DOWNLOADED_JAR}" ]]; then
  echo "[deploy-be] jar not found: ${DOWNLOADED_JAR}" >&2
  exit 3
fi

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "[deploy-be] env not found: ${ENV_FILE}" >&2
  exit 4
fi

sudo mkdir -p "${BACKUP_DIR}" "${RELEASES_DIR}"

# --- read previous version ---
PREV_ID=""
if [[ -f "${CURRENT_FILE}" ]]; then
  PREV_ID="$(cat "${CURRENT_FILE}" | tr -d '\r' | xargs || true)"
fi

if [[ -n "${PREV_ID}" && -d "${RELEASES_DIR}/${PREV_ID}" ]]; then
  echo "[deploy-be] backup prev=${PREV_ID} -> ${BACKUP_DIR}"

  PREV_JAR="${RELEASES_DIR}/${PREV_ID}/bizkit-be-${PREV_ID}.jar"
  if [[ -f "${PREV_JAR}" ]]; then
    cp -f "${PREV_JAR}" "${BACKUP_DIR}/bizkit-be-${PREV_ID}.jar"
  fi

  cp -f "${ENV_FILE}" "${BACKUP_DIR}/bizkit-be-${PREV_ID}.env" || true
else
  echo "[deploy-be] no previous version to backup (first deploy?)"
fi

TARGET_DIR="${RELEASES_DIR}/${RELEASE_ID}"
TARGET_JAR="${TARGET_DIR}/bizkit-be-${RELEASE_ID}.jar"

echo "[deploy-be] install jar -> ${TARGET_JAR}"
sudo rm -rf "${TARGET_DIR}"
sudo mkdir -p "${TARGET_DIR}"
sudo mv -f "${DOWNLOADED_JAR}" "${TARGET_JAR}"
sudo chmod 0644 "${TARGET_JAR}"

sudo chown -R ubuntu:ubuntu "${RELEASES_DIR}"

echo "[deploy-be] Updating symlink: ${CURRENT_LINK} -> ${TARGET_DIR}"
sudo ln -sfn "${TARGET_DIR}" "${CURRENT_LINK}"

OVERRIDE_DIR="/etc/systemd/system/${SERVICE_UNIT}.d"
sudo mkdir -p "${OVERRIDE_DIR}"

sudo tee "${OVERRIDE_DIR}/override.conf" >/dev/null <<EOF
[Service]
EnvironmentFile=-${ENV_FILE}
ExecStart=
ExecStart=/usr/bin/java -XX:+ExitOnOutOfMemoryError -jar ${CURRENT_LINK}/bizkit-be-${RELEASE_ID}.jar --server.port=8080 --server.address=0.0.0.0
EOF

echo "[deploy-be] Restarting ${SERVICE_UNIT}"
sudo systemctl daemon-reload
sudo systemctl restart "${SERVICE_UNIT}"

ok=0
for i in $(seq 1 30); do
  if curl -fsS --max-time 2 "${HEALTH_URL}" >/dev/null; then
    ok=1
    break
  fi
  sleep 1
done

if [[ "${ok}" -eq 1 ]]; then
  echo "${RELEASE_ID}" | sudo tee "${CURRENT_FILE}" >/dev/null

  cleanup_releases "$KEEP_RELEASES" "$RELEASES_DIR" "$RELEASE_ID" "$PREV_ID"
  cleanup_backups "$KEEP_BACKUPS" "$BACKUP_DIR"

  echo "[deploy-be] SUCCESS version=${RELEASE_ID}"
  exit 0
fi

echo "[deploy-be] FAILED healthcheck. try rollback to prev=${PREV_ID}" >&2

if [[ -n "${PREV_ID}" && -d "${RELEASES_DIR}/${PREV_ID}" ]]; then
  PREV_DIR="${RELEASES_DIR}/${PREV_ID}"
  PREV_JAR="${PREV_DIR}/bizkit-be-${PREV_ID}.jar"

  echo "[deploy-be] rollback symlink -> ${PREV_DIR}"
  sudo ln -sfn "${PREV_DIR}" "${CURRENT_LINK}"
  sudo ln -sfn "${PREV_JAR}" "${CURRENT_LINK}/app.jar" || true

  sudo systemctl restart "${SERVICE_UNIT}"

  if curl -fsS --max-time 2 "${HEALTH_URL}" >/dev/null; then
    echo "[deploy-be] ROLLBACK OK -> ${PREV_ID}" >&2
  else
    echo "[deploy-be] ROLLBACK ALSO FAILED" >&2
  fi
else
  echo "[deploy-be] no previous version to rollback to" >&2
fi

exit 1
