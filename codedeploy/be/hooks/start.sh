#!/usr/bin/env bash
set -euo pipefail

HOOK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REV_DIR="$(cd "${HOOK_DIR}/../../.." && pwd)"

echo "[start-be] HOOK_DIR=${HOOK_DIR}"
echo "[start-be] REV_DIR=${REV_DIR}"

ENV_SH="${REV_DIR}/codedeploy/be/env.sh"
if [[ -f "${ENV_SH}" ]]; then
  # shellcheck disable=SC1091
  source "${ENV_SH}"
else
  echo "[start-be] env.sh not found: ${ENV_SH}" >&2
  exit 11
fi

: "${RELEASE_ID:?RELEASE_ID is required}"

ARCHIVE_URL="${ARCHIVE_URL:-${JAR_URL:-}}"
: "${ARCHIVE_URL:?ARCHIVE_URL (or JAR_URL) is required}"

DEPLOY_SCRIPT=""
if [[ -f "${REV_DIR}/deploy-be.sh" ]]; then
  DEPLOY_SCRIPT="${REV_DIR}/deploy-be.sh"
elif [[ -f "${REV_DIR}/codedeploy/be/deploy-be.sh" ]]; then
  DEPLOY_SCRIPT="${REV_DIR}/codedeploy/be/deploy-be.sh"
elif [[ -f "/home/ubuntu/deploy-be.sh" ]]; then
  DEPLOY_SCRIPT="/home/ubuntu/deploy-be.sh"
else
  echo "[start-be] deploy-be.sh not found in expected locations" >&2
  echo "[start-be] tried:" >&2
  echo "  - ${REV_DIR}/deploy-be.sh" >&2
  echo "  - ${REV_DIR}/codedeploy/be/deploy-be.sh" >&2
  echo "  - /home/ubuntu/deploy-be.sh" >&2
  exit 12
fi

ARTIFACT_DIR="/home/ubuntu/artifact/be"
JAR_PATH="${ARTIFACT_DIR}/bizkit-be-${RELEASE_ID}.jar"

sudo mkdir -p "${ARTIFACT_DIR}"

echo "[start-be] download jar -> ${JAR_PATH}"
TMP="/tmp/bizkit-be-${RELEASE_ID}.jar"

if command -v curl >/dev/null 2>&1; then
  curl -fsSL "${ARCHIVE_URL}" -o "${TMP}"
elif command -v wget >/dev/null 2>&1; then
  wget -qO "${TMP}" "${ARCHIVE_URL}"
else
  echo "[start-be] neither curl nor wget is installed" >&2
  exit 10
fi

sudo mv "${TMP}" "${JAR_PATH}"
sudo chmod 0644 "${JAR_PATH}"
ls -al "${JAR_PATH}" || true

chmod +x "${DEPLOY_SCRIPT}"
exec "${DEPLOY_SCRIPT}" "${RELEASE_ID}"
