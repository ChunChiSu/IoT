package com.cht.iot.service.api;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cht.iot.persistence.entity.data.Rawdata;
import com.cht.iot.util.JsonUtils;

public class OpenMqttClient {
	static final Logger LOG = LoggerFactory.getLogger(OpenMqttClient.class);

	static final int QOS_NO_CONFIRMATION = 0;
	
	final String url;
	final String apiKey;
	
	int connectionTimeout = 5; // 5 seconds
	int keepAliveInterval = 30;
	
	Listener listener = new NullListener();
	List<String> topics = Collections.emptyList();
	
	Thread thread;
	
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
	 * @param topics
	 */
	public synchronized void setTopics(List<String> topics) {
		this.topics = topics;
	}
	
	// ======
	
	/**
	 * The topic we want to receive the value changed event for the specified sensor.
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @return
	 */
	public static final String getRawdataTopic(String deviceId, String sensorId) {
		return String.format("/v1/device/%s/sensor/%s/rawdata", deviceId, sensorId);
	}
	
	/**
	 * The topic we want to receive the register event for the specified product serial number.
	 * 
	 * @param serialId
	 * @return
	 */
	public static final String getRegistryTopic(String serialId) {
		return String.format("/v1/registry/%s", serialId);
	}
	
	protected Rawdata toRawdata(String json) throws IOException {
		return JsonUtils.fromJson(json, Rawdata.class);
	}
	
	protected Provision toProvision(String json) throws IOException {
		return JsonUtils.fromJson(json, Provision.class);
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
			public void connectionLost(Throwable ex) {
				LOG.error("Connection is lost", ex);				
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
	
	protected synchronized void doSubscribe(MqttClient client) throws MqttException {
		int s = topics.size();
		if (s > 0) {
			String[] tps = new String[s];
			int[] qoss = new int[s];
			for (int i = 0; i < s; i++) {
				String tp = topics.get(i);
				tps[i] = tp;
				qoss[i] = QOS_NO_CONFIRMATION;
				
				LOG.info("Subscribe - " + tp);
			}
			
			client.subscribe(tps, qoss);
		}
	}
	
	protected void process() {
		try {
			MqttClientPersistence mcp = new MqttDefaultFilePersistence(System.getProperty("java.io.tmpdir")); // should not be null
		
			while (thread != null) {
				LOG.info("Reconnect to MQTT broker: " + url);

				try {
					String clientId = RandomStringUtils.randomAlphanumeric(23); // max bytes of client id is 23
					MqttClient client = new MqttClient(url, clientId, mcp);
					try {
						doConnect(client);
						doSubscribe(client);

						synchronized (thread) {
							thread.wait();
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

				try { Thread.sleep(connectionTimeout * 1000L); } catch (InterruptedException ie) {} // sleep then retry
			}
		} catch (Exception ex) {
			LOG.error("Unknown exception", ex);
		}
	}
	
	// ======
	
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
	
	protected static class NullListener implements Listener {
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
