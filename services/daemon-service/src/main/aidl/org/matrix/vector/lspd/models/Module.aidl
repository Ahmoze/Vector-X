package org.matrix.vector.lspd.models;
import org.matrix.vector.lspd.models.PreLoadedApk;
import org.matrix.vector.lspd.service.ILSPInjectedModuleService;

parcelable Module {
    String packageName;
    int appId;
    String apkPath;
    PreLoadedApk file;
    ApplicationInfo applicationInfo;
    ILSPInjectedModuleService service;
}
