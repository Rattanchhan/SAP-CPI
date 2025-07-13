package com.iservice.bulk.jobinfo;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sforce.async.JobStateEnum;

public class JobV2Info extends HashMap<String,Object> implements IJobInfo {

/**
	 * 
	 */
	private static final long serialVersionUID = -5807753735986042323L;
	
	
	
public JobV2Info() {
	super();
	
}

public JobV2Info(Map<String, Object> m) {
	super(m);
	
}

public String getAsString(String key){
	Object val = super.get(key);
	if(val!=null){
	if(val instanceof String){
		return (String)val;
	}
	return String.valueOf(val);
	}
	return null;
}

public String getApiVersion() {
	return getAsString("apiVersion");
}
public void setApiVersion(String apiVersion) {
	this.put("apiVersion",apiVersion);
}
public String getColumnDelimiter() {
	return getAsString("columnDelimiter");
}
public void setColumnDelimiter(String columnDelimiter) {
	this.put("columnDelimiter",columnDelimiter);
}
public String getConcurrencyMode() {
	return getAsString("concurrencyMode");
}
public void setConcurrencyMode(String concurrencyMode) {
	this.put("concurrencyMode",concurrencyMode);
}
public String getContentType() {
	return getAsString("contentType");
}
public void setContentType(String contentType) {
	this.put("contentType",contentType);
}
public String getContentUrl() {
	return getAsString("contentUrl");
}
public void setContentUrl(String contentUrl) {
	this.put("contentUrl",contentUrl);
}
public String getCreatedById() {
	return getAsString("createdById");
}
public void setCreatedById(String createdById) {
	this.put("createdById",createdById);
}
public String getCreatedDate() {
	return getAsString("createdDate");
}
public void setCreatedDate(String createdDate) {
	this.put("createdDate",createdDate);
}
public String getExternalIdFieldName() {
	return getAsString("externalIdFieldName");
}
public void setExternalIdFieldName(String externalIdFieldName) {
	this.put("externalIdFieldName",externalIdFieldName);
}
@Override
public int getNumberRecordsFailed(){
	try{
		return Integer.parseInt(getAsString("numberRecordsFailed"));
	}catch(NumberFormatException e){
		
	}
	return 0;
}
public int getNumberRecordsProcessed(){
	try{
		return Integer.parseInt(getAsString("numberRecordsProcessed"));
	}catch(NumberFormatException e){
		//nothing to do
	}
	return 0;
}
@Override
public String getId() {
	return getAsString("id");
}
@Override
public void setId(String id) {
	this.put("id",id);
}
public String getJobType() {
	return getAsString("jobType");
}
public void setJobType(String jobType) {
	this.put("jobType",jobType);
}
public String getLineEnding() {
	return getAsString("lineEnding");
}
public void setLineEnding(String lineEnding) {
	this.put("lineEnding",lineEnding);
}
public String getObject() {
	return getAsString("object");
}
public void setObject(String object) {
	this.put("object",object);
}
public String getOperation() {
	return getAsString("operation");
}
public void setOperation(String operation) {
	this.put("operation",operation);
}
public String getState() {
	return getAsString("state");
}
public void setState(String state) {
	this.put("state",state);
}
public String getSystemModstamp() {
	return getAsString("systemModstamp");
}
public void setSystemModstamp(String systemModstamp) {
	this.put("systemModstamp",systemModstamp);
}


@Override
public String getJobStatus() {
	
	return this.getState();
}

@Override
public boolean isClose() {
	
	return StringUtils.equalsIgnoreCase(getJobStatus(),JobStateEnum.UploadComplete.name());
}

@Override
public boolean isFinish() {
	//job complete mean that is is complete
	return StringUtils.equalsIgnoreCase(getJobStatus(),JobStateEnum.JobComplete.name());
}

@Override
public boolean isAbort() {
	
	return StringUtils.equalsIgnoreCase(getJobStatus(),JobStateEnum.Aborted.name());
}

@Override
public boolean isFailed() {
	
	return StringUtils.equalsIgnoreCase(getJobStatus(),JobStateEnum.Failed.name());
}

@Override
public boolean isOpen() {
	
	return StringUtils.equalsIgnoreCase(getJobStatus(),JobStateEnum.Open.name());
}

@Override
public boolean isInprogress() {
	
	return StringUtils.equalsIgnoreCase(getJobStatus(),JobStateEnum.InProgress.name());
}

@Override
public String getSObject() {
	return getObject();
}

}
