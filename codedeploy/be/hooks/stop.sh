#!/usr/bin/env bash
set -euo pipefail

systemctl stop bizkit-backend.service || true
