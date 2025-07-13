package com.iservice.sforce;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import com.iservice.database.PropertySettingDao;

public class InterfaceQueryHelper {
	public static void restartQueryHelper() {
		for(InterfaceFieldVersion fieldVersion:InterfaceQueryHelper.ORDER_INTERFACE_QUERY_FIELD){
			fieldVersion.setError(false);
		}
	}
	//for interface
	public static final List<InterfaceFieldVersion> ORDER_INTERFACE_QUERY_FIELD = new ArrayList<InterfaceFieldVersion>();
	static{
		ORDER_INTERFACE_QUERY_FIELD.add(new InterfaceField_V244());
		ORDER_INTERFACE_QUERY_FIELD.add(new LatestFieldVersion());
		ORDER_INTERFACE_QUERY_FIELD.add(new BeforeV2_40FieldVersion());
		ORDER_INTERFACE_QUERY_FIELD.add(new BeforeV2_39FieldVersion());
		ORDER_INTERFACE_QUERY_FIELD.add(new BeforeV2_37FieldVersion());
		//oldest version
		ORDER_INTERFACE_QUERY_FIELD.add(new InterfaceFieldVersion());
	}
	
	public static class InterfaceField{
		protected boolean custom;
		protected String field;
		public InterfaceField(String field){
			this(field, true);
		}
		public InterfaceField(String field,boolean custom){
			this.field = field;
			this.custom = custom;
		}
		public boolean isCustom() {
			return custom;
		}
		public void setCustom(boolean custom) {
			this.custom = custom;
		}
		public String getField() {
			return field;
		}
		public void setField(String field) {
			this.field = field;
		}
	}
	
	public static class InterfaceFieldVersion{
		protected static final InterfaceField[] DEFAULT_FIELDS=new InterfaceField[]{
			new InterfaceField("id",false),
			new InterfaceField("Initialization__c"),
			new InterfaceField("Initialization_Query__c"),
			new InterfaceField("Query__c"),
			new InterfaceField("Name__c"),
			new InterfaceField("OperationType__c"),
			new InterfaceField("Type__c"),
			new InterfaceField("Sequence2__c"),
			new InterfaceField("Source_Name__c"),
			new InterfaceField("Status__c"),
			new InterfaceField("AdapterId__c"),
			new InterfaceField("Source_Adapter__c"),
			new InterfaceField("Number_of_Records_Per_Batch__c"),
			new InterfaceField("IStructure_Repository__c"),
			new InterfaceField("Interface_Mode__c"),
			////integration__c is mandatory field
			new InterfaceField("Integration__c")
		};
		
		protected List<InterfaceField> allFields;
		protected boolean error = false;
		public InterfaceFieldVersion(){
			allFields = new ArrayList<InterfaceQueryHelper.InterfaceField>();
			addFields(DEFAULT_FIELDS);
			
		}

		protected void addFields(InterfaceField[] fields) {
			for(InterfaceField f:fields){
				allFields.add(f);
			}
		}
		
		protected boolean isOldestVersion(){
			return true;
		}
		
		public boolean isError() {
			return error;
		}

		public void setError(boolean error) {
			this.error = error;
		}

		public  String generateQuery( String[] ids, boolean isSub) {
			String myPackage = PropertySettingDao.getInstance().getQueryPackage();
			StringBuffer sb = new StringBuffer("select ");
			boolean first = true;
			for(InterfaceField field:allFields){
				if(!first){
					sb.append(", ");
				}
				if(field.isCustom()){
					//add package to custom field only
					sb.append(myPackage);
				}
				sb.append(field.getField());
				first = false;
			}
			sb.append(",");
			sb.append("(Select id, Name, " 
					+ myPackage	+ "Type__c, "
					+ myPackage	+ "Sequence__c From " + myPackage + "IStructure__r)"
					+ " from " + myPackage + "Interfaces__c where id in ('" + StringUtils.join(ids,"','") + "')");
			if(!isOldestVersion()){
				sb.append(" and "+ myPackage +"interface_Type__c"+ (isSub?" ":" !") +"='Sub-Interface'");
			}
			sb.append(" order by " + myPackage + "Sequence2__c");
			return sb.toString();
		}
		
		public  String generateQuery( String[] ids, String id) {
			String myPackage = PropertySettingDao.getInstance().getQueryPackage();
			StringBuffer sb = new StringBuffer("select ");
			boolean first = true;
			for(InterfaceField field:allFields){
				if(!first){
					sb.append(", ");
				}
				if(field.isCustom()){
					//add package to custom field only
					sb.append(myPackage);
				}
				sb.append(field.getField());
				first = false;
			}
			sb.append(",");
			sb.append("(Select id, Name, " 
					+ myPackage	+ "Type__c, "
					+ myPackage	+ "Sequence__c From " + myPackage + "IStructure__r)"
					+ " from " + myPackage + "Interfaces__c where "
					+ myPackage +"MessageType__c in ('" + StringUtils.join(ids,"','") + "')"
					+ " And " + myPackage + "Integration__c ='" + id + "'");
			return sb.toString();
		}
		
		public  String generateQuery_SortByInterfaceGroup( String[] ids, boolean isSub) {
			String myPackage = PropertySettingDao.getInstance().getQueryPackage();
			StringBuffer sb = new StringBuffer("select ");
			boolean first = true;
			for(InterfaceField field:allFields){
				if(!first){
					sb.append(", ");
				}
				if(field.isCustom()){
					//add package to custom field only
					sb.append(myPackage);
				}
				sb.append(field.getField());
				first = false;
			}
			sb.append(",");
			sb.append("(Select id, Name, " 
					+ myPackage	+ "Type__c, "
					+ myPackage	+ "Sequence__c From " + myPackage + "IStructure__r)"
					+ " from " + myPackage + "Interfaces__c where id in ('" + StringUtils.join(ids,"','") + "')");
			if(!isOldestVersion()){
				sb.append(" and "+ myPackage +"interface_Type__c"+ (isSub?" ":" !") +"='Sub-Interface'");
			}
			sb.append(" order by " + myPackage + "InterfaceGroupSequence__c");
			return sb.toString();
		}
		
		public List<InterfaceField> getFields(){
			return new ArrayList<InterfaceQueryHelper.InterfaceField>(allFields);
		}
	}
	
	public static class  BeforeV2_37FieldVersion extends InterfaceFieldVersion{
		private static final InterfaceField[] NEW_FIELD_BEFORE_V2_37=new InterfaceField[]{
			
			// #2172 Enhance Agent to use integrationBulk SKYVVA Salesforce v2.31
			new InterfaceField("isBULKAPI__c"),
			new InterfaceField("Batch_Mode__c"),
			new InterfaceField("isStreamingAPI__c"),
			new InterfaceField("Bulk_Package_Size__c"),
			new InterfaceField("interface_Type__c"),
			// SKYVVA Salesforce v2.32
			new InterfaceField("Integrate_Batch_Max_Size__c"),
			new InterfaceField("Integrate_Max_Size__c"),
			new InterfaceField("MessageType__c"),
			new InterfaceField("Max_Number_Of_Retry__c"),
			new InterfaceField("ResponseInterface__c")
		};
		public BeforeV2_37FieldVersion(){
			super();
			addFields(NEW_FIELD_BEFORE_V2_37);
		}
		@Override
		protected boolean isOldestVersion(){
			return false;
		}
		
	}
	
	public static class BeforeV2_39FieldVersion extends BeforeV2_37FieldVersion{
		private static final InterfaceField[] NEW_FIELD_BEFORE_V2_39=new InterfaceField[]{
			
			// #2172 Enhance Agent to use integrationBulk SKYVVA Salesforce v2.37
			new InterfaceField("Use_Auto_Switch_Mode__c"),
			//PI is the external field id
			new InterfaceField("PI__c"),		
			new InterfaceField("ExtId__c"),
			new InterfaceField("SObjectFieldExtId__c"),
			new InterfaceField("IsNotPersistMessage__c"),
			new InterfaceField("CreateStatisticalData__c")
			
		};
		public BeforeV2_39FieldVersion(){
			super();
			addFields(NEW_FIELD_BEFORE_V2_39);
		}
	}
	
	public static class BeforeV2_40FieldVersion extends BeforeV2_39FieldVersion{
		private static final InterfaceField[] NEW_FIELD_BEFORE_V2_40=new InterfaceField[]{
				
			// replay for 2.40
			new InterfaceField("Replay_Option__c"),
			new InterfaceField("Bulk_Processing_Mode__c"),
			new InterfaceField("Bulk_Version__c")
		};
		public BeforeV2_40FieldVersion(){
			super();
			addFields(NEW_FIELD_BEFORE_V2_40);
		}
	}
	
	public static class LatestFieldVersion extends BeforeV2_40FieldVersion{
		private static final InterfaceField[] LATEST_FIELDS=new InterfaceField[]{
			
			// #3409 2.40
			new InterfaceField("Package_Size__c"),
			new InterfaceField("Operation_Deleted__c")
		};
		public LatestFieldVersion(){
			super();
			addFields(LATEST_FIELDS);
		}
	}
	
	public static class InterfaceField_V244 extends LatestFieldVersion{
		private static final InterfaceField[] INTERFACE_FIELDS_V244=new InterfaceField[]{
			new InterfaceField("Retry_Time_Interval__c"),
			new InterfaceField("Max_Number_Of_API_Retry__c"),
			new InterfaceField("By_Passing_Message__c")
		};
		public InterfaceField_V244(){
			super();
			addFields(INTERFACE_FIELDS_V244);
		}
	}
}
