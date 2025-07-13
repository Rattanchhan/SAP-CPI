package com.iservice.xmlparser;

/**
 *  @Desc: Represent an element tag
 *  @Properties: tag Name; tag Value and its Attributes
 */
public class XmlField {
	public static final String NAME_ATTR = "name";
	private String name="";
	private String value="";
	
	public XmlField() {
		super();
	}
	
	public XmlField(String name, String value) {
		super();
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
}
