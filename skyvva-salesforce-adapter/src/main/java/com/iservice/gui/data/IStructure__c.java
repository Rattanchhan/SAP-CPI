package com.iservice.gui.data;


public class IStructure__c extends SFIntegrationObject implements ISFIntegrationObject {

	private static final long serialVersionUID = -3262762453447115930L;

	public IStructure__c(String pkage){
		super(pkage);
	}
	
	private String id;
	private String name;
	private String type__c;
	//05-10-2011
	private String description__c;
	private String sample_Content__c;
	
	private String interface__c;
	
	private String sequence__c;
	
	//19-04-18 use with skyvva version 2.39
	private String Creation_Type__c;
	
	public String getCreation_Type__c() {
		return Creation_Type__c;
	}

	public void setCreation_Type__c(String creation_Type__c) {
		Creation_Type__c = creation_Type__c;
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

	public String getType__c() {
		return type__c;
	}

	public void setType__c(String type__c) {
		this.type__c = type__c;
	}

	public String getDescription__c() {
		return description__c;
	}

	public void setDescription__c(String description__c) {
		this.description__c = description__c;
	}

	public String getSample_Content__c() {
		return sample_Content__c;
	}

	public void setSample_Content__c(String sample_Content__c) {
		this.sample_Content__c = sample_Content__c;
	}

	public String getInterface__c() {
		return interface__c;
	}

	public void setInterface__c(String interface__c) {
		this.interface__c = interface__c;
	}

	public String getSequence__c() {
		return sequence__c;
	}

	public void setSequence__c(String sequence__c) {
		this.sequence__c = sequence__c;
	}

	

	
	
}
