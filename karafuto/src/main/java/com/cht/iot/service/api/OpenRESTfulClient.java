package com.cht.iot.service.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.PartBase;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cht.iot.persistence.entity.api.IDevice;
import com.cht.iot.persistence.entity.api.IId;
import com.cht.iot.persistence.entity.api.IProvision;
import com.cht.iot.persistence.entity.api.ISensor;
import com.cht.iot.persistence.entity.api.ISheet;
import com.cht.iot.persistence.entity.data.Rawdata;
import com.cht.iot.persistence.entity.data.Record;
import com.cht.iot.util.JsonUtils;

public class OpenRESTfulClient {
	static final Logger LOG = LoggerFactory.getLogger(OpenRESTfulClient.class);
	
	static final DateFormat DF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	static {
		DF.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	final String host;
	final int port;
	final String apiKey;
	
	final HttpClient client;
	
	/**
	 * Build a RESTful client to access the IoT service.
	 * 
	 * @param host		server host
	 * @param port		default is 8080
	 * @param apiKey
	 */
	public OpenRESTfulClient(String host, int port, String apiKey) {
		this.host = host;
		this.port = port;
		this.apiKey = apiKey;
		
		client = new HttpClient();
	}
	
	public void setTimeout(int timeout) {
		HttpConnectionManagerParams hcmp = client.getHttpConnectionManager().getParams();
		hcmp.setConnectionTimeout(timeout);
		hcmp.setSoTimeout(timeout);
	}
	
	public synchronized static final String now() {
		return DF.format(new Date());
	}
	
	protected InputStream http(HttpMethod hm) throws IOException {
		hm.addRequestHeader("CK", apiKey);
		int sc = client.executeMethod(hm);
		
		if (sc != HttpStatus.SC_OK) {
			throw new IOException(String.format("[%d] %s", sc, hm.getStatusText()));
		}
		
		return hm.getResponseBodyAsStream();
	}
	
	protected InputStream post(EntityEnclosingMethod eem, String json) throws IOException {
		if (json != null) {			
			StringRequestEntity sre = new StringRequestEntity(json, "application/json", "UTF-8");		
			eem.setRequestEntity(sre);
		}
		
		return http(eem);
	}
	
//	protected String toJson(Object obj) throws IOException {
//		return jackson.writeValueAsString(obj);
//	}
//	
//	protected <T> T fromJson(InputStream is, Class<T> clazz) throws IOException {
//		return jackson.readValue(is, clazz);
//	}

	protected String encode(String s) throws IOException {
		return URLEncoder.encode(s, "UTF-8");
	}
	
	// ======
	
	/**
	 * Create a new device.
	 * 
	 * @param dev	You can get the device ID by IDevice.getId() which is assigned by server.
	 * @return
	 * @throws IOException
	 */
	public IDevice saveDevice(IDevice dev) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device", host, port);
		
		PostMethod pm = new PostMethod(url);
		String json = JsonUtils.toJson(dev);		
		
		IId iid = JsonUtils.fromJson(post(pm, json), IId.class);
		dev.setId(iid.getId());
		
		return dev;
	}
	
	/**
	 * Modify the device.
	 * 
	 * @param dev
	 * @return
	 * @throws IOException
	 */
	public IDevice modifyDevice(IDevice dev) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s", host, port, dev.getId());
		
		PutMethod pm = new PutMethod(url);
		String json = JsonUtils.toJson(dev);
		
		post(pm, json);
		
		return dev;
	}
	
	/**
	 * Get the device by given ID.
	 * 
	 * @param deviceId
	 * @return
	 * @throws IOException
	 */
	public IDevice getDevice(String deviceId) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s", host, port, deviceId);
		
		GetMethod gm = new GetMethod(url);
		return JsonUtils.fromJson(http(gm), IDevice.class);		
	}
	
	/**
	 * Get all the devices in your project. (Your API KEY will bind a project)
	 * 
	 * @return
	 * @throws IOException
	 */
	public IDevice[] getDevices() throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device", host, port);
		
		GetMethod gm = new GetMethod(url);
		return JsonUtils.fromJson(http(gm), IDevice[].class);
	}
	
	/**
	 * Delete the specified device.
	 * 
	 * @param deviceId
	 * @throws IOException
	 */
	public void deleteDevice(String deviceId) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s", host, port, deviceId);
		
		DeleteMethod dm = new DeleteMethod(url);
		http(dm);
	}
	
	// ======
	
	/**
	 * Create a new sensor. The sensor id is what you specified.
	 * 
	 * @param deviceId
	 * @param sensor
	 * @return
	 * @throws IOException
	 */
	public ISensor saveSensor(String deviceId, ISensor sensor) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/sensor", host, port, deviceId);
		
		PostMethod pm = new PostMethod(url);
		String json = JsonUtils.toJson(sensor);		
		
		post(pm, json);
		
		return sensor;
	}
	
	/**
	 * Modify the sensor. The sensor ID is a key which you cannot change.
	 * 
	 * @param deviceId
	 * @param sensor
	 * @return
	 * @throws IOException
	 */
	public ISensor modifySensor(String deviceId, ISensor sensor) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/sensor/%s", host, port, deviceId, sensor.getId());
		
		PutMethod pm = new PutMethod(url);
		String json = JsonUtils.toJson(sensor);
		
		post(pm, json);
		
		return sensor;
	}
	
	/**
	 * Get the sensor by given ID.
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @return
	 * @throws IOException
	 */
	public ISensor getSensor(String deviceId, String sensorId) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/sensor/%s", host, port, deviceId, sensorId);
		
		GetMethod gm = new GetMethod(url);
		return JsonUtils.fromJson(http(gm), ISensor.class);		
	}
	
	/**
	 * Get all the sensors of the specified device.
	 * 
	 * @param deviceId
	 * @return
	 * @throws IOException
	 */
	public ISensor[] getSensors(String deviceId) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/sensor", host, port, deviceId);
		
		GetMethod gm = new GetMethod(url);
		return JsonUtils.fromJson(http(gm), ISensor[].class);
	}
	
	/**
	 * Delete a sensor.
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @throws IOException
	 */
	public void deleteSensor(String deviceId, String sensorId) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/sensor/%s", host, port, deviceId, sensorId);
		
		DeleteMethod dm = new DeleteMethod(url);
		http(dm);
	}
	
	// ======
	
	/**
	 * Insert one rawdata into the data store.
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @param time			[optional] ISO-8601 timestamp.
	 * @param lat			[optional]
	 * @param lon			[optional]
	 * @param value			
	 * @throws IOException
	 */
	public void saveRawdata(String deviceId, String sensorId, String time, Float lat, Float lon, String[] value) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/rawdata", host, port, deviceId);
		
		Rawdata rawdata = new Rawdata();
		rawdata.setId(sensorId);
		rawdata.setTime(time);
		rawdata.setLat(lat);
		rawdata.setLon(lon);
		rawdata.setValue(value);
		
		PostMethod pm = new PostMethod(url);
		String json = JsonUtils.toJson(new Rawdata[] { rawdata });
		
		post(pm, json);
	}
	
	/**
	 * Insert one rawdata into the data store.
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @param value
	 * @throws IOException
	 */
	public void saveRawdata(String deviceId, String sensorId, String value) throws IOException {
		saveRawdata(deviceId, sensorId, null, null, null, new String[] { value });
	}
	
	/**
	 * Insert one rawdata into the data store.
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @param value
	 * @throws IOException
	 */
	public void saveRawdata(String deviceId, String sensorId, String[] value) throws IOException {
		saveRawdata(deviceId, sensorId, null, null, null, value);
	}
	
	/**
	 * Get the latest rawdata of the sensor. This rawdata means the current value of the sensor.
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @return
	 * @throws IOException
	 */
	public Rawdata getRawdata(String deviceId, String sensorId) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/sensor/%s/rawdata", host, port, deviceId, sensorId);
		
		GetMethod gm = new GetMethod(url);
		return JsonUtils.fromJson(http(gm), Rawdata.class);
	}
	
	/**
	 * Get the rawdata from the data store.
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @param start			ISO-8601 timestamp.
	 * @param end			[optional]
	 * @param interval		[optional] sampling interval in minute. Not yet supported.
	 * @return
	 * @throws IOException
	 */
	public Rawdata[] getRawdatas(String deviceId, String sensorId, String start, String end, Integer interval) throws IOException {
		if (start == null) {
			throw new IOException("You must specify the start timestamp");
		}		
		start = encode(start);	
		
		StringBuilder sb = new StringBuilder(String.format("http://%s:%d/iot/v1/device/%s/sensor/%s/rawdata?start=%s&", host, port, deviceId, sensorId, start));
		if (end != null) {
			end = encode(end);
			sb.append("end=");
			sb.append(end);
			sb.append('&');
		}
		
		if (interval != null) {
			sb.append("interval=");
			sb.append(interval);
			sb.append('&');
		}
		
		String url = sb.substring(0, sb.length() - 1);
		
		GetMethod gm = new GetMethod(url);
		return JsonUtils.fromJson(http(gm), Rawdata[].class);		
	}
	
	/**
	 * Delete the rawdata.
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @param start			ISO-8601 timestamp.
	 * @param end			[optional]
	 * @throws IOException
	 */
	public void deleteRawdata(String deviceId, String sensorId, String start, String end) throws IOException {
		if (start == null) {
			throw new IOException("You must specify the start timestamp");
		}
		start = encode(start);
		
		StringBuilder sb = new StringBuilder(String.format("http://%s:%d/iot/v1/device/%s/sensor/%s/rawdata?start=%s&", host, port, deviceId, sensorId, start));
		if (end != null) {
			end = encode(end);
			sb.append("end=");
			sb.append(end);
			sb.append('&');
		}
		
		String url = sb.substring(0, sb.length() - 1);
		
		DeleteMethod dm = new DeleteMethod(url);
		http(dm);
	}
	
	// ======
	
	/**
	 * Insert a snapshot (image) into the data store.
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @param time			[optional] ISO-8601 timestamp.
	 * @param lat			[optional]
	 * @param lon			[optional]
	 * @param value			[optional] you can give the empty string array.
	 * @param imageName		
	 * @param imageType		e.g. 'image/png', 'image/jpeg', 'image/gif'
	 * @param imageBody		image byte stream
	 * @throws IOException
	 */
	public void saveSnapshot(String deviceId, String sensorId, String time, Float lat, Float lon, String[] value, String imageName, String imageType, InputStream imageBody) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/snapshot", host, port, deviceId);
		
		Rawdata rawdata = new Rawdata();
		rawdata.setId(sensorId);
		rawdata.setTime(time);
		rawdata.setLat(lat);
		rawdata.setLon(lon);
		rawdata.setValue(value);
		
		String meta = JsonUtils.toJson(rawdata);
		
		PostMethod pm = new PostMethod(url);
		
		StringPart mp = new StringPart("meta", meta, "UTF-8");
		mp.setContentType("application/json");		

		ByteArrayPart bap = new ByteArrayPart(imageName, imageType, IOUtils.toByteArray(imageBody));
		
		MultipartRequestEntity mre = new MultipartRequestEntity(new Part[] { mp, bap}, pm.getParams());
		pm.setRequestEntity(mre);
		
		http(pm);
	}
	
	/**
	 * Get the latest snapshot meta data of the sensor. You can get the snapshot UUID by Rawdata.getValues().
	 * The snapshot ID format should be 'snapshot://xxxooo'  
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @return
	 * @throws IOException
	 */
	public Rawdata getSnapshotMeta(String deviceId, String sensorId) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/sensor/%s/snapshot/meta", host, port, deviceId, sensorId);
		
		GetMethod gm = new GetMethod(url);
		return JsonUtils.fromJson(http(gm), Rawdata.class);
	}
	
	/**
	 * Get the snapshot meta data from data store.
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @param start			ISO-8601 timestamp
	 * @param end			[optional]
	 * @return
	 * @throws IOException
	 */
	public Rawdata[] getSnapshotMetas(String deviceId, String sensorId, String start, String end) throws IOException {
		if (start == null) {
			throw new IOException("You must specify the start timestamp");
		}
		start = encode(start);
		
		StringBuilder sb = new StringBuilder(String.format("http://%s:%d/iot/v1/device/%s/sensor/%s/snapshot/meta?start=%s&", host, port, deviceId, sensorId, start));
		if (end != null) {
			end = encode(end);
			sb.append("end=");
			sb.append(end);
			sb.append('&');
		}		
		
		String url = sb.substring(0, sb.length() - 1);
		
		GetMethod gm = new GetMethod(url);
		return JsonUtils.fromJson(http(gm), Rawdata[].class);		
	}
	
	/**
	 * Get the current snapshot body.
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @return
	 * @throws IOException
	 */
	public InputStream getSnapshotBody(String deviceId, String sensorId) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/sensor/%s/snapshot", host, port, deviceId, sensorId);
		
		GetMethod gm = new GetMethod(url);
		return http(gm);
	}
	
	/**
	 * Get the snapshot body by given snapshot ID which you can retrieve from meta data.
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @param imageId
	 * @return
	 * @throws IOException
	 */
	public InputStream getSnapshotBody(String deviceId, String sensorId, String imageId) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/sensor/%s/snapshot/%s", host, port, deviceId, sensorId, imageId);
		
		GetMethod gm = new GetMethod(url);
		return http(gm);
	}
	
	/**
	 * Delete the snapshot.
	 * 
	 * @param deviceId
	 * @param sensorId
	 * @param start			ISO-8601 timestamp
	 * @param end			[optional]
	 * @throws IOException
	 */
	public void deleteSnapshot(String deviceId, String sensorId, String start, String end) throws IOException {
		if (start == null) {
			throw new IOException("You must specify the start timestamp");
		}
		start = encode(start);
		
		StringBuilder sb = new StringBuilder(String.format("http://%s:%d/iot/v1/device/%s/sensor/%s/snapshot?start=%s&", host, port, deviceId, sensorId, start));
		if (end != null) {
			end = encode(end);
			sb.append("end=");
			sb.append(end);
			sb.append('&');
		}
		
		String url = sb.substring(0, sb.length() - 1);
		
		DeleteMethod dm = new DeleteMethod(url);
		http(dm);
	}
	
	// ======

	/**
	 * Declare a sheet with column definitions.
	 * 
	 * @param deviceId
	 * @param sheet
	 * @return
	 * @throws IOException
	 */
	public ISheet declareSheet(String deviceId, ISheet sheet) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/sheet", host, port, deviceId);
		
		PutMethod pm = new PutMethod(url);
		String json = JsonUtils.toJson(sheet);		
		
		post(pm, json);
		
		return sheet;
	}

	/**
	 * Get the sheet definition.
	 * 
	 * @param deviceId
	 * @param sheetId
	 * @return
	 * @throws IOException
	 */	
	public ISheet getSheet(String deviceId, String sheetId) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/sheet/%s", host, port, deviceId, sheetId);
		
		GetMethod gm = new GetMethod(url);
		
		return JsonUtils.fromJson(http(gm), ISheet.class);
	}
	
	/**
	 * Get all the sheet definitions from the specified device.
	 * 
	 * @param deviceId
	 * @return
	 * @throws IOException
	 */
	public ISheet[] getSheets(String deviceId) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/sheet", host, port, deviceId);
		
		GetMethod gm = new GetMethod(url);
		
		return JsonUtils.fromJson(http(gm), ISheet[].class);
	}
	
	/**
	 * Delete the specified sheet.
	 * 
	 * @param deviceId
	 * @param sheetId
	 * @throws IOException
	 */
	public void deleteSheet(String deviceId, String sheetId) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/sheet/%s", host, port, deviceId, sheetId);
		
		DeleteMethod dm = new DeleteMethod(url);
		
		http(dm);
	}
	
	/**
	 * Save a record into a sheet.
	 * 
	 * @param deviceId
	 * @param sheetId
	 * @param time
	 * @param value
	 * @throws IOException
	 */
	public void saveRecord(String deviceId, String sheetId, String time, Map<String, String> value) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/record", host, port, deviceId);
		
		Record record = new Record();
		record.setId(sheetId);
		record.setTime(time);
		record.setValue(value);
		
		PostMethod pm = new PostMethod(url);
		String json = JsonUtils.toJson(new Record[] { record });
		
		post(pm, json);
	}
	
	/**
	 * Get the latest record from the sheet.
	 * 	
	 * @param deviceId
	 * @param sheetId
	 * @return
	 * @throws IOException
	 */
	public Record getRecord(String deviceId, String sheetId) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/device/%s/sheet/%s/record", host, port, deviceId, sheetId);
		
		GetMethod gm = new GetMethod(url);
		
		return JsonUtils.fromJson(http(gm), Record.class);
	}
	
	/**
	 * Get the records from the sheet with criteria.
	 * 
	 * @param deviceId
	 * @param sheetId
	 * @param start
	 * @param end
	 * @param interval
	 * @return
	 * @throws IOException
	 */
	public Record[] getRecords(String deviceId, String sheetId, String start, String end, Integer interval) throws IOException {
		if (start == null) {
			throw new IOException("You must specify the start timestamp");
		}		
		start = encode(start);	
		
		StringBuilder sb = new StringBuilder(String.format("http://%s:%d/iot/v1/device/%s/sheet/%s/record?start=%s&", host, port, deviceId, sheetId, start));
		if (end != null) {
			end = encode(end);
			sb.append("end=");
			sb.append(end);
			sb.append('&');
		}
		
		if (interval != null) {
			sb.append("interval=");
			sb.append(interval);
			sb.append('&');
		}
		
		String url = sb.substring(0, sb.length() - 1);
		
		GetMethod gm = new GetMethod(url);
		
		return JsonUtils.fromJson(http(gm), Record[].class);		
	}
	
	/**
	 * Delete the records from the sheet with criteria.
	 * 
	 * @param deviceId
	 * @param sheetId
	 * @param start
	 * @param end
	 * @throws IOException
	 */
	public void deleteRecords(String deviceId, String sheetId, String start, String end) throws IOException {
		if (start == null) {
			throw new IOException("You must specify the start timestamp");
		}
		start = encode(start);
		
		StringBuilder sb = new StringBuilder(String.format("http://%s:%d/iot/v1/device/%s/sheet/%s/record?start=%s&", host, port, deviceId, sheetId, start));
		if (end != null) {
			end = encode(end);
			sb.append("end=");
			sb.append(end);
			sb.append('&');
		}
		
		String url = sb.substring(0, sb.length() - 1);
		
		DeleteMethod dm = new DeleteMethod(url);
		http(dm);
	}
	
	// ======
	
	/**
	 * Ask IoT server to reconfigure the equipment with the serial number and digest.
	 * 
	 * This digest is provided by CHT.
	 * 
	 * @param serialId
	 * @param digest
	 * @throws IOException
	 */
	public void reconfigure(String serialId, String digest) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/registry/%s", host, port, serialId);

		IProvision provision = new IProvision();
		provision.setOp(IProvision.Op.Reconfigure);
		provision.setDigest(digest);
		
		PostMethod pm = new PostMethod(url);
		String json = JsonUtils.toJson(provision);
		
		post(pm, json);
	}
	
	/**
	 * Ask IoT server to re-assign the device ID.
	 * 
	 * @param serialId
	 * @param digest
	 * @param deviceId
	 * @throws IOException
	 */
	public void setDeviceId(String serialId, String digest, String deviceId) throws IOException {
		String url = String.format("http://%s:%d/iot/v1/registry/%s", host, port, serialId);
		
		IProvision provision = new IProvision();
		provision.setOp(IProvision.Op.SetDeviceId);
		provision.setDigest(digest);
		provision.setDeviceId(deviceId);
		
		PostMethod pm = new PostMethod(url);
		String json = JsonUtils.toJson(provision);
		
		post(pm, json);
	}
	
	// ======
	
	static class ByteArrayPart extends PartBase {
		final byte[] body;
		
		public ByteArrayPart(String name, String contentType, byte[] body) {
			super(name, contentType, null, null);			
			this.body = body;
		}

		@Override
		protected void sendData(OutputStream out) throws IOException {
			out.write(body);
		}

		@Override
		protected long lengthOfData() throws IOException {			
			return body.length;
		}
	}
}
