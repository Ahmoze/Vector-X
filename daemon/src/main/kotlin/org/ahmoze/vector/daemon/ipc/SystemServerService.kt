package org.ahmoze.vector.daemon.ipc

import android.os.Build
import android.os.IBinder
import android.os.IClientCallback
import android.os.IServiceCallback
import android.os.Parcel
import android.os.ServiceManager
import android.util.Log
import org.ahmoze.vector.lspd.service.ILSPApplicationService
import org.ahmoze.vector.lspd.service.ILSPSystemServerService
import org.ahmoze.vector.daemon.*
import org.ahmoze.vector.daemon.system.getSystemServiceManager

private const val TAG = "VectorSystemServer"

private object ServiceRegistrationWatcher {

  // Written from the main thread at startup and from a binder thread when system_server dies.
  @Volatile private var watchedName: String? = null
  @Volatile private var unusedName: String? = null

  fun watch(serviceName: String) {
    // servicemanager keeps every callback it is handed, and a system_server restart brings the
    // daemon back through here; without this a long-lived daemon stacks up duplicates that all
    // report the same registration.
    if (watchedName == serviceName) return
    val callback =
        object : IServiceCallback.Stub() {
          // The IServiceCallback will tell us when the real Android service is ready,
          // allowing us to capture it and then naturally stop intercepting traffic.
          override fun onRegistration(name: String, binder: IBinder?) {
            if (name == serviceName && binder != null && binder !== SystemServerService) {
              Log.d(TAG, "Intercepted system service registration with name `$name`")
              SystemServerService.adoptOriginService(binder)
            }
          }

          override fun asBinder(): IBinder = this
        }
    runCatching { getSystemServiceManager().registerForNotifications(serviceName, callback) }
        .onSuccess { watchedName = serviceName }
        .onFailure { Log.e(TAG, "Failed to register IServiceCallback", it) }
  }

  fun unregisterWhenUnused(serviceName: String, binder: IBinder) {
    if (unusedName == serviceName) return
    val callback =
        object : IClientCallback.Stub() {
          override fun onClients(registered: IBinder, hasClients: Boolean) {
            if (hasClients) return
            runCatching { getSystemServiceManager().tryUnregisterService(serviceName, registered) }
                .onSuccess { Log.d(TAG, "Dropped the proxy service `$serviceName`") }
                .onFailure { Log.d(TAG, "Proxy service `$serviceName` stays registered: $it") }
          }

          override fun asBinder(): IBinder = this
        }
    runCatching { getSystemServiceManager().registerClientCallback(serviceName, binder, callback) }
        .onSuccess { unusedName = serviceName }
        .onFailure { Log.d(TAG, "Proxy service `$serviceName` stays registered: $it") }
  }
}

object SystemServerService : ILSPSystemServerService.Stub(), IBinder.DeathRecipient {

  private var proxyServiceName: String? = null
  private var originService: IBinder? = null

  @Volatile var systemServerRequested = false

  internal fun adoptOriginService(binder: IBinder) {
    originService = binder
    runCatching { binder.linkToDeath(this, 0) }
  }

  fun registerProxyService(serviceName: String) {
    // Register as the service name early to setup an IPC for `system_server`.
    Log.d(TAG, "Registering bridge service for `system_server` with name `$serviceName`.")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      ServiceRegistrationWatcher.watch(serviceName)
    }
    runCatching {
          ServiceManager.addService(serviceName, this)
          proxyServiceName = serviceName
        }
        .onFailure { Log.e(TAG, "Failed to register proxy service `$serviceName`", it) }
  }

  fun unregisterProxyService() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    val name = proxyServiceName ?: return
    ServiceRegistrationWatcher.unregisterWhenUnused(name, this)
  }

  override fun requestApplicationService(
      uid: Int,
      pid: Int,
      processName: String,
      heartBeat: IBinder?
  ): ILSPApplicationService? {
    if (uid != 1000 || heartBeat == null || processName != "system") return null
    systemServerRequested = true

    // Return the ApplicationService singleton if successfully registered
    return if (ApplicationService.registerHeartBeat(uid, pid, processName, heartBeat)) {
      ApplicationService
    } else null
  }

  override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
    originService?.let {
      // This is unlikely to happen unless system_server restarts / crashes, since we intentionally
      // discard our proxy upon later replacements in registerProxyService.
      Log.d(TAG, "Forwarding request to real `$proxyServiceName` service.")
      return it.transact(code, data, reply, flags)
    }

    when (code) {
      BRIDGE_TRANSACTION_CODE -> {
        val uid = data.readInt()
        val pid = data.readInt()
        val processName = data.readString() ?: ""
        val heartBeat = data.readStrongBinder()

        val service = requestApplicationService(uid, pid, processName, heartBeat)
        if (service != null) {
          reply?.writeNoException()
          reply?.writeStrongBinder(service.asBinder())
          return true
        }
        return false
      }
      DEX_TRANSACTION_CODE,
      OBFUSCATION_MAP_TRANSACTION_CODE -> {
        return ApplicationService.onTransact(code, data, reply, flags)
      }
      else -> {
        return super.onTransact(code, data, reply, flags)
      }
    }
  }

  override fun binderDied() {
    originService?.unlinkToDeath(this, 0)
    originService = null
  }
}
