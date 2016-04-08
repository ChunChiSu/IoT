package com.cht.iot.service.api;

import java.util.Arrays;
import java.util.List;

import com.cht.iot.persistence.entity.data.Rawdata;
import com.cht.iot.service.api.OpenMqttClient.Listener;

public class OpenMqttClientTest {

	public static void main(String[] args) throws Exception {
		String host = "tomcat.hiot.net.tw";		// CHANGE TO ONLINE SERVER
		int port = 1883;
		String apiKey = "F4BYE2EE3YA250K3";		// CHANGE TO YOUR PROJECT API KEY
		String serialId = "001002003004005";	// CHANGE TO YOUR EQUIPMENT SERIAL NUMBER
		
		String deviceId = "1386";					// CHANGE TO YOUR DEVICE ID
		String sensorId = "sensor-0";				// CHANGE TO YOUR SENSOR ID
		
		OpenMqttClient mqc = new OpenMqttClient(host, port, apiKey);
		
		String registryTopic = OpenMqttClient.getRegistryTopic(serialId); // '/v1/registry/001002003004005'
		String rawdataTopic = OpenMqttClient.getRawdataTopic(deviceId, sensorId); // '/v1/device/1/sensor/sensor-0/rawdata'
		
		List<String> topics = Arrays.asList(registryTopic, rawdataTopic);
		mqc.setTopics(topics);
		
		mqc.setListener(new Listener() {
			@Override
			public void onRawdata(String topic, Rawdata rawdata) {
				System.out.printf("Rawdata - deviceId: %s, id: %s, time: %s, value: %s\n", rawdata.getDeviceId(), rawdata.getId(), rawdata.getTime(), rawdata.getValue()[0]);				
			}
			
			@Override
			public void onReconfigure(String topic, String apiKey) {
				System.out.printf("Reconfigure - topic: %s, apiKey: %s\n", topic, apiKey);
			}
			
			@Override
			public void onSetDeviceId(String topic, String apiKey, String deviceId) {
				System.out.printf("SetDeviceId - topic: %s, apiKey: %s, deviceId: %s\n", topic, apiKey, deviceId);
			}
		});
		
		mqc.start(); // wait for incoming message
	}
}
