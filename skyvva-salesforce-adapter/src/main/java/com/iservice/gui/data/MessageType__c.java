package com.iservice.gui.data;

import java.util.List;

import com.iservice.database.PropertySettingDao;

public class MessageType__c extends SFIntegrationObject implements ISFIntegrationObject {

	private static final long serialVersionUID = -946774168647167546L;

	public MessageType__c(String pkage){
		super(pkage);
	}
	
	public String getPackage(){
		String myPackage = PropertySettingDao.getInstance().getQueryPackage();
		return myPackage;
	}
	
	String myPackage = getPackage();
	
	public static final String TYPE_WSDL_OPERATION="WSDL Operation";
	public static final String TYPE_WSDL_REQUEST="WSDL Request";
	public static final String TYPE_WSDL_RESPONSE="WSDL Response";
	public static final String TYPE_XSD_COMPLEXTYPE="XSD ComplexType";
	
	public static final String TYPE_WSDL_SERVICE 	= "WSDL Service";
    public static final String TYPE_WSDL_BINDING 	= "WSDL Binding";
    public static final String TYPE_WSDL_HEADER  	= "WSDL Header";
    public static final String TYPE_WSDL_FAULT	 	= "WSDL Fault";
    public static final String TYPE_PLAIN_STRUCTURE = "Plain Structure";
	
	private String id;
	private String name;
	private String type__c;
	private String sequenceNumber__c;
	private String SObjectType__c;
	private String ExternalName__c;
	private String HLevel__c;
	
	protected List<MessageType__c> messageTypes__c; //children

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id=id;
	}
	
	@Override
	protected String ensueFieldName(String fieldName){
		if(fieldName.contains("Message_Types__r")){
			return "messageTypes__c";
		}
		return super.ensueFieldName(fieldName);
	}
	

	public String getMyPackage() {
		return myPackage;
	}

	public void setMyPackage(String myPackage) {
		this.myPackage = myPackage;
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

	public String getSequenceNumber__c() {
		return sequenceNumber__c;
	}

	public void setSequenceNumber__c(String sequenceNumber__c) {
		this.sequenceNumber__c = sequenceNumber__c;
	}

	public List<MessageType__c> getMessageTypes__c() {
		return messageTypes__c;
	}

	public void setMessageTypes__c(List<MessageType__c> messageTypes__c) {
		this.messageTypes__c = messageTypes__c;
	}

	public String getSObjectType__c() {
		return SObjectType__c;
	}

	public void setSObjectType__c(String sObjectType__c) {
		SObjectType__c = sObjectType__c;
	}

	public String getExternalName__c() {
		return ExternalName__c;
	}

	public void setExternalName__c(String externalName__c) {
		ExternalName__c = externalName__c;
	}

	public String getHLevel__c() {
		return HLevel__c;
	}

	public void setHLevel__c(String hLevel__c) {
		HLevel__c = hLevel__c;
	}

}
