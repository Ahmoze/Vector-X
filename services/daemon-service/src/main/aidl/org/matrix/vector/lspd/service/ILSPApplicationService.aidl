package org.matrix.vector.lspd.service;

import org.matrix.vector.lspd.models.Module;

interface ILSPApplicationService {
    boolean isLogMuted();

    List<Module> getLegacyModulesList();

    List<Module> getModulesList();

    String getPrefsPath(String packageName);

    ParcelFileDescriptor requestInjectedManagerBinder(out List<IBinder> binder);
}
