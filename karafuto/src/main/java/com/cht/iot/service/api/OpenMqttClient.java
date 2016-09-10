package com.cht.iot.service.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang.RandomStringUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cht.iot.persistence.entity.data.Rawdata;
import com.cht.iot.util.JsonUtils;

public class OpenMqttClient {
	static final Logger LOG = LoggerFactory.getLogger(OpenMqttClient.class);
	
	public static final int DEFAULT_MQTT_PORT = 1883;
	public static final int QOS_NO_CONFIRMATION = 0;
	public static final int QOS_1 = 1;
	
	final String url;
	final String apiKey;
	
	int connectionTimeout = 5;	// 5 seconds
	int keepAliveInterval = 30;	// every 30 seconds
	
	Listener listener = new ListenerAdapter();

	Set<String> topics = Collections.synchronizedSet(new HashSet<String>());
	
	
	Thread thread;
	
	BlockingQueue<Action> actions = new LinkedBlockingQueue<OpenMqttClient.Action>();
	
	boolean connected = false;
	
	/**
	 * Create a MQTT client. It will build the connection after you call OpenMqttClient.start();
	 * 
	 * @param host		server host
	 * @param port		default must be 1883
	 * @param apiKey
	 */
	public OpenMqttClient(String host, int port, String apiKey) {
		url = String.format("tcp://%s:%d", host, port);
		this.apiKey = apiKey;
	}
	
	/**
	 * Connection timeout in second.
	 * 
	 * @param connectionTimeout
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}
	
	/**
	 * Keep alive test in second.
	 * 
	 * @param keepAliveInterval
	 */
	public void setKeepAliveInterval(int keepAliveInterval) {
		this.keepAliveInterval = keepAliveInterval;
	}
	
	/**
	 * Set the listener to read the incoming events.
	 * 
	 * @param listener
	 */
	public void setListener(Listener listener) {
		this.listener = listener;
	}
	
	/**
	 * Set the MQTT topics to subscribe.
	 * 
	 * [Deprecated] use 'subscribe(), register()'
	 * 
	 * @param topics
	 */	
	@Deprecated
	public synchronized void setTopics(List<String> topics) {
		this.topics.addAll(topics);
	}
	
	// ======
	
	/**
	 * The topic we want to receive the value changed event for the specified sensor.
	 * 
	 * [Deprecated] will be only used inside this class.
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @return
	 */
	@Deprecated
	public static final String getRawdataTopic(String deviceId, String sensorId) {
		return String.format("/v1/device/%s/sensor/%s/rawdata", deviceId, sensorId);
	}
	
	protected static final String getSavingRawdataTopic(String deviceId) {
		return String.format("/v1/device/%s/rawdata", deviceId);
	}
	
	/**
	 * The topic we want to receive the register event for the specified product serial number.
	 * 
	 * [Deprecated] will be only used inside this class.
	 * 
	 * @param serialId
	 * @return
	 */
	@Deprecated
	public static final String getRegistryTopic(String serialId) {
		return String.format("/v1/registry/%s", serialId);
	}
	
	protected Rawdata toRawdata(String json) {
		return JsonUtils.fromJson(json, Rawdata.class);
	}
	
	protected Provision toProvision(String json) {
		return JsonUtils.fromJson(json, Provision.class);
	}
	
	protected String toJson(Rawdata[] rawdata) {
		return JsonUtils.toJson(rawdata);
	}
	
	// ======
	
	protected void put(Action a) {
		try {
			actions.put(a);
			
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Listen to the rawdata changed from the specified sensor.
	 * 
	 * @param deviceId
	 * @param sensorId
	 */
	public void subscribe(String deviceId, String sensorId) {
		String topic = getRawdataTopic(deviceId, sensorId);
		
		if (topics.add(topic)) { // newbie ?
			Action a = new Action(Action.Method.subscribe, topic);
			put(a);
		}
	}
	
	/**
	 * Stop to listen to the rawdata changed from the specified sensor.
	 * 
	 * @param deviceId
	 * @param sensorId
	 */
	public void unsubscribe(String deviceId, String sensorId) {
		String topic = getRawdataTopic(deviceId, sensorId);
		
		if (topics.remove(topic)) { // existed ?
			Action a = new Action(Action.Method.unsubscribe, topic);
			put(a);
		}
	}
	
	/**
	 * Listen to the configuration from registry.
	 * 
	 * @param serialId
	 */
	public void register(String serialId) {
		String topic = getRegistryTopic(serialId);
		
		if (topics.add(topic)) { // newbie ?
			Action a = new Action(Action.Method.subscribe, topic);
			put(a);
		}
	}
	
	/**
	 * Stop to listen to the configuration from registry.
	 * 
	 * @param serialId
	 */
	public void unregister(String serialId) {
		String topic = getRegistryTopic(serialId);
		
		if (topics.remove(topic)) { // existed ?
			Action a = new Action(Action.Method.unsubscribe, topic);
			put(a);
		}
	}
	
	/**
	 * Save the rawdata into IoT platform.
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @param value
	 */
	public void save(String deviceId, String sensorId, String[] value) {
		String topic = getSavingRawdataTopic(deviceId);
		
		Rawdata rawdata = new Rawdata();
		rawdata.setId(sensorId);
		rawdata.setValue(value);
		
		Action a = new Action(Action.Method.save, topic, rawdata);
		put(a);
	}
	
	// ======
	
	/**
	 * Start the MQTT connection. 
	 */
	public synchronized void start() {
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				process();				
			}
		});
		thread.start();
	}
	
	/**
	 * Close the MQTT connection.
	 */
	public synchronized void stop() {
		if (thread != null) {
			Thread t = thread;
			thread = null;
			t.interrupt();
		}		
	}
	
	/**
	 * Rebuild the MQTT connection.
	 */
	public synchronized void reconnect() {
		if (thread != null) {
			thread.interrupt();
		}
	}
	
	/**
	 * Check the MQTT connection.
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return connected;
	}
	
	// ======
	
	protected void doConnect(MqttClient client) throws MqttException {
		client.setCallback(new MqttCallback() {
			
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				String json = new String(message.getPayload(), "UTF-8");
				if (topic.startsWith("/v1/device/")) {
					Rawdata rawdata = toRawdata(json);
					listener.onRawdata(topic, rawdata);
					
				} else if (topic.startsWith("/v1/registry/")) {
					Provision provision = toProvision(json);
					Provision.Op op = provision.getOp();
					if (op == Provision.Op.Reconfigure) {
						listener.onReconfigure(topic, provision.getCk());
						
					} else if (op == Provision.Op.SetDeviceId) {
						listener.onSetDeviceId(topic, provision.getCk(), provision.getDeviceId());
					}
				}				
			}
			
			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {				
			}
			
			@Override
			public void connectionLost(Throwable e) {
				LOG.error("Connection is lost", e);
				
				thread.interrupt();
			}
		});
	
		MqttConnectOptions opts = new MqttConnectOptions();
		opts.setUserName(apiKey);
		opts.setPassword(apiKey.toCharArray());
		opts.setConnectionTimeout(connectionTimeout);
		opts.setKeepAliveInterval(keepAliveInterval);		
		opts.setCleanSession(true);		
		
		client.connect(opts);
		
		LOG.info("MQTT is connected.");
	}
	
	protected void doSubscribe(MqttClient client) throws MqttException {		
		synchronized (topics) {
			actions.clear(); // HINT - don't worry, it will not be deadlock
			
			for (String topic : topics) {
				Action a = new Action(Action.Method.subscribe, topic);
				put(a);				
			}
		}		
	}
	
	protected void process() {
		try {
			MqttClientPersistence mcp = new MqttDefaultFilePersistence(System.getProperty("java.io.tmpdir")); // should not be null
		
			while (thread != null) {
				LOG.info("Connect to MQTT broker - " + url);

				try {
					String clientId = RandomStringUtils.randomAlphanumeric(23); // max bytes of client id is 23
					MqttClient client = new MqttClient(url, clientId, mcp);
					try {
						doConnect(client);
						doSubscribe(client);
						
						connected = true;

						while (thread != null) {
							Action a = actions.take();													
							
							if (a.method == Action.Method.subscribe) {
								LOG.info("Subscribe - {}", a.topic);
								client.subscribe(a.topic, QOS_NO_CONFIRMATION);
								
							} else if (a.method == Action.Method.unsubscribe) {
								LOG.info("Un-Subscribe - {}", a.topic);
								client.unsubscribe(a.topic);
								
							} else if (a.method == Action.Method.save) {
								MqttTopic mt = client.getTopic(a.topic);
								
								String json = toJson(new Rawdata[] { a.rawdata });
								mt.publish(json.getBytes("UTF-8"), QOS_1, false);
							}
						}
					} finally {
						try {
							client.disconnect();
							
							LOG.info("MQTT is disconnected");
							
						} catch (Exception ex) {
						}
					}
				} catch (Exception ex) {
					LOG.error("Connection is failed", ex);
				}
				
				connected = false;

				try { Thread.sleep(connectionTimeout * 1000L); } catch (InterruptedException ie) {} // sleep then retry
			}
		} catch (Exception ex) {
			LOG.error("Unknown exception", ex);
		}
	}
	
	// ======
	
	static final class Action {
		Method method;
		String topic;
		Rawdata rawdata;
		
		public Action(Method method, String topic) {
			this.method = method;
			this.topic = topic;			
		}
		
		public Action(Method method, String topic, Rawdata rawdata) {
			this.method = method;
			this.topic = topic;			
			this.rawdata = rawdata;
		}
		
		enum Method {
			subscribe, unsubscribe, save
		}
	}
	
	static class Provision {
		protected Op op;
		protected String ck;
		protected String deviceId;
		
		public Provision() {
    	}
		
		public Op getOp() {
			return op;
		}
		
		public void setOp(Op op) {
			this.op = op;
		}
		
		public String getCk() {
			return ck;
		}

		public void setCk(String ck) {
			this.ck = ck;
		}

		public String getDeviceId() {
			return deviceId;
		}

		public void setDeviceId(String deviceId) {
			this.deviceId = deviceId;
		}

		public enum Op {
			Reconfigure, SetDeviceId
		}
	}

	public static interface Listener {
		
		/**
		 * The value changed of the sensor.
		 * 
		 * @param topic
		 * @param rawdata
		 */
		public void onRawdata(String topic, Rawdata rawdata);
		
		/**
		 * The device/sensor reconfiguration event from server.
		 * 
		 * @param topic
		 * @param apiKey
		 */
		public void onReconfigure(String topic, String apiKey);
		
		/**
		 * The re-assigned device ID from server.
		 * 
		 * @param topic
		 * @param apiKey
		 * @param deviceId
		 */
		public void onSetDeviceId(String topic, String apiKey, String deviceId);
	}
	
	public static class ListenerAdapter implements Listener {
		@Override
		public void onRawdata(String topic, Rawdata rawdata) {
		}
		
		@Override
		public void onReconfigure(String topic, String apiKey) {
		}
		
		@Override
		public void onSetDeviceId(String topic, String apiKey, String deviceId) {
		}
	}
}
