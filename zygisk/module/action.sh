#!/system/bin/sh
MODDIR=$(cd "${0%/*}" && pwd)
MANAGER_PACKAGE_NAME="@MANAGER_PACKAGE_NAME@"
INJECTED_PACKAGE_NAME="@INJECTED_PACKAGE_NAME@"

# ANSI Color codes for interactive Magisk/KSU terminal
C_RESET="\033[0m"
C_BOLD="\033[1m"
C_GREEN="\033[1;32m"
C_BLUE="\033[1;34m"
C_YELLOW="\033[1;33m"
C_RED="\033[1;31m"
C_CYAN="\033[1;36m"

echo ""
echo -e "${C_CYAN}${C_BOLD}=========================================${C_RESET}"
echo -e "${C_CYAN}${C_BOLD}        Vector-X Control & Recovery      ${C_RESET}"
echo -e "${C_CYAN}${C_BOLD}=========================================${C_RESET}"
echo ""

# 1. Self-Healing: Ensure SEPolicy rules are strictly applied
echo -e "${C_BLUE}[*] Checking SEPolicy rules...${C_RESET}"
if [ -f "$MODDIR/sepolicy.rule" ]; then
    if [ -x "/data/adb/magisk/magiskpolicy" ]; then
        /data/adb/magisk/magiskpolicy --live --apply "$MODDIR/sepolicy.rule" >/dev/null 2>&1
        echo -e "${C_GREEN}[+] SEPolicy rules applied via magiskpolicy.${C_RESET}"
    elif [ -x "/data/adb/ksu/bin/magiskpolicy" ]; then
        /data/adb/ksu/bin/magiskpolicy --live --apply "$MODDIR/sepolicy.rule" >/dev/null 2>&1
        echo -e "${C_GREEN}[+] SEPolicy rules applied via magiskpolicy.${C_RESET}"
    elif [ -x "/data/adb/ap/bin/magiskpolicy" ]; then
        /data/adb/ap/bin/magiskpolicy --live --apply "$MODDIR/sepolicy.rule" >/dev/null 2>&1
        echo -e "${C_GREEN}[+] SEPolicy rules applied via magiskpolicy.${C_RESET}"
    elif command -v supolicy >/dev/null 2>&1; then
        supolicy --live --apply "$MODDIR/sepolicy.rule" >/dev/null 2>&1
        echo -e "${C_GREEN}[+] SEPolicy rules applied via supolicy.${C_RESET}"
    fi
fi

# 2. Self-Healing: Reset Rescue Mode / Bootloop counter if present
if [ -f "/data/adb/lspd/rescue_mode_active" ] || [ -f "/data/adb/lspd/bootloop_counter" ] || [ -f "/data/adb/vector/rescue_mode_active" ] || [ -f "/data/adb/vector/bootloop_counter" ]; then
    echo -e "${C_YELLOW}[!] Rescue mode or bootloop counter detected. Clearing...${C_RESET}"
    rm -f /data/adb/lspd/rescue_mode_active /data/adb/lspd/bootloop_counter /data/adb/vector/rescue_mode_active /data/adb/vector/bootloop_counter 2>/dev/null
    echo -e "${C_GREEN}[+] Rescue mode reset.${C_RESET}"
fi

# 2.5 Self-Healing: Remove stale debug/tmp APKs that could hijack daemon or manager
if [ -f "/data/local/tmp/daemon.apk" ] || [ -f "/data/local/tmp/manager.apk" ]; then
    echo -e "${C_YELLOW}[!] Removing stale temporary APKs in /data/local/tmp...${C_RESET}"
    rm -f /data/local/tmp/daemon.apk /data/local/tmp/manager.apk 2>/dev/null
fi

# 3. Health-Check: Verify Daemon status
DAEMON_PID=$(pidof lspd 2>/dev/null || pidof daemon 2>/dev/null || pidof vector 2>/dev/null)
if [ -n "$DAEMON_PID" ]; then
    echo -e "${C_GREEN}[+] Vector Daemon: ACTIVE (PID: $DAEMON_PID)${C_RESET}"
else
    echo -e "${C_YELLOW}[!] Vector Daemon not running, launching service...${C_RESET}"
    if [ -f "$MODDIR/service.sh" ]; then
        sh "$MODDIR/service.sh" >/dev/null 2>&1 &
        for i in 1 2 3 4 5; do
            sleep 1
            NEW_PID=$(pidof lspd 2>/dev/null || pidof daemon 2>/dev/null || pidof vector 2>/dev/null)
            [ -n "$NEW_PID" ] && break
        done
        if [ -n "$NEW_PID" ]; then
            echo -e "${C_GREEN}[+] Vector Daemon started (PID: $NEW_PID).${C_RESET}"
        else
            echo -e "${C_RED}[-] Failed to start daemon automatically.${C_RESET}"
        fi
    fi
fi

# 4. Manager Check & Smart Installation / Repair
echo -e "${C_BLUE}[*] Checking Vector Manager...${C_RESET}"
INSTALLED_CODE=$(dumpsys package "$MANAGER_PACKAGE_NAME" 2>/dev/null | grep -m1 "versionCode=" | sed -E 's/.*versionCode=([0-9]+).*/\1/')
MODULE_CODE=$(grep "^versionCode=" "$MODDIR/module.prop" 2>/dev/null | cut -d'=' -f2 | tr -d ' \r\n')
LATEST_LOG=$(ls -t /data/adb/lspd/log/verbose_*.log 2>/dev/null | head -n 1)

NEED_INSTALL=false
if [ -z "$INSTALLED_CODE" ]; then
    echo -e "${C_YELLOW}[!] Manager is NOT installed.${C_RESET}"
    NEED_INSTALL=true
elif [ -n "$MODULE_CODE" ] && [ "$INSTALLED_CODE" -ne "$MODULE_CODE" ] 2>/dev/null; then
    echo -e "${C_YELLOW}[!] Manager version mismatch (installed: $INSTALLED_CODE, module: $MODULE_CODE). Repairing...${C_RESET}"
    NEED_INSTALL=true
elif [ -n "$LATEST_LOG" ] && grep -E "Manager verified and found|Manager verification failed" "$LATEST_LOG" 2>/dev/null | tail -n 1 | grep -q "Manager verification failed"; then
    echo -e "${C_YELLOW}[!] Manager signature verification failure detected. Repairing...${C_RESET}"
    NEED_INSTALL=true
else
    echo -e "${C_GREEN}[+] Manager is installed and verified (vCode: $INSTALLED_CODE).${C_RESET}"
fi

if [ "$NEED_INSTALL" = true ]; then
    echo -e "${C_BLUE}[*] Installing Manager APK...${C_RESET}"
    if ! pm install -r -d -g --user 0 "$MODDIR/manager.apk" >/dev/null 2>&1; then
        # Fallback: uninstall old conflicting package (e.g. signature mismatch) and reinstall clean
        echo -e "${C_YELLOW}[!] In-place update rejected; reinstalling Manager cleanly...${C_RESET}"
        pm uninstall --user 0 "$MANAGER_PACKAGE_NAME" >/dev/null 2>&1
        pm uninstall "$MANAGER_PACKAGE_NAME" >/dev/null 2>&1
        pm install -r -d -g --user 0 "$MODDIR/manager.apk" >/dev/null 2>&1
    fi
    
    NEW_INSTALLED_CODE=$(dumpsys package "$MANAGER_PACKAGE_NAME" 2>/dev/null | grep -m1 "versionCode=" | sed -E 's/.*versionCode=([0-9]+).*/\1/')
    if [ -n "$NEW_INSTALLED_CODE" ] && { [ -z "$MODULE_CODE" ] || [ "$NEW_INSTALLED_CODE" -eq "$MODULE_CODE" ] 2>/dev/null; }; then
        echo -e "${C_GREEN}[+] Manager repaired and installed successfully (vCode: $NEW_INSTALLED_CODE)!${C_RESET}"
    else
        echo -e "${C_RED}[-] Silent install failed. Staging to /sdcard/Download...${C_RESET}"
        cp "$MODDIR/manager.apk" /sdcard/Download/Vector-Manager.apk
        chmod 644 /sdcard/Download/Vector-Manager.apk
        am start -a android.intent.action.VIEW -d "file:///sdcard/Download/Vector-Manager.apk" -t "application/vnd.android.package-archive" >/dev/null 2>&1
        echo -e "${C_YELLOW}[*] Please complete installation via the system prompt.${C_RESET}"
        exit 0
    fi
fi

# 5. Launch Manager
echo -e "${C_GREEN}[+] Launching Vector Manager...${C_RESET}"
if pm list packages --user 0 2>/dev/null | grep -q "^package:${MANAGER_PACKAGE_NAME}$"; then
    am start --user 0 -n "${MANAGER_PACKAGE_NAME}/.ui.activity.MainActivity" >/dev/null 2>&1
    echo -e "${C_GREEN}[✔] Vector Manager opened!${C_RESET}"
else
    echo -e "${C_YELLOW}[!] Falling back to Parasitic Mode...${C_RESET}"
    am start --user 0 -c "${MANAGER_PACKAGE_NAME}.LAUNCH_MANAGER" "${INJECTED_PACKAGE_NAME}/.BugreportWarningActivity" >/dev/null 2>&1
fi

echo ""
echo -e "${C_CYAN}=========================================${C_RESET}"
sleep 1
