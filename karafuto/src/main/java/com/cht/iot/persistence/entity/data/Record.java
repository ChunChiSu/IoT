package com.cht.iot.persistence.entity.data;

import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.cht.iot.persistence.entity.api.ISheet;

@JsonIgnoreProperties({ "projectId", "sheet" })
public class Record extends Material {
	private static final long serialVersionUID = 1L;
	
	String id; // SHEET NAME (not database id)
	transient Long projectId; // assign by internal service
	transient String deviceId; // assign by internal service
	transient ISheet sheet; // assign by internal service
	String time;
	Map<String, String> value;

	public Record() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public Long getProjectId() {
		return projectId;
	}
	
	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}
	
	public String getDeviceId() {
		return deviceId;
	}
	
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	
	public ISheet getSheet() {
		return sheet;
	}
	
	public void setSheet(ISheet sheet) {
		this.sheet = sheet;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}
	
	public Map<String, String> getValue() {
		return value;
	}
	
	public void setValue(Map<String, String> value) {
		this.value = value;
	}
}
