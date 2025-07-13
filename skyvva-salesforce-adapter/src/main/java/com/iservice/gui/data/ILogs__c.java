package com.iservice.gui.data;


public class ILogs__c extends SFIntegrationObject implements ISFIntegrationObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ILogs__c(String pkage) {
		super(pkage);
	}
	

	
	private String id;
	private String integration__c;
	private String error_Level__c;
	private String description__c;
	private String source__c;  //SObject Name
	private String createdDate;
	
	
	

	public String getIntegration__c() {
		return integration__c;
	}

	public void setIntegration__c(String integration__c) {
		this.integration__c = integration__c;
	}

	public String getError_Level__c() {
		return error_Level__c;
	}

	public void setError_Level__c(String error_Level__c) {
		this.error_Level__c = error_Level__c;
	}

	public String getDescription__c() {
		return description__c;
	}

	public void setDescription__c(String description__c) {
		this.description__c = description__c;
	}

	public String getSource__c() {
		return source__c;
	}

	public void setSource__c(String source__c) {
		this.source__c = source__c;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	

	public String getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(String createdDate) {
		this.createdDate = createdDate;
	}
}
