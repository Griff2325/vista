#!/system/bin/sh
# Runtime dependencies are the installed TV API and our own DEX; no vendor APK is copied.
set -eu
ROOT=/data/local/tmp
PIDFILE=$ROOT/vista-picture.pid
if [ -f "$PIDFILE" ]; then
  read -r previous < "$PIDFILE"
  case "$previous" in ""|*[!0-9]*) echo 'Invalid picture PID' >&2; exit 1;; esac
  if kill -0 "$previous" 2>/dev/null && tr '\000' ' ' < "/proc/$previous/cmdline" | grep -Fq com.griffin.solhome.PictureBridge; then exit 0; fi
fi
api=$(pm path com.panasonic.dtv.calman)
api=${api#package:}
case "$api" in /system_ext/priv-app/*/*.apk) ;; *) echo 'Unsupported installed picture API' >&2; exit 1;; esac
umask 077
printf '%s\n' "$$" > "$PIDFILE"
export CLASSPATH="$ROOT/vista-picture.jar:$api"
exec app_process /system/bin com.griffin.solhome.PictureBridge
