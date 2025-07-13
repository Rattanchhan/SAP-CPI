package com.iservice.gui.data;

import org.apache.commons.lang3.StringUtils;

public class IMapping__c extends SFIntegrationObject implements
		ISFIntegrationObject {

	private static final long serialVersionUID = -6814002595419937233L;



	public IMapping__c(String pkage){
	
		super(pkage);
	}
	
	
	
	private String id;
	private String name;
	private String interface__c;
	private String type__c;
	private String source_Object__c;
	private String target_Object__c;
	private boolean EXT_ID__c;
	private String inFixToPostFix__c;
	private String target__c;
	private String source__c;
	private String source_Long__c;
	private String source_Type__c;
	private String target_Type__c;
	private String target_Path__c;
	private String reference_Field_Parent_And_Above__c;
	private String reference_Field_Uncle__c;
	private String full_Source_Path__c;
	private String context_Parent_Filter__c;
	private String filters__c;

	public boolean getEXT_ID__c() {
		return EXT_ID__c;
	}

	public void setEXT_ID__c(String eXT_ID__c) {
		EXT_ID__c =Boolean.parseBoolean(eXT_ID__c);
	}
	public void setEXT_ID__c(boolean eXT_ID__c) {
		EXT_ID__c = eXT_ID__c;
	}

	@Override
	protected  String toLowerCase(String str){		
		if(StringUtils.equalsIgnoreCase(str, "EXT_ID__c")){
			return "EXT_ID__c";
		}
		return super.toLowerCase(str);	
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

	public String getInterface__c() {
		return interface__c;
	}

	public void setInterface__c(String interface__c) {
		this.interface__c = interface__c;
	}

	public String getType__c() {
		return type__c;
	}

	public void setType__c(String type__c) {
		this.type__c = type__c;
	}

	public String getSource_Object__c() {
		return source_Object__c;
	}

	public void setSource_Object__c(String source_Object__c) {
		this.source_Object__c = source_Object__c;
	}

	public String getTarget_Object__c() {
		return target_Object__c;
	}

	public void setTarget_Object__c(String target_Object__c) {
		this.target_Object__c = target_Object__c;
	}

	public String getInFixToPostFix__c() {
		return inFixToPostFix__c;
	}

	public void setInFixToPostFix__c(String inFixToPostFix__c) {
		this.inFixToPostFix__c = inFixToPostFix__c;
	}

	public String getTarget__c() {
		return target__c;
	}

	public void setTarget__c(String target__c) {
		this.target__c = target__c;
	}

	public String getSource__c() {
		return source__c;
	}

	public void setSource__c(String source__c) {
		this.source__c = source__c;
	}

	public String getSource_Long__c() {
		return source_Long__c;
	}

	public void setSource_Long__c(String source_Long__c) {
		this.source_Long__c = source_Long__c;
	}

	public String getSource_Type__c() {
		return source_Type__c;
	}

	public void setSource_Type__c(String source_Type__c) {
		this.source_Type__c = source_Type__c;
	}

	public String getTarget_Type__c() {
		return target_Type__c;
	}

	public void setTarget_Type__c(String target_Type__c) {
		this.target_Type__c = target_Type__c;
	}

	public String getTarget_Path__c() {
		return target_Path__c;
	}

	public void setTarget_Path__c(String target_Path__c) {
		this.target_Path__c = target_Path__c;
	}
	public boolean isExternalId(){
		return getEXT_ID__c();
	}
	
	public String getReference_Field_Parent_And_Above__c() {
		return reference_Field_Parent_And_Above__c;
	}

	public void setReference_Field_Parent_And_Above__c(String reference_Field_Parent_And_Above__c) {
		this.reference_Field_Parent_And_Above__c = reference_Field_Parent_And_Above__c;
	}

	public String getReference_Field_Uncle__c() {
		return reference_Field_Uncle__c;
	}

	public void setReference_Field_Uncle__c(String reference_Field_Uncle__c) {
		this.reference_Field_Uncle__c = reference_Field_Uncle__c;
	}
	
	public String getFull_Source_Path__c() {
		return full_Source_Path__c;
	}

	public void setFull_Source_Path__c(String full_Source_Path__c) {
		this.full_Source_Path__c = full_Source_Path__c;
	}

	public String getFilters__c() {
		return filters__c;
	}

	public void setFilters__c(String filters__c) {
		this.filters__c = filters__c;
	}

	public String getContext_Parent_Filter__c() {
		return context_Parent_Filter__c;
	}

	public void setContext_Parent_Filter__c(String context_Parent_Filter__c) {
		this.context_Parent_Filter__c = context_Parent_Filter__c;
	}
	
}
