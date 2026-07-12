🚀 **What's New in v2.0.19** 🚀

Description: 🚀 New Feature Highlight: Quick Module Uninstaller Tired of digging deep into your phone's Android Settings to remove modules that don't have a launcher icon? Vector-X v2.0.19 introduces a convenient, built-in "Uninstall" button right inside the Manager! Located directly in the top right corner of every module's configuration screen, you can now completely remove modules from your device in seconds. When you click the sleek Material Design trash can icon, Vector-X will gracefully prompt you with our signature blur dialog and instantly trigger the native Android uninstaller.

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
