🚀 **What's New in v2.0.26** 🚀

## 🚀 Vector-X v2.0.26 (Dex Optimizer, HyperOS 3 & ART Stability Edition)

This release delivers critical stability fixes for modern Android versions (Android 14 ~ 16 / Xiaomi HyperOS 3), resolves Dex Optimizer wrapper diagnostics, and ensures seamless lifecycle operation for both standalone and injected environments.

---

### 🌟 What's New & Fixed:

* **🛡️ Android 14+ / 16 ART Security & Boot Crash Fixes:**
  * Fixed a critical boot-time crash (`SIGABRT`) triggered by ART's internal `DexFile_setTrusted` on non-debuggable processes. Added proper API level guards (< API 34) and immediate JNI exception clears to prevent `AssertNoPendingException` aborts.
  * Resolved `NoSuchFieldError` on Android 16 (where `ApplicationInfo.overlayPaths` was deprecated/removed) by safeguarding all hidden `ApplicationInfo` field accesses across JNI and Java reflection bridges.

* **📱 HyperOS 3 / Android 16 Manager Launch Fix:**
  * Resolved a startup crash (`NullPointerException: Attempt to invoke virtual method 'int ApplicationInfo.getAdaptCutoutMode()'`) when launching Vector-X Manager on Xiaomi HyperOS 3.
  * Differentiated standalone/user-installed Manager initialization from parasitic host mode, preserving genuine system `ApplicationInfo` metadata while cleanly delivering the daemon service IPC binder.

* **⚡ Dex Optimizer Wrapper & APEX Bind-Mount Reliability:**
  * Aligned `Dex2OatServer` compatibility constants with `ILSPManagerService.aidl`, eliminating incorrect reporting of SEPolicy and mount errors (Dex Optimizer now properly reports **Podržano / Supported**).
  * Native dex2oat mount engine dynamically resolves absolute module paths, preventing working directory path resolution failures.
  * Added resilient fallback paths for `resetprop` (`/data/adb/magisk/resetprop`, `/data/adb/ksu/bin/resetprop`, `/data/adb/ap/bin/resetprop`) and ensured proper child process lifecycle management.
  * Eliminated legacy false-positive SEPolicy unmount triggers on Android 14~16.

* **🔄 Smart Build-Aware Update Detection:**
  * Enhanced `UpdateUtil` to detect new releases using both semantic versioning and build numbers in parentheses (e.g. `(3150)`).
  * Seamlessly triggers update prompts across both Vector-X Manager and Magisk.

---

### 📦 Downloads:
* **`Vector-v2.0.26-Release.zip`**: Standard production release build (Optimized & Minified).
* **`Vector-v2.0.26-Debug.zip`**: Troubleshooting build with full debug symbols and verbose logging.


---

🚀 **What's New in v2.0.26** 🚀

## 🚀 Vector-X v2.0.26 (Dex Optimizer, HyperOS 3 & ART Stability Edition)

This release delivers critical stability fixes for modern Android versions (Android 14 ~ 16 / Xiaomi HyperOS 3), resolves Dex Optimizer wrapper diagnostics, and ensures seamless lifecycle operation for both standalone and injected environments.

---

### 🌟 What's New & Fixed:

* **🛡️ Android 14+ / 16 ART Security & Boot Crash Fixes:**
  * Fixed a critical boot-time crash (`SIGABRT`) triggered by ART's internal `DexFile_setTrusted` on non-debuggable processes. Added proper API level guards (< API 34) and immediate JNI exception clears to prevent `AssertNoPendingException` aborts.
  * Resolved `NoSuchFieldError` on Android 16 (where `ApplicationInfo.overlayPaths` was deprecated/removed) by safeguarding all hidden `ApplicationInfo` field accesses across JNI and Java reflection bridges.

* **📱 HyperOS 3 / Android 16 Manager Launch Fix:**
  * Resolved a startup crash (`NullPointerException: Attempt to invoke virtual method 'int ApplicationInfo.getAdaptCutoutMode()'`) when launching Vector-X Manager on Xiaomi HyperOS 3.
  * Differentiated standalone/user-installed Manager initialization from parasitic host mode, preserving genuine system `ApplicationInfo` metadata while cleanly delivering the daemon service IPC binder.

* **⚡ Dex Optimizer Wrapper & APEX Bind-Mount Reliability:**
  * Aligned `Dex2OatServer` compatibility constants with `ILSPManagerService.aidl`, eliminating incorrect reporting of SEPolicy and mount errors (Dex Optimizer now properly reports **Podržano / Supported**).
  * Native dex2oat mount engine dynamically resolves absolute module paths, preventing working directory path resolution failures.
  * Added resilient fallback paths for `resetprop` (`/data/adb/magisk/resetprop`, `/data/adb/ksu/bin/resetprop`, `/data/adb/ap/bin/resetprop`) and ensured proper child process lifecycle management.
  * Eliminated legacy false-positive SEPolicy unmount triggers on Android 14~16.

* **🔄 Smart Build-Aware Update Detection:**
  * Enhanced `UpdateUtil` to detect new releases using both semantic versioning and build numbers in parentheses (e.g. `(3150)`).
  * Seamlessly triggers update prompts across both Vector-X Manager and Magisk.

---

### 📦 Downloads:
* **`Vector-v2.0.26-Release.zip`**: Standard production release build (Optimized & Minified).
* **`Vector-v2.0.26-Debug.zip`**: Troubleshooting build with full debug symbols and verbose logging.
* **`manager.apk`**: Standalone Vector-X Manager application.


---

🚀 **What's New in v2.0.26** 🚀

## 🚀 Vector-X v2.0.26 (Dex Optimizer & Smart Update Engine)

This release resolves the Dex Optimizer wrapper mount diagnostics, aligns internal daemon AIDL compatibility codes, and introduces enhanced build-aware update detection across Vector-X Manager and Magisk.

---

### 🌟 What's New & Fixed:

* **⚡ Dex Optimizer Wrapper & APEX Bind-Mount Reliability:**
  * Aligned `Dex2OatServer` compatibility constants with `ILSPManagerService.aidl`, eliminating incorrect reporting of SEPolicy and mount errors.
  * Native dex2oat mount engine now resolves absolute module paths dynamically, preventing cwd path resolution failures.
  * Added resilient fallback paths for `resetprop` (`/data/adb/magisk/resetprop`, `/data/adb/ksu/bin/resetprop`, `/data/adb/ap/bin/resetprop`) and ensured proper child process lifecycle management.
  * Eliminated legacy false-positive SEPolicy unmount triggers on Android 14~16.

* **🔄 Smart Build-Aware Update Detection:**
  * Enhanced `UpdateUtil` to detect new releases using both semantic versioning and build numbers in parentheses (e.g. `(3148)`).
  * Future revisions and hotfix builds will now seamlessly trigger the update prompt in both Vector-X Manager and Magisk.

---

### 📦 Downloads:
* **`Vector-v2.0.26-Release.zip`**: Standard production release build (Optimized & Minified).
* **`Vector-v2.0.26-Debug.zip`**: Troubleshooting build with full debug symbols and verbose logging.

---

🚀 **What's New in v2.0.25** 🚀

## 🚀 Vector-X v2.0.25 (Android 16/17 Full Support)

This release brings complete, rock-solid stability and compatibility for **Android 16** (HyperOS 3, API 36) and expands compatibility through **Android 17**, alongside powerful new Magisk / KernelSU recovery and self-healing tools.

---

### 🌟 What's New & Fixed:

* **🛡️ Android 16 ART Crash & Bootloop Prevention:**
  * Fixed critical `SecurityException: Can't exempt class, process is not debuggable.` thrown by Android 16 ART on release builds when invoking `HiddenApiBypass`.
  * Added robust JNI exception checks and clears (`env->ExceptionCheck()` / `env->ExceptionClear()`) in `context.h` and `module.cpp`, permanently preventing unhandled Java exceptions from aborting Zygote with `SIGABRT`.

* **⚡ Interactive Magisk / KernelSU / APatch Action Tool:**
  * **Instant Launch:** Tap the *Action* button inside Magisk/KernelSU/APatch modules list to instantly launch Vector Manager.
  * **Self-Healing & SEPolicy Auto-Repair:** Automatically applies live SEPolicy rules (`magiskpolicy --live --apply`) directly to the running system.
  * **Smart Auto-Update:** Detects if Manager is uninstalled or outdated and automatically extracts, installs, or updates `manager.apk` with multi-user (`--user 0`) compatibility.
  * **Health-Check & Failsafe Reset:** Clears Rescue Mode flags and resets bootloop counters on demand while displaying live daemon status (`lspd`) in an interactive colored terminal.

* **⚙️ SEPolicy & Bootloop Recovery Stabilization:**
  * System-wide boot complete listener (`sys.boot_completed == 1`) now reliably resets the bootloop counter and clears rescue mode flags once Android finishes booting.
  * Injects live SEPolicy rules during `service.sh` initialization to guarantee immediate `dex2oat` execution permissions across all vendor ROMs.

* **📚 Documentation & Compatibility Polish:**
  * Updated documentation across `README.md` and `module.prop` for full Android 8.1 ~ 17 support, APatch integration, and unified naming.

---

### 📦 Downloads:
* **`Vector-v2.0.25-Release.zip`**: Standard production release build (Optimized & Minified).
* **`Vector-v2.0.25-Debug.zip`**: Troubleshooting build with full debug symbols and verbose logging.


---

🚀 **What's New in v2.0.24** 🚀

🚀 **Highlight: Universal Safe Late Injection for System Server (Magisk Zygisk & Zygisk Next)**

Vector-X `v2.0.24` introduces an architectural breakthrough: **Universal Safe Late Injection for `system_server`**. On modern Android 14, 15, and 16 (HyperOS 2 / HyperOS 3), early hooking of `ZygoteInit.handleSystemServerProcess` and `SystemServer.startBootstrapServices` under official Magisk Zygisk caused severe ART JIT code cache corruption (`SIGILL / ILL_ILLOPC`) and assertion aborts (`Expected invalid entry`) while parsing XMLs during boot.

In `v2.0.24`, `system_server` boots 100% naturally with zero invasive hooks during early initialization. Vector-X asynchronously detects when `ActivityManagerService` is published, seamlessly binding its ClassLoader and dispatching `onSystemServerLoaded` events to all modern and legacy Xposed modules—matching the exact rock-solid behavior previously only possible on Zygisk Next!

### 🛠️ Key Fixes in v2.0.24:
* **[Architectural Breakthrough] Asynchronous Safe Late Initialization:** Replaced invasive early `handleSystemServerProcess` and `startBootstrapServices` hooks in `system_server` with an asynchronous listener for the system `activity` service. `system_server` completes early bootstrap and XML parsing unhindered without any JIT code cache interference or SIGILL faults.
* **[Guarded App Interceptors] Application-Only Hook Scope:** Strictly scoped `LoadedApk` constructors, `createAppFactory`, `createOrUpdateClassLoaderLocked`, and `ActivityThread.attach` hooks to application processes (`!isSystem`), eliminating unintended hook overhead inside `system_server`.
* **[Universal Parity] Flawless Operation on Both Loaders:** Vector-X now behaves identically whether running on stock Magisk Zygisk or standalone Zygisk Next / NeoZygisk, providing instantaneous 5-second boots without risk of bootloops.


---

🚀 **What's New in v2.0.24** 🚀

🚀 **Highlight: Universal Safe Late Injection for System Server (Magisk Zygisk & Zygisk Next)**

Vector-X `v2.0.24` introduces an architectural breakthrough: **Universal Safe Late Injection for `system_server`**. On modern Android 14, 15, and 16 (HyperOS 2 / HyperOS 3), early hooking of `ZygoteInit.handleSystemServerProcess` and `SystemServer.startBootstrapServices` under official Magisk Zygisk caused severe ART JIT code cache corruption (`SIGILL / ILL_ILLOPC`) and assertion aborts (`Expected invalid entry`) while parsing XMLs during boot.

In `v2.0.24`, `system_server` boots 100% naturally with zero invasive hooks during early initialization. Vector-X asynchronously detects when `ActivityManagerService` is published, seamlessly binding its ClassLoader and dispatching `onSystemServerLoaded` events to all modern and legacy Xposed modules—matching the exact rock-solid behavior previously only possible on Zygisk Next!

### 🛠️ Key Fixes in v2.0.24:
* **[Architectural Breakthrough] Asynchronous Safe Late Initialization:** Replaced invasive early `handleSystemServerProcess` and `startBootstrapServices` hooks in `system_server` with an asynchronous listener for the system `activity` service. `system_server` completes early bootstrap and XML parsing unhindered without any JIT code cache interference or SIGILL faults.
* **[Guarded App Interceptors] Application-Only Hook Scope:** Strictly scoped `LoadedApk` constructors, `createAppFactory`, `createOrUpdateClassLoaderLocked`, and `ActivityThread.attach` hooks to application processes (`!isSystem`), eliminating unintended hook overhead inside `system_server`.
* **[Universal Parity] Flawless Operation on Both Loaders:** Vector-X now behaves identically whether running on stock Magisk Zygisk or standalone Zygisk Next / NeoZygisk, providing instantaneous 5-second boots without risk of bootloops.

---

🚀 **What's New in v2.0.22** 🚀


Vector-X `v2.0.22` resolves the longstanding bootloop conflict between Vector-X and **official Magisk Zygisk** on Android 14, 15, and 16 (HyperOS 2 / HyperOS 3), enabling 100% stable booting without requiring third-party Zygisk Next or NeoZygisk loaders!

### 🛠️ Key Fixes in v2.0.22:
* **[Critical Fix] Eliminated JIT Debug State Corruption (`JavaDebuggableGuard`):** Removed `JavaDebuggableGuard` calls that were inadvertently switching the ART runtime state to debuggable inside `system_server` during early Magisk injection. On Android 14+, changing debug state without pre-fork initialization triggered ART JIT `AddNativeDebugInfoForJit` / `InsertNewEntry` assertions (`entry->seqlock_ & 1 == 1u Expected invalid entry`).
* **[Hook Engine] Prioritized Direct Interpreter Bridge Resolution:** LSPlant now prioritizes direct `art_quick_to_interpreter_bridge` symbol resolution from `libart.so`, eliminating the problematic `GetOptimizedCodeFor` fallback and preventing debug state mutation.
* **[System Server Optimization] Skipped Redundant Boot Image Deopt in system_server:** `VectorStartup.init` now strictly guards `VectorDeopter.deoptBootMethods()`, executing application-level method deoptimizations only in application processes and leaving `system_server` boot methods untouched.
* **[Type Alignment] 32-bit `RuntimeDebugState` Enum Alignment:** Updated `RuntimeDebugState` in LSPlant runtime header to `uint32_t` to match ART's internal representation.


---

🚀 **What's New in v2.0.22** 🚀

🚀 **Highlight: Official Magisk Zygisk & Android 16 Boot Stability Fix**

Vector-X `v2.0.22` resolves the longstanding bootloop conflict between Vector-X and **official Magisk Zygisk** on Android 14, 15, and 16 (HyperOS 2 / HyperOS 3), enabling 100% stable booting without requiring third-party Zygisk Next or NeoZygisk loaders!

### 🛠️ Key Fixes in v2.0.22:
* **[Critical Fix] Eliminated JIT Debug State Corruption (`JavaDebuggableGuard`):** Removed `JavaDebuggableGuard` calls that were inadvertently switching the ART runtime state to debuggable inside `system_server` during early Magisk injection. On Android 14+, changing debug state without pre-fork initialization triggered ART JIT `AddNativeDebugInfoForJit` / `InsertNewEntry` assertions (`entry->seqlock_ & 1 == 1u Expected invalid entry`).
* **[Hook Engine] Prioritized Direct Interpreter Bridge Resolution:** LSPlant now prioritizes direct `art_quick_to_interpreter_bridge` symbol resolution from `libart.so`, eliminating the problematic `GetOptimizedCodeFor` fallback and preventing debug state mutation.
* **[System Server Optimization] Skipped Redundant Boot Image Deopt in system_server:** `VectorStartup.init` now strictly guards `VectorDeopter.deoptBootMethods()`, executing application-level method deoptimizations only in application processes and leaving `system_server` boot methods untouched.
* **[Type Alignment] 32-bit `RuntimeDebugState` Enum Alignment:** Updated `RuntimeDebugState` in LSPlant runtime header to `uint32_t` to match ART's internal representation.

---

🚀 **What's New in v2.0.21** 🚀

🚀 **Highlight: Android 16 ART Runtime Compatibility & Hook Engine Update**

Vector-X `v2.0.21` brings full native support for **Android 16 (Baklava)**, modern HyperOS 3, and resolves critical ART runtime crashes observed on high-performance flagship chipsets (MediaTek Dimensity 9300+ / Snapdragon 8 Gen 3/4).

In earlier Android 16 builds, ART introduced strict JIT debug assertions (`art::AddNativeDebugInfoForJit` / `Check failed: entry->seqlock_ Expected invalid entry`) triggered by register signature mismatches in JNI native registration. `v2.0.21` delivers a hardened, precision-aligned hooking engine that seamlessly operates across both legacy and the latest preview platforms.

---

### 🛠️ Changelog:
* **[Critical Fix] Android 16 ART Panic Resolved:** Corrected `ClassLinker::RegisterNative` and `ClassLinker::UnregisterNative` method signatures, fixing ARM64 argument register alignment (preventing invalid sequence lock assertions during JIT compilation).
* **[Runtime] Android 16 Generic JNI ID Support:** Added `EncodeGenericIdWithClass_` runtime symbol resolution for `art::jni_id_manager` on Android 16.
* **[Infrastructure] Mirrored Submodule Resilience:** Transferred external submodule dependencies (`libxposed/service` and `libxposed/api`) to dedicated resilient GitHub mirrors under Ahmoze, ensuring persistent CI/CD build integrity.
* **[Dual Distribution] Release & Debug Builds:** Providing both streamlined, high-performance `Vector-v2.0.21-Release.zip` and diagnostic `Vector-v2.0.21-Debug.zip` for developers.
* **[Under-the-Hood]** Bumped core updates and OTA definitions to `v2.0.21`.


---

🚀 **What's New in v2.0.21** 🚀

Description: 🚀 New Feature Highlight: Official Android 16 (API 36 / HyperOS 3) Compatibility!
Vector-X v2.0.21 brings native compatibility and system stability for Android 16 (Baklava / API 36) and HyperOS 3! Resolved the ART runtime debugger interface assertion crash (`AddNativeDebugInfoForJit` / `JitCodeCache::Commit`) by correcting native method hook signatures in LSPlant. Added support for Android 16's updated `JniIdManager::EncodeGenericIdWithClass`.

Changelog:
[Fix] Fixed ART crash on Android 16 (API 36) by correcting `ArtMethod::RegisterNative` and `UnregisterNative` hook signatures in LSPlant.
[Fix] Added support for Android 16/17 `EncodeGenericIdWithClass` in `JniIdManager`.
[Fix] Fixed `RepoFragment` auto-unboxing NullPointerException in `OnlineModule.java`.
[Enhancement] Full stability and hook execution confirmed on Android 16 (HyperOS 3 / Xiaomi 14T Pro).

---

🚀 **What's New in v2.0.20** 🚀

🚀 **Highlight: Seamless Daemon-Powered Module Uninstaller & Core Fixes**

Vector-X `v2.0.20` addresses critical system-level uninstallation behavior on modern Android operating systems (Android 11–16, MIUI, and HyperOS). 

In previous builds, triggering the uninstall action relied on standard Android OS package intents, which were frequently blocked or silently dropped by vendor security policies. In `v2.0.20`, module uninstallation now hooks directly into Vector's native root daemon IPC service (`ConfigManager.uninstallPackage`). When you confirm module removal, Vector silently and cleanly uninstalls the package with system-level privileges, updates the daemon database, and seamlessly routes you back to your module list!

---

### 🛠️ Changelog:
* **[Fix] Direct Daemon Uninstallation:** Replaced legacy Intent calls with Vector's native daemon service IPC for instant, silent package removal without system blocks.
* **[Fix] Silent Failure Resolved:** Fixed an issue on modern Android versions (Android 11-16 / MIUI / HyperOS) where confirming module deletion on the configuration screen would do nothing.
* **[Enhancement] Smooth Navigation Flow:** Upon successful uninstallation, the app automatically reloads module states and returns back to the main modules screen.
* **[Infrastructure] GitHub Pages & Repo Fix:** Resolved deployment timeout issues on the module repository by adding automatic `.nojekyll` configuration and a modern web landing page.
* **[Under-the-Hood]** Bumped core updates and OTA definitions to `v2.0.20`.

---

🚀 **What's New in v2.0.19** 🚀


Changelog:

[Feature] Added an inline "Uninstall module" action directly to the Module configuration screen for rapid removal.
[Enhancement] Fully localized the new uninstaller strings into 42 different languages natively within the app.
[Enhancement] Implemented a seamless double-confirmation flow utilizing Vector-X's signature BlurBehindDialogBuilder for a secure and premium user experience.
[Under-the-Hood] Vector-X automatically and gracefully cleans up the uninstalled module's scope and config from the daemon database upon successful system package removal.

---

🚀 **What's New in v2.0.18** 🚀

🚀 New Feature Highlight: Vector Rescue Mode (Bootloop Protection) Tired of a bad module crashing your system and causing a bootloop? Vector-X now features an intelligent Rescue Mode! During every system boot, the VectorDaemon independently tracks boot failures. If the Android system fails to boot successfully 3 times in a row, the Daemon will automatically enter Rescue Mode and strictly block all module injections. Your phone will boot up safely, allowing you to enter the Manager and disable the problematic module.

📊 New Feature Highlight: Live Logcat Studio Say goodbye to constantly swiping down to refresh logs! The Logs tab in the Manager has been completely revamped into a "Live Studio". Logs are now automatically fetched and refreshed in the background every 2.5 seconds. With smart auto-scrolling and enhanced Material 3 color coding for errors and warnings, debugging is smoother and more intuitive than ever.

Changelog:

[Feature] Introduced Vector Rescue Mode: A daemon-level failsafe that detects bootloops and prevents module injection after 3 consecutive failed boots.
[Feature] Introduced Live Logcat Studio: The Logs tab now features a battery-friendly background auto-refresh mechanism tied to the fragment's lifecycle.
[Enhancement] Added a red "Bootloop Rescue Mode" warning banner to the Home tab when the failsafe is active.
[Enhancement] Improved log parsing UI: Error logs (E) are now bolded and highlighted in intense red, while Warnings (W) are distinctly colored orange for better readability.
[Enhancement] Smart auto-scrolling in the Logs tab automatically snaps to the newest logs if you are reading at the bottom of the list.
[Under-the-Hood] Bumped GitHub Actions dependencies to resolve CI/CD conflicts and deprecated Node.js warnings.

---

🚀 **What's New in v2.0.17** 🚀

🚀 **New Feature Highlight: Smart Daemon Auto-Installer (Self-Healing)**
Tired of seeing "Not Installed" after a framework update? We've completely overhauled how the Vector-X Daemon handles the Manager application! 

The `VectorDaemon` (running with `system_server` privileges) now features an **autonomous Smart Auto-Installer**. Upon every system boot, the daemon verifies the installed Manager's `versionCode` against its own internal version. If the Manager is missing, outdated, or if the Magisk ZIP failed to install it properly (a common issue on Android 14+), the Daemon will **silently extract and install the correct Manager APK directly from the module** in the background. 

No more manual APK extractions, no more "Not Installed" errors due to AIDL mismatches, and a fully automated update experience!

### Changelog:
- **[Feature]** Introduced a robust, Daemon-level silent background APK installer for the Vector-X Manager.
- **[Enhancement]** The framework is now completely "Self-Healing": if `customize.sh` is blocked from installing the APK during a module flash, the Daemon will automatically fix it upon boot.
- **[Under-the-Hood]** Safely fetches and compares `longVersionCode` (with strict backward compatibility for older Android versions) to trigger isolated `pm install` executions via the native root shell.
- **[Under-the-Hood]** Refactored `ConfigCache.kt` state management to instantly refresh IPC scopes post-installation, activating the Manager immediately without requiring an additional reboot.


---

🚀 **What's New in v2.0.17** 🚀

🚀 **New Feature Highlight: Smart Daemon Auto-Installer (Self-Healing)**
Tired of seeing "Not Installed" after a framework update? We've completely overhauled how the Vector-X Daemon handles the Manager application! 

The `VectorDaemon` (running with `system_server` privileges) now features an **autonomous Smart Auto-Installer**. Upon every system boot, the daemon verifies the installed Manager's `versionCode` against its own internal version. If the Manager is missing, outdated, or if the Magisk ZIP failed to install it properly (a common issue on Android 14+), the Daemon will **silently extract and install the correct Manager APK directly from the module** in the background. 

No more manual APK extractions, no more "Not Installed" errors due to AIDL mismatches, and a fully automated update experience!

### Changelog:
- **[Feature]** Introduced a robust, Daemon-level silent background APK installer for the Vector-X Manager.
- **[Enhancement]** The framework is now completely "Self-Healing": if `customize.sh` is blocked from installing the APK during a module flash, the Daemon will automatically fix it upon boot.
- **[Under-the-Hood]** Safely fetches and compares `longVersionCode` (with strict backward compatibility for older Android versions) to trigger isolated `pm install` executions via the native root shell.
- **[Under-the-Hood]** Refactored `ConfigCache.kt` state management to instantly refresh IPC scopes post-installation, activating the Manager immediately without requiring an additional reboot.


---

🚀 **What's New in v2.0.16** 🚀

### ⚠️ Critical Hotfix (v2.0.16)
This release primarily addresses a fatal Zygisk crash introduced in the previous build that caused the framework to silently break and display as **"Not Installed"** in the Manager app. 

*   **[Critical Fix]** Resolved a native crash in `system_server` caused by an IPC transaction code mismatch when fetching the obfuscation map. Zygisk injection now initializes flawlessly.
*   **[Fix]** Fixed native build macros (`MANAGER_PACKAGE_NAME`) which broke IPC package resolution.
*   **[Fix]** Restored ABI backward compatibility in `Module.aidl` to prevent crashes for users using older Manager APKs.
*   **[Fix]** Fixed Gradle memory allocation for faster builds and added missing `InternalApi.java`.
*   **[CI/CD]** Added a GitHub Action to automatically sync Magisk OTA files upon creating a new release.

> **Update Note:** If you are updating from a previous version and your Manager app does not refresh automatically after reboot, please manually uninstall the old Vector-X Manager and install the new APK extracted from this ZIP file.

---

### 🔥 Hot Reload & ⚙️ API 102 Features
*(Included from the major API 102 update)*

Welcome to a major developer-focused update! This brings full compatibility with the highly anticipated libxposed API 102, introducing game-changing features for module creators:

*   🔥 **Module Hot Reload Support:** The wait is over. Modules targeting API 102 can now dynamically reload their injected code in real-time. No more forced reboots or app restarts after every minor code change—drastically speeding up your development and testing iteration cycle!
*   ⚙️ **libxposed API 102 Core Integration:** Vector's internal framework bridge has been completely upgraded to seamlessly support the new API 102 architecture.
*   ⚛️ **Atomic Hook Replacements:** Introducing the new `replaceHook` capability. Developers can now atomically swap out active hooks on the fly without worrying about race conditions or manual unhook/rehook procedures.
*   🆔 **Advanced Hook Identity Tracking:** Fully adopted the new `HookHandle` system. This brings robust hook identity tracking and the ability to safely transfer active hooks (`getOldHookHandles`) during a hot reload handoff, ensuring extreme stability when updating modules on the fly.

Dive into the new API and enjoy a significantly faster and more powerful module development workflow!


---

🚀 **What's New in v2.0.15** 🚀

🚀 Vector v2.0.15 Release

Welcome to a major developer-focused update! This release brings full compatibility with the highly anticipated libxposed API 102, introducing game-changing features for module creators like Hot Reloading and advanced hook management.

🌟 What's New

* 🔥 **Module Hot Reload Support**: The wait is over. Modules targeting API 102 can now dynamically reload their injected code in real-time. No more forced reboots or app restarts after every minor code change—drastically speeding up your development and testing iteration cycle!
* ⚙️ **libxposed API 102 Core Integration**: Vector's internal framework bridge has been completely upgraded to seamlessly support the new API 102 architecture.
* ⚛️ **Atomic Hook Replacements**: Introducing the new `replaceHook` capability. Developers can now atomically swap out active hooks on the fly without worrying about race conditions or manual unhook/rehook procedures.
* 🆔 **Advanced Hook Identity Tracking**: Fully adopted the new `HookHandle` system. This brings robust hook identity tracking and the ability to safely transfer active hooks (`getOldHookHandles`) during a hot reload handoff, ensuring extreme stability when updating modules on the fly.

Dive into the new API and enjoy a significantly faster and more powerful module development workflow!


---

🚀 **What's New in v2.0.14** 🚀

# 🚀 Vector v2.0.14 Release

Welcome to a highly refined update focusing on debugging quality-of-life improvements, intelligent navigation, and essential system cleanup. This release makes module development and troubleshooting a much more pleasant experience!

🌟 **What's New**

- 🎨 **Logs UI Overhaul:** Completely revamped the log viewer interface for vastly improved readability. Logs are now dynamically colorized using `Spannable` strings based on their severity level (Errors in Red, Warnings in Orange, and metadata timestamps in a subtle Gray). We also added vertical spacing to let the text "breathe", making forensic debugging easier than ever.
- 🧭 **Intelligent Release Redirects:** Enhanced the "Open in Browser" arrow button for repository modules. The app now parses GitHub URLs and attempts to dynamically route you directly to the module's specific `/releases/tag/` or `/releases` page, instead of blindly dumping you on the repository's root page. 
- 🗂️ **Proper Log Export Naming:** Replaced the legacy archive nomenclature. Exported ZIP files containing your logs will now correctly save as `Vector-X_YYYY-MM-DD.zip` instead of the old `LSPosed_` prefix.
- 🧹 **Defunct Feature Cleanup:** Completely stripped out the dead "Hide Launcher Icon" option from the settings, IPC, and AIDL interfaces. This legacy feature relied on older Android mechanisms that are no longer supported on modern OS versions, so it was removed to reduce clutter and prevent user confusion.

🔧 **Under the Hood**
- ⚙️ **Optimized Codebase:** Cleaned up unused AIDL endpoints (`setHiddenIcon`) and related bindings across the Manager and Daemon, making the codebase leaner and slightly more optimized.


---

🚀 **What's New in v2.0.12** 🚀

# 🚀 Vector v2.0.12 Release

Welcome to the most robust and highly polished release of Vector yet! This massive update not only transforms the user downloading experience and fixes theme visibility, but also brings deep stability improvements to our build system, IDE synchronization, and introduces new localizations.

### 🌟 What's New
* **Native Downloader Engine**: Completely stripped out the unreliable Chrome Custom Tabs downloader that caused freezing/ANR on HyperOS. Module assets (APKs, ZIPs) are now downloaded flawlessly in the background using the native Android Download Manager (downloads will directly save to your `Downloads/` folder with proper progress notifications).
* **UI Light Theme Fix**: Fixed a massive readability issue where text inside the Repository and Modules tabs turned invisible/light-grey when the system was set to Light Theme. Strict Material 3 (Monet) colors are now enforced across the board, guaranteeing sharp and highly readable text regardless of your theme preference.
* **Intelligent URL Fallback**: Fixed the "URL not available" error when attempting to open a repository in the browser. The manager now intelligently falls back to the module's source or homepage URL if a specific release link is missing.
* **Internationalization (i18n)**: Introduced full UI translation and localization for the **Serbian (Latin)** language in the Manager App, greatly expanding accessibility for regional users.

### 🛠️ Developer & Build Improvements
* **CMake & AGP Synchronization**: Fully resolved Native C++ build failures in continuous integration pipelines. CMake versions are now strictly enforced natively through the project structure.
* **IDE & Language Server Fixes**: Cleared all false-positive syntax errors and Eclipse JDT/VSCode "Unsupported class file major version 69" issues. The Gradle internal environment is now strictly pinned to JDK 21.
* **Apache Commons Architecture**: Refactored internal code generation paths (`ClassUtilsX` and `SerializationUtilsX`) to comply with strict Java package guidelines, eliminating missing class exceptions.

### ⚙️ Installation
1. Flash the provided `Vector-v2.0.12-Release.zip` module via **Magisk** or KernelSU.
2. Reboot your device.
3. The stealth manager will appear automatically. (If updating from a previous version, the old manager will be seamlessly replaced).


---

🚀 **What's New in v2.0.10** 🚀

🔹 **UI Bug Fix (HyperOS/MIUI)**: Fixed a critical freeze/ANR that occurred when opening the "Assets" dialog in the module repository on Xiaomi devices with HyperOS.
🔹 **Manager Self-Update System**: Fixed the automatic background installation of updates when downloaded via Magisk. The manager will now reliably override the old app without requiring a manual restart.
🔹 **Bug Fix**: Addressed an NPE crash when parsing repositories with missing release assets or empty URL configurations.
🔹 **UI Optimization**: Enhanced the Magisk flashing process interface with a cleaner aesthetic and more verbose steps.

---

🚀 **What's New in v2.0.8** 🚀

🔹 **Ultimate Stealth Architecture (Phase 4)**: The entire core framework and application ID have been renamed to `org.ahmoze.vector` and `Vector-X` respectively, providing 100% immunity against Anti-Cheat mechanisms that scan for the presence of the `org.lsposed.manager` package.
🔹 **Parasitic Manager Removal**: Since Vector-X is now natively stealthy via deep package renaming and Anonymous Remapping, the legacy Parasitic Manager UI (and its popups) have been completely removed for a cleaner, faster experience.
🔹 **General Fixes**: Codebase completely purged of legacy LSPosed strings.

---

🚀 **What's New in v2.0.7** 🚀

🔹 **Stealth Architecture (Phase 3)**: Introduced an advanced C++ memory hiding mechanism (Anonymous Remapping) to evade user-space Anti-Cheat systems.
🔹 **UI Optimization**: Fixed an ANR (Application Not Responding) issue that caused the Vector Manager to freeze when searching for remote modules on the Repository tab.
🔹 **Update System**: OTA updates are now routed through the official Vector-X repository for seamless future releases.
🔹 **Build System**: Cleaned up generated release packages for a better user experience (e.g., `Vector-v2.0.7-Release.zip`).

### 🛠️ Fixes & Improvements
* **Zygisk Stealth Architecture:** Dynamic Binder Randomization and Kotlin IPC Cleanup.
* General stability and UI polish for the update system.
