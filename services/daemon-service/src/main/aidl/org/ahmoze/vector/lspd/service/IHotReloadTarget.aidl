package org.ahmoze.vector.lspd.service;

import android.os.Bundle;
import org.ahmoze.vector.lspd.models.Module;

interface IHotReloadTarget {
    void hotReloadModule(in Module module, in Bundle extras);
}
