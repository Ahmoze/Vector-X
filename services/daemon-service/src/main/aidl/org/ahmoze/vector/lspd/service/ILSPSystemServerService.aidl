package org.ahmoze.vector.lspd.service;

import org.ahmoze.vector.lspd.service.ILSPApplicationService;

interface ILSPSystemServerService {
    ILSPApplicationService requestApplicationService(int uid, int pid, String processName, IBinder heartBeat);
}
