package distributed.systems.example;

import static java.util.stream.Collectors.toList;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import distributed.systems.core.IMessageReceivedHandler;
import distributed.systems.core.Message;
import distributed.systems.core.Socket;
import distributed.systems.core.SynchronizedSocket;
import distributed.systems.das.BattleField;
import distributed.systems.das.presentation.BattleFieldViewer;

/**
 * A single server node
 *
 * Will also need to take care of the dragons (with fault tolerance)
 */
public class ServerNode extends UnicastRemoteObject implements IMessageReceivedHandler {

	private final static String REGISTRY_PREFIX = "server";

	private final Socket serverSocket;

	private BattleField battlefield;

	private NodeAddress address;

	//private final BattleField battlefield;

	public static void main(String[] args) throws RemoteException {
		new ServerNode();
	}

	public ServerNode() throws RemoteException{
		// TODO: register to cluster
		serverSocket = connectToCluster();
		// TODO: Acknowledge network/handshake
		// TODO: sync with network
		// TODO: Setup battlefield (self or from network)
		battlefield = BattleField.getBattleField();
		battlefield.setServerSocket(serverSocket);
		// TODO: start a dragon (if necessary)

		/* Spawn a new battlefield viewer */
		new Thread(BattleFieldViewer::new).start();
	}

	private Socket connectToCluster() throws RemoteException {
		Socket socket = new SynchronizedSocket(LocalSocket.connectToDefault());
		address = socket.determineAddress(NodeAddress.NodeType.SERVER);
		socket.register(address.toString());
		socket.addMessageReceivedHandler(this);
		return socket;
	}


	@Override
	public void onMessageReceived(Message message) throws RemoteException{
		message.setReceivedTimestamp();
		// TODO: handle other messages

		// Battlefield-specific messages
		this.battlefield.onMessageReceived(message);
	}
}