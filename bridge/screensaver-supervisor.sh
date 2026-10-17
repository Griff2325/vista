#!/system/bin/sh
# Fire OS resets its transient legacy-dream route after a dream ends. Re-arm it
# through the verified shell-authorized binder call; no per-frame or laptop work.
transaction="$1"
case "$transaction" in ''|*[!0-9]*) exit 2;; esac
pidfile=/data/local/tmp/sol-dream-supervisor.pid
echo $$ > "$pidfile"
trap 'rm -f "$pidfile"; exit 0' TERM INT
while true; do
    service call dreams "$transaction" i32 1 i32 1 \
        s16 com.griffin.solhome s16 com.griffin.solhome.SolDreamService >/dev/null 2>&1
    sleep 20 &
    wait $!
done
