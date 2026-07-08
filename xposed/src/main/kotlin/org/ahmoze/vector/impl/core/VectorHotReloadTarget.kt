package org.ahmoze.vector.impl.core

import android.os.Bundle
import org.ahmoze.vector.lspd.models.Module
import org.ahmoze.vector.lspd.service.IHotReloadTarget

internal object VectorHotReloadTarget : IHotReloadTarget.Stub() {
    override fun hotReloadModule(module: Module, extras: Bundle?) {
        VectorModuleManager.hotReloadModule(module, extras)
    }
}
