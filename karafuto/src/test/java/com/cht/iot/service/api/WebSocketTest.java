package com.cht.iot.service.api;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cht.iot.persistence.entity.api.ISubscribe;

@ClientEndpoint
public class WebSocketTest {
	final static Logger LOG = LoggerFactory.getLogger(WebSocketTest.class);
	
	Session session;
	
	public WebSocketTest(String uri) throws DeploymentException, IOException, URISyntaxException {
		WebSocketContainer wsc = ContainerProvider.getWebSocketContainer();
		wsc.connectToServer(this, new URI(uri));
	}
	
	public synchronized void waitForConnected(long timeout) throws InterruptedException {
		while (session == null) {
			wait(timeout);
		}
	}
	
	@OnOpen
	public synchronized void onOpen(Session session) {
		this.session = session;
		notifyAll();
		
		LOG.info("Connected - " + session);
	}
	
	@OnError
	public void onError(Session session, Throwable thr) {		
		LOG.info("Error", thr);
	}
	
	@OnClose
	public void onClose(Session session, CloseReason reason) {
		session = null;
		
		LOG.info("Disconnected - " + reason);
	}
	
	@OnMessage
	public void onMessage(String message) {
		LOG.info("Message: " + message); // {"id":"sensor-0","deviceId":"1","time":"2015-05-07T16:38:01.532Z","value":["on"]}
	}
	
	public void send(String message) throws IOException {
		if (session != null) {
			session.getBasicRemote().sendText(message);
		}
	}
	
	public static void main(String[] args) throws Exception {
		String host = "tomcat.hiot.net.tw"; // CHANGE TO THE ONLINE SERVER HOST
		int port = 8080;
		String apiKey = "F4BYE2EE3YA250K3";	// CHANGE TO YOUR PROJECT API KEY
		
		String deviceId = "1386";		// CHANGE TO YOUR DEVICE ID
		String sensorId = "sensor-0";	// CHANGE TO YOUR SENSOR ID
		
		String url = String.format("ws://%s:%d/iot/ws/rawdata", host, port);		
		WebSocketTest test = new WebSocketTest(url);
		
		test.waitForConnected(5000L);
		
		String topic = String.format("/v1/device/%s/sensor/%s/rawdata", deviceId, sensorId);		
		
		ISubscribe is = new ISubscribe();
		is.setCk(apiKey);
		is.setResources(Arrays.asList(topic));
		
		ObjectMapper jackson = new ObjectMapper();		
		String json = jackson.writeValueAsString(is);
		
		LOG.info("Send - " + json); // {"ck":"aabbccdd","resources":["/v1/device/1/sensor/sensor-0/rawdata"]}
		
		test.send(json);		

		synchronized (test) {
			test.wait(); // wait for message incoming
		}
	}
}
