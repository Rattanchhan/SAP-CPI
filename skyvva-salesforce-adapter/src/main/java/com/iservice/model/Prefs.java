package com.iservice.model;

public class Prefs extends AbstractDataModel{
	
	public String propertyfile;
	public String key;
	public String value;
	public String username;
	public String sfid;
	public String createdby;
	public String createddate;
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getSfid() {
		return sfid;
	}
	public void setSfid(String sfid) {
		this.sfid = sfid;
	}
	public String getCreateddate() {
		return createddate;
	}
	public void setCreateddate(String createddate) {
		this.createddate = createddate;
	}
	public String getCreatedby() {
		return createdby;
	}
	public void setCreatedby(String createdby) {
		this.createdby = createdby;
	}
	public String getPropertyfile() {
		return propertyfile;
	}
	public void setPropertyfile(String propertyfile) {
		this.propertyfile = propertyfile;
	}

}
