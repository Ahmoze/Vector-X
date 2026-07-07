package org.ahmoze.vector.lspd.service;

interface IRemotePreferenceCallback {
    oneway void onUpdate(in Bundle map);
}
