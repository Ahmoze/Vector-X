package org.ahmoze.vector;

import org.ahmoze.vector.lspd.service.ILSPApplicationService;
import org.ahmoze.vector.lspd.util.Utils;
import org.ahmoze.vector.impl.core.VectorStartup;
import org.ahmoze.vector.impl.di.VectorBootstrap;
import org.ahmoze.vector.legacy.LegacyDelegateImpl;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedInit;

public class Startup {

    public static void bootstrapXposed(boolean systemServerStarted) {
        try {
            VectorStartup.bootstrap(XposedInit.startsSystemServer, systemServerStarted);
            XposedInit.loadLegacyModules();
        } catch (Throwable t) {
            Utils.logE("Error during framework initialization", t);
        }
    }

    public static void initXposed(boolean isSystem, String processName, String appDir, ILSPApplicationService service) {
        // Establish the Dependency Injection contract
        VectorBootstrap.INSTANCE.init(new LegacyDelegateImpl());

        // Initialize legacy resources and state
        XposedBridge.initXResources();
        XposedInit.startsSystemServer = isSystem;

        // Hand off execution to the modern framework initialization
        VectorStartup.init(isSystem, processName, appDir, service);
    }
}
