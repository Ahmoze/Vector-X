package org.ahmoze.vector.lspd.models;
import org.ahmoze.vector.lspd.models.PreLoadedApk;
import org.ahmoze.vector.lspd.service.ILSPInjectedModuleService;

parcelable Module {
    String packageName;
    int appId;
    long versionCode;
    String apkPath;
    PreLoadedApk file;
    ApplicationInfo applicationInfo;
    ILSPInjectedModuleService service;
}
