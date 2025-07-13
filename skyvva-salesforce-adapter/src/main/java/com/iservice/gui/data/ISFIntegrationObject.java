package com.iservice.gui.data;


public interface ISFIntegrationObject {
	
	Object get(String fieldName) throws Exception;
	void put(String fieldName, Object value) throws Exception;
	
	void setType(String objectType);
	String getType();
	
	void setPackage(String packagee);
	String getPackage();
	
	void setId(String id);
	public boolean isSalesForceField(String field);
	//List<ISFIntegrationObject> getChildren(String relationshipName);
	//ISFIntegrationObject getLookup(String relationshipName);
	
	//void setChildren(String relationshipName,List<ISFIntegrationObject> children);
	//void setLookup(String relationshipName, ISFIntegrationObject lookupObject);
	

}
