package com.iservice.gui.data;

import org.apache.commons.lang3.StringUtils;

public class InterfaceGroup__c extends SFIntegrationObject implements ISFIntegrationObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String id;
	private String name;
	private String direction_Type__c;
	private String packageSize__c;
	private String type__c;
	private String max_Number_Of_API_Retry__c;
	private String retry_Time_Interval__c;
	
	public InterfaceGroup__c(String pkage) {
		super(pkage);
	}
	
	@Override
	protected String ensueFieldName(String fieldName){
		if(StringUtils.contains(fieldName, "Interface_Groups__r")){
			//set children
			fieldName = "interface_groups";
		}
		return super.ensueFieldName(fieldName);
	}

	@Override
	public void setId(String id) {
		this.id = id;
		
	}
	
	public String getId() {
		return this.id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDirection_Type__c() {
		return direction_Type__c;
	}

	public void setDirection_Type__c(String direction_Type__c) {
		this.direction_Type__c = direction_Type__c;
	}

	public String getPackageSize__c() {
		return packageSize__c;
	}

	public void setPackageSize__c(String packageSize__c) {
		this.packageSize__c = packageSize__c;
	}

	public String getType__c() {
		return type__c;
	}

	public void setType__c(String type__c) {
		this.type__c = type__c;
	}

	public String getRetry_Time_Interval__c() {
		if(retry_Time_Interval__c!=null) return retry_Time_Interval__c.substring(0,retry_Time_Interval__c.indexOf("."));
		return retry_Time_Interval__c;
	}

	public void setRetry_Time_Interval__c(String retry_Time_Interval__c) {
		this.retry_Time_Interval__c = retry_Time_Interval__c;
	}
	
	public String getMax_Number_Of_API_Retry__c() {
		if(max_Number_Of_API_Retry__c!=null) return max_Number_Of_API_Retry__c.substring(0,max_Number_Of_API_Retry__c.indexOf("."));
		return max_Number_Of_API_Retry__c;
	}

	public void setMax_Number_Of_API_Retry__c(String max_Number_Of_API_Retry__c) {
		this.max_Number_Of_API_Retry__c = max_Number_Of_API_Retry__c;
	}

}
