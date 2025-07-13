package com.iservice.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.iservice.database.SFProcessingJobDao;
import com.iservice.task.AbstractBulkDirectObjectEvent.TBulkVersion;

public class ProcessingJobInfo extends HashMap<String, Object> {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6616015339974462665L;
	
	
	
	public static final char ID_SPLITER = '#';
	public ProcessingJobInfo() {
		super();
		
	}

	public String getJobFolder(){
		if(StringUtils.isNotBlank(getParent_jobid())){
			return getParent_jobid();
		}
		
		return getJobid();
	}

	public ProcessingJobInfo(Map<String,Object> m) {
		super(m);		
	}

	protected String getAsString(String key){
		return (String) get(key);
	}
/**
 * 
 * if contain more than one job then id split by #
 * 
 * @return jobids
 */
	public String getJobid() {
		return getAsString(SFProcessingJobDao.JOBID);
	}
	
	public String[] getJobIds(){
		return StringUtils.split(getJobid(),ID_SPLITER);
	}

	public void setJobid(String jobid) {
		this.put(SFProcessingJobDao.JOBID, jobid);
	}

	public TStatus getStatus() {
		return (TStatus)get(SFProcessingJobDao.STATUS);
	}

	public void setStatus(TStatus status) {
		this.put(SFProcessingJobDao.STATUS, status);
	}

	

	public String getInterfaceId() {
		return getAsString(SFProcessingJobDao.INTERFACE_ID);
	}

	public void setInterfaceId(String interfaceId) {
		this.put(SFProcessingJobDao.INTERFACE_ID, interfaceId);
	}

	public String getIntegrationId() {
		return getAsString(SFProcessingJobDao.INTEGRATION_ID);
	}

	public void setIntegrationId(String integrationId) {
		this.put(SFProcessingJobDao.INTEGRATION_ID,integrationId);
	}

	public TBulkVersion getBulkversion() {
		return (TBulkVersion)get(SFProcessingJobDao.BULKVERSION);
	}

	public void setBulkversion(TBulkVersion bulkversion) {
		this.put(SFProcessingJobDao.BULKVERSION,bulkversion);
	}

	public String getSalesforce_user() {
		return getAsString(SFProcessingJobDao.SALESFORCE_USER);
	}

	public void setSalesforce_user(String salesforce_user) {
		this.put(SFProcessingJobDao.SALESFORCE_USER, salesforce_user);
	}
	
	public Long getAgent_id(){
		return (Long)get(SFProcessingJobDao.AGENT_ID);
	}
	
	public void setAgent_id(Long id){
		this.put(SFProcessingJobDao.AGENT_ID, id);
	}
	
	public String getParent_jobid() {
		return getAsString(SFProcessingJobDao.PARENT_JOBID);
	}


	public void setParent_jobid(String parent_jobid) {
		this.put(SFProcessingJobDao.PARENT_JOBID, parent_jobid);
	}
	public void setExternalFieldId(String externalField_id){
		this.put(SFProcessingJobDao.EXTERNAL_ID,externalField_id);
	}
	
	public String getExternalFieldId(){
		return this.getAsString(SFProcessingJobDao.EXTERNAL_ID);
	}
	
	public TJOBType getJobType() {
		return (TJOBType) get(SFProcessingJobDao.JOB_TYPE);
	}


	public void setJobType(TJOBType jobType) {
		this.put(SFProcessingJobDao.JOB_TYPE,jobType);
	}
	
	public static enum TJOBType{
		SOBJECT,IMESSAGE,IDATA;
	}
	public static enum TStatus{
		ERR,OK,CREATE_IMESSAGE,SEND_MESSAGE,SEND_IDATA,FINISH;
	}
	
}
