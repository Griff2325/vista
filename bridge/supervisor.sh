#!/system/bin/sh
ROOT=/data/local/tmp
PIDFILE=$ROOT/sol-bridge-supervisor.pid
if [ -f "$PIDFILE" ]; then
  read -r previous < "$PIDFILE"
  if kill -0 "$previous" 2>/dev/null; then exit 0; fi
fi
printf '%s\n' "$$" > "$PIDFILE"
child=
cleanup() {
  if [ -n "$child" ]; then kill "$child" 2>/dev/null; fi
  rm -f "$PIDFILE"
}
trap 'exit 0' TERM INT
trap cleanup EXIT
while true; do
  device=$(getevent -lp 2>/dev/null | while IFS= read -r line; do
    case "$line" in
      "add device "*": "*) current=${line##*: } ;;
      *name:*'"Amazon Fire TV Remote"'*) printf '%s\n' "$current"; break ;;
    esac
  done)
  if [ -n "$device" ]; then
    "$ROOT/sol-home-bridge" "$device" &
    child=$!
    wait "$child"
    child=
  fi
  sleep 2
done
