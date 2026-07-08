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
