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

public class OpenRESTfulClientTest {
	static final Logger LOG = LoggerFactory.getLogger(OpenRESTfulClientTest.class);	
	
	final String host = "10.144.77.186";		// CHANGE TO ONLINE SERVER
	final int port = 8080;						// CHANGE TO ONLINE SERVER
	final String apiKey = "F4BYE2EE3YA250K3";	// CHANGE TO YOUR PROJECT API KEY
	
	final OpenRESTfulClient client = new OpenRESTfulClient(host, port, apiKey);
	
	final Random random = new Random(System.currentTimeMillis());
	
	public OpenRESTfulClientTest() {		
	}
	
	protected String now() {		
		return OpenRESTfulClient.now();
	}
	
	protected boolean equals(Object src, Object dst) throws IOException {
		String a = client.jackson.writeValueAsString(src);
		String z = client.jackson.writeValueAsString(dst);
		
		if (!a.equals(z)) {
			LOG.warn("Different Objects");
			LOG.info(a);
			LOG.info(z);
			
			return false;
		}		
		
		return true;
	}
	
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
		isensor.setUnit("«×");
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
		IDevice idev = newDevice();
		idev = client.saveDevice(idev); // create a new device
				
		String deviceId = idev.getId();
		try {		
			IDevice qdev = client.getDevice(deviceId); // read the device which we created
			Assert.assertTrue(equals(qdev, idev));
			
			idev.setName("iamchanged");		
			idev = client.modifyDevice(idev); // modify some fields of the device
			
			qdev = client.getDevice(deviceId); // read the device which we modified
			Assert.assertTrue(equals(qdev, idev));
						
			testOperateSensor(deviceId);
			
			testOperateSheet(deviceId);
			
		} finally {		
			client.deleteDevice(deviceId);
		}
	}

	protected void testOperateSensor(String deviceId) throws IOException, InterruptedException {
		String sensorId = "mysensor";			
		ISensor isensor = newSensor(sensorId);
		isensor = client.saveSensor(deviceId, isensor); // create a new sensor
		
		try {
			ISensor qsensor = client.getSensor(deviceId, sensorId); // read the sensor which we created
			Assert.assertTrue(equals(qsensor, isensor));
			
			isensor.setName("iamchanged");
			isensor = client.modifySensor(deviceId, isensor); // modify some field of the sensor
			
			qsensor = client.getSensor(deviceId, sensorId); // read the sensor which we modified
			Assert.assertTrue(equals(qsensor, isensor));
			
			Rawdata rawdata = newRawdata(sensorId);				
			String start = rawdata.getTime();
			
			 // insert one rawdata of the sensor
			client.saveRawdata(deviceId, sensorId, rawdata.getTime(), rawdata.getLat(), rawdata.getLon(), rawdata.getValue());
			
			Thread.sleep(2000L); // wait for server's process (pipeline data saving)
			
			Rawdata qrawdata = client.getRawdata(deviceId, sensorId); // read rawdata which we inserted
			Assert.assertTrue(equals(qrawdata.getValue(), rawdata.getValue()));
			
			// read all the rawdata by given interval (just 1 right now) 
			Rawdata[] rawdatas = client.getRawdatas(deviceId, sensorId, start, null, null);
			Assert.assertEquals(1, rawdatas.length);
			
			String imageName = "iot.png";
			String imageType = "image/png";
			InputStream imageBody = new ByteArrayInputStream(new byte[16]);
			
			// insert one snapshot of the sensor
			client.saveSnapshot(deviceId, sensorId,
									rawdata.getTime(), rawdata.getLat(), rawdata.getLon(), rawdata.getValue(),
									imageName, imageType, imageBody);				
			
			Thread.sleep(2000L); // wait for server's process (pipeline data saving)
			
			// read snapshot which we inserted
			imageBody = client.getSnapshotBody(deviceId, sensorId);
			imageBody.close();				
			
			// read the meta data of the snapshot which we inserted
			Rawdata meta = client.getSnapshotMeta(deviceId, sensorId);
			String[] value = meta.getValue();
			if ((value == null) || (value.length < 1) || (!value[0].startsWith("snapshot://"))) {
				Assert.fail("The Rawdata.value[0] must contain 'snapshot://xxx'");
			}
			
			// get the snapshot ID (should be UUID format)
			String imageId = value[0].substring("snapshot://".length());
			
			// get the specified snapshot by given ID
			InputStream is = client.getSnapshotBody(deviceId, sensorId, imageId); // read the snapshot
			is.close();
			
			// read the meta data by given interval (just 1 right now)
			Rawdata[] metas = client.getSnapshotMetas(deviceId, sensorId, start, null);
			Assert.assertEquals(1, metas.length);
			
		} finally {			
			client.deleteSensor(deviceId, sensorId);
		}
	}

	protected void testOperateSheet(String deviceId) throws IOException, InterruptedException {
		String sheetId = "job";
		ISheet isheet = newSheet(sheetId);
		isheet = client.declareSheet(deviceId, isheet); // declare a new sheet
		
		try {
			ISheet qsheet = client.getSheet(deviceId, sheetId); // read the sheet which we created
			Assert.assertTrue(equals(qsheet, isheet));
			
			isheet.setName("iamchanged");
			isheet = client.declareSheet(deviceId, isheet); // modify some field of the sheet
			
			qsheet = client.getSheet(deviceId, sheetId); // read the sheet which we modified
			Assert.assertTrue(equals(qsheet, isheet));

			// insert one record into the sheet
			String start = now();
			Map<String, String> value = newRecord(start);				
			client.saveRecord(deviceId, sheetId, start, value);
			
			Thread.sleep(2000L); // wait for server's process (pipeline data saving)
			
			Record qrecord = client.getRecord(deviceId, sheetId); // read record which we inserted
			Assert.assertTrue(equals(qrecord.getValue(), value));
			
			// read all the records by given interval (just 1 right now)				
			Record[] qrecords = client.getRecords(deviceId, sheetId, "2016-04-06T18:30:05.077", null, null); // FIXME - start time is not working
			Assert.assertEquals(1, qrecords.length);
			
		} finally {			
			client.deleteSheet(deviceId, sheetId);
		}
	}
	
	@Test
	public void testSaveRawdata() throws Exception {		
		String deviceId = "1386";
		String sensorId = "sensor-0";
		String value = "0";
		
		client.saveRawdata(deviceId, sensorId, value);
	}
	
	@Test
	public void testLocalDateTime() {
		System.out.println(now());
	}
}
