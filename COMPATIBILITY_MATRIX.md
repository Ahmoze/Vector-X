# Vector-X: Matrica Kompatibilnosti Kroz Android Verzije i Root Rešenja

Ovaj dokument sadrži detaljnu tehničku analizu rada Vector-X (v2.0.26+) na različitim verzijama Android OS-a i root okruženjima nakon primenjenih zakrpa.

---

## 1. Pregled Primenjenih Izmena i Njihov Uticaj na Kompatibilnost

| Izmena | Šta je promenjeno? | Uticaj na stare verzije (Android 8.1 - 15) | Uticaj na nove verzije (Android 16 - 17) |
|---|---|---|---|
| **Apsolutna putanja u `daemon`** | `-Djava.class.path=$dir/daemon.apk` umesto `./daemon.apk` | **100% kompatibilno**. Sve starije verzije prihvataju apsolutne putanje. | **Kritično neophodno**. Android 17 ART linker odbija relativne putanje (`UnsatisfiedLinkError`). |
| **Izolacija `magiskpolicy`** | Direktna provera `/data/adb/magisk`, `ksu`, `ap` uz izbegavanje `/product/bin/magiskpolicy` | **100% kompatibilno**. Magisk i KSU na svim verzijama drže binarne fajlove u `/data/adb/`. | **Kritično neophodno za HyperOS 4**. Sprečava SIGABRT krah na Xiaomi fabričkom stubu. |
| **Dual AIDL `IServiceConnection`** | Implementirana oba `connected()` potpisa (3 parametra i 4 parametra sa `session`) | **100% kompatibilno**. Android 8.1–16 koristi 3-param potpis; ne ruši se. | **Kritično neophodno za Android 17**. Novi framework zahteva 4-param potpis. |
| **Xiaomi XSpace Workaround ograda** | `applyXspaceWorkaround` se izvršava samo za `Build.VERSION.SDK_INT < 36` | **100% kompatibilno**. Stariji MIUI sistemi i dalje dobijaju potreban fix. | **Sprečava krahove**. Na HyperOS 3/4 nema nepotrebnog traženja zastarelih MIUI aktivnosti. |
| **Fallback za `lspd.dex`** | Pretraga u `framework/lspd.dex` i `/data/adb/modules/zygisk_vector/framework/lspd.dex` | **100% kompatibilno**. | Osigurava da se framework DEX uvek učita bez obzira na trenutni radni direktorijum. |

---

## 2. Podrška po Android Verzijama

### Android 8.1 (Oreo MR1, API 27) & Android 9 (Pie, API 28)
- **Status**: **Podržano**
- **Detalji**:
  - `daemon` skripta sadrži posebne grane za ove verzije:
    - API 27: `-Xrunjdwp:transport=dt_android_adb,suspend=n,server=y -Xcompiler-option --debuggable`
    - API 28: `-XjdwpProvider:adbconnection -XjdwpOptions:suspend=n,server=y -Xcompiler-option --debuggable`
  - Apsolutna putanja za classpath funkcioniše besprekorno na Oreo/Pie bionic linkeru.
  - Zygisk bridge podržava 32-bitne i 64-bitne procese.

### Android 10 (Q, API 29) & Android 11 (R, API 30)
- **Status**: **Potpuno stabilno**
- **Detalji**:
  - Uveden `Dex2OatServer` i podrška za mount namepsace-ove (`unshare -m`).
  - `IServiceCallback` registracija za presretanje sistemskih servisa (API 30+) radi nativno.

### Android 12 & 12L (S / Sv2, API 31–32) & Android 13 (Tiramisu, API 33)
- **Status**: **Potpuno stabilno**
- **Detalji**:
  - LSPlant inline hook mehanizam je primarno optimizovan za ove verzije.
  - Skrivanje Zygisk modula i izolovani mount namespace-ovi rade bez detekcije.

### Android 14 (UpsideDownCake, API 34) & Android 15 (VanillaIceCream, API 35)
- **Status**: **Potpuno stabilno**
- **Detalji**:
  - Android 14+ je uveo stroža pravila za dinamičko učitavanje koda (`READ_ONLY` DEX fajlovi), što Vector rešava preko `PreloadedDex` i `memfd_create`.
  - SEPolicy pravila u `sepolicy.rule` pokrivaju sve domene za `dex2oat` i `isolated_app`.

### Android 16 (Baklava, API 36) & Android 17 (API 37 / HyperOS 4)
- **Status**: **Sada potpuno podržano nakon naših zakrpa**
- **Detalji**:
  - Uklonjen krah na ART linkeru (apsolutna putanja).
  - Ispravljen AIDL stub za 4-parametarski `IServiceConnection`.
  - Zaobiđen krahirajući fabrički `/product/bin/magiskpolicy`.

---

## 3. Kompatibilnost sa Različitim Root Rešenjima

1. **Magisk (Standardni i Magisk Alpha / Delta / Kitsune)**:
   - Modul ima potpunu podršku. Binarni fajl `/data/adb/magisk/magiskpolicy` se primarno koristi.
2. **KernelSU & KernelSU Next**:
   - Skripte `service.sh` i `action.sh` sadrže direktnu granu za `/data/adb/ksu/bin/magiskpolicy` i `/data/adb/ksu/bin/busybox`.
3. **APatch**:
   - Skripte sadrže detekciju za `/data/adb/ap/bin/magiskpolicy` i `/data/adb/ap/bin/busybox`.

---

## 4. Zaključak Analize

Primenjene zakrpe **ne sadrže nikakve 'hardkodovane' prečice** koje bi narušile rad na drugim verzijama. Sve izmene prate zvanične Android POSIX standarde i dodaju **progresivno prilagođavanje (graceful fallback)**:
- Ako je uređaj stariji (Android 8–15), sistem koristi postojeće mehanizme koji su uvek radili.
- Ako je uređaj noviji (Android 16–17 / HyperOS 4), sistem koristi nove, otpornije putanje i potpise interfejsa.

**Odgovor na pitanje**: Da, modul je sada univerzalan i bezbedan za rad na svim podržanim verzijama Androida.
