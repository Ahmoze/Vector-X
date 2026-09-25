# 🚀 Vector-X v2.0.27 (Android 17 & HyperOS 4 Full Support Edition)

This release delivers complete, rock-solid stability and compatibility for **Android 17** (Xiaomi HyperOS 4 / SDK 37), solves the ART dynamic linker absolute path restriction, eliminates OEM `magiskpolicy` crashes, introduces dual AIDL interface bridging, and enhances self-healing daemon management.

---

### 🌟 What's New & Fixed:

* **📱 Android 17 (SDK 37) & Xiaomi HyperOS 4 Full Support:**
  * **ART Linker Absolute Path Fix**: Android 17 ART linker strictly prohibits relative classpath paths (`./daemon.apk`), which previously caused an immediate boot-time fatal abort (`java.lang.UnsatisfiedLinkError: Expecting an absolute path of the library: ./daemon.apk!/lib/arm64-v8a/libdaemon.so`). The `daemon` startup script now dynamically resolves the full canonical absolute path (`/data/adb/modules/zygisk_vector/daemon.apk`).
  * **OEM `magiskpolicy` Crash Elimination**: Xiaomi HyperOS 4 ships with a broken OEM `/product/bin/magiskpolicy` stub that aborted with SIGABRT (`FORTIFY: fread: null FILE*`). Implemented a multi-tier fallback mechanism in `service.sh` and `action.sh` that prioritizes `/data/adb/magisk/magiskpolicy`, `/data/adb/ksu/bin/magiskpolicy`, and `/data/adb/ap/bin/magiskpolicy`, cleanly bypassing the faulty OEM binary.
  * **Dual AIDL `IServiceConnection` Interface**: Implemented both 3-parameter and 4-parameter (`session` token) `IServiceConnection.connected` method signatures in `ManagerService` and hidden API stubs, preventing `NoSuchMethodError` / `AbstractMethodError` across Android 8.1~16 and Android 17.
  * **Xiaomi XSpace Workaround Guard**: Scoped the legacy MIUI `SecurityAddActivity` dual-apps workaround to `Build.VERSION.SDK_INT < 36`, eliminating unhandled activity lookup exceptions on HyperOS 3 and HyperOS 4.

* **⚡ IPC Bridge & System Server Synchronization:**
  * Fixed Zygisk bridge injection failure and resulting `SecurityException: Binder invocation to an incorrect interface` by ensuring the hardware proxy (`serial`) service is registered by the daemon prior to `system_server` specialization.
  * Added resilient fallback paths for framework DEX preload (`framework/lspd.dex` and `/data/adb/modules/zygisk_vector/framework/lspd.dex`).

* **🛠️ Magisk / KernelSU / APatch Action Button Enhancements:**
  * Upgraded `action.sh` with a 5-second polling loop to reliably start and verify daemon state on demand.
  * Added automatic reset of rescue mode and bootloop counter flags upon user trigger.

---

### 📦 Downloads:

* **`Vector-v2.0.27-Release.zip`**: Standard production release build (Optimized & Minified).
* **`Vector-v2.0.27-Debug.zip`**: Troubleshooting build with full debug symbols and verbose logging.
