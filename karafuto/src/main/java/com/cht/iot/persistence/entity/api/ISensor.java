package com.cht.iot.persistence.entity.api;

public class ISensor {	
	public static final String NO_ID = "0";
	
	String id; // sensor name (not database id)	
	String name;
	String desc;
	String type;
	String uri;
	String unit;
	String formula;
	IAttribute[] attributes;
	
	public ISensor() {		
	}

	/**
	 * Sensor ID. The format could be composite characters with alphabet or decimal.
	 * 
	 * @return
	 */
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Sensor name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * [optional] Sensor description.
	 * 
	 * @return
	 */
	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	/**
	 * Sensor type. 'switch', 'gauge', 'counter' will be supported in the future. 
	 * 
	 * @return
	 */
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	/**
	 * [optional] Sensor URI.
	 * 
	 * @return
	 */
	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * [optional] Sensor Unit.
	 * 
	 * @return
	 */
	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	/**
	 * [optional] Sensor formula. Not yet supported.
	 * 
	 * @return
	 */
	public String getFormula() {
		return formula;
	}

	public void setFormula(String formula) {
		this.formula = formula;
	}
	
	/**
	 * [optional] Extra attributes. Key-Value style.
	 * 
	 * @return
	 */
	public IAttribute[] getAttributes() {
		return attributes;
	}

	public void setAttributes(IAttribute[] attributes) {
		this.attributes = attributes;
	}
}
