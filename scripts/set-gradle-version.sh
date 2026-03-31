#!/usr/bin/env bash
set -euo pipefail

VER="${1:-}"
[[ -n "$VER" ]] || { echo "missing version"; exit 2; }

VER="${VER#v}"

FILE="gradle.properties"
[[ -f "$FILE" ]] || { echo "gradle.properties not found"; exit 3; }

if grep -qE '^version=' "$FILE"; then
  sed -i "s/^version=.*/version=${VER}/" "$FILE"
else
  echo "version=${VER}" >> "$FILE"
fi

echo "[set-gradle-version] $FILE => version=${VER}"
