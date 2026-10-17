#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$ROOT/build/bridge"
"${CC:-clang}" -target armv7a-linux-androideabi -c "$ROOT/bridge/home-bridge.s" \
  -o "$ROOT/build/bridge/sol-home-bridge.o"
python3 "$ROOT/bridge/pack-arm.py" "$ROOT/build/bridge/sol-home-bridge.o"
chmod +x "$ROOT/build/bridge/sol-home-bridge"
printf 'Built %s\n' "$ROOT/build/bridge/sol-home-bridge"
