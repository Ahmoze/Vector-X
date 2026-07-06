MANAGER_PACKAGE_NAME="@MANAGER_PACKAGE_NAME@"
INJECTED_PACKAGE_NAME="@INJECTED_PACKAGE_NAME@"
MODDIR="${0%/*}"

# Proveri da li je APK vec instaliran
if ! pm list packages | grep -q "^package:${MANAGER_PACKAGE_NAME}$"; then
    # Pokusaj tihu instalaciju (cesto fail-uje na MIUI)
    pm install "$MODDIR/manager.apk" >/dev/null 2>&1
    
    # Ako tiha instalacija nije uspela, kopiraj APK u Downloads i otvori Package Installer UI
    if ! pm list packages | grep -q "^package:${MANAGER_PACKAGE_NAME}$"; then
        cp "$MODDIR/manager.apk" /sdcard/Download/Vector-Manager.apk
        chmod 644 /sdcard/Download/Vector-Manager.apk
        # Pokusaj da otvoris Androidov vizuelni instaler da korisnik sam klikne "Install"
        am start -a android.intent.action.VIEW -d "file:///sdcard/Download/Vector-Manager.apk" -t "application/vnd.android.package-archive"
        # Prekini skriptu ovde da bi korisnik mogao da zavrsi instalaciju bez otvaranja pokvarenog Parasitic menadzera
        exit 0
    fi
fi

# Pokusaj ponovo da vidis da li je instalacija bila uspesna
if pm list packages | grep -q "^package:${MANAGER_PACKAGE_NAME}$"; then
    # Ako je uspesno instaliran (ili je vec bio), pokreni ga direktno
    am start -n "${MANAGER_PACKAGE_NAME}/.ui.activity.MainActivity"
else
    # Ako nije uspelo nista od navedenog, prebaci se na Parasitic Mode
    am start -c "${MANAGER_PACKAGE_NAME}.LAUNCH_MANAGER" "${INJECTED_PACKAGE_NAME}/.BugreportWarningActivity"
fi
