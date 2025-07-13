package com.iservice.gui.data;

import java.util.ArrayList;
import java.util.List;

public class Integration__c extends SFIntegrationObject implements ISFIntegrationObject, Comparable<Integration__c> {

	private static final long serialVersionUID = -8804463736461761217L;

	public Integration__c(String pkage) {
		super(pkage);
	}
	
	public Integration__c() {
	}
	
	//must be equivalent to the sf's adapter fields
	private String id;
	private String name;
	private String source__c; 	//Adapter ID
	private String packet__c;
	private boolean clear_After_Processing__c;
	private String max_Number_Of_Retry__c;
	private String retry_Time_Interval__c;
	private String max_Number_Of_API_Retry__c;
	private List<Interfaces__c> interfaces;	//children
	private Adapter__c adapter;			//lookup field
	
	private List<IMessage__c> messages; //IMessages__r
	private List<ILogs__c> logs; //ILogs__r
	private List<InterfaceGroup__c> interface_groups = new ArrayList<InterfaceGroup__c>();
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getSource__c() {
		return this.source__c;
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

	protected String ensueFieldName(String fieldName){
		if(fieldName.contains("Interfaces__r")){
			return "interfaces";
			
		}else if(fieldName.contains("Messages__r")){
			return "messages";
			
		}else if(fieldName.contains("Logs__r")){
			return "logs";
			
		}else if(fieldName.contains("Source__r")){
			return "adapter";
		}else if(fieldName.contains("Interface_Groups__r")){
			return "interface_groups";
		}
		return super.ensueFieldName(fieldName);
	}

	public List<Interfaces__c> getInterfaces() {
		if(interfaces == null)
			interfaces = new ArrayList<Interfaces__c>();
		return interfaces;
	}

	public void setInterfaces(List<Interfaces__c> interfaces) {
		this.interfaces = interfaces;
	}

	public Adapter__c getAdapter() {
		return adapter;
	}

	public void setAdapter(Adapter__c adapter) {
		this.adapter = adapter;
	}

	public String toString(){
		return this.name;
	}

	public List<IMessage__c> getMessages() {
		return messages;
	}

	public void setMessages(List<IMessage__c> messages) {
		this.messages = messages;
	}

	public List<ILogs__c> getLogs() {
		return logs;
	}

	public void setLogs(List<ILogs__c> logs) {
		this.logs = logs;
	}

	public String getPacket__c() {
		return packet__c;
	}

	public void setPacket__c(String packet__c) {
		this.packet__c = packet__c;
	}

	public boolean getClear_After_Processing__c() {
		return this.clear_After_Processing__c;
	}
	
	public void setClear_After_Processing__c(boolean clear_After_Processing__c) {
		this.clear_After_Processing__c = clear_After_Processing__c;
	}
	
	public void setClear_After_Processing__c(String clear_After_Processing__c) {
		this.clear_After_Processing__c =Boolean.parseBoolean(clear_After_Processing__c);
	}
	
	public String getMax_Number_Of_Retry__c() {
		if(max_Number_Of_Retry__c!=null) return max_Number_Of_Retry__c.substring(0,max_Number_Of_Retry__c.indexOf("."));
		return max_Number_Of_Retry__c;
	}
	//method must be declared the name as SF Field API Name and it is not allowed double upper case
	public void setMax_Number_Of_Retry__c(String max_Number_Of_Retry__c) { 
		this.max_Number_Of_Retry__c = max_Number_Of_Retry__c;
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

	@Override
	public int compareTo(Integration__c o) {
		return this.getName().compareToIgnoreCase(o.getName());
	}

	public List<InterfaceGroup__c> getInterface_groups() {
		return interface_groups;
	}

	public void setInterface_groups(List<InterfaceGroup__c> interface_groups) {
		this.interface_groups = interface_groups;
	}
	
}
