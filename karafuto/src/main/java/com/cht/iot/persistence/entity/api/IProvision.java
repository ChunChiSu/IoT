package com.cht.iot.persistence.entity.api;

public class IProvision {
	Op op;
	String digest;
	String deviceId;
	
	public IProvision() {	
	}

	public Op getOp() {
		return op;
	}

	public void setOp(Op op) {
		this.op = op;
	}

	public String getDigest() {
		return digest;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}	
	
	public static enum Op {
		SetDeviceId, Reconfigure
	}
}
