package org.matrix.vector.lspd.service;

interface IRemotePreferenceCallback {
    oneway void onUpdate(in Bundle map);
}
