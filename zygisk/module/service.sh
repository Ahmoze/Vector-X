# Extract the directory path and change directory 
MODDIR="${0%/*}"
cd "$MODDIR" || exit 1

# Ensure /data/adb is searchable for system user (UID 1000)
chmod 711 /data/adb

# Ensure SEPolicy rules are immediately applied live
if command -v magiskpolicy >/dev/null 2>&1; then
    magiskpolicy --live --apply "$MODDIR/sepolicy.rule" >/dev/null 2>&1
elif command -v supolicy >/dev/null 2>&1; then
    supolicy --live --apply "$MODDIR/sepolicy.rule" >/dev/null 2>&1
fi

# Start the daemon directly in the background within a private mount namespace
if [ -x "/data/adb/magisk/busybox" ]; then
    /data/adb/magisk/busybox unshare --propagation slave -m "$MODDIR/daemon" --system-server-max-retry=3 "$@" &
elif [ -x "/data/adb/ksu/bin/busybox" ]; then
    /data/adb/ksu/bin/busybox unshare --propagation slave -m "$MODDIR/daemon" --system-server-max-retry=3 "$@" &
elif [ -x "/data/adb/ap/bin/busybox" ]; then
    /data/adb/ap/bin/busybox unshare --propagation slave -m "$MODDIR/daemon" --system-server-max-retry=3 "$@" &
elif command -v busybox >/dev/null 2>&1; then
    busybox unshare --propagation slave -m "$MODDIR/daemon" --system-server-max-retry=3 "$@" &
else
    unshare -m "$MODDIR/daemon" --system-server-max-retry=3 "$@" &
fi
