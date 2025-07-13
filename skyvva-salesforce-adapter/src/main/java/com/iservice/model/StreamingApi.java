package com.iservice.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StreamingApi extends AbstractDataModel {
	private static final String EMPTY = "";
	
	private String username = EMPTY;
	private String integrationid = EMPTY;
	private String integrationname = EMPTY;
	private String interfaceid = EMPTY;
	private String interfacename = EMPTY;
	private String replayFrom = EMPTY;
	private String status = EMPTY;
	private String message = EMPTY;
	
	// reference
	private String createddate = EMPTY;
	private String createdby = EMPTY;
	private String modifieddate = EMPTY;
	private String modifiedby = EMPTY;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getIntegrationid() {
		return integrationid;
	}

	public void setIntegrationid(String integrationid) {
		this.integrationid = integrationid;
	}

	public String getIntegrationname() {
		return integrationname;
	}

	public void setIntegrationname(String integrationname) {
		this.integrationname = integrationname;
	}

	public String getInterfaceid() {
		return interfaceid;
	}

	public void setInterfaceid(String interfaceid) {
		this.interfaceid = interfaceid;
	}

	public String getInterfacename() {
		return interfacename;
	}

	public void setInterfacename(String interfacename) {
		this.interfacename = interfacename;
	}

	public String getReplayFrom() {
		return replayFrom;
	}

	public void setReplayFrom(String replayFrom) {
		this.replayFrom = replayFrom==null?"-1":replayFrom;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getCreateddate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		this.createddate = dateFormat.format(new Date());
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

	public String getModifieddate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		this.modifieddate = dateFormat.format(new Date());
		return modifieddate;
	}

	public void setModifieddate(String modifieddate) {
		this.modifieddate = modifieddate;
	}

	public String getModifiedby() {
		return modifiedby;
	}

	public void setModifiedby(String modifiedby) {
		this.modifiedby = modifiedby;
	}
}
