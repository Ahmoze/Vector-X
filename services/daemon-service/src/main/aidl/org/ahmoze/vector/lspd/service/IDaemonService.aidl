package org.ahmoze.vector.lspd.service;

import org.ahmoze.vector.lspd.service.ILSPApplicationService;

interface IDaemonService {
    ILSPApplicationService requestApplicationService(int uid, int pid, String processName, IBinder heartBeat);

    oneway void dispatchSystemServerContext(in IBinder activityThread, in IBinder activityToken);

    boolean preStartManager();
}
