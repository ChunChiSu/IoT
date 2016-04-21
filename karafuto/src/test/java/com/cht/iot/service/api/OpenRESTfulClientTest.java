package com.cht.iot.service.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cht.iot.persistence.entity.api.IAttribute;
import com.cht.iot.persistence.entity.api.IColumn;
import com.cht.iot.persistence.entity.api.IDevice;
import com.cht.iot.persistence.entity.api.ISensor;
import com.cht.iot.persistence.entity.api.ISheet;
import com.cht.iot.persistence.entity.data.Rawdata;
import com.cht.iot.persistence.entity.data.Record;
import com.cht.iot.util.JsonUtils;

public class OpenRESTfulClientTest {
	static final Logger LOG = LoggerFactory.getLogger(OpenRESTfulClientTest.class);	
	
	final String host = "ap.iot.cht.com.tw";	// CHANGE TO ONLINE SERVER
	final int port = 80;						// CHANGE TO ONLINE SERVER
	final int timeout = 5000;
	final String apiKey = "H5T40KG55AWAA9U4";	// CHANGE TO YOUR PROJECT API KEY
	
//	final String host = "localhost";		// CHANGE TO ONLINE SERVER
//	final int port = 8080;						// CHANGE TO ONLINE SERVER
//	final String apiKey = "H5T40KG55AWAA9U4";	// CHANGE TO YOUR PROJECT API KEY
	
	final OpenRESTfulClient client = new OpenRESTfulClient(host, port, apiKey);
	
	final Random random = new Random(System.currentTimeMillis());
	
	public OpenRESTfulClientTest() {		
	}
	
	protected String now() {		
		return OpenRESTfulClient.now();
	}
	
	protected boolean equals(Object src, Object dst) throws IOException {
		String a = JsonUtils.toJson(src);
		String z = JsonUtils.toJson(dst);
		
		if (!a.equals(z)) {
			LOG.warn("Different Objects");
			LOG.info(a);
			LOG.info(z);
			
			return false;
		}		
		
		return true;
	}
	
	@Before
	public void before() {
		client.setTimeout(timeout);
	}
	
	// ======
	
	protected IDevice newDevice() {
		IDevice idev = new IDevice();
		idev.setName("Hygrometer");
		idev.setDesc("My Hygrometer");
		idev.setType("general");
		idev.setUri("http://a.b.c.d/hygrometer");
		idev.setLat(24.95f);
		idev.setLon(121.16f);
		
		IAttribute[] attributes = new IAttribute[] {
			new IAttribute("label", "Hygrometer"),
			new IAttribute("region", "Taiwan")
		};
		
		idev.setAttributes(attributes);
		
		return idev;
	}
	
	protected ISensor newSensor(String sensorId) {
		ISensor isensor = new ISensor();
		
		isensor.setId(sensorId);
		isensor.setName("temperature");
		isensor.setDesc("My Temperature");			
		isensor.setType("guage");
		isensor.setUri("http://a.b.c.d/hygrometer/temperature");
		isensor.setUnit("度");
		//isensor.setFormula("${value} / 100.0"); // not yet supported			
		
		IAttribute[] attributes = new IAttribute[] {
			new IAttribute("label", "Temperature"),
			new IAttribute("region", "Taiwan")
		};
		
		isensor.setAttributes(attributes);	
		
		return isensor;
	}
	
	protected Rawdata newRawdata(String sensorId) {
		Rawdata rawdata = new Rawdata();
		rawdata.setId(sensorId);
		rawdata.setTime(now());
		rawdata.setLat(24.95f + random.nextFloat());
		rawdata.setLon(121.16f + random.nextFloat());
		rawdata.setValue(new String[] {
								String.format("%.2f", 97.0 + random.nextInt(10) + random.nextFloat()),
								String.format("%.2f", 74.0 + random.nextInt(10) + random.nextFloat()) });
		
		return rawdata;
	}
	
	protected ISheet newSheet(String sheetId) {
		ISheet sheet = new ISheet();
		sheet.setId(sheetId);
		sheet.setName("job");
		sheet.setDesc("CNC job");
		
		List<IColumn> columns = new ArrayList<IColumn>();
		
		IColumn column;
		
		column = new IColumn();
		column.setName("timestamp");
		column.setType("datetime");
		columns.add(column);
		
		column = new IColumn();
		column.setName("part");
		column.setType("string");
		columns.add(column);
		
		column = new IColumn();
		column.setName("lot");
		column.setType("string");
		columns.add(column);
		
		column = new IColumn();
		column.setName("run");
		column.setType("integer");
		columns.add(column);
		
		sheet.setColumns(columns.toArray(new IColumn[columns.size()]));
		
		return sheet;
	}
	
	protected Map<String, String> newRecord(String time) {
		Map<String, String> value = new HashMap<String, String>();
		value.put("timestamp", time);
		value.put("part", "CHTL-0001");
		value.put("lot", "20160309-1-1");
		value.put("run", "32767");
		
		return value;
	}
	
	@Test
	public void testAll() throws Exception {
		LOG.info("1. Create a Device");
		IDevice idev = newDevice();
		idev = client.saveDevice(idev); // create a new device
				
		String deviceId = idev.getId();
		try {		
			LOG.info("1. Get the Device");
			IDevice qdev = client.getDevice(deviceId); // read the device which we created
			Assert.assertTrue(equals(qdev, idev));
			
			LOG.info("1. Modify the Device");
			idev.setName("iamchanged");		
			idev = client.modifyDevice(idev); // modify some fields of the device
			
			LOG.info("1. Validate the Device");
			qdev = client.getDevice(deviceId); // read the device which we modified
			Assert.assertTrue(equals(qdev, idev));
						
			testOperateSensor(deviceId);
			
			testOperateSheet(deviceId);
			
		} finally {		
			LOG.info("4. Delete the Device");
			client.deleteDevice(deviceId);
		}
	}

	protected void testOperateSensor(String deviceId) throws IOException, InterruptedException {
		LOG.info("2. Create a Sensor");
		String sensorId = "mysensor";			
		ISensor isensor = newSensor(sensorId);
		isensor = client.saveSensor(deviceId, isensor); // create a new sensor
		
		try {
			LOG.info("2. Get the Sensor");
			ISensor qsensor = client.getSensor(deviceId, sensorId); // read the sensor which we created
			Assert.assertTrue(equals(qsensor, isensor));
			
			LOG.info("2. Modify the Sensor");
			isensor.setName("iamchanged");
			isensor = client.modifySensor(deviceId, isensor); // modify some field of the sensor
			
			LOG.info("2. Validate the Sensor");
			qsensor = client.getSensor(deviceId, sensorId); // read the sensor which we modified
			Assert.assertTrue(equals(qsensor, isensor));
			
			Rawdata rawdata = newRawdata(sensorId);				
			String start = rawdata.getTime();
			
			LOG.info("2. Save a Rawdata");
			 // insert one rawdata of the sensor
			client.saveRawdata(deviceId, sensorId, rawdata.getTime(), rawdata.getLat(), rawdata.getLon(), rawdata.getValue());
			
			Thread.sleep(2000L); // wait for server's process (pipeline data saving)
			
			LOG.info("2. Get the latest Rawdata");
			Rawdata qrawdata = client.getRawdata(deviceId, sensorId); // read rawdata which we inserted
			Assert.assertTrue(equals(qrawdata.getValue(), rawdata.getValue()));
			
			LOG.info("2. Query the Rawdata");
			// read all the rawdata by given interval (just 1 right now) 
			Rawdata[] rawdatas = client.getRawdatas(deviceId, sensorId, start, null, null);
			Assert.assertEquals(1, rawdatas.length);
			
			String imageName = "iot.png";
			String imageType = "image/png";
			InputStream imageBody = new ByteArrayInputStream(new byte[16]);
			
			LOG.info("2. Save a Snapshot");
			// insert one snapshot of the sensor
			client.saveSnapshot(deviceId, sensorId,
									rawdata.getTime(), rawdata.getLat(), rawdata.getLon(), rawdata.getValue(),
									imageName, imageType, imageBody);				
			
			Thread.sleep(2000L); // wait for server's process (pipeline data saving)
			
			LOG.info("2. Load the latest Snapshot");
			// read snapshot which we inserted
			imageBody = client.getSnapshotBody(deviceId, sensorId);
			imageBody.close();				
			
			LOG.info("2. Get the latest Meta of the Snapshot");
			// read the meta data of the snapshot which we inserted
			Rawdata meta = client.getSnapshotMeta(deviceId, sensorId);
			String[] value = meta.getValue();
			if ((value == null) || (value.length < 1) || (!value[0].startsWith("snapshot://"))) {
				Assert.fail("The Rawdata.value[0] must contain 'snapshot://xxx'");
			}
			
			// get the snapshot ID (should be UUID format)
			String imageId = value[0].substring("snapshot://".length());
			
			LOG.info("2. Get the Snapshot by ID");
			// get the specified snapshot by given ID
			InputStream is = client.getSnapshotBody(deviceId, sensorId, imageId); // read the snapshot
			is.close();
			
			LOG.info("2. Query the Metas of the Snapshot");
			// read the meta data by given interval (just 1 right now)
			Rawdata[] metas = client.getSnapshotMetas(deviceId, sensorId, start, null);
			Assert.assertEquals(1, metas.length);
			
		} finally {			
			LOG.info("2. Delete the Sensor");
			client.deleteSensor(deviceId, sensorId);
		}
	}

	protected void testOperateSheet(String deviceId) throws IOException, InterruptedException {
		LOG.info("3. Declare a Sheet");
		String sheetId = "job";
		ISheet isheet = newSheet(sheetId);
		isheet = client.declareSheet(deviceId, isheet); // declare a new sheet
		
		try {
			LOG.info("3. Get the Sheet");
			ISheet qsheet = client.getSheet(deviceId, sheetId); // read the sheet which we created
			Assert.assertTrue(equals(qsheet, isheet));
			
			LOG.info("3. Modify the Sheet");
			isheet.setName("iamchanged");
			isheet = client.declareSheet(deviceId, isheet); // modify some field of the sheet
			
			LOG.info("3. Validate the Sheet");
			qsheet = client.getSheet(deviceId, sheetId); // read the sheet which we modified
			Assert.assertTrue(equals(qsheet, isheet));

			LOG.info("3. Save a Record");
			// insert one record into the sheet
			String start = now();
			Map<String, String> value = newRecord(start);				
			client.saveRecord(deviceId, sheetId, start, value);
			
			Thread.sleep(2000L); // wait for server's process (pipeline data saving)
			
			LOG.info("3. Get the latest Record");
			Record qrecord = client.getRecord(deviceId, sheetId); // read record which we inserted
			Assert.assertTrue(equals(qrecord.getValue(), value));
			
			LOG.info("3. Query the Records");
			// read all the records by given interval (just 1 right now)				
			Record[] qrecords = client.getRecords(deviceId, sheetId, "2016-04-06T18:30:05.077", null, null); // FIXME - start time is not working
			Assert.assertEquals(1, qrecords.length);
			
		} finally {			
			LOG.info("3. Delete the Sheet");
			client.deleteSheet(deviceId, sheetId);
		}
	}	
	
	// ======
	
	@Test
	public void testSaveRawdata() throws Exception {		
		String deviceId = "25";
		String sensorId = "sensor-0";
		String value = "0";
		
		client.saveRawdata(deviceId, sensorId, value);
	}
	
	@Test
	public void testLocalDateTime() {
		System.out.println(now());
	}
}
