#!/usr/bin/env bash
set -euo pipefail

curl -fsS --max-time 2 "http://127.0.0.1:8080/actuator/health" >/dev/null
systemctl is-active --quiet bizkit-backend.service
echo "[codedeploy][be] validate OK"
