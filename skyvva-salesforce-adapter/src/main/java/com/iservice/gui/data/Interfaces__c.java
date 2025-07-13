package com.iservice.gui.data;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

//import com.mchange.lang.LongUtils;
//import com.skyvva.camel.beans.tables.Interface;

public class Interfaces__c extends SFIntegrationObject implements ISFIntegrationObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Interfaces__c(String pkage) {
		super(pkage);
//		for(InterfaceFieldVersion field:InterfaceQueryHelper.ORDER_INTERFACE_QUERY_FIELD){
//			if(!field.isError()){
//				for(InterfaceField f:field.getFields()){
//					fieldGetFromSF.add(StringUtils.lowerCase(f.getField()));
//				}
//				break;
//			}
//		}
	}
	
	public Interfaces__c() {
		
	}
	
	public Interfaces__c(String id, String intergration) {
		this.id = id;
		this.integration__c = intergration;
	}
	
	//must be equivalent to the sf's adapter fields
	private String id;
	private String name__c;
	private String status__c;
	private String operationType__c;
	private String source_Name__c;  //SObject Name
	private String integration__c;  //Integration's id
	private String sequence2__c;
	private String query__c;
	private String adapterId__c;	//OutBound adapter
	private String type__c;
	private String interface_Type__c;
	private String source_Adapter__c; //InBound Adapter
	private String Interface_Mode__c; //Processing Mode
	//21092016 #2172 Enhance Agent to use IntegrationBulk
	private boolean isBULKAPI__c;
	private boolean batch_Mode__c;
	private String bulk_Package_Size__c;
	private boolean isStreamingAPI__c;
	//22112016 #2884 Automatic switch which API to use for Agent -> SFDC
	private String integrate_Batch_Max_Size__c;
	private String integrate_Max_Size__c;
	private boolean use_Auto_Switch_Mode__c;
	//20012011
	private String last_Data__c; //Last_Data__c
	//20111116 v1.14 add new field Initialization
	private boolean initialization__c;
	//30-12-2012
	private String initialization_Query__c;
	private String number_of_Records_Per_Batch__c;
	private String max_Number_Of_Retry__c;
	private String retry_Time_Interval__c;
	private String responseInterface__c;
	private List<IStructure__c> iStructures;
	private List<IChained_Interfaces__c> parent_IChained_Interfaces;

	private String messageType__c;
	
	//24-01-2018 v2.39 (beta 17+)
	private String Replay_Option__c;
	private boolean CreateStatisticalData__c;
	//bulk concurencyMode
	private String Bulk_Processing_Mode__c;
	//bulk version(skyvva,sfv1 or sfv2)
	private String Bulk_Version__c;
	//External Mapping2
	private boolean External_Mapping__c;
	//External Mapping
	private boolean PI__c;
	private String SObjectFieldExtId__c;
	private boolean IsNotPersistMessage__c;
	private String max_Number_Of_API_Retry__c;
	// 2.40 
	private String Package_Size__c;
	private String Operation_Deleted__c;
	private String IStructure_Repository__c;
	private boolean by_Passing_Message__c;
	
//	public Interfaces__c(Interface h2Intf) {
//		this.bulk_Package_Size__c = String.valueOf(getH2Field(h2Intf.getBulk_Package_Size__c()));
//		this.id = (String) getH2Field(h2Intf.getInterfaceId());
//	}

	public String getIStructure_Repository__c() {
		return IStructure_Repository__c;
	}

	public void setIStructure_Repository__c(String iStructure_Repository__c) {
		IStructure_Repository__c = iStructure_Repository__c;
	}

	
	public String getOperation_Deleted__c() {
		return Operation_Deleted__c;
	}

	public void setOperation_Deleted__c(String operation_Deleted__c) {
		Operation_Deleted__c = operation_Deleted__c;
	}

	public String getReplay_Option__c() {
		return Replay_Option__c;
	}
	
	public boolean getCreateStatisticalData__c() {
		return CreateStatisticalData__c;
	}

	public void setCreateStatisticalData__c(boolean createStatisticalData__c) {
		CreateStatisticalData__c = createStatisticalData__c;
	}

	public void setReplay_Option__c(String replay_Option__c) {
		Replay_Option__c = replay_Option__c;
	}

	public String getMessageType__c() {
		return messageType__c;
	}

	public void setMessageType__c(String messageType__c) {
		this.messageType__c = messageType__c;
	}

	public String getName__c(){
		return name__c;
	}
	
	public void setName__c(String name__c) {
		this.name__c = name__c;
	}

	public String getStatus__c() {
		return status__c;
	}

	public void setStatus__c(String status__c) {
		this.status__c = status__c;
	}

	public String getOperationType__c() {
		return operationType__c;
	}

	public void setOperationType__c(String operationType__c) {
		this.operationType__c = operationType__c;
	}

	public String getSource_Name__c() {
		return source_Name__c;
	}

	public void setSource_Name__c(String source_Name__c) {
		this.source_Name__c = source_Name__c;
	}

	public String getSequence2__c() {
		if(sequence2__c == null) return "";
		return String.valueOf(sequence2__c);
	}
	
	public void setSequence2__c(String sequence2__c) {
		if(sequence2__c.equals("")){
			this.sequence2__c = sequence2__c;
		}else{
			this.sequence2__c = ""+(int)Double.parseDouble(sequence2__c);
		}
	}

	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public String getIntegration__c() {
		return integration__c;
	}

	public void setIntegration__c(String integration__c) {
		this.integration__c = integration__c;
	}

	public String getQuery__c() {
		return query__c;
	}

	public void setQuery__c(String query__c) {
		this.query__c = query__c;
	}

	@Override
	protected String ensueFieldName(String fieldName){
		if(fieldName.contains("IStructure__r")){
			return "IStructures";
		}else if(fieldName.contains("Parent_IChained_Interfaces__r")){
			return "parent_IChained_Interfaces";
			
		}else if(StringUtils.equalsIgnoreCase(fieldName, "PI__c")){
			return "PI__c";
		}else if(StringUtils.equalsIgnoreCase(fieldName, "SObjectFieldExtId__c")){
			return "SObjectFieldExtId__c";
		}else if(StringUtils.equalsIgnoreCase(fieldName, "IStructure_Repository__c")) {
			return "IStructure_Repository__c";
		}
		return super.ensueFieldName(fieldName);
	}

	public List<IStructure__c> getIStructures() {
		return iStructures;
	}

	public void setIStructures(List<IStructure__c> structures) {
		iStructures = structures;
	}

	/**
	 * Outbound adapter
	 * @return
	 */
	public String getAdapterId__c() {
		return adapterId__c;
	}

	public void setAdapterId__c(String adapterId__c) {
		this.adapterId__c = adapterId__c;
	}

	public String getType__c() {
		return type__c;
	}

	public void setType__c(String type__c) {
		this.type__c = type__c;
	}

	/**
	 * Inbound adapter
	 * @return
	 */
	public String getSource_Adapter__c() {
		return source_Adapter__c;
	}

	public void setSource_Adapter__c(String source_Adapter__c) {
		this.source_Adapter__c = source_Adapter__c;
	}

	public boolean getIsBULKAPI__c() {
		return isBULKAPI__c;
	}
	
	public void setIsBULKAPI__c(boolean isBULKAPI__c) {
		this.isBULKAPI__c = isBULKAPI__c;
	}
	
	public void setIsBULKAPI__c(String isBULKAPI__c) {
		this.isBULKAPI__c = Boolean.parseBoolean(isBULKAPI__c);
	}

	public boolean getBatch_Mode__c() {
		return batch_Mode__c;
	}
	
	public void setBatch_Mode__c(boolean batch_Mode__c) {
		this.batch_Mode__c = batch_Mode__c;
	}
	
	public void setBatch_Mode__c(String batch_Mode__c) {
		this.batch_Mode__c = Boolean.parseBoolean(batch_Mode__c);
	}

	public String getBulk_Package_Size__c() {
		return this.bulk_Package_Size__c;
	}

	public void setBulk_Package_Size__c(String bulk_Package_Size__c) {
		this.bulk_Package_Size__c = bulk_Package_Size__c;
	}

	public boolean getIsStreamingAPI__c() {
		return isStreamingAPI__c;
	}
	
	public void setIsStreamingAPI__c(boolean isStreamingAPI__c) {
		this.isStreamingAPI__c = isStreamingAPI__c;
	}
	
	public void setIsStreamingAPI__c(String isStreamingAPI__c) {
		this.isStreamingAPI__c = Boolean.parseBoolean(isStreamingAPI__c);
	}
	
	public String getIntegrate_Batch_Max_Size__c() {
		return this.integrate_Batch_Max_Size__c;
	}

	public void setIntegrate_Batch_Max_Size__c(String integrate_Batch_Max_Size__c) {
		this.integrate_Batch_Max_Size__c = integrate_Batch_Max_Size__c;
	}

	public String getIntegrate_Max_Size__c() {
		return integrate_Max_Size__c;
	}

	public void setIntegrate_Max_Size__c(String integrate_Max_Size__c) {
		this.integrate_Max_Size__c = integrate_Max_Size__c;
	}
	
	public boolean getUse_Auto_Switch_Mode__c() {
		return this.use_Auto_Switch_Mode__c;
	}
	
	public void setUse_Auto_Switch_Mode__c(boolean use_Auto_Swtich_Mode__c) {
		this.use_Auto_Switch_Mode__c = use_Auto_Swtich_Mode__c;
	}
	
	public void setUse_Auto_Switch_Mode__c(String use_Auto_Swtich_Mode__c) {
		this.use_Auto_Switch_Mode__c = Boolean.parseBoolean(use_Auto_Swtich_Mode__c);
	}

	public String getLast_Data__c() {
		return this.last_Data__c;
	}

	public void setLast_Data__c(String last_Data__c) {
		this.last_Data__c = last_Data__c;
	}
	
	public boolean getInitialization__c() {
		return this.initialization__c;
	}
	
	public void setInitialization__c(boolean initialization__c) {
		this.initialization__c = initialization__c;
	}
	
	public void setInitialization__c(String initialization__c) {
		this.initialization__c = Boolean.parseBoolean(initialization__c);
	}

	public String getInitialization_Query__c() {
		return this.initialization_Query__c;
	}

	public void setInitialization_Query__c(String initialization_Query__c) {
		this.initialization_Query__c = initialization_Query__c;
	}
	
	public String getPackage_Size__c() {
		
		return "null".equalsIgnoreCase(this.Package_Size__c) ? null : Package_Size__c;
	}

	public void setPackage_Size__c(String package_Size__c) throws Exception {
		if(package_Size__c!=null && !package_Size__c.equalsIgnoreCase("null") && !package_Size__c.equalsIgnoreCase("")){
			try{
				Integer a=(int)Double.parseDouble(package_Size__c);
				if (a<=0) throw new Exception("Inteface package size must be positive integer!");
				this.Package_Size__c = ""+a;
			}catch(Exception e){
				throw new Exception("Inteface package size must be positive integer!");
			}
		}
		else {
			this.Package_Size__c = "";
		}
	}

	public String getNumber_of_Records_Per_Batch__c() {
		return "null".equalsIgnoreCase(this.number_of_Records_Per_Batch__c) ? null : number_of_Records_Per_Batch__c;
	}

	public void setNumber_of_Records_Per_Batch__c( String n) throws Exception {
			if(n!=null && !n.equalsIgnoreCase("null") && !n.equalsIgnoreCase("")){
				try{
					Integer a=(int)Double.parseDouble(n);
					if (a<=0) throw new Exception("Inteface Packet must be positive integer!");
					this.number_of_Records_Per_Batch__c = ""+a;
				}catch(Exception e){
					throw new Exception("Inteface Packet must be positive integer!");
				}
			}
			else {
				this.number_of_Records_Per_Batch__c = "";
			}
	}
	
	public boolean isExternalMapping(){
		return isExternal_Mapping__c()||isPI__c();
	}

	public boolean isExternal_Mapping__c() {
		return External_Mapping__c;
	}

	public void setExternal_Mapping__c(boolean external_Mapping__c) {
		External_Mapping__c = external_Mapping__c;
	}

	public boolean isPI__c() {
		return PI__c;
	}

	public void setPI__c(boolean pI__c) {
		PI__c = pI__c;
	}

	

	public String getSObjectFieldExtId__c() {
		return SObjectFieldExtId__c;
	}

	public void setSObjectFieldExtId__c(String sObjectFieldExtId__c) {
		SObjectFieldExtId__c = sObjectFieldExtId__c;
	}

	@Override
	public String toString() {
		return this.getName__c();
	}

	public String getInterface_Type__c() {
		return interface_Type__c;
	}

	public void setInterface_Type__c(String interface_Type__c) {
		this.interface_Type__c = interface_Type__c;
	}

	public List<IStructure__c> getiStructures() {
		return iStructures;
	}

	public void setiStructures(List<IStructure__c> iStructures) {
		this.iStructures = iStructures;
	}

	public List<IChained_Interfaces__c> getParent_IChained_Interfaces() {
		return parent_IChained_Interfaces;
	}

	public void setParent_IChained_Interfaces(
			List<IChained_Interfaces__c> parent_IChained_Interfaces) {
		this.parent_IChained_Interfaces = parent_IChained_Interfaces;
	}

	public void setBULKAPI__c(boolean isBULKAPI__c) {
		this.isBULKAPI__c = isBULKAPI__c;
	}

	public void setStreamingAPI__c(boolean isStreamingAPI__c) {
		this.isStreamingAPI__c = isStreamingAPI__c;
	}

	public String getBulk_Processing_Mode__c() {
		return Bulk_Processing_Mode__c;
	}

	public void setBulk_Processing_Mode__c(String bulk_Processing_Mode__c) {
		Bulk_Processing_Mode__c = bulk_Processing_Mode__c;
	}

	public String getBulk_Version__c() {
		return Bulk_Version__c;
	}

	public void setBulk_Version__c(String bulk_Version__c) {
		Bulk_Version__c = bulk_Version__c;
	}

	public boolean isIsNotPersistMessage__c() {
		return IsNotPersistMessage__c;
	}

	public void setIsNotPersistMessage__c(boolean isNotPersistMessage__c) {
		IsNotPersistMessage__c = isNotPersistMessage__c;
	}

	public String getInterface_Mode__c() {
		return Interface_Mode__c;
	}

	public void setInterface_Mode__c(String interface_Mode__c) {
		Interface_Mode__c = interface_Mode__c;
	}

	public String getMax_Number_Of_Retry__c() {
		if(max_Number_Of_Retry__c!=null) return max_Number_Of_Retry__c.substring(0,max_Number_Of_Retry__c.indexOf("."));
		return max_Number_Of_Retry__c;
	}

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
	
	public String getResponseInterface__c() {
		return responseInterface__c;
	}

	public void setResponseInterface__c(String responseInterface__c) {
		this.responseInterface__c = responseInterface__c;
	}
	
	public boolean isBy_Passing_Message__c() {
		return by_Passing_Message__c;
	}

	public void setBy_Passing_Message__c(boolean by_Passing_Message__c) {
		this.by_Passing_Message__c = by_Passing_Message__c;
	}
	
}
