package com.iservice.gui.data;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class Adapter__c extends SFIntegrationObject implements ISFIntegrationObject, Comparable<Adapter__c>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Adapter__c(String pkage) {
	
		//initCls();
		
		super(pkage);
		
	}
	
//	@Override
//	public void initCls() {
//		clsPath = this.getClass().getName();
//	}
	
	
	//must be equivalent to the sf's adapter fields
	private String id;
	private String name;
	private String type__c;
	private String name__c;
	
	private List<Property__c> properties;

	public String getId(){
		return id;
	}
	
	public void setId(String id){
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

	public String toString(){
		return name;
	}

//	public List<Property__c> getChildren(String relationshipName) {
//		if(relationshipName.contains("Property__r")){
//			return properties;
//		}
//		return null;
//	}
//
//	
//
//	public void setChildren(String relationshipName,
//			List<ISFIntegrationObject> children) {
//		if(relationshipName.contains("Property__r")){
//			properties = children;
//		}
//		
//	}
	@Override
	protected String ensueFieldName(String fieldName){
		if(StringUtils.contains(fieldName, "Property__r")){
			//set children
			fieldName = "properties";
		}
		return super.ensueFieldName(fieldName);
	}

	

	public List<Property__c> getProperties() {
		return properties;
	}

	public void setProperties(List<Property__c> properties) {
		this.properties = properties;
	}

	public String getName__c() {
		return name__c;
	}

	public void setName__c(String name__c) {
		this.name__c = name__c;
	}

	public int compareTo(Adapter__c o) {
		return this.getName().compareToIgnoreCase(o.getName());
	}
}
