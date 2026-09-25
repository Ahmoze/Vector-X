# Detaljna Analiza: Uzrok Kvara i Uspešna Popravka Vector-X na HyperOS 4 (Android 17)

## 1. Zašto Vector-X nije radio nakon ažuriranja na HyperOS 4?

Nakon detaljne analize tombstone logova, dmesg-a i logcat ispisa otkrivena su tri nezavisna problema specifična za Android 17 (SDK 37 / HyperOS 4.0.0.6 na Xiaomi 14T Pro / Warhol):

### Problem 1: ART Linker zabranjuje relativne putanje (Glavni krah daemona)
- **Simptom u logu**:
  ```text
  FATAL EXCEPTION: main
  java.lang.UnsatisfiedLinkError: Expecting an absolute path of the library: ./daemon.apk!/lib/arm64-v8a/libdaemon.so
      at java.lang.Runtime.load0(Runtime.java:935)
      at java.lang.System.load(System.java:1728)
      at org.ahmoze.vector.daemon.env.LogcatMonitor...
      at org.ahmoze.vector.daemon.VectorDaemon.main...
  ```
- **Uzrok**: Android 17 ART dinamički linker više ne dozvoljava relativne putanje (`./daemon.apk`) prosleđene kroz `-Djava.class.path=./daemon.apk`. Čim daemon pokuša da učita svoju biblioteku `libdaemon.so`, sistem baca fatalnu grešku i proces se momentalno gasi.
- **Posledica**: Daemon nije mogao da se pokrene pri boot-u uređaja.

---

### Problem 2: Krah fabričkog OEM `magiskpolicy` alata na HyperOS 4
- **Simptom u tombstone logu**:
  ```text
  FORTIFY: fread: null FILE*
  Fatal signal 6 (SIGABRT), code -1 in tid 2119 (magiskpolicy), pid 2119
  Executable: /product/bin/magiskpolicy
  Cmdline: magiskpolicy --live --apply /data/adb/modules/zygisk_vector/sepolicy.rule
  ```
- **Uzrok**: Na HyperOS 4, Xiaomi je ubacio oštećen fabrički `/product/bin/magiskpolicy` stub. Kada je `service.sh` izvršio proveru `command -v magiskpolicy`, sistem je pronašao i pokrenuo taj neispravni OEM fajl koji je odmah krahirao sa SIGABRT.
- **Posledica**: Vector SEPolicy pravila uopšte nisu bila primenjena.

---

### Problem 3: Gubitak sinhronizacije između Zygisk hook-a i `system_server`-a
- **Arhitektura Vector-X**:
  Kada telefon startuje, Zygisk modul u `system_server` procesu (PID 2419) čeka do 10 sekundi da daemon registruje IPC proxy servis pod imenom `serial`.
- **Šta se desilo pri boot-u**:
  Pošto je daemon krahirao zbog Problema 1, `system_server` je sačekao 10 sekundi, nije pronašao `serial`, i Zygisk modul je **odustao od injekcije** (`Failed to get system server IPC binder. Aborting injection.`).
- **Zašto "Action" dugme nije pomoglo bez restarta**:
  `system_server` je ostao da radi u ne-hookovanom stanju. Svaki kasniji pokušaj komunikacije slanjem `kBridgeTransactionCode` ka `activity` servisu završavao se sa:
  ```text
  java.lang.SecurityException: Binder invocation to an incorrect interface
      at android.app.IActivityManager$Stub.onTransact(IActivityManager.java:3514)
      at com.android.server.am.ActivityManagerService.onTransact(ActivityManagerService.java:3768)
  ```
  jer ne-hookovani `ActivityManagerService` odbija Vector Binder pakete.

---

## 2. Zašto se telefon zamrzao pre restarta?

Pre vašeg restarta, u pozadini je pokrenuta automatizovana instalacija popravljenog modula kroz:
`/data/adb/magisk/magisk --install-module /storage/emulated/0/Download/Vector-X-v2.0.26-HyperOS4-Fixed.zip`

Tokom instalacije, `customize.sh` skripta je na liniji 159 pozvala tihi `pm install -r -d manager.apk`.
Pošto je stara verzija aplikacije u tom trenutku bila delimično zakačena u memoriji i Android Package Manager Service (unutar `system_server`) je pokušavao da zaključa procese radi zamene APK-a dok je korisnički interfejs bio aktivan, došlo je do privremenog zastoja glavne UI niti (ANR / input dispatch timeout). Prisilni restart koji ste uradili bio je idealan korak jer je prekinuo blokirani proces i omogućio čisto podizanje sistema.

---

## 3. Zašto Vector-X SADA RADI?

Prilikom tog restarta, sistem se prvi put podigao sa **potpuno zakrpljenim modulom** koji smo pripremili i verifikovali:

1. **Apsolutna putanja rešena**: `daemon` skripta sada garantovano koristi punu apsolutnu putanju `/data/adb/modules/zygisk_vector/daemon.apk`, pa je `app_process` startovao bez ijedne ART linker greške.
2. **SEPolicy rešen**: `service.sh` i `action.sh` direktno koriste `/data/adb/magisk/magiskpolicy` umesto krahiranog fabričkog bita.
3. **Pravovremeni Zygisk Bootstrap**: Daemon se podigao odmah pri boot-u, registrovao `serial` proxy, `system_server` ga je pronašao u prvom pokušaju, učitao framework DEX i uspešno instalirao `HookBridge` na `Binder.execTransact`.
4. **Manager prepoznaje Vector-X**: Vector Manager je kroz hookovan Zygote dobio legitimni Binder i sada prikazuje potpunu funkcionalnost.

---

## 4. Gde se nalaze vaši popravljeni fajlovi?

1. **Gotov, validiran Magisk modul (sa ispravnim SHA256 sumama za sve fajlove)**:
   - `/storage/emulated/0/Download/Vector-X-v2.0.26-HyperOS4-Fixed.zip`
   - `/storage/emulated/0/Download/Vector-v2.0.26-Release.zip` (original je sačuvan kao `.orig_bak`)
2. **Lokalni git repozitorijum sa primenjenim izmenama koda**:
   - `/root/Vector-X/` (ažurirani `zygisk/module/daemon`, `zygisk/module/service.sh`, `zygisk/module/action.sh`, `zygisk/module/customize.sh`, AIDL definicije).
