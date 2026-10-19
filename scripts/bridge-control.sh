#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [[ $# != 2 ]]; then echo "Usage: $0 ADB_SERIAL install|start|stop|status" >&2; exit 2; fi
SERIAL=$1
ACTION=$2
ADB=(adb -s "$SERIAL")
stop_bridge() {
  "${ADB[@]}" shell 'if [ -f /data/local/tmp/sol-bridge-supervisor.pid ]; then read -r bridgepid </data/local/tmp/sol-bridge-supervisor.pid; kill "$bridgepid" 2>/dev/null; fi; exit 0'
  for attempt in {1..30}; do
    if ! "${ADB[@]}" shell 'test -f /data/local/tmp/sol-bridge-supervisor.pid'; then return; fi
    sleep .1
  done
  echo 'Supervisor has not exited. Inspect before restarting.' >&2; exit 1
}
start_bridge() {
  "${ADB[@]}" shell 'nohup sh /data/local/tmp/sol-bridge-supervisor.sh >/data/local/tmp/sol-bridge.log 2>&1 </dev/null &'
}
case "$ACTION" in
  install)
    DEVICE=$("${ADB[@]}" shell getprop ro.product.device | tr -d '\r')
    BUILD=$("${ADB[@]}" shell getprop ro.build.fingerprint | tr -d '\r')
    if [[ "$DEVICE" != lassen || "$BUILD" != *RS8180.3742N* ]]; then
      echo 'This installer is limited to the tested lassen / RS8180.3742N firmware.' >&2; exit 1
    fi
    test -f "$ROOT/build/bridge/sol-home-bridge" || { echo 'Run scripts/build-bridge.sh first.' >&2; exit 1; }
    "${ADB[@]}" shell pm path com.griffin.solhome | grep -q '^package:' || { echo 'Install Vista first.' >&2; exit 1; }
    "${ADB[@]}" shell cmd package query-services --brief --components -n com.griffin.solhome/.PicturePanelService | grep -Fx 'com.griffin.solhome/.PicturePanelService' >/dev/null || { echo 'Install Vista 0.10.0 or newer before intercepting Settings.' >&2; exit 1; }
    "${ADB[@]}" shell appops get com.griffin.solhome SYSTEM_ALERT_WINDOW | grep 'SYSTEM_ALERT_WINDOW: allow' >/dev/null || { echo 'Enable display-over-apps access for Vista before intercepting Settings.' >&2; exit 1; }
    stop_bridge
    "${ADB[@]}" push "$ROOT/build/bridge/sol-home-bridge" /data/local/tmp/sol-home-bridge
    "${ADB[@]}" push "$ROOT/bridge/supervisor.sh" /data/local/tmp/sol-bridge-supervisor.sh
    "${ADB[@]}" shell chmod 755 /data/local/tmp/sol-home-bridge
    start_bridge
    ;;
  start) start_bridge ;;
  stop) stop_bridge ;;
  status) "${ADB[@]}" shell 'pidof sol-home-bridge; cat /data/local/tmp/sol-bridge.log' ;;
  *) echo "Unknown action: $ACTION" >&2; exit 2 ;;
esac
