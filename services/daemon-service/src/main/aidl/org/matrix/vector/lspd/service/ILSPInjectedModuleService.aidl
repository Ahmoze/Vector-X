package org.matrix.vector.lspd.service;

import org.matrix.vector.lspd.service.IRemotePreferenceCallback;

interface ILSPInjectedModuleService {
    long getFrameworkProperties();

    Bundle requestRemotePreferences(String group, IRemotePreferenceCallback callback);

    ParcelFileDescriptor openRemoteFile(String path);

    String[] getRemoteFileList();
}
