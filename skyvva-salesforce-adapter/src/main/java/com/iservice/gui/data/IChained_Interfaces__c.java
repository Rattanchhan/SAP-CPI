package com.iservice.gui.data;


public class IChained_Interfaces__c extends SFIntegrationObject implements
ISFIntegrationObject {
	
	private static final long serialVersionUID = 8133470069409905364L;

	public IChained_Interfaces__c(String pkage){
		super(pkage);
	}

	private String id;
	private String name;
	private String childInterfaceId__c;
	private String init_Operation__c;
	private String interface_Name__c;
	private String parentInterfaceId__c;
	private String parent_Relationship_Name__c	;
	private String sequence__c;
	private String type__c;
	
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

	public String getChildInterfaceId__c() {
		return childInterfaceId__c;
	}

	public void setChildInterfaceId__c(String childInterfaceId__c) {
		this.childInterfaceId__c = childInterfaceId__c;
	}

	public String getInit_Operation__c() {
		return init_Operation__c;
	}

	public void setInit_Operation__c(String init_Operation__c) {
		this.init_Operation__c = init_Operation__c;
	}

	public String getInterface_Name__c() {
		return interface_Name__c;
	}

	public void setInterface_Name__c(String interface_Name__c) {
		this.interface_Name__c = interface_Name__c;
	}

	public String getParentInterfaceId__c() {
		return parentInterfaceId__c;
	}

	public void setParentInterfaceId__c(String parentInterfaceId__c) {
		this.parentInterfaceId__c = parentInterfaceId__c;
	}

	public String getParent_Relationship_Name__c() {
		return parent_Relationship_Name__c;
	}

	public void setParent_Relationship_Name__c(String parent_Relationship_Name__c) {
		this.parent_Relationship_Name__c = parent_Relationship_Name__c;
	}

	public String getSequence__c() {
		return sequence__c;
	}

	public void setSequence__c(String sequence__c) {
		this.sequence__c = sequence__c;
	}

	public String getType__c() {
		return type__c;
	}

	public void setType__c(String type__c) {
		this.type__c = type__c;
	}
	
}
