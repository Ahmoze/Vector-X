# Extract the directory path and change directory 
MODDIR="${0%/*}"
cd "$MODDIR" || exit 1

# Ensure /data/adb is searchable for system user (UID 1000)
chmod 711 /data/adb

if [ -x "/data/adb/magisk/magiskpolicy" ]; then
    /data/adb/magisk/magiskpolicy --live --apply "$MODDIR/sepolicy.rule" >/dev/null 2>&1
elif [ -x "/data/adb/ksu/bin/magiskpolicy" ]; then
    /data/adb/ksu/bin/magiskpolicy --live --apply "$MODDIR/sepolicy.rule" >/dev/null 2>&1
elif [ -x "/data/adb/ap/bin/magiskpolicy" ]; then
    /data/adb/ap/bin/magiskpolicy --live --apply "$MODDIR/sepolicy.rule" >/dev/null 2>&1
elif command -v magiskpolicy >/dev/null 2>&1 && [ "$(command -v magiskpolicy)" != "/product/bin/magiskpolicy" ]; then
    magiskpolicy --live --apply "$MODDIR/sepolicy.rule" >/dev/null 2>&1
elif command -v supolicy >/dev/null 2>&1; then
    supolicy --live --apply "$MODDIR/sepolicy.rule" >/dev/null 2>&1
fi

# Start the daemon directly in the background within a private mount namespace
if [ -x "/data/adb/magisk/busybox" ]; then
    nohup /data/adb/magisk/busybox unshare --propagation slave -m "$MODDIR/daemon" --system-server-max-retry=3 "$@" </dev/null >/dev/null 2>&1 &
elif [ -x "/data/adb/ksu/bin/busybox" ]; then
    nohup /data/adb/ksu/bin/busybox unshare --propagation slave -m "$MODDIR/daemon" --system-server-max-retry=3 "$@" </dev/null >/dev/null 2>&1 &
elif [ -x "/data/adb/ap/bin/busybox" ]; then
    nohup /data/adb/ap/bin/busybox unshare --propagation slave -m "$MODDIR/daemon" --system-server-max-retry=3 "$@" </dev/null >/dev/null 2>&1 &
elif command -v busybox >/dev/null 2>&1; then
    nohup busybox unshare --propagation slave -m "$MODDIR/daemon" --system-server-max-retry=3 "$@" </dev/null >/dev/null 2>&1 &
else
    nohup unshare -m "$MODDIR/daemon" --system-server-max-retry=3 "$@" </dev/null >/dev/null 2>&1 &
fi
