package distributed.systems.core;

import distributed.systems.das.BattleField;
import distributed.systems.das.units.Unit;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

/**
 * Created by mashenjun on 3-3-15.
 */
public class SynchronizedSocket implements Socket{

    private final Socket local;

    public SynchronizedSocket(Socket local) {
        this.local = local;
    }

    @Override
    public void register(String serverid) {
        this.local.register(serverid);
    }

    @Override
    public void addMessageReceivedHandler(BattleField battleField) {
        this.local.addMessageReceivedHandler(battleField);
    }

    @Override
    public void addMessageReceivedHandler(Unit unit) {
        this.local.addMessageReceivedHandler(unit);
    }

    @Override
    public void sendMessage(Message reply, String origin) {
        this.local.sendMessage(reply, origin);
    }

    @Override
    public void unRegister() {
        this.local.unRegister();
    }
}
