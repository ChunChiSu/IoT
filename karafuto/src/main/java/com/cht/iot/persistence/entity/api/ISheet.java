package com.cht.iot.persistence.entity.api;

public class ISheet {	
	String id;
	String name;
	String desc;
	IColumn[] columns;
	IAttribute[] attributes;
	
	public ISheet() {		
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	public IColumn[] getColumns() {
		return columns;
	}
	
	public void setColumns(IColumn[] columns) {
		this.columns = columns;
	}

	public IAttribute[] getAttributes() {
		return attributes;
	}

	public void setAttributes(IAttribute[] attributes) {
		this.attributes = attributes;
	}	
}
