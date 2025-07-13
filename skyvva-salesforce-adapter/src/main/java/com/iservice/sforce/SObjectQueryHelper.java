package com.iservice.sforce;

import java.util.ArrayList;
import java.util.List;
import com.iservice.database.PropertySettingDao;

public class SObjectQueryHelper {
	
	public static class SObjectField{
		protected boolean custom;
		protected String field;
		public SObjectField(String field){
			this(field, true);
		}
		public SObjectField(String field,boolean custom){
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
	
	public static abstract class FieldVersion{
		protected List<SObjectField> allFields;
		protected boolean error = false;
		public FieldVersion(){
			allFields = new ArrayList<SObjectQueryHelper.SObjectField>();
		}

		protected void addFields(SObjectField[] fields) {
			for(SObjectField f:fields){
				allFields.add(f);
			}
		}
		
		public abstract String generateQuery(String SObjId);
		
		public StringBuffer generateSelectQuery() {
			String myPackage = PropertySettingDao.getInstance().getQueryPackage();
			StringBuffer sb = new StringBuffer("select ");
			boolean first = true;
			for(SObjectField field:allFields){
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
			return sb;
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
		
		public List<SObjectField> getFields(){
			return new ArrayList<SObjectQueryHelper.SObjectField>(allFields);
		}
	}
	
	public static void restartQueryHelper() {
		for(FieldVersion fieldVersion:SObjectQueryHelper.ORDER_INTEGRATION_QUERY_FIELD){
			fieldVersion.setError(false);
		}
	}
	// integration
	public static final List<FieldVersion> ORDER_INTEGRATION_QUERY_FIELD = new ArrayList<FieldVersion>();
	static{
		ORDER_INTEGRATION_QUERY_FIELD.add(new IntegrationField_V244());
		ORDER_INTEGRATION_QUERY_FIELD.add(new IntegrationField_V222());
		ORDER_INTEGRATION_QUERY_FIELD.add(new IntegrationField_OldestVersion());
	}
	
	public static class  IntegrationField_OldestVersion extends FieldVersion{
		protected static final SObjectField[] DEFAULT_FIELDS=new SObjectField[]{
				new SObjectField("Id", false),
				new SObjectField("Name", false),
				new SObjectField("Packet__c"),
				new SObjectField("Clear_After_Processing__c"),
				new SObjectField("Schedule__c")
			};
		
		public IntegrationField_OldestVersion(){
			super();
			addFields(DEFAULT_FIELDS);
			error = false;
		}
		
		public String generateQuery(String intId) {
			String myPackage = PropertySettingDao.getInstance().getQueryPackage();
			StringBuffer sb = generateSelectQuery();
			//adapter
			sb.append(", "+myPackage + "Source__r.id, "
			+ " " + myPackage + "Source__r." + myPackage + "Type__c, "
			//interfaces
			+ " ( select id, "
			+ myPackage + "Name__c, " 
			+ myPackage + "Sequence2__c from "
			+ myPackage + "Interfaces__r )"	
			//end interface
			+ " from " + myPackage + "Integration__c where id='" + intId + "'");
			return sb.toString();
		}
		
		public String generateQueryV2(String intgId, String intfGId) {
			String myPackage = PropertySettingDao.getInstance().getQueryPackage();
			StringBuffer sb = generateSelectQuery();
			//adapter
			sb.append(", "+myPackage + "Source__r.id, "
			+ " " + myPackage + "Source__r." + myPackage + "Type__c, "
			//interfaceGroup
			+ " ( select id, Name, "
			+ myPackage + "Direction_Type__c, " 
			+ myPackage + "PackageSize__c, " 
			+ myPackage + "Type__c, "
			+ myPackage + "Max_Number_Of_API_Retry__c, " 
			+ myPackage + "Retry_Time_Interval__c from "
			+ myPackage + "Interface_Groups__r where Id='"+intfGId+"'), "
			//interfaces
			+ " ( select id from "+ myPackage + "Interfaces__r where "+myPackage+"Interface_Group__c='"+intfGId+"')"	
			//end interface
			+ " from " + myPackage + "Integration__c where id='" + intgId + "'");
			return sb.toString();
		}

	}
	
	public static class  IntegrationField_V222 extends IntegrationField_OldestVersion{
		protected static final SObjectField[] INTEGRATION_FIELDS_V222=new SObjectField[]{
				new SObjectField("Max_Number_Of_Retry__c")
			};
		
		public IntegrationField_V222(){
			super();
			addFields(INTEGRATION_FIELDS_V222);
		}
	}
	
	public static class  IntegrationField_V244 extends IntegrationField_V222{
		protected static final SObjectField[] INTEGRATION_FIELDS_V244=new SObjectField[]{
				new SObjectField("Retry_Time_Interval__c"),
				new SObjectField("Max_Number_Of_API_Retry__c")
			};
		
		public IntegrationField_V244(){
			super();
			addFields(INTEGRATION_FIELDS_V244);
		}
	}
	
	// IMapping__c
		public static final List<FieldVersion> ORDER_IMAPPING_QUERY_FIELD = new ArrayList<FieldVersion>();
		static{
			ORDER_IMAPPING_QUERY_FIELD.add(new IMappingField_V249());
			ORDER_IMAPPING_QUERY_FIELD.add(new IMappingField_V244());
			ORDER_IMAPPING_QUERY_FIELD.add(new IMappingField_V1());
			ORDER_IMAPPING_QUERY_FIELD.add(new IMapping_OldestVersion());
		}
		
		public static class  IMapping_OldestVersion extends FieldVersion{
			protected static final SObjectField[] DEFAULT_FIELDS=new SObjectField[]{
					new SObjectField("Id", false),
					new SObjectField("Name", false),
					new SObjectField("Type__c"),
					new SObjectField("Target_Object__c"),
					new SObjectField("EXT_ID__c"),
					new SObjectField("InFixToPostFix__c"),
					new SObjectField("Target__c"),
					new SObjectField("Source__c"),
					new SObjectField("Source_Long__c"),
					new SObjectField("Source_Type__c"),
					new SObjectField("Target_Type__c"),
					new SObjectField("Interface__c"),
				};
			
			public IMapping_OldestVersion(){
				super();
				addFields(DEFAULT_FIELDS);
				error = false;
			}
			
			public String generateQuery(String intfId) {
				String myPackage = PropertySettingDao.getInstance().getQueryPackage();
				StringBuffer sb = generateSelectQuery();
				//adapter
				sb.append(" from " + myPackage + "IMapping__c where "+myPackage+"Interface__c='" + intfId + "'");
				return sb.toString();
			}

		}
		
		public static class  IMappingField_V1 extends IMapping_OldestVersion{
			protected static final SObjectField[] IMapping_Field_V1=new SObjectField[]{
					new SObjectField("Source_Object__c"),
					new SObjectField("Target_Path__c")
				};
			
			public IMappingField_V1(){
				super();
				addFields(IMapping_Field_V1);
			}
		}
		
		public static class  IMappingField_V244 extends IMappingField_V1{
			protected static final SObjectField[] IMapping_Field_V244=new SObjectField[]{
					new SObjectField("Reference_Field_Parent_And_Above__c"),
					new SObjectField("Reference_Field_Uncle__c"),
					new SObjectField("Full_Source_Path__c")
				};
			
			public IMappingField_V244(){
				super();
				addFields(IMapping_Field_V244);
			}
		}
		
		public static class  IMappingField_V249 extends IMappingField_V244{
			protected static final SObjectField[] IMapping_Field_V249=new SObjectField[]{
					new SObjectField("Context_Parent_Filter__c"),
					new SObjectField("Filters__c")
				};
			
			public IMappingField_V249(){
				super();
				addFields(IMapping_Field_V249);
			}
		}
	
	//other SObject here
}
