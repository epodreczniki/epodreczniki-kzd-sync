package pl.epo.kzd.sync.amqp;

import java.io.UnsupportedEncodingException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.aeonbits.owner.ConfigCache;
import org.json.JSONObject;

import pl.epo.kzd.sync.SyncConfig;
import pl.epo.kzd.sync.SyncForEditor;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;

@Slf4j
public class MessageListener {

	private static final String JSON_ENCODING = "utf-8";

	private SyncConfig config = ConfigCache.getOrCreate(SyncConfig.class);

	private final SyncForEditor syncForEditor = new SyncForEditor();

	public void processMessages() throws Exception {
		ConnectionFactory connectionFactory = new ConnectionFactory();
		connectionFactory.setUri(config.amqpConnectionUrl());
		Connection connection = connectionFactory.newConnection();
		try {
			Channel channel = connection.createChannel();
			GetResponse response;
			while ((response = channel.basicGet(config.amqpQueue(), false)) != null) {
				long deliveryTag = response.getEnvelope().getDeliveryTag();
				try {
					processMessage(response);

					log.debug("sending ACK for the last message");
					channel.basicAck(deliveryTag, false);
				} catch (Exception e) {
					log.info("sending NACK for the last message because of " + e);
					channel.basicNack(deliveryTag, false, true);
					throw e;
				}
			}
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	private void processMessage(GetResponse response) throws Exception {
		RoutingKey routingKey = new RoutingKey(response.getEnvelope().getRoutingKey());
		String request = routingKey.getRequest();
		String messageString;
		try {
			messageString = new String(response.getBody(), JSON_ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
		log.info("message received: {} {} (from {})", request, messageString, routingKey.getSender());

		ResourceMessage message = new ResourceMessage(new JSONObject(messageString));
		switch (request) {
			case "resource.modified":
				syncForEditor.update(message.getResourceId(), message.getWomiId(), message.getWomiVersion());
				break;
			case "resource.deleted":
				syncForEditor.delete(message.getResourceId(), message.getWomiId(), message.getWomiVersion());
				break;
			default:
				log.warn("unknown request: {} {} (from {})", request, messageString, routingKey.getSender());
		}
	}

	@Data
	private class ResourceMessage {

		private final long womiId;

		private final long womiVersion;

		private final String resourceId;

		public ResourceMessage(JSONObject o) {
			womiId = o.getLong("womi_id");
			womiVersion = o.getLong("womi_version");
			resourceId = o.getString("custom_id");
		}
	}

}
