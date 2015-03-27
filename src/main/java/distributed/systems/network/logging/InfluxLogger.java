package distributed.systems.network.logging;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.concurrent.TimeUnit;

import distributed.systems.core.LogMessage;
import distributed.systems.core.Message;
import distributed.systems.network.Address;
import distributed.systems.network.NodeAddress;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Database;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Serie;

public class InfluxLogger implements Logger {

	private static final String DATABASE_DATA = "data";
	private static final String DATABASE_GRAFANA = "grafana";
	private final InfluxDB influxDB;
	private final Address databaseLocation;
	private final String username;

	private static InfluxLogger performanceLogging;

	public static InfluxLogger getInstance() {
		if(performanceLogging == null) {
			performanceLogging = new InfluxLogger(new Address("localhost", 8086), "root", "root");
		}
		return performanceLogging;
	}

	public InfluxLogger(Address databaseLocation, String username, String password) {
		this.databaseLocation = databaseLocation;
		this.username = username;

		// Connect to influx
		influxDB = InfluxDBFactory.connect(databaseLocation.toString(), username, password);

		// Create required databases if they are not present
		List<String> databases = influxDB.describeDatabases().stream().map(Database::getName).collect(toList());
		if(!databases.contains(DATABASE_DATA)) {
			influxDB.createDatabase(DATABASE_DATA);
		}
		if(!databases.contains(DATABASE_GRAFANA)) {
			influxDB.createDatabase(DATABASE_GRAFANA);
		}
	}

	public boolean checkConnection() {
		return (influxDB.ping().getStatus().equalsIgnoreCase("ok"));
	}

	public void logMessageDuration(Message message, NodeAddress messageHandler, long duration) {
		String origin = message.getOrigin() != null ? message.getOrigin().getName() : "";
		Serie serie = new Serie.Builder("messagePerformance")
				.columns("duration", "messagetype", "messagehandler", "origin")
				.values(duration, message.getMessageType(), messageHandler.getName(), origin)
				.build();
		influxDB.write(DATABASE_DATA, TimeUnit.MILLISECONDS, serie);
	}

	@Override
	public void log(LogMessage message) {
		Serie serie = new Serie.Builder("log")
				.columns("message","logtype","origin","timestamp")
				.values(message.getLogMessage(), message.getLogType(), message.getTimestamp().getTime())
				.build();
		influxDB.write(DATABASE_DATA, TimeUnit.MILLISECONDS, serie);

	}
}