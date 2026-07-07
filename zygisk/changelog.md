🚀 **What's New in v2.0.6** 🚀

This is a massive milestone! We have implemented the first part of our advanced Stealth Mode architecture and deployed a critical UI hotfix for the Manager app.

### 🛡️ Zygisk Stealth Architecture (Phase 3 - Part 1)
* **Dynamic Binder Randomization:** The native C++ Zygisk injection no longer uses statically hardcoded `_VEC`, `_DEX` or `_OBF` signatures to communicate with the Manager app. All Binder transaction codes are now dynamically generated via a compile-time mathematical hash based on the installation package name. This prevents anti-cheat memory scanners (PUBG, Genshin, Bank Apps) from easily flagging the standard signature.
* **Kotlin IPC Cleanup:** The Java/Kotlin side of the BridgeService has been refactored to unconditionally trust the highly secure C++ native hook, reducing overhead and eliminating hardcoded keys on the Dalvik side.

### 🐛 Bug Fixes & Stability
* **Repository Freeze Fix (ANR):** Resolved a critical UI lockup where clicking the "Repository" tab would completely freeze the application. This was caused by an aggressive layout animation engine attempting to concurrently render 800+ remote modules during the new Material 3 UI transition. The loading logic is now buttery smooth and stutter-free!
* **Update Checker Migration:** Fully migrated Magisk and App-level update checkers to the new `Ahmoze/Vector-X` master repository. You will now properly receive in-app update notifications for future releases.
