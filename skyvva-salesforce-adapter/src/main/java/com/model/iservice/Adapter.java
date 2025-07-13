package com.model.iservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iservice.gui.data.Adapter__c;
import com.iservice.gui.data.ISFIntegrationObject;
import com.iservice.gui.data.Property__c;
import com.iservice.gui.helper.PropertyNameHelper;
import com.model.iservice.base.PersistentDTO;

public class Adapter extends PersistentDTO {

	public Adapter() {
		
	}
	public Adapter(Adapter__c adapter__c) { 
		
		for(ISFIntegrationObject obj : adapter__c.getProperties()){
			Property__c p = (Property__c) obj;
			Property prop = new Property(p.getName__c(), p.getValue2__c());
			property.add(prop);
			//for XML Adapter, cos XMLAdapter.login() doesn't call setConnectionInfo method
			mapConInfo.put(p.getName__c(), p.getValue2__c());
		}
		this.connType = adapter__c.getType__c();
	}
	private Map <String, String> mapConInfo = new HashMap<String, String>();
	private String name;

	private String description;

	private String connType;

	protected List<Property> property = new ArrayList<Property>(0);

	protected List<Source> source = new ArrayList<Source>(0);

	protected List<Target> target = new ArrayList<Target>(0);

	public List<Source> getSource() {
		return source;
	}

	public Map<String, String> getMapConInfo() {
		return mapConInfo;
	}
	public void setSource(List<Source> source) {
		this.source = source;
	}

	public List<Target> getTarget() {
		return target;
	}

	public void setTarget(List<Target> target) {
		this.target = target;
	}

	public List<Property> getProperty() {
		return property;
	}

	public void setProperty(List<Property> property) {
		this.property = property;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void removeProperty(Property prop) {
		if (!property.isEmpty()) {
			property.remove(prop);
		}

	}

	public String getConnType() {
		return connType;
	}

	public void setConnType(String connType) {
		this.connType = connType;
	}

	public String toString() {
		return name;
	}

	public boolean isAttOrContentVersionMode() {
		return Boolean.parseBoolean(getPropertyValue(PropertyNameHelper.IS_BINARY_FILE));
		
	}

	public String getFileName() {

		return getPropertyValue(PropertyNameHelper.FILE_NAME);
	}

	public void addProperty(String name, String value) {
		this.addProperty(new Property(name, value));
	}

	public void addProperty(Property p) {
		this.property.add(p);
	}
	
	public String getPropertyValue(String propName){
		for(Property p : this.getProperty()){
			if(org.apache.commons.lang3.StringUtils.equals(propName, p.getName())){
				return p.getValue();
			}
		}
		
		return null;
	}
	
	public boolean isModFile(){
		String val = getPropertyValue(PropertyNameHelper.FILE_UPLOAD_MODE);
		return org.apache.commons.lang3.StringUtils.equals(PropertyNameHelper.MODE_FILE, val);
	}
	
	public boolean isMode_RawContent(){
		String val = getPropertyValue(PropertyNameHelper.FILE_UPLOAD_MODE);
		return org.apache.commons.lang3.StringUtils.equals(PropertyNameHelper.MODE_RAW_CONTENT, val);
	}
	
}
