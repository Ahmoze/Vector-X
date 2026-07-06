MANAGER_PACKAGE_NAME="@MANAGER_PACKAGE_NAME@"
INJECTED_PACKAGE_NAME="@INJECTED_PACKAGE_NAME@"
MODDIR="${0%/*}"

# Proveri da li je APK vec instaliran
if ! pm list packages | grep -q "^package:${MANAGER_PACKAGE_NAME}$"; then
    pm install "$MODDIR/manager.apk" >/dev/null 2>&1
fi

# Pokusaj ponovo da vidis da li je instalacija bila uspesna
if pm list packages | grep -q "^package:${MANAGER_PACKAGE_NAME}$"; then
    # Ako je uspesno instaliran (ili je vec bio), pokreni ga direktno
    am start -n "${MANAGER_PACKAGE_NAME}/.ui.activity.MainActivity"
else
    # Ako nije uspelo (ili OS blokira), prebaci se na Parasitic Mode
    am start -c "${MANAGER_PACKAGE_NAME}.LAUNCH_MANAGER" "${INJECTED_PACKAGE_NAME}/.BugreportWarningActivity"
fi
