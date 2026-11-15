package org.ahmoze.vector.daemon.data

import android.content.pm.ApplicationInfo
import android.content.pm.PackageParser
import android.os.SystemProperties
import android.system.Os
import android.util.Log
import hidden.HiddenApiBridge
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ahmoze.vector.daemon.BuildConfig
import org.ahmoze.vector.daemon.VectorDaemon
import org.ahmoze.vector.daemon.ipc.InjectedModuleService
import org.ahmoze.vector.daemon.system.*
import org.ahmoze.vector.daemon.utils.InstallerVerifier
import org.ahmoze.vector.daemon.utils.applySqliteHelperWorkaround
import org.ahmoze.vector.daemon.utils.getRealUsers
import org.ahmoze.vector.lspd.models.Application
import org.ahmoze.vector.lspd.models.Module

private const val TAG = "VectorConfigCache"

object ConfigCache {
  // Module preference operations are delegated to PreferenceStore
  // Writable operations of modules are delegated to ModuleDatabase

  @Volatile
  var state = DaemonState()
    private set

  val dbHelper = Database() // Kept public for PreferenceStore and ModuleDatabase

  private val cacheUpdateChannel = Channel<Unit>(Channel.CONFLATED)

  private val isInstallingManager = AtomicBoolean(false)
  @Volatile var suppressManagerAutoInstall = false

  init {
    VectorDaemon.scope.launch {
      for (request in cacheUpdateChannel) {
        performCacheUpdate()
      }
    }
    applySqliteHelperWorkaround()
  }

  private fun ensureCacheReady() {
    if (!state.isCacheReady && packageManager?.asBinder()?.isBinderAlive == true) {
      synchronized(this) {
        if (!state.isCacheReady) {
          Log.i(TAG, "System services are ready. Mapping modules and scopes.")
          updateManager(false)
          setupMiscPath()
          performCacheUpdate()
          state = state.copy(isCacheReady = true)
        }
      }
    }
  }

  private fun tryVerifyInstalledManager(): Boolean {
    val info =
        packageManager?.getPackageInfoCompat(BuildConfig.DEFAULT_MANAGER_PACKAGE_NAME, 0, 0)
            ?: return false
    val uid = info.applicationInfo?.uid ?: return false
    val installedApkPath = info.applicationInfo?.sourceDir ?: return false

    @Suppress("DEPRECATION")
    val currentVersionCode =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
          info.longVersionCode
        } else {
          info.versionCode.toLong()
        }
    if (currentVersionCode < BuildConfig.VERSION_CODE) {
      return false
    }

    return runCatching {
          InstallerVerifier.verifyInstallerSignature(installedApkPath)
          state = state.copy(managerUid = uid)
          Log.i(TAG, "Manager verified and found at UID: $uid")
          true
        }
        .getOrElse { e ->
          Log.w(TAG, "Manager signature/verification check failed: ${e.message}")
          false
        }
  }

  fun updateManager(uninstalled: Boolean) {
    if (uninstalled) {
      state = state.copy(managerUid = -1)
      return
    }
    if (suppressManagerAutoInstall) {
      val uid =
          packageManager
              ?.getPackageInfoCompat(BuildConfig.DEFAULT_MANAGER_PACKAGE_NAME, 0, 0)
              ?.applicationInfo
              ?.uid
      if (uid != null) {
        Log.i(TAG, "Manager auto-install suppressed during module update (UID: $uid)")
        state = state.copy(managerUid = uid)
      }
      return
    }
    if (tryVerifyInstalledManager()) {
      return
    }
    state = state.copy(managerUid = -1)
    Log.i(
        TAG,
        "Manager is missing, outdated, or unverified (target: ${BuildConfig.VERSION_CODE}). Scheduling auto-install...")
    VectorDaemon.scope.launch { autoInstallManager() }
  }

  private fun resolveManagerApkFile(): File {
    val staged = File("/data/adb/modules_update/zygisk_vector/manager.apk")
    return if (staged.isFile && staged.length() > 0L) staged else FileSystem.managerApkPath.toFile()
  }

  private fun startRootInstallShell(script: String): Process {
    synchronized(VectorDaemon.rootUidLock) {
      Os.seteuid(0)
      try {
        val cmd =
            if (File("/system/bin/nsenter").canExecute() && File("/proc/1/ns/mnt").exists()) {
              arrayOf(
                  "/system/bin/nsenter", "-t", "1", "-m", "--", "/system/bin/sh", "-c", script)
            } else {
              arrayOf("/system/bin/sh", "-c", script)
            }
        return ProcessBuilder(*cmd).redirectErrorStream(true).start()
      } finally {
        Os.seteuid(1000)
      }
    }
  }

  private suspend fun autoInstallManager() {
    if (suppressManagerAutoInstall || !isInstallingManager.compareAndSet(false, true)) return
    try {
      var waitedTicks = 0
      while (SystemProperties.get("sys.boot_completed") != "1" && waitedTicks < 180) {
        if (suppressManagerAutoInstall) return
        delay(1000)
        waitedTicks++
      }
      if (waitedTicks > 0) {
        // Give PackageInstallerService and OEM security services a moment after BOOT_COMPLETED.
        delay(1500)
      }

      for (attempt in 1..5) {
        if (suppressManagerAutoInstall || tryVerifyInstalledManager()) return

        val apkFile = resolveManagerApkFile()
        if (!apkFile.isFile || apkFile.length() <= 0L) {
          Log.e(TAG, "Manager APK missing or empty: ${apkFile.absolutePath}")
          return
        }

        Log.i(TAG, "Executing pm install for Manager APK (attempt $attempt/5)...")
        val escapedApk = "'" + apkFile.absolutePath.replace("'", "'\\''") + "'"
        val pkg = BuildConfig.DEFAULT_MANAGER_PACKAGE_NAME
        val size = apkFile.length()
        val script =
            """
            export PATH="/product/bin:/apex/com.android.runtime/bin:/system_ext/bin:/system/bin:/system/xbin:/vendor/bin:${'$'}PATH"
            APK=$escapedApk
            SIZE=$size
            PKG='$pkg'
            if pm install -r -d -S "${'$'}SIZE" < "${'$'}APK" 2>&1 || pm install -r -d "${'$'}APK" 2>&1; then
                exit 0
            fi
            pm uninstall "${'$'}PKG" >/dev/null 2>&1
            pm install -S "${'$'}SIZE" < "${'$'}APK" 2>&1 || pm install "${'$'}APK" 2>&1
            """
                .trimIndent()

        var output = ""
        var exitCode = -1
        try {
          val proc = startRootInstallShell(script)
          proc.outputStream.close()
          output = proc.inputStream.bufferedReader().use { it.readText().trim() }
          exitCode = proc.waitFor()
        } catch (e: Exception) {
          output = e.message ?: e.toString()
          Log.e(TAG, "Manager auto-install attempt $attempt/5 threw exception", e)
        }

        if (exitCode == 0) {
          repeat(5) {
            if (tryVerifyInstalledManager()) {
              Log.i(TAG, "Manager auto-installed successfully on attempt $attempt!")
              return
            }
            delay(300)
          }
        }

        Log.w(TAG, "Manager auto-install attempt $attempt/5 failed (exit=$exitCode): $output")
        if (attempt < 5) {
          delay(3000L * attempt)
        }
      }
    } finally {
      isInstallingManager.set(false)
    }
  }

  private fun setupMiscPath() {
    if (state.miscPath != null) return

    val pathStr = PreferenceStore.getModulePrefs("lspd", 0, "config")["misc_path"] as? String
    val path =
        if (pathStr == null) {
          val newPath = Paths.get("/data/misc", UUID.randomUUID().toString())
          PreferenceStore.updateModulePref("lspd", 0, "config", "misc_path", newPath.toString())
          newPath
        } else {
          Paths.get(pathStr)
        }
    state = state.copy(miscPath = path)

    runCatching {
          val perms =
              PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx--x--x"))
          Files.createDirectories(state.miscPath!!, perms)
          FileSystem.setSelinuxContextRecursive(state.miscPath!!, "u:object_r:xposed_data:s0")
        }
        .onFailure { Log.e(TAG, "Failed to create misc directory", it) }
  }

  fun isManager(uid: Int): Boolean {
    ensureCacheReady()
    return uid == state.managerUid || uid == BuildConfig.MANAGER_INJECTED_UID
  }

  fun requestCacheUpdate() {
    cacheUpdateChannel.trySend(Unit)
  }

  /** Builds a completely new Immutable State and atomically swaps it. */
  private fun performCacheUpdate() {
    if (packageManager == null) return

    Log.d(TAG, "Executing Cache Update...")
    val db = dbHelper.readableDatabase
    val oldState = state

    val newModules = mutableMapOf<String, Module>()
    val obsoleteModules = mutableSetOf<String>()
    val obsoletePaths = mutableMapOf<String, String>()

    db.query(
            "modules",
            arrayOf("module_pkg_name", "apk_path"),
            "enabled = 1",
            null,
            null,
            null,
            null)
        .use { cursor ->
          while (cursor.moveToNext()) {
            val pkgName = cursor.getString(0)
            var apkPath = cursor.getString(1)
            if (pkgName == "lspd") continue

            val oldModule = oldState.modules[pkgName]

            var pkgInfo: android.content.pm.PackageInfo? = null
            val users = userManager?.getRealUsers() ?: emptyList()
            for (user in users) {
              pkgInfo = packageManager?.getPackageInfoCompat(pkgName, MATCH_ALL_FLAGS, user.id)
              if (pkgInfo?.applicationInfo != null) break
            }

            if (pkgInfo?.applicationInfo == null) {
              Log.w(TAG, "Failed to find package info of $pkgName")
              obsoleteModules.add(pkgName)
              continue
            }

            val appInfo = pkgInfo.applicationInfo

            if (oldModule != null &&
                appInfo?.sourceDir != null &&
                apkPath != null &&
                oldModule.apkPath != null &&
                (oldModule.versionCode == pkgInfo.longVersionCode || oldModule.versionCode == 0L) &&
                FileSystem.toGlobalNamespace(apkPath).exists() &&
                apkPath == oldModule.apkPath &&
                File(appInfo.sourceDir).parent == File(apkPath).parent) {

              oldModule.versionCode = pkgInfo.longVersionCode
              oldModule.appId = appInfo.uid
              oldModule.applicationInfo = appInfo
              newModules[pkgName] = oldModule
              continue
            }

            val realApkPath = getModuleApkPath(appInfo!!)
            if (realApkPath == null) {
              Log.w(TAG, "Failed to find path of $pkgName")
              obsoleteModules.add(pkgName)
              continue
            } else {
              apkPath = realApkPath
              obsoletePaths[pkgName] = realApkPath
            }

            val preLoadedApk = FileSystem.loadModule(apkPath, state.isDexObfuscateEnabled)
            if (preLoadedApk != null) {
              val module =
                  Module().apply {
                    packageName = pkgName
                    this.apkPath = apkPath
                    appId = appInfo.uid
                    versionCode = pkgInfo.longVersionCode
                    applicationInfo = appInfo
                    service = oldModule?.service ?: InjectedModuleService(pkgName)
                    file = preLoadedApk
                  }
              newModules[pkgName] = module
            } else {
              Log.w(TAG, "Failed to parse DEX/ZIP for $pkgName, skipping.")
              obsoleteModules.add(pkgName)
            }
          }
        }

    if (packageManager?.asBinder()?.isBinderAlive == true) {
      obsoleteModules.forEach { ModuleDatabase.removeModule(it) }
      obsoletePaths.forEach { (pkg, path) -> ModuleDatabase.updateModuleApkPath(pkg, path, true) }
    }

    val newScopes = mutableMapOf<ProcessScope, MutableList<Module>>()
    db.query(
            "scope INNER JOIN modules ON scope.mid = modules.mid",
            arrayOf("app_pkg_name", "module_pkg_name", "user_id"),
            "enabled = 1",
            null,
            null,
            null,
            null)
        .use { cursor ->
          while (cursor.moveToNext()) {
            val appPkg = cursor.getString(0)
            val modPkg = cursor.getString(1)
            val userId = cursor.getInt(2)

            val module = newModules[modPkg] ?: continue

            if (appPkg == "system") {
              newScopes
                  .getOrPut(ProcessScope("system_server", 1000)) { mutableListOf() }
                  .add(module)
              continue
            }

            val pkgInfo =
                packageManager?.getPackageInfoWithComponents(appPkg, MATCH_ALL_FLAGS, userId)
            if (pkgInfo?.applicationInfo == null) continue

            val processNames = pkgInfo.fetchProcesses()
            if (processNames.isEmpty()) continue

            val appUid = pkgInfo.applicationInfo!!.uid

            for (processName in processNames) {
              val processScope = ProcessScope(processName, appUid)
              newScopes.getOrPut(processScope) { mutableListOf() }.add(module)

              if (modPkg == appPkg) {
                val appId = appUid % PER_USER_RANGE
                userManager?.getRealUsers()?.forEach { user ->
                  val moduleUid = user.id * PER_USER_RANGE + appId
                  if (moduleUid != appUid) {
                    val moduleSelf = ProcessScope(processName, moduleUid)
                    newScopes.getOrPut(moduleSelf) { mutableListOf() }.add(module)
                  }
                }
              }
            }
          }
        }

    // --- ATOMIC STATE SWAP ---
    state = oldState.copy(modules = newModules, scopes = newScopes)

    Log.d(TAG, "Cache Update Complete. Map Swap successful.")
    // Log.d(TAG, "cached modules:")
    // newModules.forEach { (pkg, mod) -> Log.d(TAG, "$pkg ${mod.apkPath}") }

    // Log.d(TAG, "cached scopes:")
    // newScopes.forEach { (ps, modules) ->
    //   Log.d(TAG, "${ps.processName}/${ps.uid}")
    //   modules.forEach { mod -> Log.d(TAG, "\t${mod.packageName}") }
    // }
  }

  fun getModuleScope(packageName: String): MutableList<Application>? {
    if (packageName == "lspd") return null
    val result = mutableListOf<Application>()
    dbHelper.readableDatabase
        .query(
            "scope INNER JOIN modules ON scope.mid = modules.mid",
            arrayOf("app_pkg_name", "user_id"),
            "modules.module_pkg_name = ?",
            arrayOf(packageName),
            null,
            null,
            null)
        .use { cursor ->
          while (cursor.moveToNext()) {
            result.add(
                Application().apply {
                  this.packageName = cursor.getString(0)
                  this.userId = cursor.getInt(1)
                })
          }
        }
    return result
  }

  fun getAutoInclude(packageName: String): Boolean {
    if (packageName == "lspd") return false

    var isAutoInclude = false
    dbHelper.readableDatabase
        .query(
            "modules",
            arrayOf("auto_include"),
            "module_pkg_name = ?",
            arrayOf(packageName),
            null,
            null,
            null)
        .use { cursor ->
          if (cursor.moveToFirst()) {
            isAutoInclude = cursor.getInt(0) == 1
          }
        }
    return isAutoInclude
  }

  fun getAutoIncludeModules(): List<String> {
    val result = mutableListOf<String>()
    ConfigCache.dbHelper.readableDatabase
        .query("modules", arrayOf("module_pkg_name"), "auto_include = 1", null, null, null, null)
        .use { cursor ->
          val idx = cursor.getColumnIndexOrThrow("module_pkg_name")
          while (cursor.moveToNext()) {
            val pkgName = cursor.getString(idx)
            if (pkgName != "lspd") result.add(pkgName)
          }
        }
    return result
  }

  fun getModulesForProcess(processName: String, uid: Int): List<Module> {
    ensureCacheReady()
    if (processName == "system_server") {
      Log.w(TAG, "Skip unexpected module queries for $processName")
      return emptyList()
    }
    return state.scopes[ProcessScope(processName, uid)] ?: emptyList()
  }

  fun getModuleByUid(uid: Int): Module? =
      state.modules.values.firstOrNull { it.appId == uid % PER_USER_RANGE }

  fun getModuleByPackage(packageName: String): Module? = state.modules[packageName]

  fun getModulesForSystemServer(): List<Module> {
    val modules = mutableListOf<Module>()
    if (!android.os.SELinux.checkSELinuxAccess(
        "u:r:system_server:s0", "u:r:system_server:s0", "process", "execmem")) {
      Log.e(TAG, "Skipping system_server injection: sepolicy execmem denied")
      return modules
    }

    synchronized(this) {
      dbHelper.readableDatabase
          .query(
              "scope INNER JOIN modules ON scope.mid = modules.mid",
              arrayOf("module_pkg_name", "apk_path"),
              "app_pkg_name=? AND enabled=1",
              arrayOf("system"),
              null,
              null,
              null)
          .use { cursor ->
            val updatedModules = state.modules.toMutableMap()
            var mutated = false
            while (cursor.moveToNext()) {
              val pkgName = cursor.getString(0)
              val apkPath = cursor.getString(1)

              val cached = updatedModules[pkgName]
              if (cached != null) {
                modules.add(cached)
                continue
              }

              val statPath = FileSystem.toGlobalNamespace("/data/user_de/0/$pkgName").absolutePath
              val module =
                  Module().apply {
                    packageName = pkgName
                    this.apkPath = apkPath
                    appId = runCatching { Os.stat(statPath).st_uid }.getOrDefault(-1)
                    versionCode = 0
                    service = InjectedModuleService(pkgName)
                  }

              runCatching {
                    @Suppress("DEPRECATION")
                    val pkg = PackageParser().parsePackage(File(apkPath), 0, false)
                    module.applicationInfo = pkg.applicationInfo
                  }
                  .onFailure {
                    module.applicationInfo = ApplicationInfo().apply { packageName = pkgName }
                  }

              if (module.applicationInfo == null) {
                module.applicationInfo = ApplicationInfo().apply { packageName = pkgName }
              }

              // Always apply the critical paths manually, even on fallback
              module.applicationInfo?.apply {
                sourceDir = apkPath
                dataDir = statPath
                deviceProtectedDataDir = statPath
                HiddenApiBridge.ApplicationInfo_credentialProtectedDataDir(this, statPath)
                processName = pkgName
                uid = module.appId
              }

              FileSystem.loadModule(apkPath, state.isDexObfuscateEnabled)?.let {
                module.file = it
                modules.add(module)
                updatedModules[pkgName] = module
                mutated = true
              }
            }
            if (mutated) {
              state = state.copy(modules = updatedModules)
            }
          }
    }
    return modules
  }

  fun getModuleApkPath(info: ApplicationInfo): String? {
    val apks = mutableListOf<String>()
    info.sourceDir?.let { apks.add(it) }
    info.splitSourceDirs?.let { apks.addAll(it) }

    return apks.firstOrNull { apk ->
      runCatching {
            java.util.zip.ZipFile(apk).use { zip ->
              zip.getEntry("META-INF/xposed/java_init.list") != null ||
                  zip.getEntry("assets/xposed_init") != null
            }
          }
          .getOrDefault(false)
    }
  }

  fun getInstalledModules(): List<ApplicationInfo> {
    val allPackages =
        packageManager?.getInstalledPackagesFromAllUsers(MATCH_ALL_FLAGS, false) ?: emptyList()
    return allPackages
        .mapNotNull { it.applicationInfo }
        .filter { info -> getModuleApkPath(info) != null }
  }

  fun shouldSkipProcess(scope: ProcessScope): Boolean {
    ensureCacheReady()
    return !state.scopes.containsKey(scope)
  }

  fun getPrefsPath(packageName: String, uid: Int): String {
    setupMiscPath()
    val basePath = state.miscPath ?: throw IllegalStateException("Fatal: miscPath not initialized!")

    val userId = uid / PER_USER_RANGE
    val userSuffix = if (userId == 0) "" else userId.toString()
    val path = basePath.resolve("prefs$userSuffix").resolve(packageName)

    val module = state.modules[packageName]
    if (module != null && module.appId == uid % PER_USER_RANGE) {
      runCatching {
            // Ensure the directory exists first
            if (!Files.exists(path)) {
              Files.createDirectories(path)
            }

            Files.walk(path).use { stream ->
              stream.forEach { p ->
                val pathStr = p.toString()

                // Change Owner
                Os.chown(pathStr, uid, uid)

                // Set Permissions using Octal
                // Root folder must be word-readable for monitoring
                val mode =
                    when {
                      p == path -> "755".toInt(8) // Root folder: 755
                      Files.isDirectory(p) -> "711".toInt(8) // Sub-folders: 711
                      else -> "744".toInt(8) // Files: 744
                    }

                Os.chmod(pathStr, mode)
              }
            }
          }
          .onFailure { Log.e(TAG, "Failed to prepare prefs path: $path", it) }
    }
    return path.toString()
  }
}
