package android.os;

public interface IClientCallback extends IInterface {
    abstract class Stub extends Binder implements IClientCallback {
    }

    void onClients(IBinder registered, boolean hasClients) throws RemoteException;
}
