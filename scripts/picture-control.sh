#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [[ $# != 2 ]]; then echo "Usage: $0 ADB_SERIAL install|start|stop|status" >&2; exit 2; fi
ADB=(adb -s "$1")
stop() {
  "${ADB[@]}" shell 'if [ -f /data/local/tmp/vista-picture.pid ]; then read -r picturepid </data/local/tmp/vista-picture.pid; case "$picturepid" in ""|*[!0-9]*) exit 1;; esac; if [ -r "/proc/$picturepid/cmdline" ] && tr "\000" " " < "/proc/$picturepid/cmdline" | grep -Fq com.griffin.solhome.PictureBridge; then kill "$picturepid"; fi; rm -f /data/local/tmp/vista-picture.pid; fi'
}
case "$2" in
  install)
    test -f "$ROOT/build/picture/vista-picture.jar"
    "${ADB[@]}" shell dumpsys package com.griffin.solhome | grep -Fq PictureBridgeProvider
    stop
    "${ADB[@]}" push "$ROOT/build/picture/vista-picture.jar" /data/local/tmp/vista-picture.jar
    "${ADB[@]}" push "$ROOT/bridge/picture/start.sh" /data/local/tmp/vista-picture-start.sh
    "${ADB[@]}" shell 'nohup sh /data/local/tmp/vista-picture-start.sh >/data/local/tmp/vista-picture.log 2>&1 </dev/null &'
    ;;
  start) "${ADB[@]}" shell 'nohup sh /data/local/tmp/vista-picture-start.sh >/data/local/tmp/vista-picture.log 2>&1 </dev/null &' ;;
  stop) stop ;;
  status) "${ADB[@]}" shell 'cat /data/local/tmp/vista-picture.pid /data/local/tmp/vista-picture.log' ;;
  *) echo 'Unknown action' >&2; exit 2 ;;
esac
