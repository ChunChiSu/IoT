package com.cht.iot.demo.pi;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cht.iot.persistence.entity.data.Rawdata;
import com.cht.iot.service.api.OpenMqttClient;
import com.cht.iot.service.api.OpenRESTfulClient;
import com.cht.iot.util.JsonUtils;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class Main {
	static final Logger LOG = LoggerFactory.getLogger(Main.class);
	
	final GpioController gpio;
	
	final String host = "ap.iot.cht.com.tw"; // CHANGE TO ONLINE SERVER	
		
	final int restfulPort = 80;
	final OpenRESTfulClient restful;	
	
	final int mqttPort = 1883;
	final int keepAliveInterval = 10;
	final OpenMqttClient mqtt;	
	
	final String apiKey = "H5T40KG55AWAA9U4"; // CHANGE TO YOUR PROJECT API KEY
	
	String deviceId = "25"; // CHANGE TO YOUR DEVICE ID
	
	String lampSensorId = "lamp"; // CHANGE TO YOUR SENSOR ID
	GpioPinDigitalOutput lamp;
	
	String buttonSensorId = "button"; // CHANGE TO YOUR SENSOR ID
	GpioPinDigitalInput button;
	HoldingTrigger<PinState> trigger;
	long triggerThreshold = 100L;
	
	String braceletSensorId = "bracelet"; // CHANGE TO YOUR SENSOR ID
	String ble = "hci0"; // BLUETOOTH LOW ENERGY
	String braceletAddress = "88:0F:10:23:65:0A"; // MiBand Bluetooth MAC address
	
	String shutterSensorId = "shutter"; // CHANGE TO YOUR SENSOR ID
	
	String cameraSensorId = "camera"; // CHANGE TO YOUR SENSOR ID
	String cameraDevice = "/dev/video0";
	int cameraWidth = 480;
	int cameraHeight = 360;
	int cameraQuality = 70;
	double cameraAngdeg = 180;
	
	ExecutorService executor = Executors.newSingleThreadExecutor();
	
	public Main() {
		gpio = GpioFactory.getInstance();
		
		restful = new OpenRESTfulClient(host, restfulPort, apiKey); // save or query the value
		
		mqtt = new OpenMqttClient(host, mqttPort, apiKey); // MQTT to listen the value changed
		mqtt.setKeepAliveInterval(keepAliveInterval);
	}
	
	public void init() {
		initGpio();
		initMqtt();
	}
	
	public void destroy() {
		gpio.shutdown();
	}
	
	// ======
	
	protected GpioPinDigitalOutput newLamp(GpioController gpio) {
		return gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, PinState.HIGH);
	}
	
	protected GpioPinDigitalInput newButton(GpioController gpio) {
		return gpio.provisionDigitalInputPin(RaspiPin.GPIO_01, PinPullResistance.PULL_UP);
	}
	
	protected void initGpio() {
		lamp = newLamp(gpio);
		
		button = newButton(gpio);
		
		// a helper to lag the hardware interrupt
		trigger = new HoldingTrigger<PinState>(new HoldingTrigger.Callback<PinState>() {
			public void onData(PinState state) {
				state = button.getState(); // TODO - listener has a bug, we get the button's state by ourself
				
				LOG.info("onButton - {}", state);	
				
				onButtonStateChanged(state); // save rawdata and take a picture
			}
		});
		trigger.setThreshold(triggerThreshold);
		
		// wait for hardware interrupt
		button.addListener(new GpioPinListenerDigital() {			
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				trigger.update(event.getState());
			}
		});
	}
	
	// subscribe the lamp & bracelet (www.mi.com [Xiaomi China])
	protected void initMqtt() {
		String lampTopic = OpenMqttClient.getRawdataTopic(deviceId, lampSensorId); // '/v1/device/25/sensor/lamp/rawdata'
		String braceletTopic = OpenMqttClient.getRawdataTopic(deviceId, braceletSensorId); // '/v1/device/25/sensor/bracelet/rawdata'
		String shutterTopic = OpenMqttClient.getRawdataTopic(deviceId, shutterSensorId); // '/v1/device/25/sensor/shutter/rawdata'
		
		mqtt.setTopics(Arrays.asList(lampTopic, braceletTopic, shutterTopic));
		
		mqtt.setListener(new OpenMqttClient.ListenerAdapter() {			
			@Override
			public void onRawdata(String topic, Rawdata rawdata) {
				handle(rawdata);
			}
		});
		
		mqtt.start(); // wait for incoming message from IoT platform
	}
	
	// ======
	
	protected boolean isOn(String value) {
		return "1".equals(value) || "on".equalsIgnoreCase(value); // TODO - hardcode is NG
	}
	
	protected void handle(Rawdata rawdata) {
		LOG.info("onRawdata - {}", JsonUtils.toJson(rawdata));
		
		if (deviceId.equals(rawdata.getDeviceId())) {
			String id = rawdata.getId();
			if (lampSensorId.equals(id)) {
				String value = rawdata.getValue()[0];
				
				lamp.setState(isOn(value)? PinState.LOW : PinState.HIGH);
				
			} else if (braceletSensorId.equals(id)) {				
				vibrateBracelet(); // to your bracelet (www.mi.com [Xiaomi China])
				
			} else if (shutterSensorId.equals(id)) {
				saveSnapshot(); // take a picture
			}
		}
	}
	
	protected void onButtonStateChanged(PinState state) {
		saveButtonValue(state); // save rawdata in background
		
		if (PinState.HIGH == state) { // take a picture in background
			saveSnapshot();
		}
	}
	
	protected void saveButtonValue(final PinState state) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					String value = (PinState.HIGH == state)? "1" : "0";				
					restful.saveRawdata(deviceId, buttonSensorId, value);
					
					LOG.info("Rawdata is saved");
					
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				}
			}
		});
	}
	
	protected void saveSnapshot() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					BufferedImage bi = PiCamera.capture(cameraDevice, cameraWidth, cameraHeight, cameraQuality);
					if (bi == null) {
						LOG.error("Cannot take the picture");
						
					} else {	
						LOG.info("Picture is taken");
						
						bi = PiCamera.rotate(bi, cameraWidth, cameraHeight, cameraAngdeg);
						byte[] snapshot = PiCamera.toBytes(bi);
						
						String time = OpenRESTfulClient.now();
						String[] value = new String[] { time };
						String imageName = "snapshot.jpg";
						String imageType = "image/jpeg";
						InputStream imageBody = new ByteArrayInputStream(snapshot);
						
						restful.saveSnapshot(deviceId, cameraSensorId, time, 0f, 0f, value, imageName, imageType, imageBody);
						
						LOG.info("Snapshot is saved");
					}					
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				}
			}
		});
	}
	
	protected void vibrateBracelet() {
		try {
			String value = MiBand.genSetUserInfo(braceletAddress);
			
			String cmd = String.format("/usr/bin/gatttool -i %s -b %s --char-write-req -a 0x0019 -n %s", ble, braceletAddress, value);
			
			Runtime r = Runtime.getRuntime();
			r.exec(new String[] { "/bin/bash", "-c", cmd }); // run in background
			
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	public static void main(String[] args) throws Exception {
		System.loadLibrary("v4l4j"); // v4l4j doesn't link the library, we do it ourself
		
		Main m = new Main();
		try {
			m.init();			
			
			Object lck = new Object(); // wait and see
			synchronized (lck) {
				lck.wait();
			}			
		} finally {
			m.destroy();
		}		
	}
}
