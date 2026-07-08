package org.ahmoze.vector.lspd.service;

import org.ahmoze.vector.lspd.models.Module;
import org.ahmoze.vector.lspd.service.IHotReloadTarget;

interface ILSPApplicationService {
    boolean isLogMuted();

    List<Module> getLegacyModulesList();

    List<Module> getModulesList();

    String getPrefsPath(String packageName);

    ParcelFileDescriptor requestInjectedManagerBinder(out List<IBinder> binder);

    long registerHotReloadTarget(String modulePackageName, long loadedVersionCode, IHotReloadTarget target);
}
