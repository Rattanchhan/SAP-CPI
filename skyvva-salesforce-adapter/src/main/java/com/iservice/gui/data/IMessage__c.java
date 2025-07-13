package com.iservice.gui.data;


public class IMessage__c extends SFIntegrationObject implements ISFIntegrationObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public IMessage__c(String pkage) {
		super(pkage);
	}
	
	private String id;
	private String name;
	private String status__c;
	private String interface__c;
	private String integration__c;
	private String comment__c;
	private String createdDate;
	private String lastModifiedDate;
	private String related_To__c;
	private String extValue__c;
	private String Type__c;
	
	// V3 some field
	private String TransactionId__c;
	private String HasChild__c;
	private String hlevel__c;  //should be declare in lower-case later
	private String hpath__c;
	private String isRoot__c;
	private String TransferId__c;
	private String ProcessMode__c;
	private String Data__c;
	private String Creation_Date__c;
	private String Modification_Date__c;
	private String External_Id2__c;
	private String ParentMessage__c;
	private String RootMessage__c;
	
	public String getType__c() {
		return Type__c;
	}

	public void setType__c(String type__c) {
		Type__c = type__c;
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

	public String getStatus__c() {
		return status__c;
	}

	public void setStatus__c(String status__c) {
		this.status__c = status__c;
	}

	public String getInterface__c() {
		return interface__c;
	}

	public void setInterface__c(String interface__c) {
		this.interface__c = interface__c;
	}

	public String getIntegration__c() {
		return integration__c;
	}

	public void setIntegration__c(String integration__c) {
		this.integration__c = integration__c;
	}

	public String getComment__c() {
		return comment__c;
	}

	public void setComment__c(String comment__c) {
		this.comment__c = comment__c;
	}

	public String getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(String createdDate) {
		this.createdDate = createdDate;
	}

	public String getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(String lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

	public String getRelated_To__c() {
		return related_To__c;
	}

	public void setRelated_To__c(String related_To__c) {
		this.related_To__c = related_To__c;
	}

	public String getExtValue__c() {
		return extValue__c;
	}

	public void setExtValue__c(String extValue__c) {
		this.extValue__c = extValue__c;
	}

	public String getTransactionId__c() {
		return TransactionId__c;
	}

	public void setTransactionId__c(String transactionId__c) {
		TransactionId__c = transactionId__c;
	}

	public String getHasChild__c() {
		return HasChild__c;
	}

	public void setHasChild__c(String hasChild__c) {
		HasChild__c = hasChild__c;
	}

	public String getHlevel__c() { //do not declare getHLevel__c it cannot compare when convert it to Salesforce Object
		return hlevel__c;
	}

	public void setHlevel__c(String hlevel__c) {
		this.hlevel__c = hlevel__c;
	}

	public String getHpath__c() {
		return this.hpath__c;
	}

	public void setHpath__c(String hpath__c) {
		this.hpath__c = hpath__c;
	}

	public String getIsRoot__c() {
		return isRoot__c;
	}

	public void setIsRoot__c(String isRoot__c) {
		this.isRoot__c = isRoot__c;
	}

	public String getTransferId__c() {
		return TransferId__c;
	}

	public void setTransferId__c(String transferId__c) {
		TransferId__c = transferId__c;
	}

	public String getProcessMode__c() {
		return ProcessMode__c;
	}

	public void setProcessMode__c(String processMode__c) {
		ProcessMode__c = processMode__c;
	}

	public String getData__c() {
		return Data__c;
	}

	public void setData__c(String data__c) {
		Data__c = data__c;
	}

	public String getCreation_Date__c() {
		return Creation_Date__c;
	}

	public void setCreation_Date__c(String creation_Date__c) {
		Creation_Date__c = creation_Date__c;
	}

	public String getModification_Date__c() {
		return Modification_Date__c;
	}

	public void setModification_Date__c(String modification_Date__c) {
		Modification_Date__c = modification_Date__c;
	}

	public String getExternal_Id2__c() {
		return External_Id2__c;
	}

	public void setExternal_Id2__c(String external_Id2__c) {
		External_Id2__c = external_Id2__c;
	}

	public String getParentMessage__c() {
		return ParentMessage__c;
	}

	public void setParentMessage__c(String parentMessage__c) {
		ParentMessage__c = parentMessage__c;
	}

	public String getRootMessage__c() {
		return RootMessage__c;
	}

	public void setRootMessage__c(String rootMessage__c) {
		RootMessage__c = rootMessage__c;
	}

}
