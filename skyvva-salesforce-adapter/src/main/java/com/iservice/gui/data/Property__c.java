package com.iservice.gui.data;


public class Property__c extends SFIntegrationObject implements ISFIntegrationObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Property__c(String pkage) {
		super(pkage);
	}
	
	//must be equivalent to the sf's adapter fields
	private String id;
	private String name__c;
	private String value2__c;
	private String adapter__c;	 //Adapter__c's id
	private String valuelong__c; //adding field must be in lower case to prevent error when insert to SF 

	public String getValuelong__c() {  //must have only one upper case. do not put like this (getValueLong__c()) it will error
		if(this.valuelong__c == null) {
			return "";
		}
		return this.valuelong__c;
	}
	
	public void setValuelong__c(String valuelong__c) {
		this.valuelong__c = valuelong__c;
	}
	
	public String getName__c() {
		if(this.name__c == null)
			return "";
		return this.name__c;
	}
	
	public void setName__c(String name__c) {
		this.name__c = name__c;
	}

	public String getValue2__c() {
		if(value2__c == null)
			return "";
		return value2__c;
	}

	public void setValue2__c(String value2__c) {
		this.value2__c = value2__c;
	}

	public String getAdapter__c() {
		if(this.adapter__c == null)
			return "";
		return adapter__c;
	}

	public void setAdapter__c(String adapter__c) {
		this.adapter__c = adapter__c;
	}

	public String getId() {
		if(id == null)
			return "";
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	

	
}
