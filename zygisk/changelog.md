🚀 **What's New in v2.0.8** 🚀

🔹 **Ultimate Stealth Architecture (Phase 4)**: The entire core framework and application ID have been renamed to `org.matrix.vector` and `Vector-X` respectively, providing 100% immunity against Anti-Cheat mechanisms that scan for the presence of the `org.lsposed.manager` package.
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
