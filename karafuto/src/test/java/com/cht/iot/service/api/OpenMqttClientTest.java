package com.cht.iot.service.api;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import com.cht.iot.persistence.entity.data.Rawdata;
import com.cht.iot.service.api.OpenMqttClient.Listener;

public class OpenMqttClientTest {
	String host = "iot.cht.com.tw";
	int port = 1883;
	String apiKey = "H5T40KG55AWAA9U4";		// CHANGE TO YOUR PROJECT API KEY
	String serialId = "001002003004005";	// CHANGE TO YOUR EQUIPMENT SERIAL NUMBER
	
	String deviceId = "25";					// CHANGE TO YOUR DEVICE ID
	String sensorId = "sensor-0";				// CHANGE TO YOUR SENSOR ID
	
	@Test
	public void test() throws Exception {
		OpenMqttClient mqc = new OpenMqttClient(host, port, apiKey);
		
		mqc.register(serialId); // '/v1/registry/001002003004005'
		mqc.subscribe(deviceId, sensorId); // '/v1/device/25/sensor/sensor-0/rawdata'
		
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
		
		for (;;) {			
			Thread.sleep(2000L);
			
			String[] value = new String[] { RandomStringUtils.randomNumeric(5) };			
			mqc.save(deviceId, sensorId, value); // change the rawdata
		}
	}
}
