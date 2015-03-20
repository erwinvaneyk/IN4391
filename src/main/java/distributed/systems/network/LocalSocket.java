package distributed.systems.network;

import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import distributed.systems.core.IMessageReceivedHandler;
import distributed.systems.core.LogMessage;
import distributed.systems.core.LogType;
import distributed.systems.core.Message;
import distributed.systems.core.Socket;
import distributed.systems.core.exception.AlreadyAssignedIDException;

public class LocalSocket implements Socket,Serializable {

	private String id;

	private Registry registry;

	private static final String PROTOCOL = "localsocket://";

	/**
	 * Creates a socket connected to the default server, ip 127.0.0.1 and port 1234
	 */
	public static LocalSocket connectToDefault() {
		return connectTo("127.0.0.1", RegistryNode.PORT);
	}

	public static LocalSocket connectTo(String ip, int port) {
		return new LocalSocket(ip, port);
	}

	protected LocalSocket(String ip, int port) {
		try {
			registry = LocateRegistry.getRegistry(ip, port);
		} catch (RemoteException e) {
			throw new RuntimeException("Could not connect LocalSocket to registry!", e);
		}
	}

	@Override
	public void register(String id) {
		this.id = id;
	}

	@Override
	public void addMessageReceivedHandler(IMessageReceivedHandler handler) {
		try {
			registry.bind(id, handler);
		}
		catch (AlreadyBoundException e) {
			throw new AlreadyAssignedIDException();
		}
		catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendMessage(Message message, String destination) {
		String binding = destination.startsWith(PROTOCOL) ? destination.substring(PROTOCOL.length()) : destination;

		try {
			message.setOriginId(this.id);
			message.put("origin", PROTOCOL + this.id);
			IMessageReceivedHandler handler = (IMessageReceivedHandler) registry.lookup(binding);
			handler.onMessageReceived(message);
		}
		catch (NotBoundException | RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void logMessage(Message logMessage) {
		try {
			List<NodeAddress> logNodes = getNodes()
					.stream()
					.filter(node -> node.getType().equals(NodeAddress.NodeType.LOGGER))
					.collect(toList());
			logNodes.forEach(logger -> sendMessage(logMessage, logger.toString()));
			// If there are no loggers, just output it to the screen.
			if(logNodes.isEmpty()) {
				System.out.println("No logger present: " + logMessage);
			}
		}
		catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void logMessage(String message, LogType type) {
		logMessage(new LogMessage(message, type));
	}

	@Override
	public void unRegister() {
		try {
			registry.unbind(id);
		}
		catch (RemoteException | NotBoundException e) {
			System.out.println(id + " was already unRegistered!");
		}
	}

	@Override
	public List<NodeAddress> getNodes() throws RemoteException {
		return Arrays
				.stream(registry.list())
				.map(NodeAddress::fromAddress)
				.collect(toList());
	}

	public NodeAddress determineAddress(NodeAddress.NodeType type) throws RemoteException {
		int highestId = getNodes()
				.stream()
				.filter(node -> node.getType().equals(type))
				.mapToInt(NodeAddress::getId)
				.max().orElse(-1);
		return new NodeAddress(type, highestId + 1);
	}

	// Load Balancer
	public Optional<NodeAddress> findServer() throws RemoteException {
		return getNodes()
				.stream()
				.filter(NodeAddress::isServer)
				.findAny();
	}
}