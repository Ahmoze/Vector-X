package org.ahmoze.vector.impl.core

import android.os.Build
import android.os.IBinder
import dalvik.system.DexFile
import org.ahmoze.vector.lspd.service.ILSPApplicationService
import org.ahmoze.vector.impl.hookers.*
import org.ahmoze.vector.impl.hooks.VectorHookBuilder

/**
 * Modern framework initialization and bootstrap sequence. Deploys interceptors into the ART runtime
 * and handles early process deoptimization.
 */
object VectorStartup {

    @JvmStatic
    fun init(
        isSystem: Boolean,
        processName: String?,
        appDir: String?,
        service: ILSPApplicationService?,
    ) {
        VectorServiceClient.init(service, processName ?: "android")
        if (!isSystem) {
            VectorDeopter.deoptBootMethods()
        }
    }

    @JvmStatic
    fun bootstrap(isSystem: Boolean, systemServerStarted: Boolean) {
        // Crash Dump Interceptor
        Thread::class
            .java
            .declaredMethods
            .firstOrNull { it.name == "dispatchUncaughtException" }
            ?.let { VectorHookBuilder(it).intercept(CrashDumpHooker) }

        if (isSystem) {
            // In system_server, always use safe late initialization.
            // Early hooking of handleSystemServerProcess and startBootstrapServices causes
            // severe JIT code cache corruption and ART runtime aborts on modern Android (14+ / 16)
            // when running under official Magisk Zygisk.
            val activityService: IBinder? = android.os.ServiceManager.getService("activity")
            if (activityService != null) {
                val classLoader = activityService.javaClass.classLoader
                if (classLoader != null) {
                    HandleSystemServerProcessHooker.initSystemServer(classLoader, isLate = true)
                    StartBootstrapServicesHooker.dispatchSystemServerLoaded(classLoader)
                }
            } else {
                kotlin.concurrent.thread(name = "Vector-LateSystemServer", isDaemon = true) {
                    var actSvc: IBinder? = null
                    var attempts = 0
                    while (actSvc == null && attempts < 600) {
                        Thread.sleep(100)
                        actSvc = android.os.ServiceManager.getService("activity")
                        attempts++
                    }
                    if (actSvc != null) {
                        val classLoader = actSvc.javaClass.classLoader
                        if (classLoader != null) {
                            org.ahmoze.vector.lspd.util.Utils.logI("VectorStartup: Activity service detected after ${attempts * 100}ms! Initializing Vector in system_server.")
                            HandleSystemServerProcessHooker.initSystemServer(classLoader, isLate = true)
                            StartBootstrapServicesHooker.dispatchSystemServerLoaded(classLoader)
                        }
                    } else {
                        org.ahmoze.vector.lspd.util.Utils.logE("VectorStartup: Timed out waiting for activity service in system_server.")
                    }
                }
            }
        } else {
            // Process-specific Interceptors for Applications
            DexFile::class
                .java
                .declaredMethods
                .filter {
                    it.name == "openDexFile" ||
                        it.name == "openInMemoryDexFile" ||
                        it.name == "openInMemoryDexFiles"
                }
                .forEach { VectorHookBuilder(it).intercept(DexTrustHooker) }

            // Application Load Interceptors
            val loadedApkClass = Class.forName("android.app.LoadedApk")
            loadedApkClass.declaredConstructors.forEach {
                // Hook all constructors of LoadedApk to catch early instantiations securely
                VectorHookBuilder(it).intercept(LoadedApkCtorHooker)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                loadedApkClass.declaredMethods
                    .filter { it.name == "createAppFactory" }
                    .forEach { VectorHookBuilder(it).intercept(LoadedApkCreateAppFactoryHooker) }
            }

            loadedApkClass.declaredMethods
                .filter { it.name == "createOrUpdateClassLoaderLocked" }
                .forEach { VectorHookBuilder(it).intercept(LoadedApkCreateCLHooker) }

            // ActivityThread Attachment Interceptor
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            activityThreadClass.declaredMethods
                .filter { it.name == "attach" }
                .forEach { VectorHookBuilder(it).intercept(AppAttachHooker) }
        }
    }
}
