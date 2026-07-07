package org.matrix.vector.lspd.service;

import org.matrix.vector.lspd.service.ILSPApplicationService;

interface ILSPSystemServerService {
    ILSPApplicationService requestApplicationService(int uid, int pid, String processName, IBinder heartBeat);
}
