package com.cht.iot.persistence.entity.api;

import java.util.List;

public class ISubscribe {
	String ck;
	List<String> resources;
	
	public ISubscribe() {
	}
	
	/**
	 * API KEY
	 * 
	 * @return
	 */
	public String getCk() {
		return ck;
	}
	
	public void setCk(String ck) {
		this.ck = ck;
	}
	
	/**
	 * Which resources we want to subscribe. e.g. '/v1/device/1/sensor/sensor-1/current'
	 * 
	 * @return
	 */
	public List<String> getResources() {
		return resources;
	}
	
	public void setResources(List<String> resources) {
		this.resources = resources;
	}	
}
