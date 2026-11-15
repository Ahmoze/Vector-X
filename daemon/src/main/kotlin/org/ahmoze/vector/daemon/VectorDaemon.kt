package org.ahmoze.vector.daemon

import android.app.ActivityManager
import android.app.ActivityThread
import android.content.Context
import android.ddm.DdmHandleAppName
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.Process
import android.os.ServiceManager
import android.os.SystemProperties
import android.system.Os
import android.util.Log
import java.io.File
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.ahmoze.vector.daemon.data.ConfigCache
import org.ahmoze.vector.daemon.data.FileSystem
import org.ahmoze.vector.daemon.env.CliSocketServer
import org.ahmoze.vector.daemon.env.Dex2OatServer
import org.ahmoze.vector.daemon.env.LogcatMonitor
import org.ahmoze.vector.daemon.ipc.BRIDGE_TRANSACTION_CODE
import org.ahmoze.vector.daemon.ipc.ManagerService
import org.ahmoze.vector.daemon.ipc.SystemServerService
import org.ahmoze.vector.daemon.utils.applyNotificationWorkaround

private const val TAG = "VectorDaemon"
private const val ACTION_SEND_BINDER = 1

object VectorDaemon {
  private val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
    Log.e(TAG, "Caught fatal coroutine exception in background task!", throwable)
  }

  // Dispatchers.IO: Uses the shared background thread pool.
  // SupervisorJob(): Ensures one failing task doesn't kill the whole daemon.
  val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)
  val bridgeServiceName = "activity"

  @Volatile var startedAfterSystemServer = false

  val proxyServiceName: String
    get() = if (startedAfterSystemServer) "serial_vector" else "serial"

  @Volatile private var bridgeBinder: IBinder? = null

  var isRescueMode = false

  @JvmStatic
  fun main(args: Array<String>) {
    if (!FileSystem.tryLock()) kotlin.system.exitProcess(0)

    startedAfterSystemServer =
        ServiceManager.checkService(bridgeServiceName) != null || args.contains("--late-inject")

    Log.i(
        TAG,
        "Vector daemon started: afterSystemServer=$startedAfterSystemServer, proxy=$proxyServiceName")
    Log.i(TAG, "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
      Log.e(TAG, "Uncaught exception in Daemon", e)
      kotlin.system.exitProcess(1)
    }

    // Setup Main Looper
    Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND)
    @Suppress("DEPRECATION") Looper.prepareMainLooper()

    // Bootloop Protection Logic
    val counterFile = File(FileSystem.basePath.toFile(), "bootloop_counter")
    var failCount = 0
    if (counterFile.exists()) {
      failCount = counterFile.readText().trim().toIntOrNull() ?: 0
    }
    if (failCount >= 3) {
      isRescueMode = true
      Log.e(TAG, "BOOTLOOP DETECTED! Rescue Mode activated. Modules will not be loaded.")
      File(FileSystem.basePath.toFile(), "rescue_mode_active").writeText("1")
    } else {
      counterFile.writeText((failCount + 1).toString())
      File(FileSystem.basePath.toFile(), "rescue_mode_active").delete()
    }

    // Monitor boot complete to reset counter and rescue mode
    scope.launch {
      while (SystemProperties.get("sys.boot_completed") != "1") {
        delay(2000)
      }
      Log.i(TAG, "System boot completed successfully. Resetting bootloop counter.")
      counterFile.delete()
      File(FileSystem.basePath.toFile(), "rescue_mode_active").delete()
    }

    // Squat on the proxy service name immediately, which creates the early IPC channel of
    // ApplicationService for our Zygisk module during system_server specialization.
    SystemServerService.registerProxyService(proxyServiceName)

    // Start Environmental Daemons
    LogcatMonitor.start()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Dex2OatServer.start()
    CliSocketServer.start()

    // Preload Framework DEX in the background
    scope.launch { FileSystem.getPreloadDex(ConfigCache.state.isDexObfuscateEnabled) }

    // Initializes system frameworks inside the daemon process
    ActivityThread.systemMain()
    DdmHandleAppName.setAppName("org.ahmoze.vector.daemon", 0)

    // Wait for Android core services
    waitForSystemService("package")
    waitForSystemService("activity") // current bridgeServiceName
    waitForSystemService(Context.USER_SERVICE)
    waitForSystemService(Context.APP_OPS_SERVICE)

    applyNotificationWorkaround()

    // Setup IPC channel for applications by injecting DaemonService binder
    sendToBridge(VectorService.asBinder(), false)

    if (!ManagerService.isVerboseLog()) {
      LogcatMonitor.stopVerbose()
    }

    Looper.loop()
    throw RuntimeException("Main thread loop unexpectedly exited")
  }

  private fun waitForSystemService(name: String) = runBlocking {
    while (ServiceManager.getService(name) == null) {
      Log.i(TAG, "Waiting system service: $name for 1s")
      delay(1000)
    }
  }

  // The bridge is setup in `system_server` via Zygisk API
  @Suppress("DEPRECATION")
  private fun sendToBridge(
      binder: IBinder,
      isRestart: Boolean,
  ) {
    check(Looper.myLooper() == Looper.getMainLooper()) {
      "sendToBridge MUST run on the main thread!"
    }

    Os.seteuid(0)

    runCatching {
          var bridgeService: IBinder?
          if (isRestart) Log.w(TAG, "system_server restarted...")

          while (true) {
            bridgeService = ServiceManager.getService(bridgeServiceName)
            if (bridgeService?.pingBinder() == true) break
            Log.i(TAG, "`$bridgeServiceName` service not ready, waiting 1s...")
            Thread.sleep(1000)
          }

          // Setup death recipient to handle system_server crashes
          val deathRecipient =
              object : IBinder.DeathRecipient {
                override fun binderDied() {
                  Log.w(TAG, "System Server died! Clearing caches and re-injecting...")
                  bridgeBinder?.unlinkToDeath(this, 0)
                  bridgeBinder = null
                  clearSystemCaches()
                  SystemServerService.binderDied() // Cleanup old references
                  if (startedAfterSystemServer) SystemServerService.unregisterProxyService()
                  startedAfterSystemServer = false
                  SystemServerService.systemServerRequested = false
                  // Re-claim the service name immediately to ensure that when system_server
                  // restarts, our proxy is already there for the Zygisk module to find.
                  SystemServerService.registerProxyService(proxyServiceName)
                  ManagerService.guard = null // Remove dead guard
                  Handler(Looper.getMainLooper()).post { sendToBridge(binder, true) }
                }
              }
          bridgeService.linkToDeath(deathRecipient, 0)
          bridgeBinder = bridgeService

          // Try sending the Binder payload (up to 3 times)
          var success = false
          for (i in 0 until 3) {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
              data.writeInt(ACTION_SEND_BINDER)
              data.writeStrongBinder(binder)
              success = bridgeService.transact(BRIDGE_TRANSACTION_CODE, data, reply, 0) == true
              reply.readException()
              if (success) break
            } finally {
              data.recycle()
              reply.recycle()
            }
            Log.w(TAG, "No response from bridge, retrying...")
            Thread.sleep(1000)
          }

          if (success) {
            Log.i(TAG, "Successfully injected Vector IPC binder for applications.")
            if (startedAfterSystemServer) SystemServerService.unregisterProxyService()
          } else {
            if (!SystemServerService.systemServerRequested) {
              Log.e(TAG, "No Vector module reached system_server; leaving it running")
            } else {
              Log.e(TAG, "Vector reached system_server but its bridge never answered")
            }
          }
        }
        .onFailure { Log.e(TAG, "Error during injecting DaemonService", it) }
    Os.seteuid(1000)
  }

  private fun clearSystemCaches() {
    Log.i(TAG, "Clearing ServiceManager and ActivityManager caches...")
    runCatching {
          // Clear ServiceManager.sServiceManager
          var field = ServiceManager::class.java.getDeclaredField("sServiceManager")
          field.isAccessible = true
          field.set(null, null)

          // Clear ServiceManager.sCache
          field = ServiceManager::class.java.getDeclaredField("sCache")
          field.isAccessible = true
          val sCache = field.get(null)
          if (sCache is MutableMap<*, *>) {
            sCache.clear()
          }

          // Clear ActivityManager.IActivityManagerSingleton
          field = ActivityManager::class.java.getDeclaredField("IActivityManagerSingleton")
          field.isAccessible = true
          val singleton = field.get(null)
          if (singleton != null) {
            val mInstanceField =
                Class.forName("android.util.Singleton").getDeclaredField("mInstance")
            mInstanceField.isAccessible = true
            synchronized(singleton) { mInstanceField.set(singleton, null) }
          }
        }
        .onFailure { Log.w(TAG, "Failed to clear system caches via reflection", it) }
  }

  fun softReboot() {
    Log.w(TAG, "Soft reboot: restarting the primary zygote")
    forgetDeliberateBoot()
    SystemProperties.set("ctl.restart", "zygote")
  }

  private fun forgetDeliberateBoot() {
    for (property in arrayOf("crashrecovery.rescue_boot_count", "sys.rescue_boot_count")) {
      runCatching { SystemProperties.set(property, "0") }
          .onFailure { Log.w(TAG, "Could not clear `$property` before restarting", it) }
    }
  }
}
