package com.iservice.gui.data;

import com.iservice.database.PropertySettingDao;

public class MsgTypeFieldEntry__c extends SFIntegrationObject implements ISFIntegrationObject {

	private static final long serialVersionUID = 6397914702070997142L;

	public MsgTypeFieldEntry__c(String pkage){
		super(pkage);
	}
	
	
	
	public String getPackage(){
		String myPackage = PropertySettingDao.getInstance().getQueryPackage();
//				SFIntegrationService.THE_PACKAGE.equals("")?"" 
//				: SFIntegrationService.THE_PACKAGE + "__";
		return myPackage;
	}
	protected String myPackage = getPackage();
	
	private String id;
	private String name;
	private String sequence__c;

	private IStructure__c IStructure__c;	//lookup field
	
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

	public String getSequence__c() {
		return sequence__c;
	}

	public void setSequence__c(String sequence__c) {
		this.sequence__c = sequence__c;
	}

	public IStructure__c getIStructure__c() {
		return IStructure__c;
	}

	public void setIStructure__c(IStructure__c iStructure__c) {
		this.IStructure__c = iStructure__c;
	}

	@Override
	protected String ensueFieldName(String fieldName){
		if(fieldName.contains(myPackage+"IstructureField__r")){
			return "IStructure__c";
			
		}
		return super.ensueFieldName(fieldName);
	}

//	@Override
//	public ISFIntegrationObject getLookup(String relationshipName) {
//		if(relationshipName.contains(myPackage+"IstructureField__r")){
//			return IStructure__c;
//		}
//		return null;
//	}
//
//	@Override
//	public void setLookup(String relationshipName, ISFIntegrationObject lookupObject) {
//		if(relationshipName.contains(myPackage+"IstructureField__r")){
//			IStructure__c = lookupObject;
//		}
//	}
}
